package com.aishac.sales.catalog.exception;

import com.aishac.sales.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** Catálogo de erros do módulo catalog (spec F01-030, seção 5; F01-039). */
public enum CatalogErrorCode implements ErrorCode {
    INVALID_IMAGE_TYPE("File is not a JPEG, PNG or WebP image.", HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE("Image file is larger than the allowed size.", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND("Category not found.", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND("Product not found.", HttpStatus.NOT_FOUND),
    IMAGE_NOT_FOUND("Product image not found.", HttpStatus.NOT_FOUND),
    CATEGORY_NAME_ALREADY_EXISTS("Category name is already in use.", HttpStatus.CONFLICT),
    SKU_ALREADY_EXISTS("SKU is already in use.", HttpStatus.CONFLICT),
    CATEGORY_INACTIVE("Category is inactive.", HttpStatus.UNPROCESSABLE_CONTENT),
    INSUFFICIENT_STOCK("Insufficient stock for the requested quantity.", HttpStatus.UNPROCESSABLE_CONTENT),
    IMAGE_LIMIT_EXCEEDED("Product already has the maximum number of images.", HttpStatus.UNPROCESSABLE_CONTENT),
    IMAGE_STORAGE_UNAVAILABLE("Image storage is unavailable. Try again later.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String defaultMessage;
    private final HttpStatus httpStatus;

    CatalogErrorCode(String defaultMessage, HttpStatus httpStatus) {
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
