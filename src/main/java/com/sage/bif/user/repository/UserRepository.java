package com.sage.bif.user.repository;

import com.sage.bif.user.entity.Bif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Bif, Long> {
} 