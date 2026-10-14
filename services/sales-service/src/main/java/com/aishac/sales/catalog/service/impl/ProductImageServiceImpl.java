package com.aishac.sales.catalog.service.impl;

import com.aishac.sales.catalog.config.StorageProperties;
import com.aishac.sales.catalog.dto.ProductImageResponse;
import com.aishac.sales.catalog.dto.UpdateProductImageRequest;
import com.aishac.sales.catalog.entity.Product;
import com.aishac.sales.catalog.entity.ProductImage;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.mapper.CatalogMapper;
import com.aishac.sales.catalog.repository.ProductImageRepository;
import com.aishac.sales.catalog.repository.ProductRepository;
import com.aishac.sales.catalog.service.CatalogRules;
import com.aishac.sales.catalog.service.ImageType;
import com.aishac.sales.catalog.service.ProductImageService;
import com.aishac.sales.catalog.service.ProductImageStorage;
import com.aishac.sales.exception.BusinessException;
import com.aishac.sales.exception.NotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Imagens de produto (rule storage). O S3 fica fora da transação: upload e substituição gravam o arquivo antes e só
 * então abrem uma transação curta, com o produto travado ({@code SELECT ... FOR UPDATE}), para limite, posição e
 * principal; se o banco falhar, o objeto novo é removido (compensação, RN-21).
 */
@Slf4j
@Service
@RequiredArgsConstructor
class ProductImageServiceImpl implements ProductImageService {

    private static final int MAX_POSITION = 8;

    /** Órfãos são limitação aceita da fase 01 (ADR-010, IF-026): a métrica mostra o volume para observar. */
    static final String ORPHANED_OBJECTS = "aishac.storage.orphaned.objects";

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductImageStorage storage;
    private final StorageProperties properties;
    private final CatalogMapper catalogMapper;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    @Override
    public ProductImageResponse upload(UUID productId, MultipartFile file, String altText) {
        // Falha rápida antes de gastar um upload; as mesmas regras são conferidas de novo com o produto travado.
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, productId);
        }
        if (productImageRepository.countByProductId(productId) >= properties.maxImagesPerProduct()) {
            throw new BusinessException(CatalogErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
        ImageFile image = read(file);
        String objectKey = CatalogRules.objectKey(productId, image.type());
        storage.put(objectKey, image.content(), image.type().contentType());
        ProductImage saved;
        try {
            saved = transactionTemplate.execute(status -> insert(productId, objectKey, image, altText.strip()));
        } catch (RuntimeException e) {
            deleteQuietly(objectKey);
            throw e;
        }
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> list(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, productId);
        }
        return productImageRepository.findByProductIdOrderByPositionAsc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductImageResponse findById(UUID productId, UUID imageId) {
        return productImageRepository
                .findByIdAndProductId(imageId, productId)
                .map(this::toResponse)
                .orElseThrow(() -> notFound(productId, imageId));
    }

    /** As constraints de posição e principal são adiáveis: a troca passa por estados intermediários sem violar. */
    @Override
    @Transactional
    public ProductImageResponse update(UUID productId, UUID imageId, UpdateProductImageRequest request) {
        List<ProductImage> images = lockImages(productId);
        ProductImage target = find(images, productId, imageId);
        if (request.altText() != null) {
            target.setAltText(request.altText());
        }
        if (request.position() != null && request.position() != target.getPosition()) {
            int previous = target.getPosition();
            images.stream()
                    .filter(image -> image.getPosition() == request.position())
                    .findFirst()
                    .ifPresent(occupant -> occupant.setPosition(previous));
            target.setPosition(request.position());
        }
        if (Boolean.TRUE.equals(request.primary())) {
            images.forEach(image -> image.setPrimaryImage(image == target));
        }
        productImageRepository.flush();
        return toResponse(target);
    }

    @Override
    public ProductImageResponse replaceFile(UUID productId, UUID imageId, MultipartFile file) {
        if (productImageRepository.findByIdAndProductId(imageId, productId).isEmpty()) {
            throw notFound(productId, imageId);
        }
        ImageFile image = read(file);
        String objectKey = CatalogRules.objectKey(productId, image.type());
        storage.put(objectKey, image.content(), image.type().contentType());
        Replacement replacement;
        try {
            replacement = transactionTemplate.execute(status -> {
                ProductImage target = find(lockImages(productId), productId, imageId);
                String previousKey = target.getObjectKey();
                target.setObjectKey(objectKey);
                target.setContentType(image.type().contentType());
                target.setSizeBytes(image.content().length);
                productImageRepository.flush();
                return new Replacement(target, previousKey);
            });
        } catch (RuntimeException e) {
            deleteQuietly(objectKey);
            throw e;
        }
        deleteQuietly(replacement.previousKey());
        return toResponse(replacement.image());
    }

    @Override
    public void delete(UUID productId, UUID imageId) {
        String objectKey = transactionTemplate.execute(status -> {
            List<ProductImage> images = lockImages(productId);
            ProductImage target = find(images, productId, imageId);
            productImageRepository.delete(target);
            if (target.isPrimaryImage()) {
                images.stream()
                        .filter(image -> image != target)
                        .findFirst()
                        .ifPresent(next -> next.setPrimaryImage(true));
            }
            productImageRepository.flush();
            return target.getObjectKey();
        });
        deleteQuietly(objectKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ProductImageResponse> findPrimaryImages(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductImage> images = productImageRepository.findByProductIdInAndPrimaryImageTrue(productIds);
        try {
            return images.stream()
                    .collect(Collectors.toMap(image -> image.getProduct().getId(), this::toResponse, (a, b) -> a));
        } catch (BusinessException e) {
            log.warn("Primary images omitted: {}", e.getErrorCode().code());
            return Map.of();
        }
    }

    private ProductImage insert(UUID productId, String objectKey, ImageFile file, String altText) {
        List<ProductImage> images = lockImages(productId);
        if (images.size() >= properties.maxImagesPerProduct()) {
            throw new BusinessException(CatalogErrorCode.IMAGE_LIMIT_EXCEEDED);
        }
        return productImageRepository.saveAndFlush(ProductImage.builder()
                .product(productRepository.getReferenceById(productId))
                .objectKey(objectKey)
                .contentType(file.type().contentType())
                .sizeBytes(file.content().length)
                .altText(altText)
                .position(firstFreePosition(images))
                .primaryImage(images.stream().noneMatch(ProductImage::isPrimaryImage))
                .build());
    }

    /** Trava o produto (operações de imagem dele ficam em fila) e devolve as imagens pela posição. */
    private List<ProductImage> lockImages(UUID productId) {
        Product product = productRepository
                .findForImageChange(productId)
                .orElseThrow(() -> new NotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, productId));
        return productImageRepository.findByProductIdOrderByPositionAsc(product.getId());
    }

    private ProductImage find(List<ProductImage> images, UUID productId, UUID imageId) {
        return images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(CatalogErrorCode.IMAGE_NOT_FOUND, imageId));
    }

    private RuntimeException notFound(UUID productId, UUID imageId) {
        return productRepository.existsById(productId)
                ? new NotFoundException(CatalogErrorCode.IMAGE_NOT_FOUND, imageId)
                : new NotFoundException(CatalogErrorCode.PRODUCT_NOT_FOUND, productId);
    }

    private static int firstFreePosition(List<ProductImage> images) {
        Map<Integer, ProductImage> taken =
                images.stream().collect(Collectors.toMap(ProductImage::getPosition, Function.identity()));
        return IntStream.rangeClosed(1, MAX_POSITION)
                .filter(position -> !taken.containsKey(position))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CatalogErrorCode.IMAGE_LIMIT_EXCEEDED));
    }

    /** RN-16: tamanho pelo limite configurado e tipo pelo conteúdo, nunca pelo nome ou {@code Content-Type}. */
    private ImageFile read(MultipartFile file) {
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new BusinessException(CatalogErrorCode.IMAGE_TOO_LARGE);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ImageType type =
                ImageType.detect(content).orElseThrow(() -> new BusinessException(CatalogErrorCode.INVALID_IMAGE_TYPE));
        return new ImageFile(content, type);
    }

    private ProductImageResponse toResponse(ProductImage image) {
        ProductImageStorage.PresignedUrl presigned = storage.presign(image.getObjectKey());
        return catalogMapper.toResponse(image, presigned.url(), presigned.expiresAt());
    }

    /** Falha aqui deixa um objeto órfão (RN-20): fica no log de aviso e na métrica, sem limpeza automática. */
    private void deleteQuietly(String objectKey) {
        try {
            storage.delete(objectKey);
        } catch (RuntimeException e) {
            log.warn("Image object left orphaned in storage: {}", objectKey);
            meterRegistry.counter(ORPHANED_OBJECTS).increment();
        }
    }

    private record ImageFile(byte[] content, ImageType type) {}

    private record Replacement(ProductImage image, String previousKey) {}
}
