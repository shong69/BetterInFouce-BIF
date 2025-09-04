package com.sage.bif.notification.repository;

import com.sage.bif.notification.entity.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

    List<WebPushSubscription> findAllByBif_BifId(Long bifId);

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    void deleteByBif_BifId(Long bifId);

}
