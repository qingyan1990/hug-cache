package me.aiyanxu.cache;

/**
 * @author Liqingyan
 * @date 3/29/21
 * @time 4:58 PM
 */

public interface CacheService {

    /**
     * 检查缓存key是否存在
     *
     * @param key
     * @return
     */
    boolean hasKey(String key);

    /**
     * 设置缓存
     *
     * @param key
     * @param value
     * @param time
     * @return
     */
    void set(String key, Object value, long time);

    /**
     * 获取缓存
     *
     * @param key
     * @return
     */
    Object get(String key);

    /**
     * (批量)删除缓存
     *
     * @param key
     * @return
     */
    long del(String... key);

    /**
     * 设置缓存过期时间
     * @param key
     * @param time
     * @return
     */
    default boolean expire(String key, long time) {
        return true;
    }

    boolean valid();
}
