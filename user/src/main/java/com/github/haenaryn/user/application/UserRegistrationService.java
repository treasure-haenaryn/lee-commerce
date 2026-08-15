package com.github.haenaryn.user.application;

import com.github.haenaryn.user.domain.Email;
import com.github.haenaryn.user.domain.PasswordEncoder;
import com.github.haenaryn.user.domain.PasswordHash;
import com.github.haenaryn.user.domain.RawPassword;
import com.github.haenaryn.user.domain.User;
import com.github.haenaryn.user.domain.UserRepository;
import com.github.haenaryn.user.domain.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterUserResult register(RegisterUserCommand command) {
        Email email = new Email(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(command.email());
        }

        RawPassword rawPassword = new RawPassword(command.rawPassword());
        PasswordHash passwordHash = passwordEncoder.encode(rawPassword);

        User user = User.register(email, passwordHash, command.name(), command.phone());
        User saved = userRepository.save(user);

        return new RegisterUserResult(saved.getId(), saved.getEmail().value(), saved.getName());
    }
}
