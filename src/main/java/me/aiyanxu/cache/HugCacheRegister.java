package me.aiyanxu.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author Liqingyan
 * @date 3/11/21
 * @time 3:50 PM
 */
public class HugCacheRegister {

    @Bean
    @Lazy
    public RedisCacheService redisCacheService(@Autowired RedisTemplate<String, Object> redisTemplate) {
        RedisCacheService redisCacheService = new RedisCacheService();
        redisCacheService.setRedisTemplate(redisTemplate);
        return redisCacheService;
    }

    @Bean
    public CacheAspect cacheAspect() {
        return new CacheAspect();
    }
}
