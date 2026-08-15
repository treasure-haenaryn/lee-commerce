package com.github.haenaryn.user.application;

import com.github.haenaryn.user.domain.Email;
import com.github.haenaryn.user.domain.PasswordEncoder;
import com.github.haenaryn.user.domain.RawPassword;
import com.github.haenaryn.user.domain.RefreshToken;
import com.github.haenaryn.user.domain.RefreshTokenRepository;
import com.github.haenaryn.user.domain.User;
import com.github.haenaryn.user.domain.UserRepository;
import com.github.haenaryn.user.domain.exception.AuthenticationFailedException;
import com.github.haenaryn.user.domain.exception.InvalidPasswordException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenIssuer tokenIssuer;
    private final SessionRepository sessionRepository;

    @Transactional
    public LoginResult login(LoginCommand command) {
        Email email = new Email(command.email());
        User user = userRepository.findByEmail(email)
            .orElseThrow(AuthenticationFailedException::new);

        // 비밀번호 형식이 규칙(길이)에 안 맞는 경우도 AuthenticationFailedException으로 합친다 —
        // 계정이 없을 때와 다른 예외가 나가면, 그 차이만으로 등록된 이메일인지 추측할 수 있다.
        boolean matches;
        try {
            RawPassword rawPassword = new RawPassword(command.rawPassword());
            matches = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        } catch (InvalidPasswordException e) {
            matches = false;
        }
        if (!matches) {
            throw new AuthenticationFailedException();
        }

        IssuedAccessToken accessToken = tokenIssuer.issueAccessToken(user.getId());
        IssuedRefreshToken refreshToken = tokenIssuer.issueRefreshToken();

        RefreshToken refreshTokenEntity = RefreshToken.issue(
            user.getId(), refreshToken.hashedValue(), Instant.now(), refreshToken.expiresAt());
        refreshTokenRepository.save(refreshTokenEntity);

        sessionRepository.save(user.getId(), command.deviceId(), refreshToken.hashedValue(), refreshToken.expiresAt());

        return new LoginResult(accessToken.value(), refreshToken.rawValue(), accessToken.expiresAt());
    }
}
