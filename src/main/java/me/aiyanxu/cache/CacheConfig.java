package me.aiyanxu.cache;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Liqingyan
 * @date 3/29/21
 * @time 9:21 PM
 */

@Data
public class CacheConfig implements Serializable {

    private String cacheName;

    private String key;

    private long expire;

    private String scene;

    private String strategy;

    private long maximumSize = 4096L;

    private String apolloSwitch;

    private String apolloNamespace;

    private boolean collectionMode;

    private Class<?> targetClass;
}
