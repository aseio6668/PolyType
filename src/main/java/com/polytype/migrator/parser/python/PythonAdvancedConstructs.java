package com.polytype.migrator.parser.python;

import com.polytype.migrator.core.ast.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Handler for advanced Python constructs including decorators, context managers,
 * metaclasses, descriptors, and other Python-specific features that require
 * special parsing and translation logic.
 */
public class PythonAdvancedConstructs {
    
    // Decorator patterns
    private static final Pattern DECORATOR_PATTERN = Pattern.compile(
        "@([\\w.]+)(?:\\(([^)]*)\\))?\\s*"
    );
    
    private static final Pattern PROPERTY_DECORATOR_PATTERN = Pattern.compile(
        "@property|@(\\w+)\\.(setter|deleter)"
    );
    
    private static final Pattern STATICMETHOD_PATTERN = Pattern.compile(
        "@staticmethod"
    );
    
    private static final Pattern CLASSMETHOD_PATTERN = Pattern.compile(
        "@classmethod"
    );
    
    private static final Pattern DATACLASS_DECORATOR_PATTERN = Pattern.compile(
        "@dataclass(?:\\(([^)]*)\\))?"
    );
    
    // Context manager patterns
    private static final Pattern WITH_STATEMENT_PATTERN = Pattern.compile(
        "^\\s*(?:(async)\\s+)?with\\s+(.+?)\\s+as\\s+(\\w+):\\s*$"
    );
    
    private static final Pattern WITH_MULTIPLE_PATTERN = Pattern.compile(
        "^\\s*(?:(async)\\s+)?with\\s+(.+?):\\s*$"
    );
    
    private static final Pattern CONTEXTMANAGER_DECORATOR_PATTERN = Pattern.compile(
        "@contextmanager|@asynccontextmanager"
    );
    
    // Metaclass patterns
    private static final Pattern METACLASS_PATTERN = Pattern.compile(
        "class\\s+(\\w+)\\(.*?metaclass\\s*=\\s*(\\w+).*?\\)"
    );
    
    // Descriptor patterns
    private static final Pattern DESCRIPTOR_PATTERN = Pattern.compile(
        "def\\s+(__get__|__set__|__delete__)\\s*\\(self"
    );
    
    // Generator and coroutine patterns
    private static final Pattern YIELD_PATTERN = Pattern.compile(
        "\\byield\\b(?:\\s+from\\s+|\\s+)"
    );
    
    private static final Pattern AWAIT_PATTERN = Pattern.compile(
        "\\bawait\\s+"
    );
    
    private static final Pattern ASYNC_COMPREHENSION_PATTERN = Pattern.compile(
        "async\\s+for\\s+.+?\\s+in\\s+"
    );
    
    /**
     * Parse and handle decorator applications on functions and classes.
     */
    public static class DecoratorInfo {
        public final String name;
        public final List<String> arguments;
        public final DecoratorType type;
        public final Map<String, String> metadata;
        
        public DecoratorInfo(String name, List<String> arguments, DecoratorType type) {
            this.name = name;
            this.arguments = arguments != null ? arguments : new ArrayList<>();
            this.type = type;
            this.metadata = new HashMap<>();
        }
        
        public boolean isBuiltin() {
            return type == DecoratorType.PROPERTY || 
                   type == DecoratorType.STATICMETHOD || 
                   type == DecoratorType.CLASSMETHOD ||
                   type == DecoratorType.DATACLASS;
        }
        
        public boolean isCustom() {
            return type == DecoratorType.CUSTOM;
        }
    }
    
    public enum DecoratorType {
        PROPERTY, STATICMETHOD, CLASSMETHOD, DATACLASS, CONTEXTMANAGER,
        VALIDATOR, CACHED_PROPERTY, LRU_CACHE, ABSTRACTMETHOD, 
        OVERRIDE, FINAL, CUSTOM
    }
    
    /**
     * Parse decorator from a line of code.
     */
    public static DecoratorInfo parseDecorator(String decoratorLine) {
        Matcher matcher = DECORATOR_PATTERN.matcher(decoratorLine.trim());
        if (!matcher.find()) {
            return null;
        }
        
        String decoratorName = matcher.group(1);
        String argumentsStr = matcher.group(2);
        
        List<String> arguments = new ArrayList<>();
        if (argumentsStr != null && !argumentsStr.trim().isEmpty()) {
            arguments = parseDecoratorArguments(argumentsStr);
        }
        
        DecoratorType type = determineDecoratorType(decoratorName);
        
        return new DecoratorInfo(decoratorName, arguments, type);
    }
    
    /**
     * Parse multiple decorators applied to a single function/class.
     */
    public static List<DecoratorInfo> parseDecorators(List<String> decoratorLines) {
        List<DecoratorInfo> decorators = new ArrayList<>();
        
        for (String line : decoratorLines) {
            DecoratorInfo decorator = parseDecorator(line);
            if (decorator != null) {
                decorators.add(decorator);
            }
        }
        
        return decorators;
    }
    
    private static DecoratorType determineDecoratorType(String name) {
        switch (name.toLowerCase()) {
            case "property": return DecoratorType.PROPERTY;
            case "staticmethod": return DecoratorType.STATICMETHOD;
            case "classmethod": return DecoratorType.CLASSMETHOD;
            case "dataclass": return DecoratorType.DATACLASS;
            case "contextmanager": 
            case "asynccontextmanager": return DecoratorType.CONTEXTMANAGER;
            case "cached_property": return DecoratorType.CACHED_PROPERTY;
            case "lru_cache": return DecoratorType.LRU_CACHE;
            case "abstractmethod": return DecoratorType.ABSTRACTMETHOD;
            case "override": return DecoratorType.OVERRIDE;
            case "final": return DecoratorType.FINAL;
            case "validator": return DecoratorType.VALIDATOR;
            default: return DecoratorType.CUSTOM;
        }
    }
    
    private static List<String> parseDecoratorArguments(String argumentsStr) {
        List<String> arguments = new ArrayList<>();
        
        // Simple parsing - split by commas but handle nested parentheses
        int parenDepth = 0;
        StringBuilder currentArg = new StringBuilder();
        
        for (char c : argumentsStr.toCharArray()) {
            if (c == '(') {
                parenDepth++;
                currentArg.append(c);
            } else if (c == ')') {
                parenDepth--;
                currentArg.append(c);
            } else if (c == ',' && parenDepth == 0) {
                arguments.add(currentArg.toString().trim());
                currentArg = new StringBuilder();
            } else {
                currentArg.append(c);
            }
        }
        
        if (currentArg.length() > 0) {
            arguments.add(currentArg.toString().trim());
        }
        
        return arguments;
    }
    
    /**
     * Context Manager handling
     */
    public static class ContextManagerInfo {
        public final String expression;
        public final String variable;
        public final boolean isAsync;
        public final List<String> multipleContexts;
        
        public ContextManagerInfo(String expression, String variable, boolean isAsync) {
            this.expression = expression;
            this.variable = variable;
            this.isAsync = isAsync;
            this.multipleContexts = new ArrayList<>();
        }
        
        public ContextManagerInfo(List<String> contexts, boolean isAsync) {
            this.expression = null;
            this.variable = null;
            this.isAsync = isAsync;
            this.multipleContexts = contexts;
        }
    }
    
    /**
     * Parse with statement for context managers.
     */
    public static ContextManagerInfo parseWithStatement(String withLine) {
        // Handle single context manager with 'as' clause
        Matcher singleMatcher = WITH_STATEMENT_PATTERN.matcher(withLine);
        if (singleMatcher.find()) {
            boolean isAsync = singleMatcher.group(1) != null;
            String expression = singleMatcher.group(2).trim();
            String variable = singleMatcher.group(3);
            return new ContextManagerInfo(expression, variable, isAsync);
        }
        
        // Handle multiple context managers or without 'as' clause
        Matcher multipleMatcher = WITH_MULTIPLE_PATTERN.matcher(withLine);
        if (multipleMatcher.find()) {
            boolean isAsync = multipleMatcher.group(1) != null;
            String contextsStr = multipleMatcher.group(2).trim();
            
            List<String> contexts = parseMultipleContexts(contextsStr);
            return new ContextManagerInfo(contexts, isAsync);
        }
        
        return null;
    }
    
    private static List<String> parseMultipleContexts(String contextsStr) {
        List<String> contexts = new ArrayList<>();
        
        // Split by comma, but handle nested parentheses and brackets
        int parenDepth = 0;
        int bracketDepth = 0;
        StringBuilder currentContext = new StringBuilder();
        
        for (char c : contextsStr.toCharArray()) {
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
            } else if (c == '[') {
                bracketDepth++;
            } else if (c == ']') {
                bracketDepth--;
            } else if (c == ',' && parenDepth == 0 && bracketDepth == 0) {
                String context = currentContext.toString().trim();
                if (!context.isEmpty()) {
                    contexts.add(context);
                }
                currentContext = new StringBuilder();
                continue;
            }
            
            currentContext.append(c);
        }
        
        String lastContext = currentContext.toString().trim();
        if (!lastContext.isEmpty()) {
            contexts.add(lastContext);
        }
        
        return contexts;
    }
    
    /**
     * Analyze if a function is a context manager (has __enter__ and __exit__).
     */
    public static boolean isContextManager(ClassDeclarationNode classNode) {
        boolean hasEnter = false;
        boolean hasExit = false;
        
        for (ASTNode child : classNode.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                String funcName = func.getName();
                
                if ("__enter__".equals(funcName)) {
                    hasEnter = true;
                } else if ("__exit__".equals(funcName)) {
                    hasExit = true;
                }
                
                // Check for async context manager methods
                if ("__aenter__".equals(funcName)) {
                    hasEnter = true;
                } else if ("__aexit__".equals(funcName)) {
                    hasExit = true;
                }
            }
        }
        
        return hasEnter && hasExit;
    }
    
    /**
     * Generator and Coroutine Analysis
     */
    public static class GeneratorInfo {
        public final boolean isGenerator;
        public final boolean isCoroutine;
        public final boolean isAsyncGenerator;
        public final List<String> yieldExpressions;
        public final List<String> awaitExpressions;
        
        public GeneratorInfo(boolean isGenerator, boolean isCoroutine, 
                           boolean isAsyncGenerator,
                           List<String> yieldExpressions, 
                           List<String> awaitExpressions) {
            this.isGenerator = isGenerator;
            this.isCoroutine = isCoroutine;
            this.isAsyncGenerator = isAsyncGenerator;
            this.yieldExpressions = yieldExpressions != null ? yieldExpressions : new ArrayList<>();
            this.awaitExpressions = awaitExpressions != null ? awaitExpressions : new ArrayList<>();
        }
    }
    
    /**
     * Analyze if a function is a generator or coroutine.
     */
    public static GeneratorInfo analyzeGeneratorCoroutine(FunctionDeclarationNode funcNode, String sourceCode) {
        boolean isAsync = funcNode.getName().contains("async"); // Simplified check
        List<String> yieldExpressions = new ArrayList<>();
        List<String> awaitExpressions = new ArrayList<>();
        
        // This would require analyzing the function body
        // For now, provide basic detection patterns
        
        boolean hasYield = sourceCode.contains("yield");
        boolean hasAwait = sourceCode.contains("await");
        
        boolean isGenerator = hasYield && !isAsync;
        boolean isCoroutine = isAsync && !hasYield;
        boolean isAsyncGenerator = isAsync && hasYield;
        
        return new GeneratorInfo(isGenerator, isCoroutine, isAsyncGenerator, 
                               yieldExpressions, awaitExpressions);
    }
    
    /**
     * Descriptor Analysis
     */
    public static boolean isDescriptor(ClassDeclarationNode classNode) {
        for (ASTNode child : classNode.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                String funcName = func.getName();
                
                if ("__get__".equals(funcName) || 
                    "__set__".equals(funcName) || 
                    "__delete__".equals(funcName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Metaclass Analysis
     */
    public static String extractMetaclass(String classDefinition) {
        Matcher matcher = METACLASS_PATTERN.matcher(classDefinition);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }
    
    /**
     * Convert Python decorators to target language equivalents.
     */
    public static class DecoratorTranslation {
        public final String targetEquivalent;
        public final List<String> imports;
        public final Map<String, String> annotations;
        
        public DecoratorTranslation(String targetEquivalent, List<String> imports, Map<String, String> annotations) {
            this.targetEquivalent = targetEquivalent;
            this.imports = imports != null ? imports : new ArrayList<>();
            this.annotations = annotations != null ? annotations : new HashMap<>();
        }
    }
    
    /**
     * Translate Python decorators to Java annotations or other target equivalents.
     */
    public static DecoratorTranslation translateDecoratorToJava(DecoratorInfo decorator) {
        Map<String, String> annotations = new HashMap<>();
        List<String> imports = new ArrayList<>();
        String equivalent = "";
        
        switch (decorator.type) {
            case PROPERTY:
                // Java doesn't have property decorators - generate getter method
                equivalent = "// Property getter - implement with getter method";
                break;
                
            case STATICMETHOD:
                equivalent = "static";
                break;
                
            case CLASSMETHOD:
                // Java static method accessing class
                equivalent = "static";
                break;
                
            case DATACLASS:
                // Use record or generate boilerplate
                annotations.put("Generated", "\"DataClass equivalent\"");
                imports.add("javax.annotation.Generated");
                equivalent = "// Dataclass - consider using Java record or Lombok @Data";
                break;
                
            case CACHED_PROPERTY:
                // Use Caffeine or Guava cache
                imports.add("com.github.benmanes.caffeine.cache.Cache");
                equivalent = "// Cached property - implement with Cache";
                break;
                
            case LRU_CACHE:
                imports.add("java.util.concurrent.ConcurrentHashMap");
                equivalent = "@LRUCache";
                break;
                
            case ABSTRACTMETHOD:
                equivalent = "abstract";
                break;
                
            case OVERRIDE:
                annotations.put("Override", "");
                equivalent = "@Override";
                break;
                
            case CONTEXTMANAGER:
                // Java try-with-resources or custom solution
                imports.add("java.lang.AutoCloseable");
                equivalent = "// Context manager - implement AutoCloseable";
                break;
                
            case CUSTOM:
                // Custom decorator - might need custom annotation
                equivalent = "// Custom decorator: " + decorator.name;
                break;
                
            default:
                equivalent = "// Decorator: " + decorator.name;
        }
        
        return new DecoratorTranslation(equivalent, imports, annotations);
    }
    
    /**
     * Translate context managers to target language equivalents.
     */
    public static String translateContextManagerToJava(ContextManagerInfo contextManager) {
        if (contextManager.expression != null) {
            return String.format(
                "try (%s %s = %s) {",
                "AutoCloseable", // Would need proper type inference
                contextManager.variable,
                contextManager.expression
            );
        } else {
            // Multiple context managers - generate nested try-with-resources
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < contextManager.multipleContexts.size(); i++) {
                sb.append("try (AutoCloseable resource").append(i)
                  .append(" = ").append(contextManager.multipleContexts.get(i))
                  .append(") {\n");
            }
            return sb.toString();
        }
    }
}