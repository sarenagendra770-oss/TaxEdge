package com.taxedge.modules.user.repository;

import com.taxedge.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobile(String mobile);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);

    /** find another user (different mobile) with the same PAN — used to block re-registration. */
    Optional<User> findFirstByPanAndMobileNot(String pan, String mobile);
    /** find another user (different mobile) with the same Aadhaar. */
    Optional<User> findFirstByAadhaarAndMobileNot(String aadhaar, String mobile);
}
