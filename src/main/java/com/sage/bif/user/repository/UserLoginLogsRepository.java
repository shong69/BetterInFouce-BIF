package com.sage.bif.user.repository;

import com.sage.bif.user.entity.UserLoginLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserLoginLogsRepository extends JpaRepository<UserLoginLogs, Long> {

    @Query("SELECT u FROM UserLoginLogs u WHERE u.socialId = :socialId AND u.logoutAt IS NULL ORDER BY u.loginAt DESC LIMIT 1")
    Optional<UserLoginLogs> findLatestActiveSession(@Param("socialId") Long socialId);

    @Modifying
    @Query("UPDATE UserLoginLogs u SET u.deletableAfter = :deletableAfter WHERE u.socialId = :socialId")
    int scheduleUserLogsDeletion(@Param("socialId") Long socialId, @Param("deletableAfter") LocalDateTime deletableAfter);

    @Query("SELECT COUNT(u) FROM UserLoginLogs u WHERE u.deletableAfter IS NOT NULL AND u.deletableAfter <= :currentTime")
    long countExpiredLogs(@Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Query("DELETE FROM UserLoginLogs u WHERE u.deletableAfter IS NOT NULL AND u.deletableAfter <= :currentTime")
    int deleteExpiredLogs(@Param("currentTime") LocalDateTime currentTime);

}
