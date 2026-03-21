package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.repository.OrderProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderProcedureService {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcedureService.class);

    private final OrderProcedureRepository orderProcedureRepository;

    public OrderProcedureService(OrderProcedureRepository orderProcedureRepository) {
        this.orderProcedureRepository = orderProcedureRepository;
    }

    @Transactional
    public FintechDtos.OrderSummaryResponse createOrder(FintechDtos.OrderCreateRequest request) {
        logger.info("Creating order for customerId={}", request.customerId());
        FintechDtos.OrderSummaryResponse orderSummary = orderProcedureRepository.createOrder(request);
        for (FintechDtos.OrderItemInput item : request.items()) {
            orderProcedureRepository.addOrderItem(orderSummary.orderId(), item);
        }
        int clearedItems = orderProcedureRepository.clearActiveCartForCustomer(request.customerId());
        logger.info("Cleared {} cart items for customerId={} after orderId={}", clearedItems, request.customerId(), orderSummary.orderId());
        return orderProcedureRepository.getOrderSummary(orderSummary.orderId());
    }

    public List<FintechDtos.OrderSummaryResponse> getOrders(FintechDtos.OrderListRequest request) {
        logger.info("Fetching orders for customerId={}", request.customerId());
        return orderProcedureRepository.getOrders(request.customerId());
    }

    public FintechDtos.OrderDetailResponse getOrderDetail(FintechDtos.OrderDetailRequest request) {
        logger.info("Fetching order detail for orderId={}", request.orderId());
        FintechDtos.OrderSummaryResponse summary = orderProcedureRepository.getOrderSummary(request.orderId());
        List<FintechDtos.OrderItemResponse> items = orderProcedureRepository.getOrderItems(request.orderId());
        return new FintechDtos.OrderDetailResponse(
                summary.orderId(),
                summary.customerId(),
                summary.totalAmount(),
                summary.status(),
                summary.paymentStatus(),
                summary.createdAt(),
                items
        );
    }
}
