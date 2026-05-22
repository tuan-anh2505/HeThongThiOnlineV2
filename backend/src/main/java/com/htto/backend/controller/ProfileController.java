package com.htto.backend.controller;

import com.htto.backend.dto.request.ChangePasswordRequest;
import com.htto.backend.dto.response.MessageResponse;
import com.htto.backend.dto.response.ProfileMeResponse;
import com.htto.backend.service.PasswordService;
import com.htto.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final PasswordService passwordService;

    public ProfileController(ProfileService profileService, PasswordService passwordService) {
        this.profileService = profileService;
        this.passwordService = passwordService;
    }

    @GetMapping("/me")
    public ProfileMeResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return profileService.getCurrentProfile(authentication.getName());
    }

    @PutMapping("/change-password")
    public MessageResponse changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return new MessageResponse(passwordService.changePassword(authentication.getName(), request));
    }
}
