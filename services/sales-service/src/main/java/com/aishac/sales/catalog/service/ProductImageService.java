package com.aishac.sales.catalog.service;

import com.aishac.sales.catalog.dto.ProductImageResponse;
import com.aishac.sales.catalog.dto.UpdateProductImageRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageService {

    /** Arquivo no S3, depois o registro; se o banco falhar, o objeto é removido (RN-16, RN-17, RN-21). */
    ProductImageResponse upload(UUID productId, MultipartFile file, String altText);

    List<ProductImageResponse> list(UUID productId);

    ProductImageResponse findById(UUID productId, UUID imageId);

    /** Texto alternativo, posição (troca com a imagem que a ocupa) e/ou principal. */
    ProductImageResponse update(UUID productId, UUID imageId, UpdateProductImageRequest request);

    /** Chave nova → banco → remove o objeto antigo (RN-19). */
    ProductImageResponse replaceFile(UUID productId, UUID imageId, MultipartFile file);

    /** Banco → S3; a próxima pela posição vira principal (RN-20). */
    void delete(UUID productId, UUID imageId);

    /**
     * Imagem principal de cada produto, numa só consulta. Para a vitrine: com o S3 indisponível devolve o mapa vazio, e
     * os produtos saem sem imagem em vez de falhar.
     */
    Map<UUID, ProductImageResponse> findPrimaryImages(Collection<UUID> productIds);
}
