package me.aiyanxu.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import me.aiyanxu.cache.util.CacheUtil;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Liqingyan
 * @date 2021/11/15
 * @time 8:43 PM
 */

@Service
public class HugCacheHelper {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public <K,V> Map<K, V> batchGetFromLocalCache(List<K> list, Cache<K, V> cache, Function<List<K>, Map<K, V>> function) {
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }
        List<K> missingIds = new ArrayList<>(list);
        Map<K, V> cacheResult = new HashMap<>(cache.getAllPresent(list));
        missingIds.removeAll(cacheResult.keySet());
        if (!CollectionUtils.isEmpty(missingIds)) {
            Map<K, V> missingRecords = function.apply(missingIds);
            if (!CollectionUtils.isEmpty(missingRecords)) {
                cache.putAll(missingRecords);
                cacheResult.putAll(missingRecords);
            }
        }
        return cacheResult;
    }

    public <K, V> Map<K, V> batchGetFromRedisCache(List<K> list, Class<V> clazz, Function<List<K>, Map<K, V>> invokeFunction, MultiCacheConfig cacheConfig) {
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }

        List<K> finalList = list.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<K, V> cacheResult = new HashMap<>();
        List<K> keyInCache = new ArrayList<>();
        boolean enableCachePenetration = cacheConfig.isEnableCachePenetration();
        long expire = cacheConfig.getExpire();
        boolean enabled = CacheUtil.checkIsEnabled(cacheConfig);

        if (enabled) {
            List<String> redisKeys = finalList.stream()
                    .map(item -> StrUtil.isEmpty(cacheConfig.getKeySuffix()) ? CacheUtil.buildKey(cacheConfig.getKeyPrefix(), item) : CacheUtil.buildKey(cacheConfig.getKeyPrefix(), item, cacheConfig.getKeySuffix()))
                    .collect(Collectors.toList());
            List<Object> results = redisTemplate.opsForValue().multiGet(redisKeys);
            for (int i = 0; i < finalList.size(); i++) {
                K key = finalList.get(i);
                Object result = results.get(i);
                String value = String.valueOf(result);
                if (Objects.isNull(result)) {
                    continue;
                }
                if (StrUtil.isEmpty(value) && !enableCachePenetration) {
                    continue;
                }
                V item = null;
                if (StrUtil.isNotEmpty(value)) {
                    item = JSON.parseObject(value, clazz);
                }
                keyInCache.add(key);
                cacheResult.put(key, item);
            }
            if (!CollectionUtils.isEmpty(keyInCache)) {
                /**
                 * 缓存命中打点
                 */
            }
        }

        List<K> missingIds = finalList.stream()
                .filter(o -> !keyInCache.contains(o))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(missingIds)) {
            if (enabled) {
                /**
                 * 缓存未命中打点
                 */
            }
            Map<K, V> missingRecords = invokeFunction.apply(missingIds);
            Map<String, String> missingRedisCache = new HashMap<>();
            for (K missingId : missingIds) {
                String key = StrUtil.isEmpty(cacheConfig.getKeySuffix()) ? CacheUtil.buildKey(cacheConfig.getKeyPrefix(), missingId) : CacheUtil.buildKey(cacheConfig.getKeyPrefix(), missingId, cacheConfig.getKeyPrefix());
                V item = missingRecords.getOrDefault(missingId, null);
                if (Objects.nonNull(item)) {
                    cacheResult.put(missingId, item);
                    if (enabled) {
                        missingRedisCache.put(key, JSON.toJSONString(item));
                    }
                } else {
                    if (enableCachePenetration && enabled) {
                        missingRedisCache.put(key, "");
                    }
                    if (!cacheConfig.isFilterNull()) {
                        cacheResult.put(missingId, null);
                    }
                }
            }
            batchSetRedisCache(missingRedisCache, expire);
        }
        return cacheResult;
    }

    public <K, V> Map<K, List<V>> batchGetFromRedisCacheCollectionMode(List<K> list, Class<V> clazz, Function<List<K>, Map<K, List<V>>> invokeFunction, MultiCacheConfig cacheConfig) {
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }

        List<K> finalList = list.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<K, List<V>> cacheResult = new HashMap<>();
        List<K> keyInCache = new ArrayList<>();
        boolean enableCachePenetration = cacheConfig.isEnableCachePenetration();
        long expire = cacheConfig.getExpire();
        boolean enabled = CacheUtil.checkIsEnabled(cacheConfig);

        if (enabled) {
            List<String> redisKeys = finalList.stream()
                    .map(item -> StrUtil.isEmpty(cacheConfig.getKeySuffix()) ? CacheUtil.buildKey(cacheConfig.getKeyPrefix(), item) : CacheUtil.buildKey(cacheConfig.getKeyPrefix(), item, cacheConfig.getKeySuffix()))
                    .collect(Collectors.toList());
            List<Object> results = redisTemplate.opsForValue().multiGet(redisKeys);
            for (int i = 0; i < finalList.size(); i++) {
                K key = finalList.get(i);
                Object result = results.get(i);
                String value = String.valueOf(result);
                if (Objects.isNull(result)) {
                    continue;
                }
                if (StrUtil.isEmpty(value) && !enableCachePenetration) {
                    continue;
                }
                List<V> item = null;
                if (StrUtil.isNotEmpty(value)) {
                    item = JSON.parseArray(value, clazz);
                }
                keyInCache.add(key);
                cacheResult.put(key, item);
            }
            if (!CollectionUtils.isEmpty(keyInCache)) {
                /**
                 * 缓存命中打点
                 */
            }
        }

        List<K> missingIds = finalList.stream()
                .filter(o -> !keyInCache.contains(o))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(missingIds)) {
            if (enabled) {
                /**
                 * 缓存未命中打点
                 */
            }
            Map<K, List<V>> missingRecords = invokeFunction.apply(missingIds);
            Map<String, String> missingRedisCache = new HashMap<>();
            for (K missingId : missingIds) {
                String key = StrUtil.isEmpty(cacheConfig.getKeySuffix()) ? CacheUtil.buildKey(cacheConfig.getKeyPrefix(), missingId) : CacheUtil.buildKey(cacheConfig.getKeyPrefix(), missingId, cacheConfig.getKeyPrefix());
                List<V> item = missingRecords.getOrDefault(missingId, null);
                if (Objects.nonNull(item)) {
                    cacheResult.put(missingId, item);
                    if (enabled) {
                        missingRedisCache.put(key, JSON.toJSONString(item));
                    }
                } else {
                    if (enableCachePenetration && enabled) {
                        missingRedisCache.put(key, "");
                    }
                    if (!cacheConfig.isFilterNull()) {
                        cacheResult.put(missingId, null);
                    }
                }
            }
            batchSetRedisCache(missingRedisCache, expire);
        }
        return cacheResult;
    }

    private void batchSetRedisCache(Map<String, String> keyValueMap, long expire) {
        if (CollectionUtils.isEmpty(keyValueMap)) {
            return;
        }
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.openPipeline();
            for (String key : keyValueMap.keySet()) {
                if (key == null) {
                    continue;
                }
                byte[] redisKey = keySerializer.serialize(key);
                byte[] value = keySerializer.serialize(keyValueMap.get(key));
                connection.setEx(redisKey, expire, value);
            }
            return null;
        });
    }

}
