package com.aishac.customer.customer.adapter.in.web.dto;

import java.util.List;

public record CustomerPageResponse(List<CustomerResponse> content, PageMetadata page) {

    public record PageMetadata(int number, int size, long totalElements, int totalPages) {}
}
