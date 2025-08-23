package com.davajava.migrator.decompiler;

import com.davajava.migrator.core.ParseException;
import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.core.ast.ProgramNode;
import com.davajava.migrator.core.ast.ClassDeclarationNode;
import com.davajava.migrator.parser.kotlin.KotlinParser;
import com.davajava.migrator.parser.java.JavaParser;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.logging.Logger;

/**
 * APK Decompiler that extracts structured Java code from Android APK files.
 * Supports decompilation of Kotlin, Java, and other JVM languages.
 */
public class ApkDecompiler {
    private static final Logger logger = Logger.getLogger(ApkDecompiler.class.getName());
    
    // APK structure patterns
    private static final Pattern DEX_FILE_PATTERN = Pattern.compile(".*\\.dex$");
    private static final Pattern KOTLIN_CLASS_PATTERN = Pattern.compile(".*\\.kt\\.(class|kotlin_metadata)$");
    private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile(".*\\.class$");
    private static final Pattern NATIVE_LIB_PATTERN = Pattern.compile("lib/.*/.*\\.so$");
    private static final Pattern ASSET_PATTERN = Pattern.compile("assets/.*");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("res/.*");
    
    private final DexDecompiler dexDecompiler;
    private final ClassFileDecompiler classDecompiler;
    private final ManifestParser manifestParser;
    private Path tempDirectory;
    
    public ApkDecompiler() {
        this.dexDecompiler = new DexDecompiler();
        this.classDecompiler = new ClassFileDecompiler();
        this.manifestParser = new ManifestParser();
    }
    
    /**
     * Decompile an APK file to structured Java source code
     */
    public ApkDecompilationResult decompile(String apkPath) throws IOException, ParseException {
        return decompile(Paths.get(apkPath));
    }
    
    public ApkDecompilationResult decompile(Path apkPath) throws IOException, ParseException {
        if (!Files.exists(apkPath) || !apkPath.toString().toLowerCase().endsWith(".apk")) {
            throw new IllegalArgumentException("Invalid APK file: " + apkPath);
        }
        
        ApkDecompilationResult result = new ApkDecompilationResult();
        result.setApkPath(apkPath.toString());
        
        // Create temporary directory
        tempDirectory = Files.createTempDirectory("apk_decompile_");
        
        try (ZipFile apkZip = new ZipFile(apkPath.toFile())) {
            logger.info("Starting APK decompilation: " + apkPath);
            
            // Extract APK contents
            extractApkContents(apkZip, result);
            
            // Parse Android manifest
            parseManifest(apkZip, result);
            
            // Decompile DEX files
            decompileDexFiles(result);
            
            // Process class files (if any)
            processClassFiles(result);
            
            // Extract resources and assets
            extractResources(apkZip, result);
            
            logger.info("APK decompilation completed successfully");
            
        } finally {
            // Cleanup temporary directory
            cleanup();
        }
        
        return result;
    }
    
    private void extractApkContents(ZipFile apkZip, ApkDecompilationResult result) throws IOException {
        Enumeration<? extends ZipEntry> entries = apkZip.entries();
        
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();
            
            if (entry.isDirectory()) continue;
            
            Path entryPath = tempDirectory.resolve(entryName);
            Files.createDirectories(entryPath.getParent());
            
            try (InputStream inputStream = apkZip.getInputStream(entry);
                 OutputStream outputStream = Files.newOutputStream(entryPath)) {
                
                inputStream.transferTo(outputStream);
            }
            
            // Categorize files
            if (DEX_FILE_PATTERN.matcher(entryName).matches()) {
                result.addDexFile(entryPath.toString());
            } else if (JAVA_CLASS_PATTERN.matcher(entryName).matches()) {
                result.addClassFile(entryPath.toString());
            } else if (NATIVE_LIB_PATTERN.matcher(entryName).matches()) {
                result.addNativeLibrary(entryPath.toString());
            } else if (ASSET_PATTERN.matcher(entryName).matches()) {
                result.addAsset(entryPath.toString());
            } else if (RESOURCE_PATTERN.matcher(entryName).matches()) {
                result.addResource(entryPath.toString());
            }
        }
    }
    
    private void parseManifest(ZipFile apkZip, ApkDecompilationResult result) throws IOException {
        ZipEntry manifestEntry = apkZip.getEntry("AndroidManifest.xml");
        if (manifestEntry != null) {
            try (InputStream manifestStream = apkZip.getInputStream(manifestEntry)) {
                AndroidManifest manifest = manifestParser.parse(manifestStream);
                result.setManifest(manifest);
            }
        }
    }
    
    private void decompileDexFiles(ApkDecompilationResult result) throws IOException, ParseException {
        for (String dexFilePath : result.getDexFiles()) {
            logger.info("Decompiling DEX file: " + dexFilePath);
            
            DexDecompilationResult dexResult = dexDecompiler.decompile(dexFilePath);
            
            // Convert decompiled classes to AST
            for (DecompiledClass decompiledClass : dexResult.getClasses()) {
                try {
                    ASTNode classAst = convertToAST(decompiledClass);
                    result.addDecompiledClass(decompiledClass.getClassName(), classAst);
                } catch (Exception e) {
                    logger.warning("Failed to convert class to AST: " + decompiledClass.getClassName() + " - " + e.getMessage());
                }
            }
        }
    }
    
    private void processClassFiles(ApkDecompilationResult result) throws IOException, ParseException {
        for (String classFilePath : result.getClassFiles()) {
            try {
                DecompiledClass decompiledClass = classDecompiler.decompile(classFilePath);
                ASTNode classAst = convertToAST(decompiledClass);
                result.addDecompiledClass(decompiledClass.getClassName(), classAst);
            } catch (Exception e) {
                logger.warning("Failed to decompile class file: " + classFilePath + " - " + e.getMessage());
            }
        }
    }
    
    private void extractResources(ZipFile apkZip, ApkDecompilationResult result) throws IOException {
        // Process resources.arsc
        ZipEntry resourcesEntry = apkZip.getEntry("resources.arsc");
        if (resourcesEntry != null) {
            // TODO: Parse resources.arsc binary format
            // For now, just note its presence
            result.setHasCompiledResources(true);
        }
        
        // Extract raw resources and assets are already handled in extractApkContents
    }
    
    private ASTNode convertToAST(DecompiledClass decompiledClass) throws ParseException {
        String sourceCode = decompiledClass.getSourceCode();
        SourceLanguage language = detectLanguage(decompiledClass);
        
        switch (language) {
            case KOTLIN:
                KotlinParser kotlinParser = new KotlinParser();
                return kotlinParser.parse(sourceCode);
                
            case JAVA:
                // Assume JavaParser exists or use a simplified parser
                return parseJavaSource(sourceCode);
                
            case SCALA:
                // Handle Scala if detected
                return parseScalaSource(sourceCode);
                
            default:
                // Generic parsing or treat as Java
                return parseJavaSource(sourceCode);
        }
    }
    
    private SourceLanguage detectLanguage(DecompiledClass decompiledClass) {
        String className = decompiledClass.getClassName();
        String sourceCode = decompiledClass.getSourceCode();
        
        // Check for Kotlin metadata or patterns
        if (className.contains("$Companion") || 
            className.contains("Kt") ||
            sourceCode.contains("@kotlin.Metadata") ||
            sourceCode.contains("kotlin.jvm.internal")) {
            return SourceLanguage.KOTLIN;
        }
        
        // Check for Scala patterns
        if (className.contains("$") && (className.contains("anonfun") || className.contains("apply"))) {
            return SourceLanguage.SCALA;
        }
        
        // Default to Java
        return SourceLanguage.JAVA;
    }
    
    private ASTNode parseJavaSource(String sourceCode) throws ParseException {
        // Simplified Java parsing - in a real implementation, use a proper Java parser
        ProgramNode program = new ProgramNode(1, 1);
        
        // Extract class name from source
        Pattern classPattern = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
        java.util.regex.Matcher matcher = classPattern.matcher(sourceCode);
        
        if (matcher.find()) {
            String className = matcher.group(1);
            ClassDeclarationNode classNode = new ClassDeclarationNode(className, true, 1, 1);
            program.addChild(classNode);
        }
        
        return program;
    }
    
    private ASTNode parseScalaSource(String sourceCode) throws ParseException {
        // Placeholder for Scala parsing
        return parseJavaSource(sourceCode); // Fallback to Java parsing
    }
    
    private void cleanup() throws IOException {
        if (tempDirectory != null && Files.exists(tempDirectory)) {
            Files.walk(tempDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
    
    // Inner classes and enums
    
    public enum SourceLanguage {
        JAVA, KOTLIN, SCALA, UNKNOWN
    }
    
    /**
     * Result of APK decompilation
     */
    public static class ApkDecompilationResult {
        private String apkPath;
        private AndroidManifest manifest;
        private List<String> dexFiles = new ArrayList<>();
        private List<String> classFiles = new ArrayList<>();
        private List<String> nativeLibraries = new ArrayList<>();
        private List<String> assets = new ArrayList<>();
        private List<String> resources = new ArrayList<>();
        private Map<String, ASTNode> decompiledClasses = new HashMap<>();
        private boolean hasCompiledResources = false;
        
        // Getters and setters
        public String getApkPath() { return apkPath; }
        public void setApkPath(String apkPath) { this.apkPath = apkPath; }
        
        public AndroidManifest getManifest() { return manifest; }
        public void setManifest(AndroidManifest manifest) { this.manifest = manifest; }
        
        public List<String> getDexFiles() { return dexFiles; }
        public void addDexFile(String dexFile) { this.dexFiles.add(dexFile); }
        
        public List<String> getClassFiles() { return classFiles; }
        public void addClassFile(String classFile) { this.classFiles.add(classFile); }
        
        public List<String> getNativeLibraries() { return nativeLibraries; }
        public void addNativeLibrary(String library) { this.nativeLibraries.add(library); }
        
        public List<String> getAssets() { return assets; }
        public void addAsset(String asset) { this.assets.add(asset); }
        
        public List<String> getResources() { return resources; }
        public void addResource(String resource) { this.resources.add(resource); }
        
        public Map<String, ASTNode> getDecompiledClasses() { return decompiledClasses; }
        public void addDecompiledClass(String className, ASTNode ast) { 
            this.decompiledClasses.put(className, ast); 
        }
        
        public boolean hasCompiledResources() { return hasCompiledResources; }
        public void setHasCompiledResources(boolean hasCompiledResources) { 
            this.hasCompiledResources = hasCompiledResources; 
        }
        
        public int getTotalClasses() { return decompiledClasses.size(); }
        public Set<String> getClassNames() { return decompiledClasses.keySet(); }
    }
    
    /**
     * Represents the Android manifest
     */
    public static class AndroidManifest {
        private String packageName;
        private String versionName;
        private int versionCode;
        private int minSdkVersion;
        private int targetSdkVersion;
        private List<String> permissions = new ArrayList<>();
        private List<AndroidComponent> activities = new ArrayList<>();
        private List<AndroidComponent> services = new ArrayList<>();
        private List<AndroidComponent> receivers = new ArrayList<>();
        private List<AndroidComponent> providers = new ArrayList<>();
        
        // Getters and setters
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        
        public String getVersionName() { return versionName; }
        public void setVersionName(String versionName) { this.versionName = versionName; }
        
        public int getVersionCode() { return versionCode; }
        public void setVersionCode(int versionCode) { this.versionCode = versionCode; }
        
        public int getMinSdkVersion() { return minSdkVersion; }
        public void setMinSdkVersion(int minSdkVersion) { this.minSdkVersion = minSdkVersion; }
        
        public int getTargetSdkVersion() { return targetSdkVersion; }
        public void setTargetSdkVersion(int targetSdkVersion) { this.targetSdkVersion = targetSdkVersion; }
        
        public List<String> getPermissions() { return permissions; }
        public void addPermission(String permission) { this.permissions.add(permission); }
        
        public List<AndroidComponent> getActivities() { return activities; }
        public void addActivity(AndroidComponent activity) { this.activities.add(activity); }
        
        public List<AndroidComponent> getServices() { return services; }
        public void addService(AndroidComponent service) { this.services.add(service); }
        
        public List<AndroidComponent> getReceivers() { return receivers; }
        public void addReceiver(AndroidComponent receiver) { this.receivers.add(receiver); }
        
        public List<AndroidComponent> getProviders() { return providers; }
        public void addProvider(AndroidComponent provider) { this.providers.add(provider); }
    }
    
    /**
     * Represents an Android component (Activity, Service, etc.)
     */
    public static class AndroidComponent {
        private String name;
        private String className;
        private boolean exported;
        private List<IntentFilter> intentFilters = new ArrayList<>();
        
        public AndroidComponent(String name, String className) {
            this.name = name;
            this.className = className;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public boolean isExported() { return exported; }
        public void setExported(boolean exported) { this.exported = exported; }
        
        public List<IntentFilter> getIntentFilters() { return intentFilters; }
        public void addIntentFilter(IntentFilter intentFilter) { this.intentFilters.add(intentFilter); }
    }
    
    /**
     * Represents an Intent Filter
     */
    public static class IntentFilter {
        private List<String> actions = new ArrayList<>();
        private List<String> categories = new ArrayList<>();
        private String dataScheme;
        
        public List<String> getActions() { return actions; }
        public void addAction(String action) { this.actions.add(action); }
        
        public List<String> getCategories() { return categories; }
        public void addCategory(String category) { this.categories.add(category); }
        
        public String getDataScheme() { return dataScheme; }
        public void setDataScheme(String dataScheme) { this.dataScheme = dataScheme; }
    }
    
    /**
     * Represents a decompiled class
     */
    public static class DecompiledClass {
        private String className;
        private String sourceCode;
        private SourceLanguage language;
        
        public DecompiledClass(String className, String sourceCode, SourceLanguage language) {
            this.className = className;
            this.sourceCode = sourceCode;
            this.language = language;
        }
        
        public String getClassName() { return className; }
        public String getSourceCode() { return sourceCode; }
        public SourceLanguage getLanguage() { return language; }
    }
}