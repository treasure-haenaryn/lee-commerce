package com.github.haenaryn.user.infrastructure.session;

import com.github.haenaryn.user.application.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class RedissonSessionRepository implements SessionRepository {

    private final RedissonClient redissonClient;

    @Override
    public void save(Long userId, String deviceId, String refreshTokenHash, Instant expiresAt) {
        RBucket<String> bucket = redissonClient.getBucket(key(userId, deviceId));
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        bucket.set(refreshTokenHash, ttl);
    }

    @Override
    public void revoke(Long userId, String deviceId) {
        redissonClient.getBucket(key(userId, deviceId)).delete();
    }

    @Override
    public Optional<String> findRefreshTokenHash(Long userId, String deviceId) {
        RBucket<String> bucket = redissonClient.getBucket(key(userId, deviceId));
        return Optional.ofNullable(bucket.get());
    }

    private String key(Long userId, String deviceId) {
        return "session:%d:%s".formatted(userId, deviceId);
    }
}
