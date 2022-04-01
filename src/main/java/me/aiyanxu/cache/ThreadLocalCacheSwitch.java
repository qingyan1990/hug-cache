package me.aiyanxu.cache;

/**
 * @author Liqingyan
 * @date 2022/3/11
 * @time 4:20 PM
 */
public class ThreadLocalCacheSwitch {

    private static ThreadLocal<Boolean> cacheSwitch = new ThreadLocal<>();

    public static void set(Boolean enabled) {
        ThreadLocalCacheSwitch.cacheSwitch.set(enabled);
    }

    public static Boolean get() {
        return ThreadLocalCacheSwitch.cacheSwitch.get();
    }

    public static Boolean getAndRemove() {
        Boolean enabled = ThreadLocalCacheSwitch.cacheSwitch.get();
        ThreadLocalCacheSwitch.cacheSwitch.remove();// 防止内存泄漏
        return enabled;
    }
}
