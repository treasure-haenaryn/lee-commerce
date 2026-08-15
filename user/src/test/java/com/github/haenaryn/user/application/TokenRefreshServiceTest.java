package com.github.haenaryn.user.application;

import com.github.haenaryn.user.domain.RefreshToken;
import com.github.haenaryn.user.domain.RefreshTokenRepository;
import com.github.haenaryn.user.domain.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRefreshServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private SessionRepository sessionRepository;

    private TokenRefreshService service;

    @BeforeEach
    void setUp() {
        service = new TokenRefreshService(refreshTokenRepository, tokenIssuer, sessionRepository);
    }

    // getUserId()는 revoked/expired 검사를 통과한 뒤에만 호출되므로, 예외를 던지는
    // 케이스에서까지 stub해두면 "쓰이지 않은 stub"으로 걸린다 — 필요한 테스트에서만 스텁한다.
    private RefreshToken mockToken(boolean revoked, boolean expired) {
        RefreshToken token = mock(RefreshToken.class);
        when(token.isRevoked()).thenReturn(revoked);
        if (!revoked) {
            when(token.isExpired(any())).thenReturn(expired);
        }
        return token;
    }

    @Test
    void 정상_토큰이면_새_토큰을_발급하고_기존_토큰을_폐기한다() {
        RefreshToken existing = mockToken(false, false);
        when(existing.getUserId()).thenReturn(1L);
        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(sessionRepository.findRefreshTokenHash(any(), any())).thenReturn(Optional.of("existing-hash"));

        Instant accessExpiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        Instant refreshExpiresAt = Instant.now().plus(14, ChronoUnit.DAYS);
        when(tokenIssuer.issueAccessToken(any())).thenReturn(new IssuedAccessToken("new-access", accessExpiresAt));
        when(tokenIssuer.issueRefreshToken())
            .thenReturn(new IssuedRefreshToken("new-raw", "new-hash", refreshExpiresAt));

        RefreshToken savedNew = mock(RefreshToken.class);
        when(savedNew.getId()).thenReturn(99L);
        when(refreshTokenRepository.save(any())).thenReturn(savedNew);

        LoginResult result = service.refresh(new TokenRefreshCommand("raw-token", "device-1"));

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-raw");

        verify(existing).revoke(any(), eq(99L));
        verify(sessionRepository).save(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void 토큰을_찾지_못하면_예외() {
        when(tokenIssuer.hashRefreshToken(any())).thenReturn("missing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(new TokenRefreshCommand("raw-token", "device-1")))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokenIssuer, never()).issueAccessToken(any());
    }

    @Test
    void 이미_폐기된_토큰이면_예외() {
        RefreshToken existing = mockToken(true, false);
        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.refresh(new TokenRefreshCommand("raw-token", "device-1")))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokenIssuer, never()).issueAccessToken(any());
    }

    @Test
    void 만료된_토큰이면_예외() {
        RefreshToken existing = mockToken(false, true);
        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.refresh(new TokenRefreshCommand("raw-token", "device-1")))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokenIssuer, never()).issueAccessToken(any());
    }

    @Test
    void 제출한_deviceId에_해당하는_세션이_없으면_예외() {
        RefreshToken existing = mockToken(false, false);
        when(existing.getUserId()).thenReturn(1L);
        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(sessionRepository.findRefreshTokenHash(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(new TokenRefreshCommand("raw-token", "다른-device")))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokenIssuer, never()).issueAccessToken(any());
    }

    // 토큰 자체는 유효해도, 그 토큰이 실제로 이 deviceId의 세션이 아니면(다른 디바이스의
    // 토큰을 도용해서 deviceId만 바꿔 제출한 경우) 거부해야 한다.
    @Test
    void 세션에_저장된_해시와_다르면_예외() {
        RefreshToken existing = mockToken(false, false);
        when(existing.getUserId()).thenReturn(1L);
        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(sessionRepository.findRefreshTokenHash(any(), any())).thenReturn(Optional.of("다른-hash"));

        assertThatThrownBy(() -> service.refresh(new TokenRefreshCommand("raw-token", "device-1")))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokenIssuer, never()).issueAccessToken(any());
    }

    // 같은 토큰으로 동시에 재발급 요청이 들어와 한쪽이 먼저 폐기에 성공하면, 뒤늦게 저장을
    // 시도하는 쪽은 낙관적 락 예외를 받는다 — 이것도 통일된 예외로 변환돼야 한다.
    @Test
    void 동시_재발급으로_낙관적_락_충돌이_나면_예외() {
        RefreshToken existing = mockToken(false, false);
        when(existing.getUserId()).thenReturn(1L);
        when(tokenIssuer.hashRefreshToken(any())).thenReturn("existing-hash");
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(sessionRepository.findRefreshTokenHash(any(), any())).thenReturn(Optional.of("existing-hash"));

        when(tokenIssuer.issueAccessToken(any()))
            .thenReturn(new IssuedAccessToken("new-access", Instant.now().plus(15, ChronoUnit.MINUTES)));
        when(tokenIssuer.issueRefreshToken())
            .thenReturn(new IssuedRefreshToken("new-raw", "new-hash", Instant.now().plus(14, ChronoUnit.DAYS)));

        RefreshToken savedNew = mock(RefreshToken.class);
        when(savedNew.getId()).thenReturn(99L);
        // save()는 순서대로 두 번 호출된다: ① 새 엔티티 저장(성공) ② 기존 엔티티 폐기
        // 저장(충돌로 실패) — 같은 stub에 연속 응답을 걸어 호출 순서대로 흉내낸다.
        when(refreshTokenRepository.save(any()))
            .thenReturn(savedNew)
            .thenThrow(new ObjectOptimisticLockingFailureException(RefreshToken.class, 1L));

        assertThatThrownBy(() -> service.refresh(new TokenRefreshCommand("raw-token", "device-1")))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
