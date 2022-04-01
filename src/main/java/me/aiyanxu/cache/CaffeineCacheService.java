package me.aiyanxu.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Liqingyan
 * @date 3/29/21
 * @time 5:05 PM
 */
public class CaffeineCacheService implements CacheService {

    private final Cache<String, Object> instance;

    public CaffeineCacheService(CacheConfig cacheConfig) {
        this.instance = Caffeine.newBuilder()
                .maximumSize(cacheConfig.getMaximumSize())
                .expireAfterAccess(cacheConfig.getExpire(), TimeUnit.SECONDS)
                .softValues()
                .build();
    }

    @Override
    public boolean hasKey(String key) {
        return Objects.nonNull(instance.getIfPresent(key));
    }

    @Override
    public void set(String key, Object value, long time) {
        instance.put(key,value);
    }

    @Override
    public Object get(String key) {
        return instance.getIfPresent(key);
    }

    @Override
    public long del(String... key) {
        instance.invalidate(key);
        return 0;
    }

    @Override
    public boolean valid() {
        return true;
    }
}
