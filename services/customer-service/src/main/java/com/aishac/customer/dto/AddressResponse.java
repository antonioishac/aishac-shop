package com.aishac.customer.dto;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String label,
        String zipCode,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String country,
        boolean isDefault) {}
