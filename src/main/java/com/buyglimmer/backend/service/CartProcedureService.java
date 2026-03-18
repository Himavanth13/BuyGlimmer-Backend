package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.exception.ApiException;
import com.buyglimmer.backend.repository.CartProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartProcedureService {

    private static final Logger logger = LoggerFactory.getLogger(CartProcedureService.class);

    private final CartProcedureRepository cartProcedureRepository;

    public CartProcedureService(CartProcedureRepository cartProcedureRepository) {
        this.cartProcedureRepository = cartProcedureRepository;
    }

    public FintechDtos.CartItemResponse addToCart(FintechDtos.CartAddRequest request) {
        logger.info("Adding item to cart for customerId={}", request.customerId());
        return cartProcedureRepository.addToCart(request);
    }

    public List<FintechDtos.CartItemResponse> getCart(FintechDtos.CartGetRequest request) {
        logger.info("Fetching cart for customerId={}", request.customerId());
        return cartProcedureRepository.getCart(request.customerId());
    }

    public void updateCartItem(FintechDtos.CartUpdateRequest request) {
        logger.info("Updating cart item {} quantity={}", request.cartItemId(), request.quantity());
        int rows = cartProcedureRepository.updateCartItem(request);
        if (rows <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
    }

    public void removeCartItem(FintechDtos.CartRemoveRequest request) {
        logger.info("Removing cart item {}", request.cartItemId());
        int rows = cartProcedureRepository.removeCartItem(request.cartItemId());
        if (rows <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
    }
}
