package gov.cms.dpc.api.auth.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@Singleton
public class CaffeineJTICache implements IJTICache {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineJTICache.class);

    private final Cache<String, Boolean> cache;

    @Inject
    public CaffeineJTICache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public boolean isJTIOk(String jti, boolean persist) {
        final Boolean isPresent = this.cache.getIfPresent(jti);

        // If the JTI is present in the cache, that means it's being replayed. Which is no go
        if (isPresent == null) {
            if (persist)
                this.cache.put(jti, true);
            return true;
        }
        logger.warn("JTI {} is being replayed", jti);
        return false;
    }
}
