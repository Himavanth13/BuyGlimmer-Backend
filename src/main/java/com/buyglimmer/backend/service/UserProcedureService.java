package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.FintechDtos;
import com.buyglimmer.backend.repository.UserProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserProcedureService {

    private static final Logger logger = LoggerFactory.getLogger(UserProcedureService.class);

    private final UserProcedureRepository userProcedureRepository;

    public UserProcedureService(UserProcedureRepository userProcedureRepository) {
        this.userProcedureRepository = userProcedureRepository;
    }

    public FintechDtos.UserProfileResponse getProfile(FintechDtos.UserProfileRequest request) {
        logger.info("Fetching profile for customerId={}", request.customerId());
        return userProcedureRepository.getProfile(request.customerId());
    }

    public FintechDtos.UserProfileResponse updateProfile(FintechDtos.UserUpdateRequest request) {
        logger.info("Updating profile for customerId={}", request.customerId());
        return userProcedureRepository.updateProfile(request);
    }

    public FintechDtos.AddressResponse addAddress(FintechDtos.AddressAddRequest request) {
        logger.info("Adding address for customerId={}", request.customerId());
        return userProcedureRepository.addAddress(request);
    }

    public List<FintechDtos.AddressResponse> listAddresses(FintechDtos.AddressListRequest request) {
        logger.info("Listing addresses for customerId={}", request.customerId());
        return userProcedureRepository.listAddresses(request.customerId());
    }
}
