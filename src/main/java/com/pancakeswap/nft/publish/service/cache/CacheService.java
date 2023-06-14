package com.pancakeswap.nft.publish.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import static com.pancakeswap.nft.publish.service.cache.CacheStatus.*;
import static java.util.concurrent.TimeUnit.MINUTES;

@Service
public class CacheService {

    @Value("${nft.concurrent.processing.max.size}")
    private Integer maxCacheSize;
    private Cache<String, String> cache;

    @PostConstruct
    public void initializeCache() {
        cache = Caffeine.newBuilder()
                // assuming that this is the max amount of time that could be need to list a collection
                .expireAfterWrite(5, MINUTES)
                .maximumSize(maxCacheSize)
                .build();
    }

    public CacheStatus add(String key) {
        if (cache.getIfPresent(key) != null) {
            return ALREADY_CACHED;
        } else if (cache.estimatedSize() >= maxCacheSize) {
            return MAX_CACHE_SIZE_REACHED;
        } else {
            cache.put(key, String.valueOf(PROCESSING));
            return PROCESSING;
        }
    }

    public void remove(String key) {
        cache.invalidate(key);
    }
}
