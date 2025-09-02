package com.polytype.migrator.translator;

import com.polytype.migrator.core.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Enhanced multi-language translator with advanced optimization and semantic preservation.
 * 
 * This translator provides:
 * - Intelligent syntax mapping between languages
 * - Semantic preservation across paradigms (OOP ↔ Functional ↔ Procedural)
 * - Memory management translation (GC ↔ Manual ↔ Ownership)
 * - Concurrency model translation (Threads ↔ Async/Await ↔ Actors ↔ CSP)
 * - Type system translation (Dynamic ↔ Static ↔ Gradual)
 * - Error handling model translation (Exceptions ↔ Result Types ↔ Error Codes)
 * 
 * Optimization Features:
 * - Pattern-based translation caching
 * - Language idiom recognition and adaptation
 * - Performance-aware code generation
 * - Cross-language library mapping
 */
public class EnhancedMultiLanguageTranslator extends AbstractMultiTargetTranslator {
    
    private static final Logger logger = Logger.getLogger(EnhancedMultiLanguageTranslator.class.getName());
    
    // Translation caches for performance
    private final Map<String, String> expressionCache = new ConcurrentHashMap<>();
    private final Map<String, String> patternCache = new ConcurrentHashMap<>();
    
    // Language feature mappings
    private final Map<TargetLanguage, LanguageFeatures> languageFeatures = new HashMap<>();
    private final Map<LanguagePair, TranslationRules> translationRules = new HashMap<>();
    
    // Semantic analysis patterns
    private final Map<String, Pattern> semanticPatterns = new HashMap<>();
    
    public EnhancedMultiLanguageTranslator(SourceLanguage sourceLanguage) {
        super(sourceLanguage);
        initializeLanguageFeatures();
        initializeTranslationRules();
        initializeSemanticPatterns();
    }
    
    /**
     * Initialize language-specific features and capabilities.
     */
    private void initializeLanguageFeatures() {
        // Java - Enterprise, OOP, GC, Threads, Exceptions
        languageFeatures.put(TargetLanguage.JAVA, new LanguageFeatures()
            .setParadigm(Paradigm.OBJECT_ORIENTED)
            .setMemoryModel(MemoryModel.GARBAGE_COLLECTED)
            .setConcurrencyModel(ConcurrencyModel.THREADS)
            .setErrorHandling(ErrorHandling.EXCEPTIONS)
            .setTypeSystem(TypeSystem.STATIC_STRONG)
            .addIdiomatic("builder_pattern", "dependency_injection", "annotations"));
        
        // Python - Scripting, Multi-paradigm, GC, Async/Threading, Exceptions  
        languageFeatures.put(TargetLanguage.PYTHON, new LanguageFeatures()
            .setParadigm(Paradigm.MULTI_PARADIGM)
            .setMemoryModel(MemoryModel.GARBAGE_COLLECTED)
            .setConcurrencyModel(ConcurrencyModel.ASYNC_AWAIT)
            .setErrorHandling(ErrorHandling.EXCEPTIONS)
            .setTypeSystem(TypeSystem.DYNAMIC_DUCK)
            .addIdiomatic("list_comprehensions", "decorators", "context_managers", "generators"));
        
        // Rust - Systems, Ownership, Zero-cost abstractions, Result types
        languageFeatures.put(TargetLanguage.RUST, new LanguageFeatures()
            .setParadigm(Paradigm.FUNCTIONAL_IMPERATIVE)
            .setMemoryModel(MemoryModel.OWNERSHIP)
            .setConcurrencyModel(ConcurrencyModel.ASYNC_FUTURES)
            .setErrorHandling(ErrorHandling.RESULT_TYPES)
            .setTypeSystem(TypeSystem.STATIC_STRONG)
            .addIdiomatic("pattern_matching", "traits", "lifetimes", "zero_cost_abstractions"));
        
        // Go - Systems, Simple, GC, Goroutines, Error values
        languageFeatures.put(TargetLanguage.GO, new LanguageFeatures()
            .setParadigm(Paradigm.PROCEDURAL_CONCURRENT)
            .setMemoryModel(MemoryModel.GARBAGE_COLLECTED)
            .setConcurrencyModel(ConcurrencyModel.CSP_CHANNELS)
            .setErrorHandling(ErrorHandling.ERROR_VALUES)
            .setTypeSystem(TypeSystem.STATIC_STRUCTURAL)
            .addIdiomatic("interfaces", "goroutines", "channels", "defer"));
        
        // JavaScript - Web, Prototypal, GC, Event loop, Exceptions
        languageFeatures.put(TargetLanguage.JAVASCRIPT, new LanguageFeatures()
            .setParadigm(Paradigm.PROTOTYPE_FUNCTIONAL)
            .setMemoryModel(MemoryModel.GARBAGE_COLLECTED)
            .setConcurrencyModel(ConcurrencyModel.EVENT_LOOP)
            .setErrorHandling(ErrorHandling.EXCEPTIONS)
            .setTypeSystem(TypeSystem.DYNAMIC_WEAK)
            .addIdiomatic("closures", "promises", "destructuring", "arrow_functions"));
        
        // TypeScript - Web, OOP/Functional, GC, Event loop, Exceptions with strong typing
        languageFeatures.put(TargetLanguage.TYPESCRIPT, new LanguageFeatures()
            .setParadigm(Paradigm.OBJECT_FUNCTIONAL)
            .setMemoryModel(MemoryModel.GARBAGE_COLLECTED)
            .setConcurrencyModel(ConcurrencyModel.EVENT_LOOP)
            .setErrorHandling(ErrorHandling.EXCEPTIONS)
            .setTypeSystem(TypeSystem.GRADUAL_TYPING)
            .addIdiomatic("interfaces", "generics", "union_types", "decorators"));
        
        // C++ - Systems, Multi-paradigm, Manual/RAII, Threads, Exceptions
        languageFeatures.put(TargetLanguage.CPP, new LanguageFeatures()
            .setParadigm(Paradigm.MULTI_PARADIGM)
            .setMemoryModel(MemoryModel.MANUAL_RAII)
            .setConcurrencyModel(ConcurrencyModel.THREADS)
            .setErrorHandling(ErrorHandling.EXCEPTIONS)
            .setTypeSystem(TypeSystem.STATIC_STRONG)
            .addIdiomatic("raii", "smart_pointers", "templates", "move_semantics"));
        
        // C# - Enterprise, OOP, GC, Async/await, Exceptions
        languageFeatures.put(TargetLanguage.CSHARP, new LanguageFeatures()
            .setParadigm(Paradigm.OBJECT_ORIENTED)
            .setMemoryModel(MemoryModel.GARBAGE_COLLECTED)
            .setConcurrencyModel(ConcurrencyModel.ASYNC_AWAIT)
            .setErrorHandling(ErrorHandling.EXCEPTIONS)
            .setTypeSystem(TypeSystem.STATIC_STRONG)
            .addIdiomatic("properties", "linq", "generics", "nullable_types"));
        
        // Kotlin - JVM/Android, OOP/Functional, GC, Coroutines, Exceptions
        languageFeatures.put(TargetLanguage.KOTLIN, new LanguageFeatures()
            .setParadigm(Paradigm.OBJECT_FUNCTIONAL)
            .setMemoryModel(MemoryModel.GARBAGE_COLLECTED)
            .setConcurrencyModel(ConcurrencyModel.COROUTINES)
            .setErrorHandling(ErrorHandling.EXCEPTIONS)
            .setTypeSystem(TypeSystem.STATIC_NULL_SAFE)
            .addIdiomatic("extension_functions", "data_classes", "null_safety", "coroutines"));
        
        // Swift - iOS/macOS, OOP/Functional, ARC, Async/await, Optionals
        languageFeatures.put(TargetLanguage.SWIFT, new LanguageFeatures()
            .setParadigm(Paradigm.OBJECT_FUNCTIONAL)
            .setMemoryModel(MemoryModel.AUTOMATIC_RC)
            .setConcurrencyModel(ConcurrencyModel.ASYNC_AWAIT)
            .setErrorHandling(ErrorHandling.OPTIONALS_RESULTS)
            .setTypeSystem(TypeSystem.STATIC_STRONG)
            .addIdiomatic("optionals", "protocols", "extensions", "closures"));
        
        logger.info("Initialized language features for " + languageFeatures.size() + " languages");
    }
    
    /**
     * Initialize translation rules between language pairs.
     */
    private void initializeTranslationRules() {
        // High-level translation strategies
        
        // Python → Java: OOP emphasis, collection mapping, exception preservation
        addTranslationRule(SourceLanguage.PYTHON, TargetLanguage.JAVA,
            new TranslationRules()
                .addRule("list_comprehensions", "stream_operations")
                .addRule("decorators", "annotations_aspects")
                .addRule("dynamic_typing", "generics_with_bounds")
                .addRule("duck_typing", "interfaces")
                .addRule("generators", "iterators_streams")
                .addRule("context_managers", "try_with_resources")
                .addRule("multiple_inheritance", "interfaces_composition"));
        
        // Java → Python: Simplification, duck typing, comprehensions
        addTranslationRule(SourceLanguage.JAVA, TargetLanguage.PYTHON,
            new TranslationRules()
                .addRule("stream_operations", "list_comprehensions")
                .addRule("interfaces", "duck_typing_protocols")
                .addRule("generics", "type_hints")
                .addRule("annotations", "decorators")
                .addRule("try_with_resources", "context_managers")
                .addRule("verbose_loops", "comprehensions")
                .addRule("builders", "dataclasses_constructors"));
        
        // Python → Rust: Memory safety, error handling, ownership
        addTranslationRule(SourceLanguage.PYTHON, TargetLanguage.RUST,
            new TranslationRules()
                .addRule("exceptions", "result_types")
                .addRule("dynamic_typing", "strong_typing_enums")
                .addRule("list_comprehensions", "iterator_methods")
                .addRule("duck_typing", "traits")
                .addRule("reference_semantics", "ownership_borrowing")
                .addRule("runtime_errors", "compile_time_safety")
                .addRule("gc_memory", "stack_heap_explicit"));
        
        // JavaScript → TypeScript: Type safety, interfaces
        addTranslationRule(SourceLanguage.JAVASCRIPT, TargetLanguage.TYPESCRIPT,
            new TranslationRules()
                .addRule("dynamic_typing", "static_typing")
                .addRule("duck_typing", "interfaces")
                .addRule("prototype_inheritance", "class_inheritance")
                .addRule("weak_typing", "strong_typing")
                .addRule("runtime_checks", "compile_time_checks")
                .addRule("any_type", "specific_types")
                .addRule("loose_equality", "strict_equality"));
        
        // C++ → Rust: Memory safety preservation, RAII → ownership
        addTranslationRule(SourceLanguage.CPP, TargetLanguage.RUST,
            new TranslationRules()
                .addRule("raw_pointers", "references_boxes")
                .addRule("manual_memory", "ownership_system")
                .addRule("raii", "drop_trait")
                .addRule("exceptions", "result_types")
                .addRule("templates", "generics_traits")
                .addRule("multiple_inheritance", "traits_composition")
                .addRule("undefined_behavior", "memory_safety"));
        
        // Go → other languages: Simplicity preservation
        addTranslationRule(SourceLanguage.GO, TargetLanguage.RUST,
            new TranslationRules()
                .addRule("goroutines", "async_tasks")
                .addRule("channels", "mpsc_channels")
                .addRule("interfaces", "traits")
                .addRule("error_values", "result_types")
                .addRule("defer", "drop_raii")
                .addRule("gc", "ownership")
                .addRule("simplicity", "zero_cost_abstractions"));
        
        logger.info("Initialized translation rules for " + translationRules.size() + " language pairs");
    }
    
    /**
     * Initialize semantic pattern recognition.
     */
    private void initializeSemanticPatterns() {
        // Common programming patterns that need semantic translation
        semanticPatterns.put("singleton_pattern", Pattern.compile(
            "class\\s+(\\w+).*?private\\s+static\\s+\\w+\\s+instance.*?getInstance\\(\\)", 
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
            
        semanticPatterns.put("factory_pattern", Pattern.compile(
            "class\\s+(\\w+)Factory.*?create\\w*\\(.*?\\).*?return\\s+new", 
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
            
        semanticPatterns.put("observer_pattern", Pattern.compile(
            "interface\\s+\\w*Observer.*?notify|update\\(", 
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
            
        semanticPatterns.put("async_pattern", Pattern.compile(
            "async\\s+\\w+|await\\s+|Promise\\s*<|Future\\s*<|CompletableFuture",
            Pattern.CASE_INSENSITIVE));
            
        semanticPatterns.put("error_handling", Pattern.compile(
            "try\\s*\\{|catch\\s*\\(|Result\\s*<|Option\\s*<|Maybe\\s*<",
            Pattern.CASE_INSENSITIVE));
            
        logger.info("Initialized " + semanticPatterns.size() + " semantic patterns");
    }
    
    /**
     * Enhanced translation with semantic analysis and optimization.
     */
    @Override
    public String translateTo(ASTNode ast, TargetLanguage targetLanguage, TranslationOptions options) 
            throws TranslationException {
        
        // Check cache first
        String cacheKey = generateCacheKey(ast, targetLanguage, options);
        String cachedResult = expressionCache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // Perform semantic analysis
        SemanticContext context = analyzeSemantics(ast, targetLanguage);
        
        // Apply language-specific optimizations
        optimizeForTarget(context, targetLanguage, options);
        
        // Perform translation with enhanced context
        String result = performEnhancedTranslation(ast, context, targetLanguage, options);
        
        // Post-process and optimize
        result = postProcessTranslation(result, context, targetLanguage, options);
        
        // Cache result
        expressionCache.put(cacheKey, result);
        
        return result;
    }
    
    /**
     * Analyze semantic context for better translation decisions.
     */
    private SemanticContext analyzeSemantics(ASTNode ast, TargetLanguage targetLanguage) {
        SemanticContext context = new SemanticContext();
        
        // Detect programming patterns
        String astString = ast.toString();
        for (Map.Entry<String, Pattern> entry : semanticPatterns.entrySet()) {
            if (entry.getValue().matcher(astString).find()) {
                context.addDetectedPattern(entry.getKey());
            }
        }
        
        // Analyze language feature usage
        LanguageFeatures sourceFeatures = languageFeatures.get(
            TargetLanguage.valueOf(getSourceLanguage().name()));
        LanguageFeatures targetFeatures = languageFeatures.get(targetLanguage);
        
        if (sourceFeatures != null && targetFeatures != null) {
            context.setFeatureCompatibility(
                calculateFeatureCompatibility(sourceFeatures, targetFeatures));
        }
        
        return context;
    }
    
    /**
     * Optimize translation strategy for target language.
     */
    private void optimizeForTarget(SemanticContext context, TargetLanguage targetLanguage, 
                                 TranslationOptions options) {
        LanguageFeatures targetFeatures = languageFeatures.get(targetLanguage);
        
        if (targetFeatures != null) {
            // Adjust translation options based on target capabilities
            switch (targetFeatures.getMemoryModel()) {
                case OWNERSHIP:
                    options.setOption("explicit_memory_management", true);
                    options.setOption("use_borrowing", true);
                    break;
                case GARBAGE_COLLECTED:
                    options.setOption("explicit_memory_management", false);
                    options.setOption("use_gc_idioms", true);
                    break;
            }
            
            switch (targetFeatures.getConcurrencyModel()) {
                case ASYNC_AWAIT:
                    options.setOption("prefer_async_await", true);
                    break;
                case COROUTINES:
                    options.setOption("use_coroutines", true);
                    break;
                case CSP_CHANNELS:
                    options.setOption("use_channels", true);
                    break;
            }
            
            switch (targetFeatures.getErrorHandling()) {
                case RESULT_TYPES:
                    options.setOption("convert_exceptions_to_results", true);
                    break;
                case OPTIONALS_RESULTS:
                    options.setOption("use_optionals", true);
                    break;
            }
        }
    }
    
    /**
     * Perform enhanced translation with semantic context.
     */
    private String performEnhancedTranslation(ASTNode ast, SemanticContext context,
                                            TargetLanguage targetLanguage, TranslationOptions options)
            throws TranslationException {
        
        // Get appropriate visitor for target language
        TargetVisitor visitor = getVisitor(targetLanguage);
        if (visitor == null) {
            throw new TranslationException("No visitor available for target language: " + targetLanguage);
        }
        
        // Apply semantic context to visitor
        if (visitor instanceof SemanticAwareVisitor) {
            ((SemanticAwareVisitor) visitor).setSemanticContext(context);
        }
        
        // Perform translation
        return visitor.visit(ast, options);
    }
    
    /**
     * Post-process translation with optimizations and idiom adaptation.
     */
    private String postProcessTranslation(String translatedCode, SemanticContext context,
                                        TargetLanguage targetLanguage, TranslationOptions options) {
        StringBuilder result = new StringBuilder(translatedCode);
        
        // Apply target-language specific idioms
        LanguageFeatures targetFeatures = languageFeatures.get(targetLanguage);
        if (targetFeatures != null) {
            for (String idiom : targetFeatures.getIdiomaticFeatures()) {
                applyIdiom(result, idiom, targetLanguage);
            }
        }
        
        // Apply semantic pattern adaptations
        for (String pattern : context.getDetectedPatterns()) {
            adaptPattern(result, pattern, targetLanguage);
        }
        
        // Optimize for performance
        if (options.getBooleanOption("optimize_performance", false)) {
            optimizePerformance(result, targetLanguage);
        }
        
        return result.toString();
    }
    
    // Helper methods
    
    private void addTranslationRule(SourceLanguage source, TargetLanguage target, TranslationRules rules) {
        translationRules.put(new LanguagePair(source, target), rules);
    }
    
    private double calculateFeatureCompatibility(LanguageFeatures source, LanguageFeatures target) {
        double compatibility = 1.0;
        
        // Paradigm compatibility
        if (source.getParadigm() != target.getParadigm()) {
            compatibility *= 0.8;
        }
        
        // Memory model compatibility
        if (source.getMemoryModel() != target.getMemoryModel()) {
            compatibility *= 0.7;
        }
        
        // Error handling compatibility
        if (source.getErrorHandling() != target.getErrorHandling()) {
            compatibility *= 0.9;
        }
        
        return compatibility;
    }
    
    private String generateCacheKey(ASTNode ast, TargetLanguage target, TranslationOptions options) {
        return ast.hashCode() + ":" + target.name() + ":" + options.hashCode();
    }
    
    private void applyIdiom(StringBuilder code, String idiom, TargetLanguage targetLanguage) {
        // Apply language-specific idioms
        switch (idiom) {
            case "list_comprehensions":
                if (targetLanguage == TargetLanguage.PYTHON) {
                    convertToListComprehensions(code);
                }
                break;
            case "stream_operations":
                if (targetLanguage == TargetLanguage.JAVA) {
                    convertToStreamOperations(code);
                }
                break;
            // Add more idiom conversions
        }
    }
    
    private void adaptPattern(StringBuilder code, String pattern, TargetLanguage targetLanguage) {
        // Adapt design patterns to target language idioms
        switch (pattern) {
            case "singleton_pattern":
                adaptSingletonPattern(code, targetLanguage);
                break;
            case "factory_pattern":
                adaptFactoryPattern(code, targetLanguage);
                break;
            // Add more pattern adaptations
        }
    }
    
    private void optimizePerformance(StringBuilder code, TargetLanguage targetLanguage) {
        // Apply performance optimizations specific to target language
        switch (targetLanguage) {
            case JAVA:
                optimizeJavaPerformance(code);
                break;
            case PYTHON:
                optimizePythonPerformance(code);
                break;
            case RUST:
                optimizeRustPerformance(code);
                break;
            // Add more language-specific optimizations
        }
    }
    
    // Placeholder implementations for optimization methods
    private void convertToListComprehensions(StringBuilder code) {
        // Implementation for converting loops to list comprehensions
    }
    
    private void convertToStreamOperations(StringBuilder code) {
        // Implementation for converting loops to stream operations
    }
    
    private void adaptSingletonPattern(StringBuilder code, TargetLanguage targetLanguage) {
        // Adapt singleton pattern to target language (enum in Java, module in Python, etc.)
    }
    
    private void adaptFactoryPattern(StringBuilder code, TargetLanguage targetLanguage) {
        // Adapt factory pattern to target language idioms
    }
    
    private void optimizeJavaPerformance(StringBuilder code) {
        // Java-specific performance optimizations
    }
    
    private void optimizePythonPerformance(StringBuilder code) {
        // Python-specific performance optimizations
    }
    
    private void optimizeRustPerformance(StringBuilder code) {
        // Rust-specific performance optimizations
    }
    
    // Supporting classes and enums
    
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
    
    public static class LanguageFeatures {
        private Paradigm paradigm;
        private MemoryModel memoryModel;
        private ConcurrencyModel concurrencyModel;
        private ErrorHandling errorHandling;
        private TypeSystem typeSystem;
        private Set<String> idiomaticFeatures = new HashSet<>();
        
        public LanguageFeatures setParadigm(Paradigm paradigm) {
            this.paradigm = paradigm; return this;
        }
        
        public LanguageFeatures setMemoryModel(MemoryModel memoryModel) {
            this.memoryModel = memoryModel; return this;
        }
        
        public LanguageFeatures setConcurrencyModel(ConcurrencyModel concurrencyModel) {
            this.concurrencyModel = concurrencyModel; return this;
        }
        
        public LanguageFeatures setErrorHandling(ErrorHandling errorHandling) {
            this.errorHandling = errorHandling; return this;
        }
        
        public LanguageFeatures setTypeSystem(TypeSystem typeSystem) {
            this.typeSystem = typeSystem; return this;
        }
        
        public LanguageFeatures addIdiomatic(String... features) {
            this.idiomaticFeatures.addAll(Arrays.asList(features)); return this;
        }
        
        // Getters
        public Paradigm getParadigm() { return paradigm; }
        public MemoryModel getMemoryModel() { return memoryModel; }
        public ConcurrencyModel getConcurrencyModel() { return concurrencyModel; }
        public ErrorHandling getErrorHandling() { return errorHandling; }
        public TypeSystem getTypeSystem() { return typeSystem; }
        public Set<String> getIdiomaticFeatures() { return idiomaticFeatures; }
    }
    
    public static class TranslationRules {
        private final Map<String, String> rules = new HashMap<>();
        
        public TranslationRules addRule(String from, String to) {
            rules.put(from, to);
            return this;
        }
        
        public String getMapping(String from) {
            return rules.get(from);
        }
    }
    
    public static class SemanticContext {
        private final Set<String> detectedPatterns = new HashSet<>();
        private double featureCompatibility = 1.0;
        
        public void addDetectedPattern(String pattern) {
            detectedPatterns.add(pattern);
        }
        
        public Set<String> getDetectedPatterns() {
            return detectedPatterns;
        }
        
        public void setFeatureCompatibility(double compatibility) {
            this.featureCompatibility = compatibility;
        }
        
        public double getFeatureCompatibility() {
            return featureCompatibility;
        }
    }
    
    public interface SemanticAwareVisitor {
        void setSemanticContext(SemanticContext context);
    }
    
    public enum Paradigm {
        OBJECT_ORIENTED, FUNCTIONAL, PROCEDURAL, MULTI_PARADIGM,
        PROTOTYPE_FUNCTIONAL, OBJECT_FUNCTIONAL, FUNCTIONAL_IMPERATIVE,
        PROCEDURAL_CONCURRENT
    }
    
    public enum MemoryModel {
        GARBAGE_COLLECTED, MANUAL_RAII, OWNERSHIP, AUTOMATIC_RC
    }
    
    public enum ConcurrencyModel {
        THREADS, ASYNC_AWAIT, COROUTINES, CSP_CHANNELS, 
        EVENT_LOOP, ASYNC_FUTURES, ACTORS
    }
    
    public enum ErrorHandling {
        EXCEPTIONS, RESULT_TYPES, ERROR_VALUES, OPTIONALS_RESULTS
    }
    
    public enum TypeSystem {
        STATIC_STRONG, DYNAMIC_DUCK, DYNAMIC_WEAK, GRADUAL_TYPING,
        STATIC_NULL_SAFE, STATIC_STRUCTURAL
    }
}