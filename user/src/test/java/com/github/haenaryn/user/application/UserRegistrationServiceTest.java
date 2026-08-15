package com.github.haenaryn.user.application;

import com.github.haenaryn.user.domain.Email;
import com.github.haenaryn.user.domain.PasswordEncoder;
import com.github.haenaryn.user.domain.PasswordHash;
import com.github.haenaryn.user.domain.User;
import com.github.haenaryn.user.domain.UserRepository;
import com.github.haenaryn.user.domain.exception.DuplicateEmailException;
import com.github.haenaryn.user.domain.exception.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new UserRegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void 가입에_성공하면_저장된_사용자_정보를_반환한다() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn(new PasswordHash("$2a$10$hashed"));

        User saved = User.register(new Email("user@example.com"),
            new PasswordHash("$2a$10$hashed"), "홍길동", null);
        when(userRepository.save(any())).thenReturn(saved);

        RegisterUserResult result = service.register(
            new RegisterUserCommand("user@example.com", "password123", "홍길동", null));

        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
    }

    @Test
    void 이미_등록된_이메일이면_예외() {
        when(userRepository.existsByEmail(any())).thenReturn(true);

        assertThatThrownBy(() -> service.register(
            new RegisterUserCommand("user@example.com", "password123", "홍길동", null)))
            .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void 비밀번호가_규칙에_안_맞으면_예외() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        assertThatThrownBy(() -> service.register(
            new RegisterUserCommand("user@example.com", "short", "홍길동", null)))
            .isInstanceOf(InvalidPasswordException.class);

        verify(userRepository, never()).save(any());
    }
}
