package com.github.haenaryn.user.interfaces;

import com.github.haenaryn.user.application.RegisterUserCommand;
import com.github.haenaryn.user.application.RegisterUserResult;
import com.github.haenaryn.user.application.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserRegistrationController {

    private final UserRegistrationService userRegistrationService;

    @PostMapping
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserResult result = userRegistrationService.register(
            new RegisterUserCommand(request.email(), request.password(), request.name(), request.phone()));

        RegisterResponse response = new RegisterResponse(result.userId(), result.email(), result.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}
