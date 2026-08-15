package com.github.haenaryn.user.infrastructure.security;

import com.github.haenaryn.user.domain.PasswordEncoder;
import com.github.haenaryn.user.domain.PasswordHash;
import com.github.haenaryn.user.domain.RawPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public PasswordHash encode(RawPassword rawPassword) {
        return new PasswordHash(delegate.encode(rawPassword.value()));
    }

    @Override
    public boolean matches(RawPassword rawPassword, PasswordHash passwordHash) {
        return delegate.matches(rawPassword.value(), passwordHash.value());
    }
}
