package com.polytype.migrator.cli;

import com.polytype.migrator.core.SourceLanguage;
import com.polytype.migrator.core.TargetLanguage;

public class MigrationCommand {
    private String inputPath;
    private String outputPath;
    private String packageName;
    private String configPath;
    private SourceLanguage sourceLanguage;
    private TargetLanguage targetLanguage = TargetLanguage.JAVA; // Default to Java
    private boolean verbose;
    private boolean recursive;
    private boolean preserveComments;
    private boolean generateJavaDoc;
    
    // Android-specific options
    private boolean apkDecompile;
    private boolean androidToWeb;
    private boolean androidProject;
    private String webFramework;

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public SourceLanguage getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(SourceLanguage sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public boolean isPreserveComments() {
        return preserveComments;
    }

    public void setPreserveComments(boolean preserveComments) {
        this.preserveComments = preserveComments;
    }

    public boolean isGenerateJavaDoc() {
        return generateJavaDoc;
    }

    public void setGenerateJavaDoc(boolean generateJavaDoc) {
        this.generateJavaDoc = generateJavaDoc;
    }
    
    public TargetLanguage getTargetLanguage() {
        return targetLanguage;
    }
    
    public void setTargetLanguage(TargetLanguage targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
    
    // Android-specific getters and setters
    public boolean isApkDecompile() {
        return apkDecompile;
    }
    
    public void setApkDecompile(boolean apkDecompile) {
        this.apkDecompile = apkDecompile;
    }
    
    public boolean isAndroidToWeb() {
        return androidToWeb;
    }
    
    public void setAndroidToWeb(boolean androidToWeb) {
        this.androidToWeb = androidToWeb;
    }
    
    public boolean isAndroidProject() {
        return androidProject;
    }
    
    public void setAndroidProject(boolean androidProject) {
        this.androidProject = androidProject;
    }
    
    public String getWebFramework() {
        return webFramework;
    }
    
    public void setWebFramework(String webFramework) {
        this.webFramework = webFramework;
    }
}