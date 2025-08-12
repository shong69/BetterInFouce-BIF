package com.sage.bif.user.repository;

import com.sage.bif.user.entity.Bif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BifRepository extends JpaRepository<Bif, Long> {

    Optional<Bif> findByNickname(String nickname);
    Optional<Bif> findByConnectionCode(String connectionCode);
    Optional<Bif> findBySocialLogin_SocialId(Long socialId);

}
