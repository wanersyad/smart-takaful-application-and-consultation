package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    List<PasswordResetToken> findByCustomerAndUsedAtIsNull(Customer customer);

    List<PasswordResetToken> findAllByCustomer(Customer customer);
}
