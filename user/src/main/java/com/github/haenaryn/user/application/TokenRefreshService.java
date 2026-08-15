package com.github.haenaryn.user.application;

import com.github.haenaryn.user.domain.RefreshToken;
import com.github.haenaryn.user.domain.RefreshTokenRepository;
import com.github.haenaryn.user.domain.exception.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenIssuer tokenIssuer;
    private final SessionRepository sessionRepository;

    @Transactional
    public LoginResult refresh(TokenRefreshCommand command) {
        String hash = tokenIssuer.hashRefreshToken(command.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = Instant.now();
        if (existing.isRevoked() || existing.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        // 토큰만 유효하면 아무 deviceId나 대서 재발급받을 수 있으면 안 된다 — 제출된
        // deviceId가 실제로 이 토큰을 발급받은 세션인지 Redis에 저장된 값과 대조한다.
        String sessionHash = sessionRepository.findRefreshTokenHash(existing.getUserId(), command.deviceId())
            .orElseThrow(InvalidRefreshTokenException::new);
        if (!sessionHash.equals(hash)) {
            throw new InvalidRefreshTokenException();
        }

        IssuedAccessToken accessToken = tokenIssuer.issueAccessToken(existing.getUserId());
        IssuedRefreshToken refreshToken = tokenIssuer.issueRefreshToken();

        // 새 토큰을 먼저 저장해야 id가 생기고, 그 id로 기존 토큰의 replacedByTokenId를 채울 수 있다.
        RefreshToken newEntity = RefreshToken.issue(
            existing.getUserId(), refreshToken.hashedValue(), now, refreshToken.expiresAt());
        RefreshToken savedNewEntity = refreshTokenRepository.save(newEntity);

        existing.revoke(now, savedNewEntity.getId());
        try {
            // 같은 토큰으로 동시에 들어온 재발급 요청은 여기서 하나만 통과한다 — @Version이
            // 이미 다른 트랜잭션이 먼저 폐기한 걸 감지하면 예외를 던진다(RefreshToken.version).
            refreshTokenRepository.save(existing);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new InvalidRefreshTokenException();
        }

        sessionRepository.save(
            existing.getUserId(), command.deviceId(), refreshToken.hashedValue(), refreshToken.expiresAt());

        return new LoginResult(accessToken.value(), refreshToken.rawValue(), accessToken.expiresAt());
    }
}
