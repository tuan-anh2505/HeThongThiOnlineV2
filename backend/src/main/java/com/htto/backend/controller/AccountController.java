package com.htto.backend.controller;

import com.htto.backend.domain.AccountStatus;
import com.htto.backend.domain.Role;
import com.htto.backend.dto.request.AccountCreateRequest;
import com.htto.backend.dto.request.AccountUpdateRequest;
import com.htto.backend.dto.response.AccountResponse;
import com.htto.backend.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> search(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status
    ) {
        return accountService.search(username, fullName, email, phone, role, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountCreateRequest request) {
        return accountService.create(request);
    }

    @PutMapping("/{id}")
    public AccountResponse update(
            @PathVariable String id,
            @Valid @RequestBody AccountUpdateRequest request
    ) {
        return accountService.update(id, request);
    }

    @PatchMapping("/{id}/lock")
    public AccountResponse lock(@PathVariable String id) {
        return accountService.lock(id);
    }

    @PatchMapping("/{id}/unlock")
    public AccountResponse unlock(@PathVariable String id) {
        return accountService.unlock(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        accountService.softDelete(id);
    }
}
