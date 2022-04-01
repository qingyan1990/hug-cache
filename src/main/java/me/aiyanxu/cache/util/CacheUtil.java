package me.aiyanxu.cache.util;

import cn.hutool.core.util.StrUtil;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import me.aiyanxu.cache.CacheConfig;
import me.aiyanxu.cache.MultiCacheConfig;
import me.aiyanxu.cache.ThreadLocalCacheSwitch;

import java.util.Objects;

public class CacheUtil {

    public static String buildKey(Object... params) {
        return StrUtil.join(":", params);
    }

    public static boolean checkIsEnabled(MultiCacheConfig cacheConfig) {
        boolean enabled = cacheConfig.isEnableCache();
        if (enabled) {
            if (StrUtil.isNotEmpty(cacheConfig.getApolloSwitch()) && StrUtil.isNotEmpty(cacheConfig.getApolloNamespace())) {
                Config config = ConfigService.getConfig(cacheConfig.getApolloNamespace());
                if (Objects.nonNull(config)) {
                    enabled = config.getBooleanProperty(cacheConfig.getApolloSwitch(), true);
                }
            }
        }
        return enabled;
    }

    public static boolean checkIsEnabled(CacheConfig cacheConfig) {
        if (Boolean.FALSE.equals(ThreadLocalCacheSwitch.getAndRemove())) {
            return false;
        }
        boolean enabled = true;
        if (StrUtil.isNotEmpty(cacheConfig.getApolloSwitch()) && StrUtil.isNotEmpty(cacheConfig.getApolloNamespace())) {
            Config config = ConfigService.getConfig(cacheConfig.getApolloNamespace());
            if (Objects.nonNull(config)) {
                enabled = config.getBooleanProperty(cacheConfig.getApolloSwitch(), true);
            }
        }
        return enabled;
    }
}
