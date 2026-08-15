package com.github.haenaryn.user.interfaces;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String email,
    @NotBlank String password,
    @NotBlank String name,
    String phone
) {
}
