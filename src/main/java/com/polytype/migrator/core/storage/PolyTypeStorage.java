package com.polytype.migrator.core.storage;

import com.polytype.migrator.core.logging.PolyTypeLogger;
import com.polytype.migrator.core.config.PolyTypeConfig;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Comprehensive data persistence and storage system for PolyType.
 * Supports multiple storage backends, compression, encryption, and data versioning.
 */
public class PolyTypeStorage {
    
    public enum StorageType {
        FILE_SYSTEM,     // Local file system storage
        MEMORY,          // In-memory storage (volatile)
        DATABASE,        // Database storage (SQLite, H2, etc.)
        CLOUD,           // Cloud storage (S3, Azure, etc.)
        HYBRID           // Combination of multiple storage types
    }
    
    public enum DataCategory {
        TRANSLATIONS,     // Translation results and cache
        ML_MODELS,       // Machine learning models and training data
        BINARY_ANALYSIS, // Binary analysis results
        CONFIGURATION,   // System configuration backups
        USER_DATA,       // User preferences and history
        LOGS,           // System logs and audit trails
        METRICS,        // Performance metrics and analytics
        PLUGINS,        // Plugin data and state
        TEMPORARY       // Temporary data with automatic cleanup
    }
    
    public static class StorageEntry {
        private final String id;
        private final String key;
        private final DataCategory category;
        private final byte[] data;
        private final Map<String, String> metadata;
        private final LocalDateTime created;
        private final LocalDateTime modified;
        private final long version;
        private final boolean compressed;
        private final boolean encrypted;
        
        public StorageEntry(String id, String key, DataCategory category, byte[] data,
                           Map<String, String> metadata, LocalDateTime created,
                           LocalDateTime modified, long version, boolean compressed,
                           boolean encrypted) {
            this.id = id;
            this.key = key;
            this.category = category;
            this.data = data != null ? data.clone() : new byte[0];
            this.metadata = new HashMap<>(metadata != null ? metadata : Collections.emptyMap());
            this.created = created;
            this.modified = modified;
            this.version = version;
            this.compressed = compressed;
            this.encrypted = encrypted;
        }
        
        public String getId() { return id; }
        public String getKey() { return key; }
        public DataCategory getCategory() { return category; }
        public byte[] getData() { return data.clone(); }
        public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
        public LocalDateTime getCreated() { return created; }
        public LocalDateTime getModified() { return modified; }
        public long getVersion() { return version; }
        public boolean isCompressed() { return compressed; }
        public boolean isEncrypted() { return encrypted; }
        
        public int getSize() { return data.length; }
    }
    
    public static class StorageStats {
        private long totalEntries = 0;
        private long totalSize = 0;
        private final Map<DataCategory, Long> categoryStats = new HashMap<>();
        private final Map<DataCategory, Long> categorySizes = new HashMap<>();
        
        public void updateStats(StorageEntry entry, boolean added) {
            if (added) {
                totalEntries++;
                totalSize += entry.getSize();
                categoryStats.merge(entry.getCategory(), 1L, Long::sum);
                categorySizes.merge(entry.getCategory(), (long) entry.getSize(), Long::sum);
            } else {
                totalEntries--;
                totalSize -= entry.getSize();
                categoryStats.merge(entry.getCategory(), -1L, Long::sum);
                categorySizes.merge(entry.getCategory(), -(long) entry.getSize(), Long::sum);
            }
        }
        
        public long getTotalEntries() { return totalEntries; }
        public long getTotalSize() { return totalSize; }
        public Map<DataCategory, Long> getCategoryStats() { return new HashMap<>(categoryStats); }
        public Map<DataCategory, Long> getCategorySizes() { return new HashMap<>(categorySizes); }
    }
    
    public interface StorageBackend {
        void initialize() throws IOException;
        void store(StorageEntry entry) throws IOException;
        StorageEntry retrieve(String key) throws IOException;
        boolean exists(String key) throws IOException;
        void delete(String key) throws IOException;
        List<String> listKeys(DataCategory category) throws IOException;
        List<StorageEntry> listEntries(DataCategory category) throws IOException;
        void cleanup() throws IOException;
        void close() throws IOException;
        StorageStats getStats();
    }
    
    // File System Storage Backend
    public static class FileSystemBackend implements StorageBackend {
        private final Path basePath;
        private final StorageStats stats = new StorageStats();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final PolyTypeLogger logger = PolyTypeLogger.getLogger(FileSystemBackend.class);
        
        public FileSystemBackend(String basePath) {
            this.basePath = Paths.get(basePath);
        }
        
        @Override
        public void initialize() throws IOException {
            Files.createDirectories(basePath);
            
            // Create category directories
            for (DataCategory category : DataCategory.values()) {
                Files.createDirectories(basePath.resolve(category.name().toLowerCase()));
            }
            
            logger.info(PolyTypeLogger.LogCategory.IO, 
                       "Initialized file system storage: " + basePath.toAbsolutePath());
        }
        
        @Override
        public void store(StorageEntry entry) throws IOException {
            lock.writeLock().lock();
            try {
                Path categoryDir = basePath.resolve(entry.getCategory().name().toLowerCase());
                Path filePath = categoryDir.resolve(sanitizeFileName(entry.getKey()) + ".dat");
                Path metaPath = categoryDir.resolve(sanitizeFileName(entry.getKey()) + ".meta");
                
                // Write data
                Files.write(filePath, entry.getData(), StandardOpenOption.CREATE, 
                           StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                
                // Write metadata
                Properties metadata = new Properties();
                metadata.putAll(entry.getMetadata());
                metadata.setProperty("id", entry.getId());
                metadata.setProperty("key", entry.getKey());
                metadata.setProperty("category", entry.getCategory().name());
                metadata.setProperty("created", entry.getCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                metadata.setProperty("modified", entry.getModified().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                metadata.setProperty("version", String.valueOf(entry.getVersion()));
                metadata.setProperty("compressed", String.valueOf(entry.isCompressed()));
                metadata.setProperty("encrypted", String.valueOf(entry.isEncrypted()));
                metadata.setProperty("size", String.valueOf(entry.getSize()));
                
                try (OutputStream os = Files.newOutputStream(metaPath)) {
                    metadata.store(os, "PolyType Storage Entry Metadata");
                }
                
                stats.updateStats(entry, true);
                
                logger.debug(PolyTypeLogger.LogCategory.IO, 
                           "Stored entry: " + entry.getKey() + " (" + entry.getSize() + " bytes)");
                
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        @Override
        public StorageEntry retrieve(String key) throws IOException {
            lock.readLock().lock();
            try {
                for (DataCategory category : DataCategory.values()) {
                    Path categoryDir = basePath.resolve(category.name().toLowerCase());
                    Path filePath = categoryDir.resolve(sanitizeFileName(key) + ".dat");
                    Path metaPath = categoryDir.resolve(sanitizeFileName(key) + ".meta");
                    
                    if (Files.exists(filePath) && Files.exists(metaPath)) {
                        // Read data
                        byte[] data = Files.readAllBytes(filePath);
                        
                        // Read metadata
                        Properties metadata = new Properties();
                        try (InputStream is = Files.newInputStream(metaPath)) {
                            metadata.load(is);
                        }
                        
                        return createEntryFromMetadata(data, metadata);
                    }
                }
                
                return null; // Entry not found
                
            } finally {
                lock.readLock().unlock();
            }
        }
        
        private StorageEntry createEntryFromMetadata(byte[] data, Properties metadata) {
            String id = metadata.getProperty("id");
            String key = metadata.getProperty("key");
            DataCategory category = DataCategory.valueOf(metadata.getProperty("category"));
            LocalDateTime created = LocalDateTime.parse(metadata.getProperty("created"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime modified = LocalDateTime.parse(metadata.getProperty("modified"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long version = Long.parseLong(metadata.getProperty("version", "1"));
            boolean compressed = Boolean.parseBoolean(metadata.getProperty("compressed", "false"));
            boolean encrypted = Boolean.parseBoolean(metadata.getProperty("encrypted", "false"));
            
            Map<String, String> userMetadata = new HashMap<>();
            for (String propKey : metadata.stringPropertyNames()) {
                if (!isSystemProperty(propKey)) {
                    userMetadata.put(propKey, metadata.getProperty(propKey));
                }
            }
            
            return new StorageEntry(id, key, category, data, userMetadata, 
                                   created, modified, version, compressed, encrypted);
        }
        
        private boolean isSystemProperty(String key) {
            return Arrays.asList("id", "key", "category", "created", "modified", 
                               "version", "compressed", "encrypted", "size").contains(key);
        }
        
        @Override
        public boolean exists(String key) throws IOException {
            lock.readLock().lock();
            try {
                for (DataCategory category : DataCategory.values()) {
                    Path categoryDir = basePath.resolve(category.name().toLowerCase());
                    Path filePath = categoryDir.resolve(sanitizeFileName(key) + ".dat");
                    if (Files.exists(filePath)) {
                        return true;
                    }
                }
                return false;
            } finally {
                lock.readLock().unlock();
            }
        }
        
        @Override
        public void delete(String key) throws IOException {
            lock.writeLock().lock();
            try {
                for (DataCategory category : DataCategory.values()) {
                    Path categoryDir = basePath.resolve(category.name().toLowerCase());
                    Path filePath = categoryDir.resolve(sanitizeFileName(key) + ".dat");
                    Path metaPath = categoryDir.resolve(sanitizeFileName(key) + ".meta");
                    
                    if (Files.exists(filePath)) {
                        // Get entry for stats update before deletion
                        StorageEntry entry = retrieve(key);
                        
                        Files.deleteIfExists(filePath);
                        Files.deleteIfExists(metaPath);
                        
                        if (entry != null) {
                            stats.updateStats(entry, false);
                        }
                        
                        logger.debug(PolyTypeLogger.LogCategory.IO, "Deleted entry: " + key);
                        return;
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        @Override
        public List<String> listKeys(DataCategory category) throws IOException {
            List<String> keys = new ArrayList<>();
            Path categoryDir = basePath.resolve(category.name().toLowerCase());
            
            if (!Files.exists(categoryDir)) {
                return keys;
            }
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(categoryDir, "*.dat")) {
                for (Path path : stream) {
                    String fileName = path.getFileName().toString();
                    String key = fileName.substring(0, fileName.lastIndexOf(".dat"));
                    keys.add(desanitizeFileName(key));
                }
            }
            
            return keys;
        }
        
        @Override
        public List<StorageEntry> listEntries(DataCategory category) throws IOException {
            List<StorageEntry> entries = new ArrayList<>();
            List<String> keys = listKeys(category);
            
            for (String key : keys) {
                StorageEntry entry = retrieve(key);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            
            return entries;
        }
        
        @Override
        public void cleanup() throws IOException {
            // Clean up temporary files older than 24 hours
            Path tempDir = basePath.resolve(DataCategory.TEMPORARY.name().toLowerCase());
            if (!Files.exists(tempDir)) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir)) {
                for (Path path : stream) {
                    if (Files.getLastModifiedTime(path).toMillis() < cutoffTime) {
                        Files.deleteIfExists(path);
                        logger.debug(PolyTypeLogger.LogCategory.IO, "Cleaned up old file: " + path.getFileName());
                    }
                }
            }
        }
        
        @Override
        public void close() throws IOException {
            // Nothing special to close for file system
            logger.info(PolyTypeLogger.LogCategory.IO, "File system storage closed");
        }
        
        @Override
        public StorageStats getStats() {
            return stats;
        }
        
        private String sanitizeFileName(String fileName) {
            return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        
        private String desanitizeFileName(String fileName) {
            return fileName; // Simple implementation
        }
    }
    
    // In-Memory Storage Backend
    public static class MemoryBackend implements StorageBackend {
        private final Map<String, StorageEntry> storage = new ConcurrentHashMap<>();
        private final StorageStats stats = new StorageStats();
        private final PolyTypeLogger logger = PolyTypeLogger.getLogger(MemoryBackend.class);
        
        @Override
        public void initialize() throws IOException {
            logger.info(PolyTypeLogger.LogCategory.IO, "Initialized memory storage");
        }
        
        @Override
        public void store(StorageEntry entry) throws IOException {
            StorageEntry oldEntry = storage.put(entry.getKey(), entry);
            
            if (oldEntry != null) {
                stats.updateStats(oldEntry, false);
            }
            stats.updateStats(entry, true);
            
            logger.debug(PolyTypeLogger.LogCategory.IO, 
                       "Stored entry in memory: " + entry.getKey() + " (" + entry.getSize() + " bytes)");
        }
        
        @Override
        public StorageEntry retrieve(String key) throws IOException {
            return storage.get(key);
        }
        
        @Override
        public boolean exists(String key) throws IOException {
            return storage.containsKey(key);
        }
        
        @Override
        public void delete(String key) throws IOException {
            StorageEntry removed = storage.remove(key);
            if (removed != null) {
                stats.updateStats(removed, false);
                logger.debug(PolyTypeLogger.LogCategory.IO, "Deleted entry from memory: " + key);
            }
        }
        
        @Override
        public List<String> listKeys(DataCategory category) throws IOException {
            return storage.values().stream()
                         .filter(entry -> entry.getCategory() == category)
                         .map(StorageEntry::getKey)
                         .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        @Override
        public List<StorageEntry> listEntries(DataCategory category) throws IOException {
            return storage.values().stream()
                         .filter(entry -> entry.getCategory() == category)
                         .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        
        @Override
        public void cleanup() throws IOException {
            // Remove temporary entries older than 1 hour
            long cutoffTime = System.currentTimeMillis() - (60 * 60 * 1000); // 1 hour ago
            
            List<String> toRemove = storage.values().stream()
                                          .filter(entry -> entry.getCategory() == DataCategory.TEMPORARY)
                                          .filter(entry -> entry.getModified().isBefore(LocalDateTime.now().minusHours(1)))
                                          .map(StorageEntry::getKey)
                                          .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            for (String key : toRemove) {
                delete(key);
            }
            
            if (!toRemove.isEmpty()) {
                logger.debug(PolyTypeLogger.LogCategory.IO, 
                           "Cleaned up " + toRemove.size() + " temporary entries from memory");
            }
        }
        
        @Override
        public void close() throws IOException {
            storage.clear();
            logger.info(PolyTypeLogger.LogCategory.IO, "Memory storage closed");
        }
        
        @Override
        public StorageStats getStats() {
            return stats;
        }
    }
    
    // Main Storage Manager
    private final StorageBackend primaryBackend;
    private final StorageBackend secondaryBackend; // Optional backup backend
    private final PolyTypeLogger logger = PolyTypeLogger.getLogger(PolyTypeStorage.class);
    private final PolyTypeConfig config = PolyTypeConfig.getInstance();
    
    // Compression and encryption
    private boolean compressionEnabled = true;
    private boolean encryptionEnabled = false;
    
    public PolyTypeStorage(StorageType primaryType) throws IOException {
        this(primaryType, null);
    }
    
    public PolyTypeStorage(StorageType primaryType, StorageType secondaryType) throws IOException {
        this.primaryBackend = createBackend(primaryType);
        this.secondaryBackend = secondaryType != null ? createBackend(secondaryType) : null;
        
        initialize();
    }
    
    private StorageBackend createBackend(StorageType type) {
        switch (type) {
            case FILE_SYSTEM:
                String storageDir = config.getString(PolyTypeConfig.TEMP_DIR) + "/polytype-storage";
                return new FileSystemBackend(storageDir);
            case MEMORY:
                return new MemoryBackend();
            default:
                throw new UnsupportedOperationException("Storage type not implemented: " + type);
        }
    }
    
    private void initialize() throws IOException {
        primaryBackend.initialize();
        if (secondaryBackend != null) {
            secondaryBackend.initialize();
        }
        
        logger.info(PolyTypeLogger.LogCategory.IO, "Storage system initialized");
    }
    
    // Core storage operations
    public void store(String key, DataCategory category, byte[] data) throws IOException {
        store(key, category, data, Collections.emptyMap());
    }
    
    public void store(String key, DataCategory category, byte[] data, 
                     Map<String, String> metadata) throws IOException {
        
        byte[] processedData = processDataForStorage(data);
        
        StorageEntry entry = new StorageEntry(
            generateId(),
            key,
            category,
            processedData,
            metadata,
            LocalDateTime.now(),
            LocalDateTime.now(),
            1L,
            compressionEnabled && shouldCompress(data),
            encryptionEnabled && shouldEncrypt(category)
        );
        
        // Store in primary backend
        primaryBackend.store(entry);
        
        // Store in secondary backend if available
        if (secondaryBackend != null) {
            try {
                secondaryBackend.store(entry);
            } catch (Exception e) {
                logger.warn(PolyTypeLogger.LogCategory.IO, 
                           "Failed to store in secondary backend", e);
            }
        }
        
        logger.debug(PolyTypeLogger.LogCategory.IO, 
                   "Stored data: " + key + " in category " + category);
    }
    
    public byte[] retrieve(String key) throws IOException {
        StorageEntry entry = primaryBackend.retrieve(key);
        
        if (entry == null && secondaryBackend != null) {
            entry = secondaryBackend.retrieve(key);
        }
        
        if (entry == null) {
            return null;
        }
        
        byte[] processedData = processDataFromStorage(entry.getData(), entry.isCompressed(), entry.isEncrypted());
        
        logger.debug(PolyTypeLogger.LogCategory.IO, 
                   "Retrieved data: " + key + " (" + processedData.length + " bytes)");
        
        return processedData;
    }
    
    public StorageEntry retrieveEntry(String key) throws IOException {
        StorageEntry entry = primaryBackend.retrieve(key);
        
        if (entry == null && secondaryBackend != null) {
            entry = secondaryBackend.retrieve(key);
        }
        
        return entry;
    }
    
    public boolean exists(String key) throws IOException {
        return primaryBackend.exists(key) || (secondaryBackend != null && secondaryBackend.exists(key));
    }
    
    public void delete(String key) throws IOException {
        primaryBackend.delete(key);
        
        if (secondaryBackend != null) {
            try {
                secondaryBackend.delete(key);
            } catch (Exception e) {
                logger.warn(PolyTypeLogger.LogCategory.IO, 
                           "Failed to delete from secondary backend", e);
            }
        }
        
        logger.debug(PolyTypeLogger.LogCategory.IO, "Deleted data: " + key);
    }
    
    // Convenience methods for common data types
    public void storeString(String key, DataCategory category, String value) throws IOException {
        store(key, category, value.getBytes("UTF-8"));
    }
    
    public String retrieveString(String key) throws IOException {
        byte[] data = retrieve(key);
        return data != null ? new String(data, "UTF-8") : null;
    }
    
    public void storeObject(String key, DataCategory category, Serializable object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            
            oos.writeObject(object);
            store(key, category, baos.toByteArray());
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T retrieveObject(String key, Class<T> type) throws IOException, ClassNotFoundException {
        byte[] data = retrieve(key);
        if (data == null) return null;
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            
            return (T) ois.readObject();
        }
    }
    
    // Data processing
    private byte[] processDataForStorage(byte[] data) throws IOException {
        byte[] processedData = data;
        
        if (compressionEnabled && shouldCompress(data)) {
            processedData = compress(processedData);
        }
        
        if (encryptionEnabled) {
            processedData = encrypt(processedData);
        }
        
        return processedData;
    }
    
    private byte[] processDataFromStorage(byte[] data, boolean compressed, boolean encrypted) throws IOException {
        byte[] processedData = data;
        
        if (encrypted) {
            processedData = decrypt(processedData);
        }
        
        if (compressed) {
            processedData = decompress(processedData);
        }
        
        return processedData;
    }
    
    private boolean shouldCompress(byte[] data) {
        return data.length > 1024; // Compress data larger than 1KB
    }
    
    private boolean shouldEncrypt(DataCategory category) {
        return category == DataCategory.USER_DATA || category == DataCategory.CONFIGURATION;
    }
    
    private byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            
            gzos.write(data);
            gzos.close();
            return baos.toByteArray();
        }
    }
    
    private byte[] decompress(byte[] data) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }
    
    private byte[] encrypt(byte[] data) {
        // Simple XOR encryption for demonstration
        // In production, use proper encryption like AES
        byte[] key = "PolyTypeSecretKey".getBytes();
        byte[] encrypted = new byte[data.length];
        
        for (int i = 0; i < data.length; i++) {
            encrypted[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        
        return encrypted;
    }
    
    private byte[] decrypt(byte[] data) {
        // Same as encrypt for XOR
        return encrypt(data);
    }
    
    private String generateId() {
        return "entry-" + System.currentTimeMillis() + "-" + Math.abs(new Random().nextInt());
    }
    
    // Query and management operations
    public List<String> listKeys(DataCategory category) throws IOException {
        Set<String> allKeys = new HashSet<>(primaryBackend.listKeys(category));
        
        if (secondaryBackend != null) {
            allKeys.addAll(secondaryBackend.listKeys(category));
        }
        
        return new ArrayList<>(allKeys);
    }
    
    public List<StorageEntry> listEntries(DataCategory category) throws IOException {
        Map<String, StorageEntry> allEntries = new HashMap<>();
        
        // Get entries from primary backend
        for (StorageEntry entry : primaryBackend.listEntries(category)) {
            allEntries.put(entry.getKey(), entry);
        }
        
        // Merge with secondary backend entries (primary takes precedence)
        if (secondaryBackend != null) {
            for (StorageEntry entry : secondaryBackend.listEntries(category)) {
                allEntries.putIfAbsent(entry.getKey(), entry);
            }
        }
        
        return new ArrayList<>(allEntries.values());
    }
    
    public void cleanup() throws IOException {
        primaryBackend.cleanup();
        
        if (secondaryBackend != null) {
            secondaryBackend.cleanup();
        }
        
        logger.info(PolyTypeLogger.LogCategory.IO, "Storage cleanup completed");
    }
    
    public StorageStats getStats() {
        StorageStats primaryStats = primaryBackend.getStats();
        
        if (secondaryBackend == null) {
            return primaryStats;
        }
        
        // Combine stats from both backends (this is simplified)
        return primaryStats;
    }
    
    public void printStats() {
        StorageStats stats = getStats();
        
        System.out.println("\\n=== Storage Statistics ===");
        System.out.println("Total Entries: " + stats.getTotalEntries());
        System.out.println("Total Size: " + formatBytes(stats.getTotalSize()));
        System.out.println("Compression: " + (compressionEnabled ? "enabled" : "disabled"));
        System.out.println("Encryption: " + (encryptionEnabled ? "enabled" : "disabled"));
        System.out.println("Secondary Backend: " + (secondaryBackend != null ? "enabled" : "disabled"));
        
        System.out.println("\\nCategory Distribution:");
        for (Map.Entry<DataCategory, Long> entry : stats.getCategoryStats().entrySet()) {
            long size = stats.getCategorySizes().getOrDefault(entry.getKey(), 0L);
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + 
                             " entries (" + formatBytes(size) + ")");
        }
        
        System.out.println("==========================\\n");
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    // Configuration
    public void setCompressionEnabled(boolean enabled) {
        this.compressionEnabled = enabled;
        logger.info(PolyTypeLogger.LogCategory.IO, 
                   "Compression " + (enabled ? "enabled" : "disabled"));
    }
    
    public void setEncryptionEnabled(boolean enabled) {
        this.encryptionEnabled = enabled;
        logger.info(PolyTypeLogger.LogCategory.IO, 
                   "Encryption " + (enabled ? "enabled" : "disabled"));
    }
    
    // Backup and restore
    public void exportData(DataCategory category, String exportPath) throws IOException {
        List<StorageEntry> entries = listEntries(category);
        
        try (FileOutputStream fos = new FileOutputStream(exportPath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            
            oos.writeObject(entries);
        }
        
        logger.info(PolyTypeLogger.LogCategory.IO, 
                   "Exported " + entries.size() + " entries to " + exportPath);
    }
    
    @SuppressWarnings("unchecked")
    public void importData(String importPath) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(importPath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            List<StorageEntry> entries = (List<StorageEntry>) ois.readObject();
            
            for (StorageEntry entry : entries) {
                primaryBackend.store(entry);
            }
            
            logger.info(PolyTypeLogger.LogCategory.IO, 
                       "Imported " + entries.size() + " entries from " + importPath);
        }
    }
    
    // Shutdown
    public void close() throws IOException {
        primaryBackend.close();
        
        if (secondaryBackend != null) {
            secondaryBackend.close();
        }
        
        logger.info(PolyTypeLogger.LogCategory.IO, "Storage system closed");
    }
}