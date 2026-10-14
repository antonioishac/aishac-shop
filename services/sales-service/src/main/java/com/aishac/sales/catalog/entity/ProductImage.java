package com.aishac.sales.catalog.entity;

import com.aishac.sales.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Imagem de produto. O arquivo fica no S3; aqui só a chave do objeto (RN-18). Posição única e uma principal por
 * produto são garantidas também pelo banco (constraints adiáveis).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "product_images")
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    /** {@code products/{productId}/{uuid}.{ext}}; muda a cada substituição de arquivo (RN-19). */
    @Column(name = "object_key", nullable = false, length = 200)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "alt_text", nullable = false, length = 150)
    private String altText;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryImage;
}
