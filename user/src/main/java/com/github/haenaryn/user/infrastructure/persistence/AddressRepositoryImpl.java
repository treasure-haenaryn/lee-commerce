package com.github.haenaryn.user.infrastructure.persistence;

import com.github.haenaryn.user.domain.Address;
import com.github.haenaryn.user.domain.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class AddressRepositoryImpl implements AddressRepository {

    private final AddressJpaRepository jpaRepository;

    @Override
    public Address save(Address address) {
        return jpaRepository.save(address);
    }

    @Override
    public Optional<Address> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Address> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId);
    }
}
