package com.davajava.migrator.core;

import java.io.File;

public class MigrationCommand {
    private final String inputPath;
    private final String outputPath;
    private final SourceLanguage language;
    private final boolean recursive;
    private final String packageName;
    private final boolean preserveComments;
    private final boolean generateJavaDoc;

    public MigrationCommand(String inputPath, String outputPath, SourceLanguage language) {
        this(inputPath, outputPath, language, false, "com.migrated", true, false);
    }

    public MigrationCommand(String inputPath, String outputPath, SourceLanguage language, 
                          boolean recursive, String packageName, boolean preserveComments, boolean generateJavaDoc) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.language = language;
        this.recursive = recursive;
        this.packageName = packageName;
        this.preserveComments = preserveComments;
        this.generateJavaDoc = generateJavaDoc;
    }

    public String getInputPath() { return inputPath; }
    public String getOutputPath() { return outputPath; }
    public SourceLanguage getLanguage() { return language; }
    public SourceLanguage getSourceLanguage() { return language; }
    public boolean isRecursive() { return recursive; }
    public String getPackageName() { return packageName; }
    public boolean isPreserveComments() { return preserveComments; }
    public boolean isGenerateJavaDoc() { return generateJavaDoc; }
    
    public File getInputFile() { return new File(inputPath); }
    public File getOutputDir() { return new File(outputPath); }
}