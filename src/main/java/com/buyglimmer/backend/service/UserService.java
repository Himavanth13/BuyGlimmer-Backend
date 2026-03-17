package com.buyglimmer.backend.service;

import com.buyglimmer.backend.dto.UserDtos;
import com.buyglimmer.backend.repository.UserStoredProcedureRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public static final String DEFAULT_USER_ID = "39225d99-1f70-11f1-9651-ed7fb304f8d2";

    private final UserStoredProcedureRepository userRepository;

    public UserService(UserStoredProcedureRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDtos.UserProfileResponse fetchProfile() {
        return userRepository.fetchProfile(DEFAULT_USER_ID);
    }

    public UserDtos.UserProfileResponse updateProfile(UserDtos.UpdateProfileRequest request) {
        return userRepository.updateProfile(DEFAULT_USER_ID, request);
    }

    public UserDtos.UserProfileResponse register(String name, String email, String password, String phone) {
        return userRepository.register(name, email, password, phone);
    }
}