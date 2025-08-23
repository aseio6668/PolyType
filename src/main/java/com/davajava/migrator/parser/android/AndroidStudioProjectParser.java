package com.davajava.migrator.parser.android;

import com.davajava.migrator.core.ParseException;
import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.core.ast.ProgramNode;
import com.davajava.migrator.parser.kotlin.KotlinParser;
import com.davajava.migrator.parser.java.JavaParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Android Studio projects and extracts structured information.
 * Supports Gradle-based Android projects with Kotlin, Java, and resource parsing.
 */
public class AndroidStudioProjectParser {
    private static final Logger logger = Logger.getLogger(AndroidStudioProjectParser.class.getName());
    
    // File patterns
    private static final Pattern GRADLE_FILE_PATTERN = Pattern.compile(".*\\.gradle(\\.kts)?$");
    private static final Pattern KOTLIN_FILE_PATTERN = Pattern.compile(".*\\.kt$");
    private static final Pattern JAVA_FILE_PATTERN = Pattern.compile(".*\\.java$");
    private static final Pattern XML_LAYOUT_PATTERN = Pattern.compile(".*res/layout/.*\\.xml$");
    private static final Pattern XML_VALUES_PATTERN = Pattern.compile(".*res/values/.*\\.xml$");
    private static final Pattern DRAWABLE_PATTERN = Pattern.compile(".*res/drawable.*");
    
    // Gradle configuration patterns
    private static final Pattern ANDROID_BLOCK_PATTERN = Pattern.compile(
        "android\\s*\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern DEPENDENCIES_BLOCK_PATTERN = Pattern.compile(
        "dependencies\\s*\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "(?:compileSdkVersion|targetSdkVersion|minSdkVersion)\\s+(\\d+)");
    private static final Pattern APPLICATION_ID_PATTERN = Pattern.compile(
        "applicationId\\s+[\"']([^\"']+)[\"']");
    
    private final KotlinParser kotlinParser;
    private final JavaParser javaParser;
    
    public AndroidStudioProjectParser() {
        this.kotlinParser = new KotlinParser();
        this.javaParser = new JavaParser(); // Assume this exists
    }
    
    /**
     * Parse an Android Studio project
     */
    public AndroidProjectParseResult parseProject(String projectPath) throws IOException, ParseException {
        return parseProject(Paths.get(projectPath));
    }
    
    public AndroidProjectParseResult parseProject(Path projectPath) throws IOException, ParseException {
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            throw new IllegalArgumentException("Invalid project directory: " + projectPath);
        }
        
        logger.info("Parsing Android Studio project: " + projectPath);
        
        AndroidProjectParseResult result = new AndroidProjectParseResult();
        result.setProjectPath(projectPath.toString());
        
        // Parse project structure
        parseProjectStructure(projectPath, result);
        
        // Parse Gradle configuration
        parseGradleConfiguration(projectPath, result);
        
        // Parse Android manifest
        parseAndroidManifest(projectPath, result);
        
        // Parse source files
        parseSourceFiles(projectPath, result);
        
        // Parse resources
        parseResources(projectPath, result);
        
        // Parse build configuration
        parseBuildConfiguration(projectPath, result);
        
        logger.info("Android Studio project parsing completed");
        
        return result;
    }
    
    private void parseProjectStructure(Path projectPath, AndroidProjectParseResult result) throws IOException {
        ProjectStructure structure = new ProjectStructure();
        
        // Find app module (main module)
        Path appModule = projectPath.resolve("app");
        if (Files.exists(appModule)) {
            structure.setAppModulePath(appModule.toString());
            
            // Find source directories
            Path javaSrc = appModule.resolve("src/main/java");
            Path kotlinSrc = appModule.resolve("src/main/kotlin");
            Path testSrc = appModule.resolve("src/test/java");
            Path androidTestSrc = appModule.resolve("src/androidTest/java");
            
            if (Files.exists(javaSrc)) {
                structure.addSourceDirectory(javaSrc.toString(), "java");
            }
            if (Files.exists(kotlinSrc)) {
                structure.addSourceDirectory(kotlinSrc.toString(), "kotlin");
            }
            if (Files.exists(testSrc)) {
                structure.addTestDirectory(testSrc.toString(), "unit");
            }
            if (Files.exists(androidTestSrc)) {
                structure.addTestDirectory(androidTestSrc.toString(), "instrumented");
            }
            
            // Find resource directories
            Path resDir = appModule.resolve("src/main/res");
            if (Files.exists(resDir)) {
                structure.setResourceDirectory(resDir.toString());
            }
            
            // Find assets directory
            Path assetsDir = appModule.resolve("src/main/assets");
            if (Files.exists(assetsDir)) {
                structure.setAssetsDirectory(assetsDir.toString());
            }
        }
        
        // Find additional modules
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(projectPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && !entry.getFileName().toString().equals("app")) {
                    Path moduleBuildGradle = entry.resolve("build.gradle");
                    if (Files.exists(moduleBuildGradle)) {
                        structure.addModule(entry.getFileName().toString(), entry.toString());
                    }
                }
            }
        }
        
        result.setProjectStructure(structure);
    }
    
    private void parseGradleConfiguration(Path projectPath, AndroidProjectParseResult result) throws IOException {
        GradleConfiguration config = new GradleConfiguration();
        
        // Parse project-level build.gradle
        Path projectBuildGradle = projectPath.resolve("build.gradle");
        if (Files.exists(projectBuildGradle)) {
            String buildContent = Files.readString(projectBuildGradle);
            config.setProjectBuildGradle(buildContent);
        }
        
        // Parse app-level build.gradle
        Path appBuildGradle = projectPath.resolve("app/build.gradle");
        if (Files.exists(appBuildGradle)) {
            String appBuildContent = Files.readString(appBuildGradle);
            config.setAppBuildGradle(appBuildContent);
            
            // Extract Android configuration
            parseAndroidBlock(appBuildContent, config);
            
            // Extract dependencies
            parseDependenciesBlock(appBuildContent, config);
        }
        
        // Parse gradle.properties
        Path gradleProperties = projectPath.resolve("gradle.properties");
        if (Files.exists(gradleProperties)) {
            String propertiesContent = Files.readString(gradleProperties);
            config.setGradleProperties(parseProperties(propertiesContent));
        }
        
        result.setGradleConfiguration(config);
    }
    
    private void parseAndroidBlock(String buildContent, GradleConfiguration config) {
        Matcher androidMatcher = ANDROID_BLOCK_PATTERN.matcher(buildContent);
        if (androidMatcher.find()) {
            String androidBlock = androidMatcher.group(1);
            
            // Extract versions
            Matcher versionMatcher = VERSION_PATTERN.matcher(androidBlock);
            while (versionMatcher.find()) {
                String versionType = versionMatcher.group(0).split("\\s+")[0];
                int version = Integer.parseInt(versionMatcher.group(1));
                
                switch (versionType) {
                    case "compileSdkVersion":
                        config.setCompileSdk(version);
                        break;
                    case "targetSdkVersion":
                        config.setTargetSdk(version);
                        break;
                    case "minSdkVersion":
                        config.setMinSdk(version);
                        break;
                }
            }
            
            // Extract application ID
            Matcher appIdMatcher = APPLICATION_ID_PATTERN.matcher(androidBlock);
            if (appIdMatcher.find()) {
                config.setApplicationId(appIdMatcher.group(1));
            }
            
            // Extract version code and name
            Pattern versionCodePattern = Pattern.compile("versionCode\\s+(\\d+)");
            Matcher versionCodeMatcher = versionCodePattern.matcher(androidBlock);
            if (versionCodeMatcher.find()) {
                config.setVersionCode(Integer.parseInt(versionCodeMatcher.group(1)));
            }
            
            Pattern versionNamePattern = Pattern.compile("versionName\\s+[\"']([^\"']+)[\"']");
            Matcher versionNameMatcher = versionNamePattern.matcher(androidBlock);
            if (versionNameMatcher.find()) {
                config.setVersionName(versionNameMatcher.group(1));
            }
        }
    }
    
    private void parseDependenciesBlock(String buildContent, GradleConfiguration config) {
        Matcher depMatcher = DEPENDENCIES_BLOCK_PATTERN.matcher(buildContent);
        if (depMatcher.find()) {
            String dependenciesBlock = depMatcher.group(1);
            
            // Parse implementation dependencies
            Pattern implPattern = Pattern.compile("implementation\\s+[\"']([^\"']+)[\"']");
            Matcher implMatcher = implPattern.matcher(dependenciesBlock);
            while (implMatcher.find()) {
                config.addDependency("implementation", implMatcher.group(1));
            }
            
            // Parse test dependencies
            Pattern testPattern = Pattern.compile("testImplementation\\s+[\"']([^\"']+)[\"']");
            Matcher testMatcher = testPattern.matcher(dependenciesBlock);
            while (testMatcher.find()) {
                config.addDependency("testImplementation", testMatcher.group(1));
            }
            
            // Parse Android test dependencies
            Pattern androidTestPattern = Pattern.compile("androidTestImplementation\\s+[\"']([^\"']+)[\"']");
            Matcher androidTestMatcher = androidTestPattern.matcher(dependenciesBlock);
            while (androidTestMatcher.find()) {
                config.addDependency("androidTestImplementation", androidTestMatcher.group(1));
            }
        }
    }
    
    private Map<String, String> parseProperties(String propertiesContent) {
        Map<String, String> properties = new HashMap<>();
        String[] lines = propertiesContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    properties.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        
        return properties;
    }
    
    private void parseAndroidManifest(Path projectPath, AndroidProjectParseResult result) throws IOException {
        Path manifestPath = projectPath.resolve("app/src/main/AndroidManifest.xml");
        if (Files.exists(manifestPath)) {
            String manifestContent = Files.readString(manifestPath);
            AndroidManifestInfo manifest = parseManifestContent(manifestContent);
            result.setManifest(manifest);
        }
    }
    
    private AndroidManifestInfo parseManifestContent(String manifestContent) {
        AndroidManifestInfo manifest = new AndroidManifestInfo();
        
        // Extract package name
        Pattern packagePattern = Pattern.compile("package\\s*=\\s*[\"']([^\"']+)[\"']");
        Matcher packageMatcher = packagePattern.matcher(manifestContent);
        if (packageMatcher.find()) {
            manifest.setPackageName(packageMatcher.group(1));
        }
        
        // Extract activities
        Pattern activityPattern = Pattern.compile("<activity[^>]*android:name\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
        Matcher activityMatcher = activityPattern.matcher(manifestContent);
        while (activityMatcher.find()) {
            manifest.addActivity(activityMatcher.group(1));
        }
        
        // Extract services
        Pattern servicePattern = Pattern.compile("<service[^>]*android:name\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
        Matcher serviceMatcher = servicePattern.matcher(manifestContent);
        while (serviceMatcher.find()) {
            manifest.addService(serviceMatcher.group(1));
        }
        
        // Extract receivers
        Pattern receiverPattern = Pattern.compile("<receiver[^>]*android:name\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>");
        Matcher receiverMatcher = receiverPattern.matcher(manifestContent);
        while (receiverMatcher.find()) {
            manifest.addReceiver(receiverMatcher.group(1));
        }
        
        // Extract permissions
        Pattern permissionPattern = Pattern.compile("<uses-permission[^>]*android:name\\s*=\\s*[\"']([^\"']+)[\"']");
        Matcher permissionMatcher = permissionPattern.matcher(manifestContent);
        while (permissionMatcher.find()) {
            manifest.addPermission(permissionMatcher.group(1));
        }
        
        return manifest;
    }
    
    private void parseSourceFiles(Path projectPath, AndroidProjectParseResult result) throws IOException, ParseException {
        ProjectStructure structure = result.getProjectStructure();
        
        for (Map.Entry<String, String> entry : structure.getSourceDirectories().entrySet()) {
            String srcPath = entry.getKey();
            String language = entry.getValue();
            
            Path sourcePath = Paths.get(srcPath);
            if (Files.exists(sourcePath)) {
                parseSourceDirectory(sourcePath, language, result);
            }
        }
    }
    
    private void parseSourceDirectory(Path sourceDir, String language, AndroidProjectParseResult result) 
            throws IOException, ParseException {
        
        Files.walk(sourceDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String fileName = file.toString();
                        
                        if ("kotlin".equals(language) && KOTLIN_FILE_PATTERN.matcher(fileName).matches()) {
                            ASTNode ast = kotlinParser.parseFile(fileName);
                            result.addSourceFile(fileName, ast, "kotlin");
                        } else if ("java".equals(language) && JAVA_FILE_PATTERN.matcher(fileName).matches()) {
                            // Assume JavaParser exists
                            // ASTNode ast = javaParser.parseFile(fileName);
                            // result.addSourceFile(fileName, ast, "java");
                            result.addSourceFile(fileName, new ProgramNode(1, 1), "java"); // Placeholder
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to parse source file: " + file + " - " + e.getMessage());
                    }
                });
    }
    
    private void parseResources(Path projectPath, AndroidProjectParseResult result) throws IOException {
        Path resDir = projectPath.resolve("app/src/main/res");
        if (!Files.exists(resDir)) return;
        
        ResourceInfo resources = new ResourceInfo();
        
        Files.walk(resDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        String fileName = file.toString();
                        String relativePath = resDir.relativize(file).toString().replace('\\', '/');
                        
                        if (XML_LAYOUT_PATTERN.matcher(fileName).matches()) {
                            String layoutContent = Files.readString(file);
                            resources.addLayout(relativePath, parseLayoutFile(layoutContent));
                        } else if (XML_VALUES_PATTERN.matcher(fileName).matches()) {
                            String valuesContent = Files.readString(file);
                            resources.addValues(relativePath, parseValuesFile(valuesContent));
                        } else if (DRAWABLE_PATTERN.matcher(fileName).matches()) {
                            resources.addDrawable(relativePath, file.toString());
                        } else {
                            resources.addRawResource(relativePath, file.toString());
                        }
                    } catch (IOException e) {
                        logger.warning("Failed to parse resource file: " + file + " - " + e.getMessage());
                    }
                });
        
        result.setResourceInfo(resources);
    }
    
    private LayoutInfo parseLayoutFile(String layoutContent) {
        LayoutInfo layout = new LayoutInfo();
        
        // Extract root element
        Pattern rootPattern = Pattern.compile("<(\\w+)[^>]*>");
        Matcher rootMatcher = rootPattern.matcher(layoutContent);
        if (rootMatcher.find()) {
            layout.setRootElement(rootMatcher.group(1));
        }
        
        // Extract UI elements
        Pattern elementPattern = Pattern.compile("<(\\w+)(?:[^>]*android:id\\s*=\\s*[\"']@\\+?id/([^\"']+)[\"'])?[^>]*>");
        Matcher elementMatcher = elementPattern.matcher(layoutContent);
        while (elementMatcher.find()) {
            String elementType = elementMatcher.group(1);
            String elementId = elementMatcher.group(2);
            layout.addElement(elementType, elementId);
        }
        
        return layout;
    }
    
    private ValuesInfo parseValuesFile(String valuesContent) {
        ValuesInfo values = new ValuesInfo();
        
        // Extract strings
        Pattern stringPattern = Pattern.compile("<string[^>]*name\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>([^<]*)</string>");
        Matcher stringMatcher = stringPattern.matcher(valuesContent);
        while (stringMatcher.find()) {
            values.addString(stringMatcher.group(1), stringMatcher.group(2));
        }
        
        // Extract colors
        Pattern colorPattern = Pattern.compile("<color[^>]*name\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>([^<]*)</color>");
        Matcher colorMatcher = colorPattern.matcher(valuesContent);
        while (colorMatcher.find()) {
            values.addColor(colorMatcher.group(1), colorMatcher.group(2));
        }
        
        // Extract dimensions
        Pattern dimenPattern = Pattern.compile("<dimen[^>]*name\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>([^<]*)</dimen>");
        Matcher dimenMatcher = dimenPattern.matcher(valuesContent);
        while (dimenMatcher.find()) {
            values.addDimension(dimenMatcher.group(1), dimenMatcher.group(2));
        }
        
        return values;
    }
    
    private void parseBuildConfiguration(Path projectPath, AndroidProjectParseResult result) throws IOException {
        BuildConfigInfo buildConfig = new BuildConfigInfo();
        
        // Parse local.properties
        Path localProperties = projectPath.resolve("local.properties");
        if (Files.exists(localProperties)) {
            String localPropsContent = Files.readString(localProperties);
            buildConfig.setLocalProperties(parseProperties(localPropsContent));
        }
        
        // Parse gradle wrapper properties
        Path wrapperProperties = projectPath.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (Files.exists(wrapperProperties)) {
            String wrapperContent = Files.readString(wrapperProperties);
            buildConfig.setWrapperProperties(parseProperties(wrapperContent));
        }
        
        result.setBuildConfiguration(buildConfig);
    }
    
    // Data classes for parsing results
    
    public static class AndroidProjectParseResult {
        private String projectPath;
        private ProjectStructure projectStructure;
        private GradleConfiguration gradleConfiguration;
        private AndroidManifestInfo manifest;
        private ResourceInfo resourceInfo;
        private BuildConfigInfo buildConfiguration;
        private Map<String, SourceFileInfo> sourceFiles = new HashMap<>();
        
        // Getters and setters
        public String getProjectPath() { return projectPath; }
        public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
        
        public ProjectStructure getProjectStructure() { return projectStructure; }
        public void setProjectStructure(ProjectStructure projectStructure) { this.projectStructure = projectStructure; }
        
        public GradleConfiguration getGradleConfiguration() { return gradleConfiguration; }
        public void setGradleConfiguration(GradleConfiguration gradleConfiguration) { this.gradleConfiguration = gradleConfiguration; }
        
        public AndroidManifestInfo getManifest() { return manifest; }
        public void setManifest(AndroidManifestInfo manifest) { this.manifest = manifest; }
        
        public ResourceInfo getResourceInfo() { return resourceInfo; }
        public void setResourceInfo(ResourceInfo resourceInfo) { this.resourceInfo = resourceInfo; }
        
        public BuildConfigInfo getBuildConfiguration() { return buildConfiguration; }
        public void setBuildConfiguration(BuildConfigInfo buildConfiguration) { this.buildConfiguration = buildConfiguration; }
        
        public Map<String, SourceFileInfo> getSourceFiles() { return sourceFiles; }
        public void addSourceFile(String path, ASTNode ast, String language) {
            sourceFiles.put(path, new SourceFileInfo(path, ast, language));
        }
    }
    
    public static class ProjectStructure {
        private String appModulePath;
        private String resourceDirectory;
        private String assetsDirectory;
        private Map<String, String> sourceDirectories = new HashMap<>();
        private Map<String, String> testDirectories = new HashMap<>();
        private Map<String, String> modules = new HashMap<>();
        
        // Getters and setters
        public String getAppModulePath() { return appModulePath; }
        public void setAppModulePath(String appModulePath) { this.appModulePath = appModulePath; }
        
        public String getResourceDirectory() { return resourceDirectory; }
        public void setResourceDirectory(String resourceDirectory) { this.resourceDirectory = resourceDirectory; }
        
        public String getAssetsDirectory() { return assetsDirectory; }
        public void setAssetsDirectory(String assetsDirectory) { this.assetsDirectory = assetsDirectory; }
        
        public Map<String, String> getSourceDirectories() { return sourceDirectories; }
        public void addSourceDirectory(String path, String type) { this.sourceDirectories.put(path, type); }
        
        public Map<String, String> getTestDirectories() { return testDirectories; }
        public void addTestDirectory(String path, String type) { this.testDirectories.put(path, type); }
        
        public Map<String, String> getModules() { return modules; }
        public void addModule(String name, String path) { this.modules.put(name, path); }
    }
    
    public static class GradleConfiguration {
        private String projectBuildGradle;
        private String appBuildGradle;
        private Map<String, String> gradleProperties = new HashMap<>();
        private Map<String, List<String>> dependencies = new HashMap<>();
        private String applicationId;
        private int compileSdk;
        private int targetSdk;
        private int minSdk;
        private int versionCode;
        private String versionName;
        
        // Getters and setters with implementations...
        public String getProjectBuildGradle() { return projectBuildGradle; }
        public void setProjectBuildGradle(String projectBuildGradle) { this.projectBuildGradle = projectBuildGradle; }
        
        public String getAppBuildGradle() { return appBuildGradle; }
        public void setAppBuildGradle(String appBuildGradle) { this.appBuildGradle = appBuildGradle; }
        
        public Map<String, String> getGradleProperties() { return gradleProperties; }
        public void setGradleProperties(Map<String, String> gradleProperties) { this.gradleProperties = gradleProperties; }
        
        public Map<String, List<String>> getDependencies() { return dependencies; }
        public void addDependency(String type, String dependency) {
            dependencies.computeIfAbsent(type, k -> new ArrayList<>()).add(dependency);
        }
        
        public String getApplicationId() { return applicationId; }
        public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
        
        public int getCompileSdk() { return compileSdk; }
        public void setCompileSdk(int compileSdk) { this.compileSdk = compileSdk; }
        
        public int getTargetSdk() { return targetSdk; }
        public void setTargetSdk(int targetSdk) { this.targetSdk = targetSdk; }
        
        public int getMinSdk() { return minSdk; }
        public void setMinSdk(int minSdk) { this.minSdk = minSdk; }
        
        public int getVersionCode() { return versionCode; }
        public void setVersionCode(int versionCode) { this.versionCode = versionCode; }
        
        public String getVersionName() { return versionName; }
        public void setVersionName(String versionName) { this.versionName = versionName; }
    }
    
    public static class AndroidManifestInfo {
        private String packageName;
        private List<String> activities = new ArrayList<>();
        private List<String> services = new ArrayList<>();
        private List<String> receivers = new ArrayList<>();
        private List<String> permissions = new ArrayList<>();
        
        // Getters and setters...
        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }
        
        public List<String> getActivities() { return activities; }
        public void addActivity(String activity) { this.activities.add(activity); }
        
        public List<String> getServices() { return services; }
        public void addService(String service) { this.services.add(service); }
        
        public List<String> getReceivers() { return receivers; }
        public void addReceiver(String receiver) { this.receivers.add(receiver); }
        
        public List<String> getPermissions() { return permissions; }
        public void addPermission(String permission) { this.permissions.add(permission); }
    }
    
    public static class ResourceInfo {
        private Map<String, LayoutInfo> layouts = new HashMap<>();
        private Map<String, ValuesInfo> values = new HashMap<>();
        private Map<String, String> drawables = new HashMap<>();
        private Map<String, String> rawResources = new HashMap<>();
        
        // Getters and setters...
        public Map<String, LayoutInfo> getLayouts() { return layouts; }
        public void addLayout(String name, LayoutInfo layout) { this.layouts.put(name, layout); }
        
        public Map<String, ValuesInfo> getValues() { return values; }
        public void addValues(String name, ValuesInfo values) { this.values.put(name, values); }
        
        public Map<String, String> getDrawables() { return drawables; }
        public void addDrawable(String name, String path) { this.drawables.put(name, path); }
        
        public Map<String, String> getRawResources() { return rawResources; }
        public void addRawResource(String name, String path) { this.rawResources.put(name, path); }
    }
    
    public static class LayoutInfo {
        private String rootElement;
        private Map<String, String> elements = new HashMap<>();
        
        public String getRootElement() { return rootElement; }
        public void setRootElement(String rootElement) { this.rootElement = rootElement; }
        
        public Map<String, String> getElements() { return elements; }
        public void addElement(String type, String id) { 
            if (id != null) {
                this.elements.put(id, type);
            }
        }
    }
    
    public static class ValuesInfo {
        private Map<String, String> strings = new HashMap<>();
        private Map<String, String> colors = new HashMap<>();
        private Map<String, String> dimensions = new HashMap<>();
        
        public Map<String, String> getStrings() { return strings; }
        public void addString(String name, String value) { this.strings.put(name, value); }
        
        public Map<String, String> getColors() { return colors; }
        public void addColor(String name, String value) { this.colors.put(name, value); }
        
        public Map<String, String> getDimensions() { return dimensions; }
        public void addDimension(String name, String value) { this.dimensions.put(name, value); }
    }
    
    public static class BuildConfigInfo {
        private Map<String, String> localProperties = new HashMap<>();
        private Map<String, String> wrapperProperties = new HashMap<>();
        
        public Map<String, String> getLocalProperties() { return localProperties; }
        public void setLocalProperties(Map<String, String> localProperties) { this.localProperties = localProperties; }
        
        public Map<String, String> getWrapperProperties() { return wrapperProperties; }
        public void setWrapperProperties(Map<String, String> wrapperProperties) { this.wrapperProperties = wrapperProperties; }
    }
    
    public static class SourceFileInfo {
        private String path;
        private ASTNode ast;
        private String language;
        
        public SourceFileInfo(String path, ASTNode ast, String language) {
            this.path = path;
            this.ast = ast;
            this.language = language;
        }
        
        public String getPath() { return path; }
        public ASTNode getAst() { return ast; }
        public String getLanguage() { return language; }
    }
}