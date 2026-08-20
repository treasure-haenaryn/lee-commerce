package com.github.haenaryn.catalog.interfaces;

import jakarta.validation.constraints.NotBlank;

public record ChangeProductStatusRequest(@NotBlank String status) {
}
