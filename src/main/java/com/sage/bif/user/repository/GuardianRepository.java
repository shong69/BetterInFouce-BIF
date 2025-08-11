package com.sage.bif.user.repository;

import com.sage.bif.user.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    Optional<Guardian> findByNickname(String nickname);
    Optional<Guardian> findBySocialLogin_SocialId(Long socialId);

}
