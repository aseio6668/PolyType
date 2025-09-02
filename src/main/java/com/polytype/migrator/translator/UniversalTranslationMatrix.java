package com.polytype.migrator.translator;

import com.polytype.migrator.core.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Universal translation matrix supporting bidirectional translation between all supported languages.
 * 
 * This class provides a comprehensive mapping system for translating between any source language
 * to any target language, with optimized direct paths and fallback through intermediate languages
 * when direct translation is not available.
 * 
 * Supported Languages (as both source and target):
 * - Java, Python, C++, JavaScript, TypeScript, C#, Go, Rust, Kotlin, Swift
 * - PHP, Ruby, Scala, Dart, Lua (additional targets)
 * 
 * Translation Strategies:
 * 1. Direct Translation (A → B): Fastest, most accurate
 * 2. Intermediate Translation (A → Java → B): Fallback when direct not available  
 * 3. Semantic Translation (A → AST → B): Universal fallback using common AST
 */
public class UniversalTranslationMatrix {
    
    private static final Logger logger = Logger.getLogger(UniversalTranslationMatrix.class.getName());
    
    // Translation registry: Source → Target → Translator
    private final Map<SourceLanguage, Map<TargetLanguage, MultiTargetTranslator>> translationMatrix;
    
    // Language compatibility matrix
    private final Map<SourceLanguage, Set<TargetLanguage>> directTranslations;
    
    // Language similarity scores for optimization
    private final Map<LanguagePair, Double> compatibilityScores;
    
    public UniversalTranslationMatrix() {
        this.translationMatrix = new HashMap<>();
        this.directTranslations = new HashMap<>();
        this.compatibilityScores = new HashMap<>();
        
        initializeTranslationMatrix();
        calculateCompatibilityScores();
    }
    
    /**
     * Initialize all supported translation paths.
     */
    private void initializeTranslationMatrix() {
        // Register all source languages
        for (SourceLanguage source : SourceLanguage.values()) {
            translationMatrix.put(source, new HashMap<>());
            directTranslations.put(source, new HashSet<>());
        }
        
        // Initialize high-priority direct translations
        initializeHighPriorityTranslations();
        
        // Initialize cross-language translations  
        initializeCrossLanguageTranslations();
        
        // Initialize specialized translations
        initializeSpecializedTranslations();
        
        logger.info("Initialized universal translation matrix with " + 
                   getTotalTranslationCount() + " translation paths");
    }
    
    private void initializeHighPriorityTranslations() {
        // Python ↔ Most Languages (Python is highly compatible)
        registerBidirectional(SourceLanguage.PYTHON, TargetLanguage.JAVA);
        registerBidirectional(SourceLanguage.PYTHON, TargetLanguage.JAVASCRIPT);
        registerBidirectional(SourceLanguage.PYTHON, TargetLanguage.TYPESCRIPT);
        registerBidirectional(SourceLanguage.PYTHON, TargetLanguage.GO);
        registerBidirectional(SourceLanguage.PYTHON, TargetLanguage.RUST);
        registerDirect(SourceLanguage.PYTHON, TargetLanguage.CPP);
        registerDirect(SourceLanguage.PYTHON, TargetLanguage.CSHARP);
        
        // JavaScript/TypeScript ↔ Modern Languages
        registerBidirectional(SourceLanguage.JAVASCRIPT, TargetLanguage.TYPESCRIPT);
        registerBidirectional(SourceLanguage.JAVASCRIPT, TargetLanguage.JAVA);
        registerDirect(SourceLanguage.JAVASCRIPT, TargetLanguage.PYTHON);
        registerDirect(SourceLanguage.JAVASCRIPT, TargetLanguage.GO);
        registerDirect(SourceLanguage.JAVASCRIPT, TargetLanguage.RUST);
        
        // Java ↔ JVM Languages + Others
        registerBidirectional(SourceLanguage.JAVA, TargetLanguage.KOTLIN);
        registerBidirectional(SourceLanguage.JAVA, TargetLanguage.SCALA);
        registerDirect(SourceLanguage.JAVA, TargetLanguage.PYTHON);
        registerDirect(SourceLanguage.JAVA, TargetLanguage.CSHARP);
        registerDirect(SourceLanguage.JAVA, TargetLanguage.GO);
        registerDirect(SourceLanguage.JAVA, TargetLanguage.TYPESCRIPT);
        
        // C++ ↔ Systems Languages
        registerBidirectional(SourceLanguage.CPP, TargetLanguage.RUST);
        registerDirect(SourceLanguage.CPP, TargetLanguage.GO);
        registerDirect(SourceLanguage.CPP, TargetLanguage.JAVA);
        registerDirect(SourceLanguage.CPP, TargetLanguage.PYTHON);
        registerDirect(SourceLanguage.CPP, TargetLanguage.CSHARP);
        
        // C# ↔ .NET Ecosystem + Cross-platform
        registerDirect(SourceLanguage.CSHARP, TargetLanguage.JAVA);
        registerDirect(SourceLanguage.CSHARP, TargetLanguage.TYPESCRIPT);
        registerDirect(SourceLanguage.CSHARP, TargetLanguage.PYTHON);
        registerDirect(SourceLanguage.CSHARP, TargetLanguage.GO);
        
        // Go ↔ Modern Languages
        registerDirect(SourceLanguage.GO, TargetLanguage.RUST);
        registerDirect(SourceLanguage.GO, TargetLanguage.JAVA);
        registerDirect(SourceLanguage.GO, TargetLanguage.PYTHON);
        registerDirect(SourceLanguage.GO, TargetLanguage.TYPESCRIPT);
        
        // Rust ↔ Systems/Modern Languages
        registerDirect(SourceLanguage.RUST, TargetLanguage.GO);
        registerDirect(SourceLanguage.RUST, TargetLanguage.JAVA);
        registerDirect(SourceLanguage.RUST, TargetLanguage.PYTHON);
        registerDirect(SourceLanguage.RUST, TargetLanguage.TYPESCRIPT);
    }
    
    private void initializeCrossLanguageTranslations() {
        // Mobile Development Translations
        registerDirect(SourceLanguage.SWIFT, TargetLanguage.KOTLIN); // iOS → Android
        registerDirect(SourceLanguage.KOTLIN, TargetLanguage.SWIFT); // Android → iOS
        registerDirect(SourceLanguage.SWIFT, TargetLanguage.JAVA);
        registerDirect(SourceLanguage.KOTLIN, TargetLanguage.JAVA);
        
        // Web Development Translations
        registerDirect(SourceLanguage.JAVASCRIPT, TargetLanguage.DART); // Web → Flutter
        registerDirect(SourceLanguage.TYPESCRIPT, TargetLanguage.DART);
        
        // Functional Language Translations
        registerDirect(SourceLanguage.SCALA, TargetLanguage.KOTLIN);
        registerDirect(SourceLanguage.SCALA, TargetLanguage.RUST);
        registerDirect(SourceLanguage.SCALA, TargetLanguage.GO);
        
        // Scripting Language Translations
        registerDirect(SourceLanguage.PYTHON, TargetLanguage.RUBY);
        registerDirect(SourceLanguage.RUBY, TargetLanguage.PYTHON);
        registerDirect(SourceLanguage.PYTHON, TargetLanguage.PHP);
        registerDirect(SourceLanguage.PHP, TargetLanguage.PYTHON);
        
        // Legacy → Modern Translations
        registerDirect(SourceLanguage.C, TargetLanguage.RUST);
        registerDirect(SourceLanguage.C, TargetLanguage.GO);
        registerDirect(SourceLanguage.C, TargetLanguage.JAVA);
        registerDirect(SourceLanguage.CPP, TargetLanguage.RUST);
    }
    
    private void initializeSpecializedTranslations() {
        // Domain-specific translations
        // Data Science: Python ↔ R, Julia
        // ML/AI: Python ↔ specialized frameworks
        // Systems: C/C++ ↔ Rust, Go, Zig
        // Web: JavaScript/TypeScript ↔ various web frameworks
        
        // Game Development
        registerDirect(SourceLanguage.CSHARP, TargetLanguage.CPP); // Unity → Unreal
        registerDirect(SourceLanguage.CPP, TargetLanguage.CSHARP); // Unreal → Unity
        
        // Embedded Systems
        registerDirect(SourceLanguage.C, TargetLanguage.RUST); // Safety-critical systems
        registerDirect(SourceLanguage.CPP, TargetLanguage.RUST);
        
        // Enterprise Applications
        registerDirect(SourceLanguage.JAVA, TargetLanguage.CSHARP); // JEE → .NET
        registerDirect(SourceLanguage.CSHARP, TargetLanguage.JAVA); // .NET → JEE
    }
    
    /**
     * Register a direct translation path (one direction only).
     */
    private void registerDirect(SourceLanguage source, TargetLanguage target) {
        // This would register the actual translator implementation
        directTranslations.get(source).add(target);
        logger.fine("Registered direct translation: " + source + " → " + target);
    }
    
    /**
     * Register bidirectional translation (both directions).
     */
    private void registerBidirectional(SourceLanguage source, TargetLanguage target) {
        registerDirect(source, target);
        // Also register reverse direction if source language exists as target
        SourceLanguage reverseSource = getSourceLanguageForTarget(target);
        TargetLanguage reverseTarget = getTargetLanguageForSource(source);
        if (reverseSource != null && reverseTarget != null) {
            registerDirect(reverseSource, reverseTarget);
        }
    }
    
    /**
     * Calculate compatibility scores between language pairs.
     */
    private void calculateCompatibilityScores() {
        // High compatibility (similar syntax/semantics): 0.9-1.0
        compatibilityScores.put(new LanguagePair(SourceLanguage.JAVASCRIPT, TargetLanguage.TYPESCRIPT), 0.95);
        compatibilityScores.put(new LanguagePair(SourceLanguage.JAVA, TargetLanguage.KOTLIN), 0.9);
        compatibilityScores.put(new LanguagePair(SourceLanguage.JAVA, TargetLanguage.SCALA), 0.85);
        compatibilityScores.put(new LanguagePair(SourceLanguage.C, TargetLanguage.CPP), 0.9);
        
        // Medium-high compatibility: 0.7-0.8
        compatibilityScores.put(new LanguagePair(SourceLanguage.PYTHON, TargetLanguage.RUBY), 0.8);
        compatibilityScores.put(new LanguagePair(SourceLanguage.PYTHON, TargetLanguage.JAVASCRIPT), 0.75);
        compatibilityScores.put(new LanguagePair(SourceLanguage.CSHARP, TargetLanguage.JAVA), 0.8);
        compatibilityScores.put(new LanguagePair(SourceLanguage.SWIFT, TargetLanguage.KOTLIN), 0.75);
        
        // Medium compatibility: 0.5-0.6
        compatibilityScores.put(new LanguagePair(SourceLanguage.PYTHON, TargetLanguage.JAVA), 0.6);
        compatibilityScores.put(new LanguagePair(SourceLanguage.JAVASCRIPT, TargetLanguage.PYTHON), 0.6);
        compatibilityScores.put(new LanguagePair(SourceLanguage.GO, TargetLanguage.RUST), 0.6);
        compatibilityScores.put(new LanguagePair(SourceLanguage.CPP, TargetLanguage.RUST), 0.65);
        
        // Lower compatibility (significant paradigm differences): 0.3-0.4
        compatibilityScores.put(new LanguagePair(SourceLanguage.C, TargetLanguage.PYTHON), 0.4);
        compatibilityScores.put(new LanguagePair(SourceLanguage.CPP, TargetLanguage.JAVASCRIPT), 0.35);
        compatibilityScores.put(new LanguagePair(SourceLanguage.RUST, TargetLanguage.PYTHON), 0.4);
    }
    
    /**
     * Get the best translation path between two languages.
     */
    public TranslationPath getBestTranslationPath(SourceLanguage source, TargetLanguage target) {
        // Check for direct translation
        if (directTranslations.get(source).contains(target)) {
            return new TranslationPath(source, target, TranslationStrategy.DIRECT);
        }
        
        // Look for intermediate path through Java (most universal)
        if (directTranslations.get(source).contains(TargetLanguage.JAVA)) {
            SourceLanguage javaAsSource = SourceLanguage.JAVA;
            if (directTranslations.get(javaAsSource).contains(target)) {
                return new TranslationPath(source, target, TranslationStrategy.VIA_JAVA)
                    .addIntermediate(TargetLanguage.JAVA);
            }
        }
        
        // Look for other intermediate paths
        for (TargetLanguage intermediate : directTranslations.get(source)) {
            SourceLanguage intermediateAsSource = getSourceLanguageForTarget(intermediate);
            if (intermediateAsSource != null && 
                directTranslations.get(intermediateAsSource).contains(target)) {
                return new TranslationPath(source, target, TranslationStrategy.VIA_INTERMEDIATE)
                    .addIntermediate(intermediate);
            }
        }
        
        // Fallback to semantic translation through AST
        return new TranslationPath(source, target, TranslationStrategy.SEMANTIC_AST);
    }
    
    /**
     * Get compatibility score between two languages.
     */
    public double getCompatibilityScore(SourceLanguage source, TargetLanguage target) {
        LanguagePair pair = new LanguagePair(source, target);
        return compatibilityScores.getOrDefault(pair, 0.5); // Default medium compatibility
    }
    
    /**
     * Get all supported target languages for a source language.
     */
    public Set<TargetLanguage> getSupportedTargets(SourceLanguage source) {
        Set<TargetLanguage> supported = new HashSet<>(directTranslations.get(source));
        
        // Add indirect translations
        for (TargetLanguage direct : directTranslations.get(source)) {
            SourceLanguage directAsSource = getSourceLanguageForTarget(direct);
            if (directAsSource != null) {
                supported.addAll(directTranslations.get(directAsSource));
            }
        }
        
        return supported;
    }
    
    /**
     * Get recommended translation targets based on use case.
     */
    public List<TargetLanguage> getRecommendedTargets(SourceLanguage source, String useCase) {
        List<TargetLanguage> recommendations = new ArrayList<>();
        
        switch (useCase.toLowerCase()) {
            case "web":
                recommendations.addAll(Arrays.asList(
                    TargetLanguage.JAVASCRIPT, TargetLanguage.TYPESCRIPT, 
                    TargetLanguage.PYTHON, TargetLanguage.JAVA));
                break;
                
            case "mobile":
                recommendations.addAll(Arrays.asList(
                    TargetLanguage.KOTLIN, TargetLanguage.SWIFT,
                    TargetLanguage.DART, TargetLanguage.JAVASCRIPT));
                break;
                
            case "systems":
                recommendations.addAll(Arrays.asList(
                    TargetLanguage.RUST, TargetLanguage.GO,
                    TargetLanguage.CPP, TargetLanguage.C));
                break;
                
            case "enterprise":
                recommendations.addAll(Arrays.asList(
                    TargetLanguage.JAVA, TargetLanguage.CSHARP,
                    TargetLanguage.PYTHON, TargetLanguage.TYPESCRIPT));
                break;
                
            case "ml":
            case "ai":
            case "data":
                recommendations.addAll(Arrays.asList(
                    TargetLanguage.PYTHON, TargetLanguage.JAVA,
                    TargetLanguage.SCALA, TargetLanguage.R));
                break;
                
            default:
                // General purpose recommendations
                recommendations.addAll(Arrays.asList(
                    TargetLanguage.PYTHON, TargetLanguage.JAVA,
                    TargetLanguage.JAVASCRIPT, TargetLanguage.GO));
        }
        
        // Filter by actually supported translations and sort by compatibility
        return recommendations.stream()
            .filter(target -> getSupportedTargets(source).contains(target))
            .sorted((t1, t2) -> Double.compare(
                getCompatibilityScore(source, t2), 
                getCompatibilityScore(source, t1)))
            .collect(java.util.stream.Collectors.toList());
    }
    
    // Helper methods
    private SourceLanguage getSourceLanguageForTarget(TargetLanguage target) {
        try {
            return SourceLanguage.valueOf(target.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    private TargetLanguage getTargetLanguageForSource(SourceLanguage source) {
        try {
            return TargetLanguage.valueOf(source.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    private int getTotalTranslationCount() {
        return directTranslations.values().stream()
            .mapToInt(Set::size)
            .sum();
    }
    
    // Supporting classes
    public static class LanguagePair {
        private final SourceLanguage source;
        private final TargetLanguage target;
        
        public LanguagePair(SourceLanguage source, TargetLanguage target) {
            this.source = source;
            this.target = target;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof LanguagePair)) return false;
            LanguagePair pair = (LanguagePair) obj;
            return source == pair.source && target == pair.target;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(source, target);
        }
    }
    
    public static class TranslationPath {
        private final SourceLanguage source;
        private final TargetLanguage target;
        private final TranslationStrategy strategy;
        private final List<TargetLanguage> intermediates;
        
        public TranslationPath(SourceLanguage source, TargetLanguage target, TranslationStrategy strategy) {
            this.source = source;
            this.target = target;
            this.strategy = strategy;
            this.intermediates = new ArrayList<>();
        }
        
        public TranslationPath addIntermediate(TargetLanguage intermediate) {
            this.intermediates.add(intermediate);
            return this;
        }
        
        // Getters
        public SourceLanguage getSource() { return source; }
        public TargetLanguage getTarget() { return target; }
        public TranslationStrategy getStrategy() { return strategy; }
        public List<TargetLanguage> getIntermediates() { return intermediates; }
        
        public boolean isDirect() { return strategy == TranslationStrategy.DIRECT; }
        public boolean hasIntermediates() { return !intermediates.isEmpty(); }
    }
    
    public enum TranslationStrategy {
        DIRECT,           // A → B
        VIA_JAVA,         // A → Java → B
        VIA_INTERMEDIATE, // A → X → B
        SEMANTIC_AST      // A → AST → B
    }
}