package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.exception.ApiException;
import com.buyglimmer.backend.repository.CartProcedureRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartProcedureService {

    private static final Logger logger = LoggerFactory.getLogger(CartProcedureService.class);

    private final CartProcedureRepository cartProcedureRepository;
    private final String itemNotFoundMessage;
    private final String actorRequiredMessage;
    private final String invalidQuantityMessage;
    private final int minQuantity;

    public CartProcedureService(
            CartProcedureRepository cartProcedureRepository,
            @Value("${cart.module.message.item-not-found}") String itemNotFoundMessage,
            @Value("${cart.module.message.actor-required}") String actorRequiredMessage,
            @Value("${cart.module.message.invalid-quantity}") String invalidQuantityMessage,
            @Value("${cart.module.validation.min-quantity}") int minQuantity
    ) {
        this.cartProcedureRepository = cartProcedureRepository;
        this.itemNotFoundMessage = itemNotFoundMessage;
        this.actorRequiredMessage = actorRequiredMessage;
        this.invalidQuantityMessage = invalidQuantityMessage;
        this.minQuantity = minQuantity;
    }

    public FintechDtos.CartItemResponse addToCart(FintechDtos.CartAddRequest request) {
        validateQuantity(request.quantity());
        String actorId = resolveActorId(request.customerId(), request.guestId());
        logger.info("Adding item to cart for actorId={}", actorId);
        return cartProcedureRepository.addToCart(request, actorId);
    }

    public List<FintechDtos.CartItemResponse> getCart(FintechDtos.CartGetRequest request) {
        String actorId = resolveActorId(request.customerId(), request.guestId());
        logger.info("Fetching cart for actorId={}", actorId);
        return cartProcedureRepository.getCart(actorId);
    }

    public void updateCartItem(FintechDtos.CartUpdateRequest request) {
        validateQuantity(request.quantity());
        String actorId = resolveActorId(request.customerId(), request.guestId());
        logger.info("Updating cart item {} quantity={} actorId={}", request.cartItemId(), request.quantity(), actorId);
        int rows = cartProcedureRepository.updateCartItem(request, actorId);
        if (rows <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, itemNotFoundMessage);
        }
    }

    public void removeCartItem(FintechDtos.CartRemoveRequest request) {
        String actorId = resolveActorId(request.customerId(), request.guestId());
        logger.info("Removing cart item {} actorId={}", request.cartItemId(), actorId);
        int rows = cartProcedureRepository.removeCartItem(request.cartItemId(), actorId);
        if (rows <= 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, itemNotFoundMessage);
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < minQuantity) {
            throw new ApiException(HttpStatus.BAD_REQUEST, invalidQuantityMessage);
        }
    }

    private String resolveActorId(String customerId, String guestId) {
        if (customerId != null && !customerId.isBlank()) {
            return customerId;
        }
        if (guestId != null && !guestId.isBlank()) {
            return guestId;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, actorRequiredMessage);
    }
}
