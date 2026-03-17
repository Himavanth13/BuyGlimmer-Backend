package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.CartDtos;
import com.buyglimmer.backend.dto.OrderDtos;
import com.buyglimmer.backend.exception.ApiException;
import com.buyglimmer.backend.repository.OrderStoredProcedureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final CartService cartService;
    private final OrderStoredProcedureRepository orderRepository;

    public OrderService(CartService cartService, OrderStoredProcedureRepository orderRepository) {
        this.cartService = cartService;
        this.orderRepository = orderRepository;
    }

    public OrderDtos.OrderResponse checkout(OrderDtos.CheckoutRequest request) {
        List<CartDtos.CartItemResponse> cart = cartService.currentLines();
        if (cart.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty", List.of("Add products to the cart before checkout."));
        }
        return orderRepository.createOrder(UserService.DEFAULT_USER_ID, request);
    }

    public List<OrderDtos.OrderResponse> fetchOrders() {
        return orderRepository.fetchOrders(UserService.DEFAULT_USER_ID);
    }

    public OrderDtos.OrderResponse fetchOrder(String orderId) {
        return orderRepository.fetchOrder(orderId);
    }
}