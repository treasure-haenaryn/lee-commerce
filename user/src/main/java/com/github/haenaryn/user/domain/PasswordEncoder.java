package com.github.haenaryn.user.domain;

// 구현(BCrypt 등)은 Infrastructure에 둔다 — Domain은 추상화만 선언해서 해싱 알고리즘이
// 바뀌어도 Domain 코드는 영향받지 않게 한다.
public interface PasswordEncoder {

    PasswordHash encode(RawPassword rawPassword);

    boolean matches(RawPassword rawPassword, PasswordHash passwordHash);
}
