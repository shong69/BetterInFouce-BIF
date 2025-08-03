package com.sage.bif.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // @Value("${spring.data.redis.host}")
    // private String host;

    // @Value("${spring.data.redis.port}")
    // private int port;

    // @Value("${spring.data.redis.password}")
    // private String password;

    // @Bean
    // public RedisConnectionFactory redisConnectionFactory() {
    //     RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
    //     redisStandaloneConfiguration.setHostName(host);
    //     redisStandaloneConfiguration.setPort(port);
    //     redisStandaloneConfiguration.setPassword(password);
    //     redisStandaloneConfiguration.setUsername("default");

    //     LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
    //                                                 .useSsl()
    //                                                 .build();

    //     return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    // }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
