package com.aishac.sales.exception;

import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Nome da constraint do banco violada, extraído pelo Hibernate (não pelo texto da mensagem, que traz SQL e dados da
 * linha e não deve ir para logs nem respostas).
 */
public final class ConstraintViolations {

    private ConstraintViolations() {}

    public static Optional<String> constraintName(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return Optional.of(violation.getConstraintName());
            }
        }
        return Optional.empty();
    }

    public static boolean isCheckConstraint(String constraintName) {
        return constraintName.startsWith("ck_");
    }

    /**
     * Campo da API correspondente a um CHECK do banco, para a resposta não expor o nome da constraint. CHECK sem campo
     * equivalente na API responde como {@code request}.
     */
    public static String apiField(String constraintName) {
        return switch (constraintName) {
            case "ck_categories_name_length", "ck_products_name_length" -> "name";
            case "ck_products_sku_format" -> "sku";
            case "ck_products_price_positive" -> "price";
            case "ck_stock_movements_quantity_positive" -> "quantity";
            case "ck_stock_movements_reason_length" -> "reason";
            case "ck_product_images_alt_text_not_blank" -> "altText";
            case "ck_product_images_position_range" -> "position";
            default -> "request";
        };
    }
}
