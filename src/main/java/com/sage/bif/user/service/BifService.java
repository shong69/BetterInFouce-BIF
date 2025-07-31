package com.sage.bif.user.service;

import com.sage.bif.user.entity.Bif;

import java.util.Optional;

public interface BifService {

    Bif registerBySocialId(Long socialId, String email);
    Optional<Bif> findByConnectionCode(String connectionCode);
    Optional<Bif> findBySocialId(Long socialId);
}