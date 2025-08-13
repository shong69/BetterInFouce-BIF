package com.sage.bif.user.service;

import java.util.Optional;

import com.sage.bif.user.entity.Guardian;

public interface GuardianService {

    Guardian registerBySocialId(Long socialId, String email, String connectionCode);
    Optional<Guardian> findBySocialId(Long socialId);
    void updateNickname(Long socialId, String newNickname);
    void deleteBySocialId(Long socialId);

}
