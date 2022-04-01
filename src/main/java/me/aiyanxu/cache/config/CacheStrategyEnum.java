package me.aiyanxu.cache.config;

/**
 * @author Liqingyan
 * @date 2022/3/9
 * @time 2:42 PM
 */
public enum CacheStrategyEnum {
    REDIS("redis", "redis缓存"),
    LOCAL("local", "本地缓存")
    ;

    private String strategy;

    private String desc;

    CacheStrategyEnum(String strategy, String desc) {
        this.strategy = strategy;
        this.desc = desc;
    }

    public String getStrategy() {
        return strategy;
    }
}
