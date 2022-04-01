package me.aiyanxu.cache;

import lombok.Data;
import java.io.Serializable;

/**
 * @author Liqingyan
 * @date 2021/12/14
 * @time 2:28 PM
 */

@Data
public class MultiCacheConfig implements Serializable {

    private String keyPrefix;

    private String keySuffix = "";

    private Long expire = 60L;

    private boolean enableCachePenetration = true;

    private boolean filterNull = true;

    private String apolloNamespace = "application";

    private String apolloSwitch = "";

    private boolean enableCache = true;
}
