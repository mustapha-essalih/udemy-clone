package com.dev.lms.admin_service.service;

import org.springframework.stereotype.Service;

import com.dev.lms.admin_service.client.AuthServiceClient;
import com.dev.lms.common.request.RegisterRequest;
import com.dev.lms.common.response.RegistrationResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

    private final AuthServiceClient authServiceClient;

    public RegistrationResponse createManager(RegisterRequest request) {

        return authServiceClient.registerManager(request);
    }

    
    

}
