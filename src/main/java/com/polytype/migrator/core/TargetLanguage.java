package com.polytype.migrator.core;

/**
 * Enumeration of supported target languages for code migration.
 * PolyType supports multiple output formats from any input language.
 */
public enum TargetLanguage {
    JAVA(".java", "Java", "Java source code"),
    CPP(".cpp", "C++", "C++ source code"),
    PYTHON(".py", "Python", "Python source code"),
    JAVASCRIPT(".js", "JavaScript", "JavaScript source code"),
    TYPESCRIPT(".ts", "TypeScript", "TypeScript source code"),
    CSHARP(".cs", "C#", "C# source code"),
    GO(".go", "Go", "Go source code"),
    RUST(".rs", "Rust", "Rust source code"),
    KOTLIN(".kt", "Kotlin", "Kotlin source code"),
    SWIFT(".swift", "Swift", "Swift source code");
    
    private final String extension;
    private final String displayName;
    private final String description;
    
    TargetLanguage(String extension, String displayName, String description) {
        this.extension = extension;
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getExtension() {
        return extension;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static TargetLanguage fromString(String languageString) {
        if (languageString == null) {
            return JAVA; // Default target
        }
        
        String normalized = languageString.toLowerCase().trim();
        
        switch (normalized) {
            case "java":
                return JAVA;
            case "cpp":
            case "c++":
                return CPP;
            case "python":
            case "py":
                return PYTHON;
            case "javascript":
            case "js":
                return JAVASCRIPT;
            case "typescript":
            case "ts":
                return TYPESCRIPT;
            case "csharp":
            case "c#":
                return CSHARP;
            case "go":
            case "golang":
                return GO;
            case "rust":
            case "rs":
                return RUST;
            case "kotlin":
            case "kt":
                return KOTLIN;
            case "swift":
                return SWIFT;
            default:
                throw new IllegalArgumentException("Unsupported target language: " + languageString);
        }
    }
}