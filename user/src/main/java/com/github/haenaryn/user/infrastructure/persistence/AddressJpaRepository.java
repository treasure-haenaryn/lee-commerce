package com.github.haenaryn.user.infrastructure.persistence;

import com.github.haenaryn.user.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AddressJpaRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByUserId(Long userId);
}
