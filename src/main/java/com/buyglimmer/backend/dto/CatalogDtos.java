package com.buyglimmer.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryResponse(
            String name,
            long productCount
    ) {
    }

    public record ColorOptionResponse(
            String name,
            String hex,
            String image
    ) {
    }

    public record ReviewResponse(
            String user,
            int rating,
            String comment,
            String date
    ) {
    }

    public record ProductResponse(
            String id,
            String name,
            BigDecimal price,
            String category,
            List<String> images,
            String description,
            List<String> sizes,
            List<ColorOptionResponse> colors,
            Map<String, String> specs,
            List<ReviewResponse> reviews
    ) {
    }
}