package com.buyglimmer.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class CartDtos {

    private CartDtos() {
    }

    public record CartItemRequest(
            @NotNull String productId,
            @NotNull @Min(1) Integer quantity,
            String selectedSize,
            String selectedColor
    ) {
    }

    public record CartItemResponse(
            String cartItemId,
            CatalogDtos.ProductResponse product,
            Integer quantity,
            String selectedSize,
            String selectedColor,
            BigDecimal lineTotal
    ) {
    }
}