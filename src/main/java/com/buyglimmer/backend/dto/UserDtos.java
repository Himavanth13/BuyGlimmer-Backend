package com.buyglimmer.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserProfileResponse(
            String id,
            String name,
            String email,
            String phone,
            String address,
            String avatar
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String phone,
            @NotBlank String address,
            @NotBlank String avatar
    ) {
    }

    public record WishlistToggleRequest(
            @NotBlank String productId
    ) {
    }
}