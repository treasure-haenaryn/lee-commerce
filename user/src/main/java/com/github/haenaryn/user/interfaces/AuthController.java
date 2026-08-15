package com.github.haenaryn.user.interfaces;

import com.github.haenaryn.user.application.LoginCommand;
import com.github.haenaryn.user.application.LoginResult;
import com.github.haenaryn.user.application.LoginService;
import com.github.haenaryn.user.application.LogoutCommand;
import com.github.haenaryn.user.application.LogoutService;
import com.github.haenaryn.user.application.TokenRefreshCommand;
import com.github.haenaryn.user.application.TokenRefreshService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;
    private final TokenRefreshService tokenRefreshService;
    private final LogoutService logoutService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginService.login(
            new LoginCommand(request.email(), request.password(), request.deviceId()));

        return ResponseEntity.ok(ApiResponse.of(toResponse(result)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        LoginResult result = tokenRefreshService.refresh(
            new TokenRefreshCommand(request.refreshToken(), request.deviceId()));

        return ResponseEntity.ok(ApiResponse.of(toResponse(result)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        logoutService.logout(new LogoutCommand(request.refreshToken(), request.deviceId()));
        return ResponseEntity.noContent().build();
    }

    private LoginResponse toResponse(LoginResult result) {
        return new LoginResponse(result.accessToken(), result.refreshToken(), result.accessTokenExpiresAt());
    }
}
