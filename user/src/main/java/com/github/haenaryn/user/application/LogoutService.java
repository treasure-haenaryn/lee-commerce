package com.github.haenaryn.user.application;

import com.github.haenaryn.user.domain.RefreshToken;
import com.github.haenaryn.user.domain.RefreshTokenRepository;
import com.github.haenaryn.user.domain.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenIssuer tokenIssuer;
    private final SessionRepository sessionRepository;

    @Transactional
    public void logout(LogoutCommand command) {
        String hash = tokenIssuer.hashRefreshToken(command.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(InvalidRefreshTokenException::new);

        // 토큰만 유효하면 아무 deviceId나 대서 다른 디바이스의 세션을 지울 수 있으면 안 된다 —
        // 제출된 deviceId가 실제로 이 토큰의 세션인지 Redis에 저장된 값과 대조한다.
        String sessionHash = sessionRepository.findRefreshTokenHash(existing.getUserId(), command.deviceId())
            .orElseThrow(InvalidRefreshTokenException::new);
        if (!sessionHash.equals(hash)) {
            throw new InvalidRefreshTokenException();
        }

        if (!existing.isRevoked()) {
            existing.revoke(Instant.now(), null);
            refreshTokenRepository.save(existing);
        }

        sessionRepository.revoke(existing.getUserId(), command.deviceId());
    }
}
