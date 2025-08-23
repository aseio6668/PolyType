package com.davajava.migrator.core;

public enum SourceLanguage {
    RUST(".rs", "Rust"),
    CRYSTAL(".cr", "Crystal"),
    C(".c", "C"),
    CPP(".cpp", "C++"),
    PYTHON(".py", "Python"),
    CSHARP(".cs", "C#"),
    KOTLIN(".kt", "Kotlin"),
    SCALA(".scala", "Scala"),
    JAVASCRIPT(".js", "JavaScript"),
    TYPESCRIPT(".ts", "TypeScript"),
    GO(".go", "Go"),
    SWIFT(".swift", "Swift");

    private final String fileExtension;
    private final String displayName;

    SourceLanguage(String fileExtension, String displayName) {
        this.fileExtension = fileExtension;
        this.displayName = displayName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SourceLanguage fromFileExtension(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        for (SourceLanguage lang : values()) {
            if (lang.fileExtension.equals(extension)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unsupported file extension: " + extension);
    }
}