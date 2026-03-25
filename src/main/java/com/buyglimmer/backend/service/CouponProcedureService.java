package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.repository.CouponProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponProcedureService {

    private static final Logger logger = LoggerFactory.getLogger(CouponProcedureService.class);

    private final CouponProcedureRepository couponProcedureRepository;

    public CouponProcedureService(CouponProcedureRepository couponProcedureRepository) {
        this.couponProcedureRepository = couponProcedureRepository;
    }

    public FintechDtos.CouponValidationResponse validateCoupon(FintechDtos.CouponValidateRequest request) {
        logger.info("Validating coupon={} for customerId={}", request.couponCode(), request.customerId());
        return couponProcedureRepository.validateCoupon(request);
    }

    public List<FintechDtos.CouponSummaryResponse> listCoupons(FintechDtos.CouponListRequest request) {
        logger.info("Fetching coupons for customerId={}", request.customerId());
        return couponProcedureRepository.listCoupons();
    }
}
