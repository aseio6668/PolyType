package com.davajava.migrator.core;

import java.util.HashMap;
import java.util.Map;

public class TranslationOptions {
    private final Map<String, Object> options;

    public TranslationOptions() {
        this.options = new HashMap<>();
    }

    public TranslationOptions(Map<String, Object> options) {
        this.options = new HashMap<>(options);
    }

    public <T> T getOption(String key, Class<T> type, T defaultValue) {
        Object value = options.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return defaultValue;
    }

    public void setOption(String key, Object value) {
        options.put(key, value);
    }

    public boolean getBooleanOption(String key, boolean defaultValue) {
        return getOption(key, Boolean.class, defaultValue);
    }

    public String getStringOption(String key, String defaultValue) {
        return getOption(key, String.class, defaultValue);
    }

    public int getIntOption(String key, int defaultValue) {
        return getOption(key, Integer.class, defaultValue);
    }

    public TranslationOptions copy() {
        return new TranslationOptions(this.options);
    }

    public static TranslationOptions defaultOptions() {
        TranslationOptions options = new TranslationOptions();
        options.setOption("generateComments", true);
        options.setOption("preserveOriginalNames", true);
        options.setOption("includeTypeAnnotations", true);
        options.setOption("generateJavaDoc", false);
        options.setOption("indentSize", 4);
        return options;
    }
}