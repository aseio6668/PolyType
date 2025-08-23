package com.davajava.migrator.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MigrationConfig {
    @JsonProperty("general")
    private GeneralConfig general = new GeneralConfig();
    
    @JsonProperty("translation")
    private TranslationConfig translation = new TranslationConfig();
    
    @JsonProperty("language_specific")
    private Map<String, Map<String, Object>> languageSpecific = new HashMap<>();

    public static class GeneralConfig {
        @JsonProperty("default_package")
        private String defaultPackage = "com.generated";
        
        @JsonProperty("preserve_comments")
        private boolean preserveComments = true;
        
        @JsonProperty("generate_javadoc")
        private boolean generateJavaDoc = false;
        
        @JsonProperty("verbose")
        private boolean verbose = false;

        // Getters and setters
        public String getDefaultPackage() { return defaultPackage; }
        public void setDefaultPackage(String defaultPackage) { this.defaultPackage = defaultPackage; }
        
        public boolean isPreserveComments() { return preserveComments; }
        public void setPreserveComments(boolean preserveComments) { this.preserveComments = preserveComments; }
        
        public boolean isGenerateJavaDoc() { return generateJavaDoc; }
        public void setGenerateJavaDoc(boolean generateJavaDoc) { this.generateJavaDoc = generateJavaDoc; }
        
        public boolean isVerbose() { return verbose; }
        public void setVerbose(boolean verbose) { this.verbose = verbose; }
    }

    public static class TranslationConfig {
        @JsonProperty("indent_size")
        private int indentSize = 4;
        
        @JsonProperty("include_type_annotations")
        private boolean includeTypeAnnotations = true;
        
        @JsonProperty("generate_constructors")
        private boolean generateConstructors = true;
        
        @JsonProperty("generate_getters_setters")
        private boolean generateGettersSetters = true;
        
        @JsonProperty("type_mappings")
        private Map<String, String> typeMappings = new HashMap<>();

        // Getters and setters
        public int getIndentSize() { return indentSize; }
        public void setIndentSize(int indentSize) { this.indentSize = indentSize; }
        
        public boolean isIncludeTypeAnnotations() { return includeTypeAnnotations; }
        public void setIncludeTypeAnnotations(boolean includeTypeAnnotations) { this.includeTypeAnnotations = includeTypeAnnotations; }
        
        public boolean isGenerateConstructors() { return generateConstructors; }
        public void setGenerateConstructors(boolean generateConstructors) { this.generateConstructors = generateConstructors; }
        
        public boolean isGenerateGettersSetters() { return generateGettersSetters; }
        public void setGenerateGettersSetters(boolean generateGettersSetters) { this.generateGettersSetters = generateGettersSetters; }
        
        public Map<String, String> getTypeMappings() { return typeMappings; }
        public void setTypeMappings(Map<String, String> typeMappings) { this.typeMappings = typeMappings; }
    }

    // Main getters and setters
    public GeneralConfig getGeneral() { return general; }
    public void setGeneral(GeneralConfig general) { this.general = general; }
    
    public TranslationConfig getTranslation() { return translation; }
    public void setTranslation(TranslationConfig translation) { this.translation = translation; }
    
    public Map<String, Map<String, Object>> getLanguageSpecific() { return languageSpecific; }
    public void setLanguageSpecific(Map<String, Map<String, Object>> languageSpecific) { this.languageSpecific = languageSpecific; }

    // Utility methods
    public static MigrationConfig fromFile(Path configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(Files.readString(configPath), MigrationConfig.class);
    }

    public void toFile(Path configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        Files.writeString(configPath, json);
    }

    public static MigrationConfig createDefault() {
        MigrationConfig config = new MigrationConfig();
        
        // Set up default type mappings
        config.translation.typeMappings.put("rust.i32", "int");
        config.translation.typeMappings.put("rust.i64", "long");
        config.translation.typeMappings.put("rust.f32", "float");
        config.translation.typeMappings.put("rust.f64", "double");
        config.translation.typeMappings.put("rust.String", "String");
        config.translation.typeMappings.put("rust.bool", "boolean");
        
        config.translation.typeMappings.put("python.int", "int");
        config.translation.typeMappings.put("python.float", "double");
        config.translation.typeMappings.put("python.str", "String");
        config.translation.typeMappings.put("python.bool", "boolean");
        config.translation.typeMappings.put("python.list", "List");
        config.translation.typeMappings.put("python.dict", "Map");
        
        // Language-specific settings
        Map<String, Object> rustSettings = new HashMap<>();
        rustSettings.put("handle_ownership", true);
        rustSettings.put("convert_traits", true);
        rustSettings.put("handle_lifetimes", false);
        config.languageSpecific.put("rust", rustSettings);
        
        Map<String, Object> pythonSettings = new HashMap<>();
        pythonSettings.put("generate_imports", true);
        pythonSettings.put("handle_duck_typing", true);
        pythonSettings.put("add_type_comments", true);
        config.languageSpecific.put("python", pythonSettings);
        
        return config;
    }
}