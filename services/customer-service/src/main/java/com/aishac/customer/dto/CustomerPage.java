package com.aishac.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Só documentação OpenAPI: a forma de {@code PageResponse<CustomerResponse>}, com o nome do contrato. */
@Schema(name = "CustomerPage")
public record CustomerPage(List<CustomerResponse> content, PageResponse.PageMetadata page) {}
