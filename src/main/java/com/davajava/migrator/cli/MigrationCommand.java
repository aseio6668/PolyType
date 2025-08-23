package com.davajava.migrator.cli;

import com.davajava.migrator.core.SourceLanguage;

public class MigrationCommand {
    private String inputPath;
    private String outputPath;
    private String packageName;
    private String configPath;
    private SourceLanguage sourceLanguage;
    private boolean verbose;
    private boolean recursive;
    private boolean preserveComments;
    private boolean generateJavaDoc;

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
}