package com.aishac.sales.catalog.service;

import java.net.URI;
import java.time.Instant;

/**
 * Armazenamento dos arquivos de imagem (rule storage). A implementação real usa o S3 com Circuit Breaker; os testes
 * trocam por uma em memória. Falha de armazenamento sempre vira {@code IMAGE_STORAGE_UNAVAILABLE} (RN-22).
 */
public interface ProductImageStorage {

    /** Grava o objeto. Nunca sobrescreve: cada arquivo ganha uma chave nova (RN-19). */
    void put(String objectKey, byte[] content, String contentType);

    void delete(String objectKey);

    /** URL de leitura temporária, calculada localmente (não chama o S3). */
    PresignedUrl presign(String objectKey);

    record PresignedUrl(URI url, Instant expiresAt) {}
}
