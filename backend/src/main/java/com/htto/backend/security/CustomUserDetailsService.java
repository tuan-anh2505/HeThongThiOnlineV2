package com.htto.backend.security;

import com.htto.backend.domain.Account;
import com.htto.backend.repository.AccountRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public CustomUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) {
        Account account = accountRepository.findByUsernameAndDeletedFalse(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .disabled(!account.isEnabled())
                .authorities(toAuthority(account))
                .build();
    }

    private GrantedAuthority toAuthority(Account account) {
        return new SimpleGrantedAuthority("ROLE_" + account.getRole().name());
    }
}
