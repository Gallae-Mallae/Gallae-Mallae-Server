package com.practice.OAuth2.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories // 리프레시 토큰 저장용 - Redis 리포지토리 활성화 (CrudRepository 상속 받으면 활성화)
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    // Redis 연결용
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    // 재발급 시 Double-checked lock 에서 Result Cashing 용
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // Key, Value 직렬화 설정 (문자열 위주로 저장하므로 StringSerializer 사용)
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // value 는 유예기간 리프레시토큰용 JSON 객체
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return redisTemplate;
    }

    // 재발급 시 동시성 제어를 위한 Redisson lock 발급용
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // Redis 주소 앞에 "redis://"
        String address = "redis://" + host + ":" + port;

        config.useSingleServer()
                .setAddress(address);

        return Redisson.create(config);
    }

    // 조회수 정렬 및 조회를 위한 ZSET, 랭킹 시스템(ZSET)은 Key(랭킹 이름)와 Member(관광지 ID)가 모두 문자열이므로 RedisTemplate 미사용
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
