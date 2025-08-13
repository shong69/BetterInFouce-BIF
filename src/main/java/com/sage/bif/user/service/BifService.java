package com.sage.bif.user.service;

import com.sage.bif.user.entity.Bif;

import java.util.Optional;

public interface BifService {

    Bif registerBySocialId(Long socialId, String email);
    Optional<Bif> findBySocialId(Long socialId);
    void updateNickname(Long socialId, String newNickname);
    void deleteBySocialId(Long socialId);

}
