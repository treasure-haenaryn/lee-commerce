package com.github.haenaryn.user.interfaces;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank String refreshToken,
    @NotBlank String deviceId
) {
}
