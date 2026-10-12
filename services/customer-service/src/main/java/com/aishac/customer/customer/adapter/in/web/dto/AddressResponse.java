package com.aishac.customer.customer.adapter.in.web.dto;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record AddressResponse(
        UUID id,
        String label,
        String zipCode,
        String street,
        String number,
        @Nullable String complement,
        String district,
        String city,
        String state,
        String country,
        boolean isDefault) {}
