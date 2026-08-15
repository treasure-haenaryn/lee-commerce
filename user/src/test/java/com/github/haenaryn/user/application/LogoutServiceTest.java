package com.github.haenaryn.user.application;

import com.github.haenaryn.user.domain.RefreshToken;
import com.github.haenaryn.user.domain.RefreshTokenRepository;
import com.github.haenaryn.user.domain.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private SessionRepository sessionRepository;

    private LogoutService service;

    @BeforeEach
    void setUp() {
        service = new LogoutService(refreshTokenRepository, tokenIssuer, sessionRepository);
    }

    @Test
    void 로그아웃하면_토큰을_폐기하고_세션을_지운다() {
        RefreshToken existing = mock(RefreshToken.class);
        when(existing.getUserId()).thenReturn(1L);
        when(existing.isRevoked()).thenReturn(false);

        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(sessionRepository.findRefreshTokenHash(any(), any())).thenReturn(Optional.of("existing-hash"));

        service.logout(new LogoutCommand("raw-token", "device-1"));

        verify(existing).revoke(any(), eq((Long) null));
        verify(refreshTokenRepository).save(existing);
        verify(sessionRepository).revoke(1L, "device-1");
    }

    @Test
    void 이미_폐기된_토큰이면_다시_폐기하지_않고_세션만_지운다() {
        RefreshToken existing = mock(RefreshToken.class);
        when(existing.getUserId()).thenReturn(1L);
        when(existing.isRevoked()).thenReturn(true);

        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(sessionRepository.findRefreshTokenHash(any(), any())).thenReturn(Optional.of("existing-hash"));

        service.logout(new LogoutCommand("raw-token", "device-1"));

        verify(existing, never()).revoke(any(), any());
        verify(refreshTokenRepository, never()).save(any());
        verify(sessionRepository).revoke(1L, "device-1");
    }

    @Test
    void 토큰을_찾지_못하면_예외() {
        when(tokenIssuer.hashRefreshToken(any())).thenReturn("missing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logout(new LogoutCommand("raw-token", "device-1")))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(sessionRepository, never()).revoke(any(), any());
    }

    // 다른 디바이스의 토큰을 도용해서 deviceId만 바꿔 제출하면, 그 deviceId의 세션이
    // 존재하지 않거나 다른 토큰을 가리키므로 거부해야 한다 — 다른 디바이스 세션을
    // 임의로 지울 수 있으면 안 된다.
    @Test
    void 세션에_저장된_해시와_다르면_예외() {
        RefreshToken existing = mock(RefreshToken.class);
        when(existing.getUserId()).thenReturn(1L);

        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(sessionRepository.findRefreshTokenHash(any(), any())).thenReturn(Optional.of("다른-hash"));

        assertThatThrownBy(() -> service.logout(new LogoutCommand("raw-token", "다른-device")))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(sessionRepository, never()).revoke(any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }
}
