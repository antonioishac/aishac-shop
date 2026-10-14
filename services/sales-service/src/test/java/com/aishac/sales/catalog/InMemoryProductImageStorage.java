package com.aishac.sales.catalog;

import com.aishac.sales.catalog.exception.CatalogErrorCode;
import com.aishac.sales.catalog.service.ProductImageStorage;
import com.aishac.sales.exception.BusinessException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Armazenamento em memória para os testes de integração (o S3 real só no {@code S3ProductImageStorageIT}, com
 * LocalStack). Como o adapter real, a URL pré-assinada não depende do S3 estar no ar: são duas falhas separadas, o S3
 * fora ({@link #setAvailable(boolean)}) e a URL que não pode ser gerada ({@link #setPresignAvailable(boolean)}).
 */
public class InMemoryProductImageStorage implements ProductImageStorage {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private final AtomicBoolean available = new AtomicBoolean(true);
    private final AtomicBoolean presignAvailable = new AtomicBoolean(true);

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        ensure(available);
        objects.put(objectKey, content.clone());
    }

    @Override
    public void delete(String objectKey) {
        ensure(available);
        objects.remove(objectKey);
    }

    @Override
    public PresignedUrl presign(String objectKey) {
        ensure(presignAvailable);
        return new PresignedUrl(
                URI.create("http://storage.test/" + objectKey + "?signature=test"),
                Instant.parse("2030-01-01T00:00:00Z"));
    }

    public boolean contains(String objectKey) {
        return objects.containsKey(objectKey);
    }

    public byte[] content(String objectKey) {
        return objects.get(objectKey);
    }

    /** Objetos guardados com o prefixo (ex.: {@code products/{id}/}), para conferir que não sobrou órfão. */
    public long count(String prefix) {
        return objects.keySet().stream().filter(key -> key.startsWith(prefix)).count();
    }

    public void setAvailable(boolean value) {
        available.set(value);
    }

    public void setPresignAvailable(boolean value) {
        presignAvailable.set(value);
    }

    private static void ensure(AtomicBoolean flag) {
        if (!flag.get()) {
            throw new BusinessException(CatalogErrorCode.IMAGE_STORAGE_UNAVAILABLE);
        }
    }
}
