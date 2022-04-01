package me.aiyanxu.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Liqingyan
 * @date 3/29/21
 * @time 5:02 PM
 */

@Slf4j
public class RedisCacheService implements CacheService {

    private RedisTemplate<String, Object> redisTemplate;

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void set(String key, Object value, long time) {
        redisTemplate.opsForValue().set(key, value, time);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public long del(String... key) {
        Long result = redisTemplate.delete(Arrays.asList(key));
        return result == null ? 0L : result;
    }

    @Override
    public boolean valid() {
        boolean valid = redisTemplate != null;
        if (!valid) {
            log.error("启用缓存注解需要注入redisTemplate");
        }
        return valid;
    }

    @Override
    public boolean expire(String key, long time) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, time, TimeUnit.SECONDS));
    }
}
