package com.github.haenaryn.user.interfaces;

import com.github.haenaryn.user.application.LoginCommand;
import com.github.haenaryn.user.application.LoginResult;
import com.github.haenaryn.user.application.LoginService;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginService.login(
            new LoginCommand(request.email(), request.password(), request.deviceId()));

        LoginResponse response = new LoginResponse(
            result.accessToken(), result.refreshToken(), result.accessTokenExpiresAt());
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
