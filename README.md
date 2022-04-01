# HUG-CACHE
仿照spring cache实现的缓存框架

## 单一缓存
### 基本原理
原生的spring cache中提供了`@Cacheable`、`@CachePut`、`@CacheEvict`三种常用注解，hug-cache框架中提供了对应的实现，即`@HugCacheable`、`@HugCachePut`、`@HugCacheEvict`。

**@HugCacheable伪代码**
```text
Object cacheResult = getFromCache(key);
if (cacheResult != null) {
    return cacheResult;
}
Object result = joinPoint.proceed();
cache.set(key, result);
return result;
```
**@HugCachePut伪代码**
```text
Object result = joinPoint.proceed();
cache.set(key, result);
return result;
```
**@HugCacheEvict伪代码**
```text
Object result = joinPoint.proceed();
cache.delete(key);
```
目前hug-cache主要针对Cacheable做了扩展，其余两个注解使用频率不高，实现了基本功能的对齐。
### 开启缓存注解支持
通过`@EnableHugCache`注解来启用缓存框架，一般会直接注解在启动类上
### 基本注解参数说明
以HugCacheable注解为例
```java
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
```
cacheName和key在一起确定了缓存中的key。若缓存中的key
为`hug:gift:1`，则cacheName为`hug:gift`，key为`1`。
cacheName和key之间用`:`做连接，暂不提供其他选项。
key参数支持SPEL表达式，可以根据被注解方法的参数或返回值灵活设置。

expire为设置缓存过期时间，单位为秒。

### 扩展点
#### 支持本地缓存
hug-cache框架针对redis缓存和本地缓存提供了统一抽象实现，默认采用redis进行缓存，本地缓存是基于caffeine实现。
使用本地缓存的话，strategy参数要设置为`local`，此时scene参数代表本地缓存的名称，在一个运行实例上全局唯一，maximumSize参数用来指定本地缓存的大小限制。
#### 处理缓存穿透
enableCachePenetration参数为true的话，当方法返回为null时，框架会使用一个空字符串作为空值缓存，后续请求过来时，若空值缓存存在，则不会去访问db。
#### 支持在apollo上动态配置缓存开关
当缓存方法发生变动，尤其是返回值发生大的变化时，我们需要将缓存临时关闭。hug-cache框架支持在apollo上配置缓存开关，实现缓存的动态开关闭，
apolloNamespace和apolloSwitch参数一起可实现此功能。

apolloNamespace为配置开关对应的命名空间，apolloSwitch为具体的配置项名称，apolloSwitch不为空字符串时表明开启了此功能，apolloSwitch对应的
配置项必须是布尔类型。
#### 支持方法返回值为List
当方法的返回值为List时，为了使注解正常工作，我们需要把collectionMode设为true；
targetClass设置为List中元素的类型。
#### 特定请求支持不走缓存实现
若有特定方法不希望从缓存中读取数据，可以通过设置ThreadLocalCacheSwitch.set(false)来实现。

## 批量缓存
基于注解很难实现批量缓存的功能，hug-cache框架中是在HugCacheHelper中通过JAVA泛型和函数机制来实现了对批量缓存的支持。

batchGetFromRedisCache为最常使用的方法，下面已此为例进行说明
```text
public <K, V> Map<K, V> batchGetFromRedisCache(List<K> list, Class<V> clazz, Function<List<K>, Map<K, V>> invokeFunction, MultiCacheConfig cacheConfig)
```
假设我们现在要根据uid list来批量获取用户的基本信息（对应结构体为UserInfo），那么list为uid list，K为Long型，clazz为UserInfo.class。
invokeFunction为批量获取用户信息的实现方法，参数为`List<Long>`，返回值为`Map<Long,UserInfo>`。
```java
public class MultiCacheConfig implements Serializable {

    @NotNull
    private String keyPrefix;

    private String keySuffix = "";

    private Long expire = 60L;

    private boolean enableCachePenetration = true;

    private boolean filterNull = true;

    private String apolloNamespace = "application";

    private String apolloSwitch = "";

    private boolean enableCache = true;
}
```
keyPrefix、keySuffix与参数列表中的元素一起构成了缓存中的key。
若keyPrefix为`user`, 用户uid为`123456`，keySuffix为`normal`，则
缓存中的key为`user:123456:normal`，当keySuffix为空字符串时不会被计入key中。

expire设置缓存过期时间，单位为秒。

enableCachePenetration参数同样是用来处理缓存穿透的场景。

apolloNamespace和apolloSwitch用来配置动态开关。

enableCache可以针对单一请求，精确控制是否走缓存实现，优先级在apollo开关之上。

filterNull是用来配置返回的map中是否包含value为null的项，默认为true会过滤。

HugCacheHelper中同样提供了
```text
public <K, V> Map<K, List<V>> batchGetFromRedisCacheCollectionMode(List<K> list, Class<V> clazz, Function<List<K>, Map<K, List<V>>> invokeFunction, MultiCacheConfig cacheConfig)
```
来处理返回值为`Map<K, List<V>>`的情况。







