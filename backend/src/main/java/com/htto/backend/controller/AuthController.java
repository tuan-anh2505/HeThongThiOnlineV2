package com.htto.backend.controller;

import com.htto.backend.domain.Account;
import com.htto.backend.dto.request.ForgotPasswordRequest;
import com.htto.backend.dto.request.LoginRequest;
import com.htto.backend.dto.request.ResetPasswordRequest;
import com.htto.backend.dto.response.AccountResponse;
import com.htto.backend.dto.response.LoginResponse;
import com.htto.backend.dto.response.MessageResponse;
import com.htto.backend.security.JwtService;
import com.htto.backend.service.AccountService;
import com.htto.backend.service.PasswordService;
import com.htto.backend.service.SystemLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountService accountService;
    private final PasswordService passwordService;
    private final SystemLogService systemLogService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AccountService accountService,
            PasswordService passwordService,
            SystemLogService systemLogService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accountService = accountService;
        this.passwordService = passwordService;
        this.systemLogService = systemLogService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (DisabledException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked");
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        Account account = accountService.getActiveAccountByUsername(request.username());
        String token = jwtService.generateToken((UserDetails) authentication.getPrincipal());
        systemLogService.log(account.getId(), "LOGIN", "ACCOUNT", account.getId(), "User logged in");
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationMillis(),
                AccountResponse.from(account)
        );
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return new MessageResponse(passwordService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return new MessageResponse(passwordService.resetPassword(request));
    }

    @GetMapping("/me")
    public AccountResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return accountService.getByUsername(authentication.getName());
    }
}
