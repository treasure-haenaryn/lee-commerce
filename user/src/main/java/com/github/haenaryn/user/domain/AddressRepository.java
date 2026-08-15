package com.github.haenaryn.user.domain;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {

    Address save(Address address);

    Optional<Address> findById(Long id);

    List<Address> findAllByUserId(Long userId);
}
