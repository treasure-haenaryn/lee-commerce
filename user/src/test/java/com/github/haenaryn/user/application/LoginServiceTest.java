package com.github.haenaryn.user.application;

import com.github.haenaryn.user.domain.Email;
import com.github.haenaryn.user.domain.PasswordEncoder;
import com.github.haenaryn.user.domain.PasswordHash;
import com.github.haenaryn.user.domain.RefreshTokenRepository;
import com.github.haenaryn.user.domain.User;
import com.github.haenaryn.user.domain.UserRepository;
import com.github.haenaryn.user.domain.exception.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private SessionRepository sessionRepository;

    private LoginService service;

    private User user;

    @BeforeEach
    void setUp() {
        service = new LoginService(userRepository, passwordEncoder, refreshTokenRepository, tokenIssuer, sessionRepository);
        user = User.register(new Email("user@example.com"), new PasswordHash("$2a$10$hashed"), "홍길동", null);
    }

    // User.getId()는 JPA가 저장 시점에 채우는 값이라, 저장 없이 만든 인스턴스는 null이다.
    // 로그인 흐름에서 id가 실제로 쓰이는 걸 검증하려면 mock으로 고정값을 흉내낸다.
    private User userWithId(long id) {
        User mocked = mock(User.class);
        when(mocked.getId()).thenReturn(id);
        when(mocked.getPasswordHash()).thenReturn(new PasswordHash("$2a$10$hashed"));
        return mocked;
    }

    @Test
    void 로그인에_성공하면_토큰과_세션을_등록한다() {
        // 중첩된 when()이 바깥쪽 when().thenReturn() 사이에 끼면 Mockito가 어느 스텁을
        // 완성하는 건지 헷갈려 해서(UnfinishedStubbingException), mock 생성을 먼저 끝내둔다.
        User mockedUser = userWithId(1L);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(mockedUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        Instant accessExpiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        Instant refreshExpiresAt = Instant.now().plus(14, ChronoUnit.DAYS);
        when(tokenIssuer.issueAccessToken(any())).thenReturn(new IssuedAccessToken("access-token", accessExpiresAt));
        when(tokenIssuer.issueRefreshToken()).thenReturn(new IssuedRefreshToken("raw-refresh", "hashed-refresh", refreshExpiresAt));

        LoginResult result = service.login(new LoginCommand("user@example.com", "password123", "device-1"));

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh");
        assertThat(result.accessTokenExpiresAt()).isEqualTo(accessExpiresAt);

        verify(refreshTokenRepository).save(any());
        verify(sessionRepository).save(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void 이메일이_없으면_예외() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("nobody@example.com", "password123", "device-1")))
            .isInstanceOf(AuthenticationFailedException.class);

        verify(tokenIssuer, never()).issueAccessToken(any());
    }

    @Test
    void 비밀번호가_틀리면_예외() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("user@example.com", "wrongpassword", "device-1")))
            .isInstanceOf(AuthenticationFailedException.class);

        verify(tokenIssuer, never()).issueAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    // 존재하는 이메일 + 규칙에 안 맞는(너무 짧은) 비밀번호와, 존재하지 않는 이메일이 같은
    // 예외를 반환해야 한다 — 다르면 그 차이로 등록된 이메일인지 추측할 수 있다(계정 열거 공격).
    @Test
    void 짧은_비밀번호는_이메일_존재_여부와_상관없이_같은_예외() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginCommand("user@example.com", "short", "device-1")))
            .isInstanceOf(AuthenticationFailedException.class);

        verify(tokenIssuer, never()).issueAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }
}
