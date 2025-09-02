package com.polytype.migrator.core.cache;

import com.polytype.migrator.core.logging.PolyTypeLogger;
import com.polytype.migrator.core.config.PolyTypeConfig;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * High-performance multi-level caching system for PolyType.
 * Supports LRU eviction, TTL expiration, memory monitoring, and cache statistics.
 */
public class PolyTypeCache {
    
    public enum CacheType {
        TRANSLATION_RESULT,    // Translated code cache
        AST_CACHE,            // Parsed AST cache
        PATTERN_CACHE,        // ML pattern recognition cache
        SIMILARITY_CACHE,     // Semantic similarity cache
        BINARY_ANALYSIS,      // Binary analysis results
        METADATA_CACHE        // File metadata cache
    }
    
    public static class CacheEntry<T> {
        private final String key;
        private final T value;
        private final long createTime;
        private final long lastAccess;
        private final long ttl;
        private final int size;
        private volatile int accessCount;
        
        public CacheEntry(String key, T value, long ttl) {
            this.key = key;
            this.value = value;
            this.createTime = System.currentTimeMillis();
            this.lastAccess = this.createTime;
            this.ttl = ttl;
            this.size = estimateSize(value);
            this.accessCount = 0;
        }
        
        private int estimateSize(T value) {
            if (value == null) return 0;
            if (value instanceof String) return ((String) value).length() * 2; // UTF-16
            if (value instanceof byte[]) return ((byte[]) value).length;
            if (value instanceof Collection) return ((Collection<?>) value).size() * 50; // Estimate
            if (value instanceof Map) return ((Map<?, ?>) value).size() * 100; // Estimate
            return 100; // Default estimate
        }
        
        public String getKey() { return key; }
        public T getValue() { return value; }
        public long getCreateTime() { return createTime; }
        public long getLastAccess() { return lastAccess; }
        public long getTtl() { return ttl; }
        public int getSize() { return size; }
        public int getAccessCount() { return accessCount; }
        
        public boolean isExpired() {
            return ttl > 0 && (System.currentTimeMillis() - createTime) > ttl;
        }
        
        public void recordAccess() {
            accessCount++;
        }
        
        public long getAge() {
            return System.currentTimeMillis() - createTime;
        }
    }
    
    public static class CacheStats {
        private volatile long hits = 0;
        private volatile long misses = 0;
        private volatile long evictions = 0;
        private volatile long expirations = 0;
        private volatile long totalSize = 0;
        private volatile int entryCount = 0;
        
        public void recordHit() { hits++; }
        public void recordMiss() { misses++; }
        public void recordEviction() { evictions++; }
        public void recordExpiration() { expirations++; }
        public void setTotalSize(long size) { totalSize = size; }
        public void setEntryCount(int count) { entryCount = count; }
        
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public long getExpirations() { return expirations; }
        public long getTotalSize() { return totalSize; }
        public int getEntryCount() { return entryCount; }
        
        public double getHitRatio() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }
        
        public void reset() {
            hits = misses = evictions = expirations = totalSize = entryCount = 0;
        }
    }
    
    private final CacheType type;
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final CacheStats stats = new CacheStats();
    private final PolyTypeLogger logger = PolyTypeLogger.getLogger(PolyTypeCache.class);
    private final PolyTypeConfig config = PolyTypeConfig.getInstance();
    
    // Configuration
    private volatile int maxSize;
    private volatile long defaultTtl;
    private volatile boolean enabled;
    
    // Background cleanup
    private final ScheduledExecutorService cleanupExecutor;
    
    public PolyTypeCache(CacheType type) {
        this.type = type;
        loadConfiguration();
        
        // Initialize cleanup executor
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PolyTypeCache-Cleanup-" + type);
            t.setDaemon(true);
            return t;
        });
        
        // Start cleanup task
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 60, 60, TimeUnit.SECONDS);
        
        logger.info(PolyTypeLogger.LogCategory.CACHE, 
                   "Initialized cache: " + type + " (maxSize=" + maxSize + ", ttl=" + defaultTtl + "ms)");
    }
    
    private void loadConfiguration() {
        this.enabled = config.getBoolean(PolyTypeConfig.ENABLE_CACHE);
        this.maxSize = config.getInt(PolyTypeConfig.CACHE_MAX_SIZE);
        this.defaultTtl = config.getInt(PolyTypeConfig.CACHE_EXPIRE_MINUTES) * 60 * 1000L;
    }
    
    public <T> void put(String key, T value) {
        put(key, value, defaultTtl);
    }
    
    @SuppressWarnings("unchecked")
    public <T> void put(String key, T value, long ttl) {
        if (!enabled || key == null || value == null) {
            return;
        }
        
        String hashedKey = hashKey(key);
        CacheEntry<T> entry = new CacheEntry<>(hashedKey, value, ttl);
        
        lock.writeLock().lock();
        try {
            // Check if we need to evict entries
            if (cache.size() >= maxSize) {
                evictLRU();
            }
            
            CacheEntry<?> oldEntry = cache.put(hashedKey, entry);
            updateStats();
            
            if (oldEntry != null) {
                logger.trace(PolyTypeLogger.LogCategory.CACHE, 
                           "Replaced cache entry: " + type + ":" + hashedKey);
            } else {
                logger.trace(PolyTypeLogger.LogCategory.CACHE, 
                           "Added cache entry: " + type + ":" + hashedKey + " (size=" + entry.getSize() + ")");
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> valueType) {
        if (!enabled || key == null) {
            stats.recordMiss();
            return null;
        }
        
        String hashedKey = hashKey(key);
        
        lock.readLock().lock();
        try {
            CacheEntry<?> entry = cache.get(hashedKey);
            
            if (entry == null) {
                stats.recordMiss();
                logger.trace(PolyTypeLogger.LogCategory.CACHE, 
                           "Cache miss: " + type + ":" + hashedKey);
                return null;
            }
            
            if (entry.isExpired()) {
                // Remove expired entry (upgrade to write lock)
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cache.remove(hashedKey);
                    stats.recordExpiration();
                    updateStats();
                    logger.trace(PolyTypeLogger.LogCategory.CACHE, 
                               "Expired cache entry: " + type + ":" + hashedKey);
                } finally {
                    lock.writeLock().unlock();
                }
                stats.recordMiss();
                return null;
            }
            
            entry.recordAccess();
            stats.recordHit();
            
            logger.trace(PolyTypeLogger.LogCategory.CACHE, 
                       "Cache hit: " + type + ":" + hashedKey + " (age=" + entry.getAge() + "ms)");
            
            return (T) entry.getValue();
            
        } finally {
            try {
                lock.readLock().unlock();
            } catch (IllegalMonitorStateException e) {
                // Lock not held by current thread, ignore
            }
        }
    }
    
    public boolean containsKey(String key) {
        if (!enabled || key == null) {
            return false;
        }
        
        String hashedKey = hashKey(key);
        
        lock.readLock().lock();
        try {
            CacheEntry<?> entry = cache.get(hashedKey);
            return entry != null && !entry.isExpired();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void remove(String key) {
        if (!enabled || key == null) {
            return;
        }
        
        String hashedKey = hashKey(key);
        
        lock.writeLock().lock();
        try {
            CacheEntry<?> removed = cache.remove(hashedKey);
            if (removed != null) {
                updateStats();
                logger.trace(PolyTypeLogger.LogCategory.CACHE, 
                           "Removed cache entry: " + type + ":" + hashedKey);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            int size = cache.size();
            cache.clear();
            updateStats();
            logger.info(PolyTypeLogger.LogCategory.CACHE, 
                       "Cleared cache: " + type + " (" + size + " entries)");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void cleanup() {
        if (!enabled) return;
        
        lock.writeLock().lock();
        try {
            int initialSize = cache.size();
            int expired = 0;
            
            // Remove expired entries
            Iterator<Map.Entry<String, CacheEntry<?>>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry<?>> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    expired++;
                }
            }
            
            if (expired > 0) {
                stats.expirations += expired;
                updateStats();
                logger.debug(PolyTypeLogger.LogCategory.CACHE, 
                           "Cleanup: " + type + " - removed " + expired + " expired entries");
            }
            
            // Check memory usage and evict if needed
            if (cache.size() > maxSize * 0.9) { // Start evicting at 90% capacity
                int toEvict = (int) (cache.size() - maxSize * 0.8); // Evict down to 80%
                evictLRU(toEvict);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void evictLRU() {
        evictLRU(1);
    }
    
    private void evictLRU(int count) {
        if (cache.isEmpty()) return;
        
        // Sort by last access time (LRU)
        List<Map.Entry<String, CacheEntry<?>>> entries = new ArrayList<>(cache.entrySet());
        entries.sort(Comparator.comparing(e -> e.getValue().getLastAccess()));
        
        int evicted = 0;
        for (Map.Entry<String, CacheEntry<?>> entry : entries) {
            if (evicted >= count) break;
            
            cache.remove(entry.getKey());
            evicted++;
            stats.recordEviction();
            
            logger.trace(PolyTypeLogger.LogCategory.CACHE, 
                       "Evicted LRU entry: " + type + ":" + entry.getKey());
        }
        
        if (evicted > 0) {
            updateStats();
            logger.debug(PolyTypeLogger.LogCategory.CACHE, 
                       "Evicted " + evicted + " LRU entries from " + type);
        }
    }
    
    private void updateStats() {
        long totalSize = cache.values().stream()
                             .mapToInt(CacheEntry::getSize)
                             .sum();
        
        stats.setTotalSize(totalSize);
        stats.setEntryCount(cache.size());
    }
    
    private String hashKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16); // Use first 16 characters
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(key.hashCode());
        }
    }
    
    // Cache key builders for common use cases
    public static String buildTranslationKey(String sourceCode, String fromLang, String toLang) {
        return "trans:" + fromLang + ">" + toLang + ":" + sourceCode.hashCode();
    }
    
    public static String buildASTKey(String sourceCode, String language) {
        return "ast:" + language + ":" + sourceCode.hashCode();
    }
    
    public static String buildPatternKey(String sourceCode, String language) {
        return "pattern:" + language + ":" + sourceCode.hashCode();
    }
    
    public static String buildSimilarityKey(String code1, String code2) {
        int hash1 = code1.hashCode();
        int hash2 = code2.hashCode();
        // Ensure consistent ordering
        if (hash1 > hash2) {
            int temp = hash1;
            hash1 = hash2;
            hash2 = temp;
        }
        return "similarity:" + hash1 + ":" + hash2;
    }
    
    public static String buildBinaryKey(String binaryPath, String analysisType) {
        return "binary:" + analysisType + ":" + binaryPath.hashCode();
    }
    
    public static String buildMetadataKey(String filePath) {
        return "metadata:" + filePath.hashCode();
    }
    
    // Statistics and monitoring
    public CacheStats getStats() {
        updateStats();
        return stats;
    }
    
    public void resetStats() {
        stats.reset();
        logger.info(PolyTypeLogger.LogCategory.CACHE, "Reset statistics for cache: " + type);
    }
    
    public void printStats() {
        CacheStats currentStats = getStats();
        
        System.out.println("\n=== Cache Statistics: " + type + " ===");
        System.out.println("Enabled: " + enabled);
        System.out.println("Entries: " + currentStats.getEntryCount() + " / " + maxSize);
        System.out.println("Total Size: " + formatBytes(currentStats.getTotalSize()));
        System.out.println("Hit Ratio: " + String.format("%.2f%%", currentStats.getHitRatio() * 100));
        System.out.println("Hits: " + currentStats.getHits());
        System.out.println("Misses: " + currentStats.getMisses());
        System.out.println("Evictions: " + currentStats.getEvictions());
        System.out.println("Expirations: " + currentStats.getExpirations());
        System.out.println("Default TTL: " + (defaultTtl / 1000) + " seconds");
        System.out.println("=====================================\n");
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    // Configuration updates
    public void updateConfiguration() {
        lock.writeLock().lock();
        try {
            boolean wasEnabled = enabled;
            loadConfiguration();
            
            if (!enabled && wasEnabled) {
                clear();
                logger.info(PolyTypeLogger.LogCategory.CACHE, "Cache disabled: " + type);
            } else if (enabled && !wasEnabled) {
                logger.info(PolyTypeLogger.LogCategory.CACHE, "Cache enabled: " + type);
            }
            
            // If max size decreased, evict excess entries
            if (enabled && cache.size() > maxSize) {
                int toEvict = cache.size() - maxSize;
                evictLRU(toEvict);
                logger.info(PolyTypeLogger.LogCategory.CACHE, 
                           "Evicted " + toEvict + " entries due to size limit change");
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // Warmup methods
    public void warmup(Map<String, Object> warmupData) {
        if (!enabled || warmupData == null) return;
        
        logger.info(PolyTypeLogger.LogCategory.CACHE, 
                   "Warming up cache: " + type + " with " + warmupData.size() + " entries");
        
        for (Map.Entry<String, Object> entry : warmupData.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        
        logger.info(PolyTypeLogger.LogCategory.CACHE, 
                   "Cache warmup completed: " + type);
    }
    
    // Health check
    public boolean isHealthy() {
        if (!enabled) return true;
        
        CacheStats currentStats = getStats();
        
        // Health criteria
        boolean sizeOk = currentStats.getEntryCount() <= maxSize;
        boolean hitRatioOk = currentStats.getHitRatio() >= 0.1 || (currentStats.getHits() + currentStats.getMisses()) < 100;
        boolean memoryOk = currentStats.getTotalSize() < (maxSize * 1024L); // Rough memory check
        
        return sizeOk && hitRatioOk && memoryOk;
    }
    
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        health.put("enabled", enabled);
        health.put("healthy", isHealthy());
        health.put("type", type);
        health.put("stats", getStats());
        return health;
    }
    
    // Shutdown
    public void shutdown() {
        logger.info(PolyTypeLogger.LogCategory.CACHE, "Shutting down cache: " + type);
        
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
        }
        
        clear();
        logger.info(PolyTypeLogger.LogCategory.CACHE, "Cache shutdown completed: " + type);
    }
}