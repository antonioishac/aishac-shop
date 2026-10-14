package com.aishac.sales.catalog.service.impl;

import static com.aishac.sales.catalog.CatalogFixtures.PRODUCT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.aishac.sales.catalog.CatalogFixtures;
import com.aishac.sales.catalog.InMemoryProductImageStorage;
import com.aishac.sales.catalog.TestImages;
import com.aishac.sales.catalog.config.StorageProperties;
import com.aishac.sales.catalog.dto.ProductImageResponse;
import com.aishac.sales.catalog.dto.UpdateProductImageRequest;
import com.aishac.sales.catalog.entity.Product;
import com.aishac.sales.catalog.entity.ProductImage;
import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.mapper.CatalogMapper;
import com.aishac.sales.catalog.repository.ProductImageRepository;
import com.aishac.sales.catalog.repository.ProductRepository;
import com.aishac.sales.catalog.service.ProductImageStorage;
import com.aishac.sales.exception.BusinessException;
import com.aishac.sales.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.unit.DataSize;

/** Fluxos de imagem com o armazenamento em memória e o banco simulado: compensação, ordem e degradação. */
class ProductImageServiceImplTest {

    private static final StorageProperties PROPERTIES = new StorageProperties(
            "bucket", "us-east-1", null, null, true, Duration.ofMinutes(15), DataSize.ofKilobytes(10), 8);

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductImageRepository imageRepository = mock(ProductImageRepository.class);
    private final InMemoryProductImageStorage storage = new InMemoryProductImageStorage();
    private final Product product = CatalogFixtures.product(CatalogFixtures.category(true), 1);
    private final List<ProductImage> images = new ArrayList<>();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ProductImageServiceImpl service(ProductImageStorage storage) {
        return new ProductImageServiceImpl(
                productRepository,
                imageRepository,
                storage,
                PROPERTIES,
                Mappers.getMapper(CatalogMapper.class),
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                meterRegistry);
    }

    private ProductImageServiceImpl storedProduct() {
        given(productRepository.existsById(PRODUCT_ID)).willReturn(true);
        given(productRepository.findForImageChange(PRODUCT_ID)).willReturn(Optional.of(product));
        given(productRepository.getReferenceById(PRODUCT_ID)).willReturn(product);
        given(imageRepository.findByProductIdOrderByPositionAsc(PRODUCT_ID)).willReturn(images);
        given(imageRepository.countByProductId(PRODUCT_ID)).willAnswer(invocation -> (long) images.size());
        given(imageRepository.saveAndFlush(any())).willAnswer(invocation -> {
            ProductImage image = invocation.getArgument(0);
            image.setId(UUID.randomUUID());
            return image;
        });
        return service(storage);
    }

    private static MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "foto.jpg", "image/jpeg", content);
    }

    private static void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).extracting("errorCode").isEqualTo(code);
    }

    // CA-16, RN-17
    @Test
    void shouldStoreFileAndMakeFirstImagePrimaryAtFirstFreePosition() {
        // Given
        ProductImageServiceImpl service = storedProduct();
        images.add(CatalogFixtures.image(product, 1, false));

        // When
        ProductImageResponse response = service.upload(PRODUCT_ID, file(TestImages.png(64)), " Vista lateral ");

        // Then
        assertThat(response.primary()).isTrue();
        assertThat(response.position()).isEqualTo(2);
        assertThat(response.altText()).isEqualTo("Vista lateral");
        assertThat(response.contentType()).isEqualTo("image/png");
        ArgumentCaptor<ProductImage> saved = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).saveAndFlush(saved.capture());
        assertThat(storage.content(saved.getValue().getObjectKey())).hasSize(64);
    }

    // CA-17, CA-18: nada chega ao S3 nem ao banco
    @Test
    void shouldValidateFileBeforeUploading() {
        ProductImageServiceImpl service = storedProduct();

        assertError(
                () -> service.upload(PRODUCT_ID, file(TestImages.text()), "Texto"),
                CatalogErrorCode.INVALID_IMAGE_TYPE);
        assertError(
                () -> service.upload(PRODUCT_ID, file(TestImages.jpeg(11 * 1024)), "Grande"),
                CatalogErrorCode.IMAGE_TOO_LARGE);
        verify(imageRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectNinthImageAndUnknownProductBeforeUploading() {
        ProductImageStorage failing = mock(ProductImageStorage.class);
        storedProduct();
        for (int position = 1; position <= 8; position++) {
            images.add(CatalogFixtures.image(product, position, position == 1));
        }

        assertError(
                () -> service(failing).upload(PRODUCT_ID, file(TestImages.jpeg(64)), "Nona"),
                CatalogErrorCode.IMAGE_LIMIT_EXCEEDED);
        UUID unknown = UUID.randomUUID();
        assertError(
                () -> service(failing).upload(unknown, file(TestImages.jpeg(64)), "Sem produto"),
                CatalogErrorCode.PRODUCT_NOT_FOUND);
        verify(failing, never()).put(any(), any(), any());
    }

    // CA-22, RN-21
    @Test
    void shouldRemoveUploadedObjectWhenDatabaseFails() {
        // Given
        ProductImageServiceImpl service = storedProduct();
        willThrow(new DataAccessResourceFailureException("db down"))
                .given(imageRepository)
                .saveAndFlush(any());
        ProductImageStorage spy = spy(storage);

        // When
        assertThatThrownBy(() -> service(spy).upload(PRODUCT_ID, file(TestImages.jpeg(64)), "Falha"))
                .isInstanceOf(DataAccessResourceFailureException.class);

        // Then
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(spy).put(key.capture(), any(), any());
        verify(spy).delete(key.getValue());
        assertThat(storage.contains(key.getValue())).isFalse();
    }

    // CA-23, RN-22
    @Test
    void shouldNotTouchDatabaseWhenStorageIsDown() {
        ProductImageServiceImpl service = storedProduct();
        storage.setAvailable(false);

        assertError(
                () -> service.upload(PRODUCT_ID, file(TestImages.jpeg(64)), "Fora"),
                CatalogErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        verify(productRepository, never()).findForImageChange(any());
    }

    // CA-19, RN-19
    @Test
    void shouldReplaceFileKeepingMetadataAndRemovingOldObject() {
        // Given
        ProductImageServiceImpl service = storedProduct();
        ProductImage image = CatalogFixtures.image(product, 1, true);
        images.add(image);
        String oldKey = image.getObjectKey();
        storage.put(oldKey, TestImages.jpeg(64), "image/jpeg");
        given(imageRepository.findByIdAndProductId(image.getId(), PRODUCT_ID)).willReturn(Optional.of(image));

        // When
        ProductImageResponse response = service.replaceFile(PRODUCT_ID, image.getId(), file(TestImages.webp(32)));

        // Then
        assertThat(response.id()).isEqualTo(image.getId());
        assertThat(response.primary()).isTrue();
        assertThat(response.contentType()).isEqualTo("image/webp");
        assertThat(image.getObjectKey()).isNotEqualTo(oldKey).endsWith(".webp");
        assertThat(storage.contains(image.getObjectKey())).isTrue();
        assertThat(storage.contains(oldKey)).isFalse();
    }

    // CA-20
    @Test
    void shouldSwapPositionAndMovePrimary() {
        // Given
        ProductImageServiceImpl service = storedProduct();
        ProductImage a = CatalogFixtures.image(product, 1, true);
        ProductImage b = CatalogFixtures.image(product, 2, false);
        images.addAll(List.of(a, b));

        // When
        service.update(PRODUCT_ID, b.getId(), new UpdateProductImageRequest("B", 1, true));

        // Then
        assertThat(b.getPosition()).isEqualTo(1);
        assertThat(a.getPosition()).isEqualTo(2);
        assertThat(b.isPrimaryImage()).isTrue();
        assertThat(a.isPrimaryImage()).isFalse();
        assertThat(b.getAltText()).isEqualTo("B");
    }

    // CA-21, RN-20
    @Test
    void shouldPromoteNextImageAndKeepDeletionWhenStorageFails() {
        // Given
        ProductImageStorage failing = mock(ProductImageStorage.class);
        doThrow(new BusinessException(CatalogErrorCode.IMAGE_STORAGE_UNAVAILABLE))
                .when(failing)
                .delete(any());
        storedProduct();
        ProductImage a = CatalogFixtures.image(product, 1, true);
        ProductImage b = CatalogFixtures.image(product, 3, false);
        images.addAll(List.of(a, b));

        // When
        service(failing).delete(PRODUCT_ID, a.getId());

        // Then
        verify(imageRepository).delete(a);
        verify(failing).delete(a.getObjectKey());
        assertThat(b.isPrimaryImage()).isTrue();
        assertThat(meterRegistry
                        .counter(ProductImageServiceImpl.ORPHANED_OBJECTS)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void shouldAnswerImageOrProductNotFound() {
        ProductImageServiceImpl service = storedProduct();
        UUID imageId = UUID.randomUUID();

        assertError(() -> service.findById(PRODUCT_ID, imageId), CatalogErrorCode.IMAGE_NOT_FOUND);
        given(productRepository.existsById(PRODUCT_ID)).willReturn(false);
        assertError(() -> service.findById(PRODUCT_ID, imageId), CatalogErrorCode.PRODUCT_NOT_FOUND);
        assertError(() -> service.list(PRODUCT_ID), CatalogErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void shouldOmitPrimaryImagesWhenUrlCannotBeGenerated() {
        // Given
        ProductImageServiceImpl service = storedProduct();
        ProductImage primary = CatalogFixtures.image(product, 1, true);
        given(imageRepository.findByProductIdInAndPrimaryImageTrue(List.of(PRODUCT_ID)))
                .willReturn(List.of(primary));

        // Then
        assertThat(service.findPrimaryImages(List.of(PRODUCT_ID))).containsOnlyKeys(PRODUCT_ID);
        storage.setPresignAvailable(false);
        assertThat(service.findPrimaryImages(List.of(PRODUCT_ID))).isEmpty();
        assertThat(service.findPrimaryImages(List.of())).isEqualTo(Map.of());
    }
}
