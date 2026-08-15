package com.github.haenaryn.user.interfaces;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
    @NotBlank String refreshToken,
    @NotBlank String deviceId
) {
}
