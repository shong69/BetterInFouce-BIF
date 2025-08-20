package com.sage.bif.notification.config;

import com.sage.bif.notification.exception.WebPushConfigurationException;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
public class WebPushConfig {

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    @Bean
    public PushService pushService() throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (vapidPublicKey.isEmpty() || vapidPrivateKey.isEmpty() || vapidSubject.isEmpty()) {
            throw new WebPushConfigurationException("VAPID 키가 설정되지 않았습니다. vapid.public-key, vapid.private-key, vapid.subject를 설정해주세요.");
        }

        PushService pushService = new PushService();
        pushService.setPublicKey(vapidPublicKey);
        pushService.setPrivateKey(vapidPrivateKey);
        pushService.setSubject(vapidSubject);

        return pushService;
    }

}
