package me.aiyanxu.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Liqingyan
 * @date 3/10/21
 * @time 8:21 PM
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HugCacheable {

    String cacheName() default "";

    String key() default "";

    long expire() default 60L;

    String strategy() default "redis";

    String scene() default "default";

    boolean enableCachePenetration() default false;

    long maximumSize() default 4096L;

    String apolloNamespace() default "application";

    String apolloSwitch() default "";

    boolean collectionMode() default false;

    Class<?> targetClass() default Void.class;
}
