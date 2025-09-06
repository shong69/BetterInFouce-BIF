package com.sage.bif.user.repository;

import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    Optional<Guardian> findByNickname(String nickname);

    Optional<Guardian> findBySocialLogin_SocialId(Long socialId);

    List<Guardian> findByBif_BifId(Long bifId);

    @Query("SELECT g.bif.bifId FROM Guardian g WHERE g.socialLogin.socialId = :socialId")
    Optional<Long> findBifIdBySocialId(@Param("socialId") Long socialId);

    boolean existsByBif(Bif bif);

}
