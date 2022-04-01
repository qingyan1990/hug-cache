package me.aiyanxu.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Liqingyan
 * @date 3/10/21
 * @time 8:29 PM
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HugCacheEvict {

    String cacheName() default "";

    String key() default "";

    String strategy() default "redis";

    String scene() default "default";
}
