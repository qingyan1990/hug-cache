package me.aiyanxu.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import me.aiyanxu.cache.config.CacheStrategyEnum;
import me.aiyanxu.cache.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Liqingyan
 * @date 3/10/21
 * @time 8:30 PM
 */

@Slf4j
@Aspect
public class CacheAspect implements Ordered {

    private ExpressionParser parser = new SpelExpressionParser();

    private LocalVariableTableParameterNameDiscoverer discoverer = new LocalVariableTableParameterNameDiscoverer();

    private static Map<String, CacheService> localCacheServices = new ConcurrentHashMap<>();

    @Resource
    private RedisCacheService redisCacheService;

    @Pointcut("@annotation(com.yupaopao.hug.platform.cache.HugCacheable)")
    public void cacheable() {}

    @Pointcut("@annotation(com.yupaopao.hug.platform.cache.HugCachePut)")
    public void cachePut() {}

    @Pointcut(" @annotation(com.yupaopao.hug.platform.cache.HugCacheEvict)")
    public void cacheEvict() {}

    @Around("cacheable()")
    public Object cacheableAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Signature signature = joinPoint.getSignature();
        Method method = ((MethodSignature) signature).getMethod();
        Class returnType = ((MethodSignature) signature).getReturnType();
        HugCacheable cacheable = method.getAnnotation(HugCacheable.class);
        CacheConfig cacheConfig = getCacheConfig(cacheable);
        String redisKey = generateRedisKey(joinPoint, method, cacheConfig);
        boolean enableCachePenetration = cacheable.enableCachePenetration();
        long expire = cacheConfig.getExpire();
        boolean enabled = CacheUtil.checkIsEnabled(cacheConfig);
        CacheService cacheService = this.getCacheService(cacheConfig);

        if (StrUtil.isNotEmpty(redisKey) && cacheService.valid() && enabled) {
            Object redisObj = getFromCache(cacheService, redisKey, returnType, cacheConfig);
            if (Objects.nonNull(redisObj) || (cacheService.hasKey(redisKey) && enableCachePenetration)) {
                /**
                 * 命中打点
                 */
                return redisObj;
            } else {
                if (!cacheService.hasKey(redisKey)) {
                    /**
                     * 未命中打点
                     */
                }
            }
        }

        try {
            Object response = joinPoint.proceed();
            String value = generateReturnValue(response, enableCachePenetration, redisKey, cacheService, enabled);
            if (Objects.nonNull(value)) {
                cacheService.set(redisKey, value, expire);
            }
            return response;
        } catch (Throwable e) {
            String errorMsg = String.format("error occurred in XxqCacheable annotation, key: %s, error: %s", redisKey, e.getMessage());
            log.warn(errorMsg);
            throw e;
        }
    }

    @Around("cachePut()")
    public Object cachePutAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Signature signature = joinPoint.getSignature();
        Method method = ((MethodSignature) signature).getMethod();
        HugCachePut cachePut = method.getAnnotation(HugCachePut.class);
        CacheConfig cacheConfig = getCacheConfig(cachePut);
        String redisKey = generateRedisKey(joinPoint, method, cacheConfig);
        boolean enableCachePenetration = cachePut.enableCachePenetration();
        long expire = cacheConfig.getExpire();
        boolean enabled = CacheUtil.checkIsEnabled(cacheConfig);
        CacheService cacheService = this.getCacheService(cacheConfig);

        // 先将缓存时间缩短为2s
        if (cacheService.valid() && enabled) {
            cacheService.expire(redisKey, 2L);
        }

        try {
            Object response = joinPoint.proceed();
            String value = generateReturnValue(response, enableCachePenetration, redisKey, cacheService, enabled);
            if (Objects.nonNull(value)) {
                cacheService.set(redisKey, value, expire);
            }
            return response;
        } catch (Throwable e) {
            String errorMsg = String.format("error occurred in XxqCachePut annotation, key: %s, error: %s", redisKey, e.getMessage());
            log.warn(errorMsg);
            throw e;
        }
    }

    @Around("cacheEvict()")
    public Object cacheEvictAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Signature signature = joinPoint.getSignature();
        Method method = ((MethodSignature) signature).getMethod();
        HugCacheEvict cacheEvict = method.getAnnotation(HugCacheEvict.class);
        CacheConfig cacheConfig = getCacheConfig(cacheEvict);
        String redisKey = generateRedisKey(joinPoint, method, cacheConfig);

        CacheService cacheService = this.getCacheService(cacheConfig);

        if (cacheService.valid() && StrUtil.isNotEmpty(redisKey)) {
            cacheService.del(redisKey);
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            String errorMsg = String.format("error occurred in XxqCacheEvict annotation, key: %s, error: %s", redisKey, e.getMessage());
            log.warn(errorMsg);
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    private CacheService getCacheService(CacheConfig cacheConfig) {
        String strategy = cacheConfig.getStrategy();
        if (strategy.equalsIgnoreCase(CacheStrategyEnum.LOCAL.getStrategy())) {
            String scene = cacheConfig.getScene();
            CacheService target = localCacheServices.getOrDefault(scene, null);
            if (Objects.nonNull(target)) {
                return target;
            }
            target = new CaffeineCacheService(cacheConfig);
            localCacheServices.putIfAbsent(scene, target);
            return localCacheServices.get(scene);
        }
        return redisCacheService;
    }

    private Object getFromCache(CacheService cacheService, String redisKey, Class<?> clazz, CacheConfig cacheConfig) {
        if (!cacheService.valid()) {
            return null;
        }
        Object redisObj = cacheService.get(redisKey);
        if (redisObj != null) {
            String value = String.valueOf(redisObj);
            if (StrUtil.isEmpty(value)) {
                return null;
            }
            try {
                if (cacheConfig.isCollectionMode()) {
                    return JSON.parseArray(value, cacheConfig.getTargetClass());
                } else {
                    return JSON.parseObject(value, clazz);
                }
            } catch (Exception e) {
                log.error("fastJson parse error in hug-cache json:{} class:{} errMsg:{}", value, clazz.getName(), e.getMessage());
                return null;
            }
        }
        return null;
    }

    private String generateRedisKey(ProceedingJoinPoint joinPoint, Method method, CacheConfig cacheConfig) {
        String cacheName = cacheConfig.getCacheName();
        if (StrUtil.isEmpty(cacheName)) {
            return cacheName;
        }

        String annotationKey = cacheConfig.getKey();
        if (StrUtil.isEmpty(annotationKey)) {
            return cacheName;
        }
        String[] params = discoverer.getParameterNames(method);
        if (Objects.isNull(params)) {
            return cacheName;
        }
        Object[] arguments = joinPoint.getArgs();
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < params.length; i++) {
            context.setVariable(params[i], arguments[i]);
        }
        try {
            Expression expression = parser.parseExpression(annotationKey);
            Object expressionValue = expression.getValue(context);
            if (Objects.isNull(expressionValue)) {
                return cacheName;
            }
            return CacheUtil.buildKey(cacheName, String.valueOf(expressionValue));
        } catch (Exception e) {
            return "";
        }
    }

    private <T extends Annotation> CacheConfig getCacheConfig(T annotation) {
        CacheConfig cacheConfig = new CacheConfig();
        if (annotation instanceof HugCacheable) {
            HugCacheable hugCacheable = (HugCacheable) annotation;
            cacheConfig.setExpire(hugCacheable.expire());
            cacheConfig.setStrategy(hugCacheable.strategy());
            cacheConfig.setKey(hugCacheable.key());
            cacheConfig.setCacheName(hugCacheable.cacheName());
            cacheConfig.setScene(hugCacheable.scene());
            cacheConfig.setMaximumSize(hugCacheable.maximumSize());
            cacheConfig.setApolloSwitch(hugCacheable.apolloSwitch());
            cacheConfig.setApolloNamespace(hugCacheable.apolloNamespace());
            cacheConfig.setCollectionMode(hugCacheable.collectionMode());
            cacheConfig.setTargetClass(hugCacheable.targetClass());
        }
        if (annotation instanceof HugCachePut) {
            HugCachePut hugCachePut = (HugCachePut) annotation;
            cacheConfig.setExpire(hugCachePut.expire());
            cacheConfig.setStrategy(hugCachePut.strategy());
            cacheConfig.setKey(hugCachePut.key());
            cacheConfig.setCacheName(hugCachePut.cacheName());
            cacheConfig.setScene(hugCachePut.scene());
            cacheConfig.setMaximumSize(hugCachePut.maximumSize());
            cacheConfig.setApolloSwitch(hugCachePut.apolloSwitch());
            cacheConfig.setApolloNamespace(hugCachePut.apolloNamespace());
        }
        if (annotation instanceof HugCacheEvict) {
            HugCacheEvict hugCacheEvict = (HugCacheEvict) annotation;
            cacheConfig.setStrategy(hugCacheEvict.strategy());
            cacheConfig.setKey(hugCacheEvict.key());
            cacheConfig.setCacheName(hugCacheEvict.cacheName());
            cacheConfig.setScene(hugCacheEvict.scene());
        }
        return cacheConfig;
    }

    private String generateReturnValue(Object response, boolean enableCachePenetration, String redisKey, CacheService cacheService, boolean enabled) {
        String value = null;
        if ((!ObjectUtils.isEmpty(response) || enableCachePenetration) && StrUtil.isNotEmpty(redisKey) && cacheService.valid() && enabled) {
            if (ObjectUtils.isEmpty(response)) {
                value = "";
            } else {
                value = JSON.toJSONString(response);
            }
        }
        return value;
    }

}
