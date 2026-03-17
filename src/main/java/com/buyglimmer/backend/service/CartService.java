package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.CartDtos;
import com.buyglimmer.backend.dto.CatalogDtos;
import com.buyglimmer.backend.exception.ApiException;
import com.buyglimmer.backend.repository.CartStoredProcedureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CatalogService catalogService;
    private final CartStoredProcedureRepository cartRepository;

    public CartService(CatalogService catalogService, CartStoredProcedureRepository cartRepository) {
        this.catalogService = catalogService;
        this.cartRepository = cartRepository;
    }

    public List<CartDtos.CartItemResponse> fetchCart() {
        return cartRepository.fetchCart(UserService.DEFAULT_USER_ID).stream()
                .map(this::toResponse)
                .toList();
    }

    public CartDtos.CartItemResponse addItem(CartDtos.CartItemRequest request) {
        CatalogDtos.ProductResponse product = catalogService.fetchProduct(request.productId());
        CartStoredProcedureRepository.CartLine line = cartRepository.addItem(
                UserService.DEFAULT_USER_ID,
                request.productId(),
                request.quantity(),
                normalizeSize(product, request.selectedSize()),
                normalizeColor(product, request.selectedColor())
        );
        return toResponse(line);
    }

    public void removeItem(String cartItemId) {
        if (!cartRepository.removeItem(cartItemId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Cart item not found", List.of("Requested cart item id: " + cartItemId));
        }
    }

    public void clearCart() {
        cartRepository.clearCart(UserService.DEFAULT_USER_ID);
    }

    public List<CartDtos.CartItemResponse> currentLines() {
        return fetchCart();
    }

    private CartDtos.CartItemResponse toResponse(CartStoredProcedureRepository.CartLine line) {
        CatalogDtos.ProductResponse product = catalogService.fetchProduct(line.productId());
        return new CartDtos.CartItemResponse(
                line.cartItemId(),
                product,
                line.quantity(),
                line.selectedSize(),
                line.selectedColor(),
                product.price().multiply(BigDecimal.valueOf(line.quantity()))
        );
    }

    private String normalizeSize(CatalogDtos.ProductResponse product, String selectedSize) {
        if (selectedSize != null && !selectedSize.isBlank()) {
            return selectedSize;
        }
        return product.sizes().isEmpty() ? null : product.sizes().get(0);
    }

    private String normalizeColor(CatalogDtos.ProductResponse product, String selectedColor) {
        if (selectedColor != null && !selectedColor.isBlank()) {
            return selectedColor;
        }
        return product.colors().isEmpty() ? null : product.colors().get(0).name();
    }
}