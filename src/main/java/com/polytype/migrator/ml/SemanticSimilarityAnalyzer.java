package com.polytype.migrator.ml;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Advanced semantic similarity analyzer for deep code understanding and comparison.
 * Implements multiple similarity metrics including structural, semantic, and behavioral
 * similarity measures for cross-language code analysis.
 */
public class SemanticSimilarityAnalyzer {
    
    public enum SimilarityMetric {
        STRUCTURAL,      // AST structure similarity
        LEXICAL,         // Token/identifier similarity  
        SEMANTIC,        // Meaning/purpose similarity
        BEHAVIORAL,      // Runtime behavior similarity
        CONTROL_FLOW,    // Control flow graph similarity
        DATA_FLOW,       // Data dependency similarity
        PATTERN,         // Design pattern similarity
        COMPLEXITY       // Algorithmic complexity similarity
    }
    
    public static class SimilarityResult {
        private final double overallSimilarity;
        private final Map<SimilarityMetric, Double> metricScores;
        private final List<String> similarityReasons;
        private final List<String> differences;
        private final double confidence;
        
        public SimilarityResult(double overallSimilarity, Map<SimilarityMetric, Double> metricScores,
                              List<String> similarityReasons, List<String> differences, double confidence) {
            this.overallSimilarity = overallSimilarity;
            this.metricScores = new HashMap<>(metricScores);
            this.similarityReasons = new ArrayList<>(similarityReasons);
            this.differences = new ArrayList<>(differences);
            this.confidence = confidence;
        }
        
        public double getOverallSimilarity() { return overallSimilarity; }
        public Map<SimilarityMetric, Double> getMetricScores() { return metricScores; }
        public List<String> getSimilarityReasons() { return similarityReasons; }
        public List<String> getDifferences() { return differences; }
        public double getConfidence() { return confidence; }
        
        public boolean isHighSimilarity(double threshold) {
            return overallSimilarity >= threshold;
        }
    }
    
    public static class CodeRepresentation {
        private final String sourceCode;
        private final String language;
        private final List<String> tokens;
        private final Map<String, Integer> tokenFrequency;
        private final List<String> identifiers;
        private final List<String> keywords;
        private final List<String> operators;
        private final Map<String, Object> structuralFeatures;
        private final Map<String, Object> semanticFeatures;
        private final double[] embeddingVector;
        
        public CodeRepresentation(String sourceCode, String language) {
            this.sourceCode = sourceCode;
            this.language = language;
            this.tokens = extractTokens(sourceCode);
            this.tokenFrequency = calculateTokenFrequency(tokens);
            this.identifiers = extractIdentifiers(sourceCode);
            this.keywords = extractKeywords(sourceCode);
            this.operators = extractOperators(sourceCode);
            this.structuralFeatures = extractStructuralFeatures(sourceCode);
            this.semanticFeatures = extractSemanticFeatures(sourceCode);
            this.embeddingVector = generateEmbedding(sourceCode);
        }
        
        private List<String> extractTokens(String code) {
            // Simple tokenization - in practice would use language-specific parsers
            return Arrays.stream(code.split("\\s+|[\\(\\)\\{\\}\\[\\];,]"))
                        .filter(token -> !token.trim().isEmpty())
                        .collect(Collectors.toList());
        }
        
        private Map<String, Integer> calculateTokenFrequency(List<String> tokens) {
            Map<String, Integer> frequency = new HashMap<>();
            for (String token : tokens) {
                frequency.merge(token.toLowerCase(), 1, Integer::sum);
            }
            return frequency;
        }
        
        private List<String> extractIdentifiers(String code) {
            // Extract variable/function names (simplified)
            List<String> identifiers = new ArrayList<>();
            String[] words = code.split("\\s+|[\\(\\)\\{\\}\\[\\];,=]");
            
            for (String word : words) {
                if (word.matches("[a-zA-Z_][a-zA-Z0-9_]*") && 
                    !isKeyword(word) && word.length() > 1) {
                    identifiers.add(word);
                }
            }
            
            return identifiers;
        }
        
        private List<String> extractKeywords(String code) {
            Set<String> commonKeywords = Set.of(
                "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
                "function", "def", "class", "struct", "enum", "interface", "public", "private",
                "protected", "static", "final", "const", "let", "var", "async", "await",
                "try", "catch", "finally", "throw", "return", "yield", "import", "export"
            );
            
            return tokens.stream()
                       .filter(token -> commonKeywords.contains(token.toLowerCase()))
                       .collect(Collectors.toList());
        }
        
        private List<String> extractOperators(String code) {
            List<String> operators = new ArrayList<>();
            String[] ops = {"+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", 
                           "&&", "||", "!", "&", "|", "^", "<<", ">>", "++", "--", "+=", "-="};
            
            for (String op : ops) {
                if (code.contains(op)) {
                    operators.add(op);
                }
            }
            
            return operators;
        }
        
        private Map<String, Object> extractStructuralFeatures(String code) {
            Map<String, Object> features = new HashMap<>();
            
            // Basic metrics
            features.put("lineCount", code.split("\n").length);
            features.put("characterCount", code.length());
            features.put("tokenCount", tokens.size());
            
            // Nesting depth
            int maxDepth = 0;
            int currentDepth = 0;
            for (char c : code.toCharArray()) {
                if (c == '{' || c == '(' || c == '[') {
                    currentDepth++;
                    maxDepth = Math.max(maxDepth, currentDepth);
                } else if (c == '}' || c == ')' || c == ']') {
                    currentDepth--;
                }
            }
            features.put("maxNestingDepth", maxDepth);
            
            // Structural patterns
            features.put("hasClasses", code.contains("class"));
            features.put("hasFunctions", code.contains("def") || code.contains("function"));
            features.put("hasLoops", code.contains("for") || code.contains("while"));
            features.put("hasConditionals", code.contains("if"));
            features.put("hasExceptionHandling", code.contains("try") || code.contains("catch"));
            features.put("hasAsync", code.contains("async") || code.contains("await"));
            
            return features;
        }
        
        private Map<String, Object> extractSemanticFeatures(String code) {
            Map<String, Object> features = new HashMap<>();
            
            // Design patterns
            features.put("singletonPattern", detectSingletonPattern(code));
            features.put("factoryPattern", detectFactoryPattern(code));
            features.put("observerPattern", detectObserverPattern(code));
            features.put("builderPattern", detectBuilderPattern(code));
            
            // Programming paradigms
            features.put("objectOriented", code.contains("class") && code.contains("extends"));
            features.put("functional", code.contains("lambda") || code.contains("=>"));
            features.put("procedural", !((Boolean) features.get("objectOriented")) && 
                                     !((Boolean) features.get("functional")));
            
            // Complexity indicators
            features.put("cyclomaticComplexity", calculateCyclomaticComplexity(code));
            features.put("halsteadVolume", calculateHalsteadVolume(code));
            
            return features;
        }
        
        private boolean detectSingletonPattern(String code) {
            return (code.contains("private") && code.contains("static") && 
                   code.contains("instance")) || 
                   code.contains("enum") && code.contains("INSTANCE");
        }
        
        private boolean detectFactoryPattern(String code) {
            return code.contains("create") && (code.contains("Factory") || 
                   (code.contains("switch") && code.contains("case")));
        }
        
        private boolean detectObserverPattern(String code) {
            return (code.contains("notify") || code.contains("update")) &&
                   (code.contains("observer") || code.contains("listener"));
        }
        
        private boolean detectBuilderPattern(String code) {
            return code.contains("build") && code.contains("return this");
        }
        
        private int calculateCyclomaticComplexity(String code) {
            int complexity = 1; // Base complexity
            
            // Count decision points
            String[] decisionKeywords = {"if", "else", "while", "for", "case", "catch", "&&", "||"};
            for (String keyword : decisionKeywords) {
                complexity += countOccurrences(code, keyword);
            }
            
            return complexity;
        }
        
        private double calculateHalsteadVolume(String code) {
            Set<String> uniqueOperators = new HashSet<>(operators);
            Set<String> uniqueOperands = new HashSet<>(identifiers);
            
            int n1 = uniqueOperators.size();  // Unique operators
            int n2 = uniqueOperands.size();   // Unique operands  
            int N1 = operators.size();        // Total operators
            int N2 = identifiers.size();      // Total operands
            
            if (n1 == 0 || n2 == 0) return 0.0;
            
            double vocabulary = n1 + n2;
            double length = N1 + N2;
            
            return length * Math.log(vocabulary) / Math.log(2);
        }
        
        private int countOccurrences(String text, String pattern) {
            return (text.length() - text.replace(pattern, "").length()) / pattern.length();
        }
        
        private double[] generateEmbedding(String code) {
            // Simplified embedding generation - in practice would use transformer models
            int embeddingSize = 128;
            double[] embedding = new double[embeddingSize];
            
            // Use hash-based features for consistency
            int hashCode = code.hashCode();
            Random random = new Random(hashCode);
            
            for (int i = 0; i < embeddingSize; i++) {
                embedding[i] = random.nextGaussian();
            }
            
            // Normalize
            double norm = Math.sqrt(Arrays.stream(embedding)
                                         .map(x -> x * x)
                                         .sum());
            if (norm > 0) {
                for (int i = 0; i < embeddingSize; i++) {
                    embedding[i] /= norm;
                }
            }
            
            return embedding;
        }
        
        private boolean isKeyword(String word) {
            Set<String> keywords = Set.of(
                "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
                "function", "def", "class", "struct", "enum", "interface", "public", "private",
                "protected", "static", "final", "const", "let", "var", "int", "string", "bool",
                "true", "false", "null", "void", "return"
            );
            return keywords.contains(word.toLowerCase());
        }
        
        // Getters
        public String getSourceCode() { return sourceCode; }
        public String getLanguage() { return language; }
        public List<String> getTokens() { return tokens; }
        public Map<String, Integer> getTokenFrequency() { return tokenFrequency; }
        public List<String> getIdentifiers() { return identifiers; }
        public List<String> getKeywords() { return keywords; }
        public List<String> getOperators() { return operators; }
        public Map<String, Object> getStructuralFeatures() { return structuralFeatures; }
        public Map<String, Object> getSemanticFeatures() { return semanticFeatures; }
        public double[] getEmbeddingVector() { return embeddingVector; }
    }
    
    private final Map<SimilarityMetric, Double> metricWeights;
    private final Map<String, CodeRepresentation> codeCache;
    
    public SemanticSimilarityAnalyzer() {
        this.metricWeights = initializeDefaultWeights();
        this.codeCache = new ConcurrentHashMap<>();
    }
    
    private Map<SimilarityMetric, Double> initializeDefaultWeights() {
        Map<SimilarityMetric, Double> weights = new HashMap<>();
        weights.put(SimilarityMetric.STRUCTURAL, 0.15);
        weights.put(SimilarityMetric.LEXICAL, 0.10);
        weights.put(SimilarityMetric.SEMANTIC, 0.25);
        weights.put(SimilarityMetric.BEHAVIORAL, 0.20);
        weights.put(SimilarityMetric.CONTROL_FLOW, 0.10);
        weights.put(SimilarityMetric.DATA_FLOW, 0.10);
        weights.put(SimilarityMetric.PATTERN, 0.05);
        weights.put(SimilarityMetric.COMPLEXITY, 0.05);
        return weights;
    }
    
    public SimilarityResult analyzeSimilarity(String code1, String language1,
                                            String code2, String language2) {
        // Get or create code representations
        CodeRepresentation repr1 = getCodeRepresentation(code1, language1);
        CodeRepresentation repr2 = getCodeRepresentation(code2, language2);
        
        // Calculate similarity scores for each metric
        Map<SimilarityMetric, Double> metricScores = new HashMap<>();
        metricScores.put(SimilarityMetric.STRUCTURAL, calculateStructuralSimilarity(repr1, repr2));
        metricScores.put(SimilarityMetric.LEXICAL, calculateLexicalSimilarity(repr1, repr2));
        metricScores.put(SimilarityMetric.SEMANTIC, calculateSemanticSimilarity(repr1, repr2));
        metricScores.put(SimilarityMetric.BEHAVIORAL, calculateBehavioralSimilarity(repr1, repr2));
        metricScores.put(SimilarityMetric.CONTROL_FLOW, calculateControlFlowSimilarity(repr1, repr2));
        metricScores.put(SimilarityMetric.DATA_FLOW, calculateDataFlowSimilarity(repr1, repr2));
        metricScores.put(SimilarityMetric.PATTERN, calculatePatternSimilarity(repr1, repr2));
        metricScores.put(SimilarityMetric.COMPLEXITY, calculateComplexitySimilarity(repr1, repr2));
        
        // Calculate weighted overall similarity
        double overallSimilarity = calculateWeightedSimilarity(metricScores);
        
        // Generate explanations
        List<String> similarityReasons = generateSimilarityReasons(metricScores);
        List<String> differences = generateDifferences(repr1, repr2, metricScores);
        
        // Calculate confidence based on consistency across metrics
        double confidence = calculateConfidence(metricScores);
        
        return new SimilarityResult(overallSimilarity, metricScores, 
                                   similarityReasons, differences, confidence);
    }
    
    private CodeRepresentation getCodeRepresentation(String code, String language) {
        String key = code.hashCode() + ":" + language;
        return codeCache.computeIfAbsent(key, k -> new CodeRepresentation(code, language));
    }
    
    private double calculateStructuralSimilarity(CodeRepresentation repr1, CodeRepresentation repr2) {
        Map<String, Object> struct1 = repr1.getStructuralFeatures();
        Map<String, Object> struct2 = repr2.getStructuralFeatures();
        
        double similarity = 0.0;
        int comparisons = 0;
        
        // Compare numeric features
        String[] numericFeatures = {"lineCount", "tokenCount", "maxNestingDepth", 
                                   "cyclomaticComplexity", "halsteadVolume"};
        
        for (String feature : numericFeatures) {
            if (struct1.containsKey(feature) && struct2.containsKey(feature)) {
                Number val1 = (Number) struct1.get(feature);
                Number val2 = (Number) struct2.get(feature);
                
                if (val1.doubleValue() == 0 && val2.doubleValue() == 0) {
                    similarity += 1.0;
                } else {
                    double ratio = Math.min(val1.doubleValue(), val2.doubleValue()) /
                                 Math.max(val1.doubleValue(), val2.doubleValue());
                    similarity += ratio;
                }
                comparisons++;
            }
        }
        
        // Compare boolean features
        String[] booleanFeatures = {"hasClasses", "hasFunctions", "hasLoops", 
                                   "hasConditionals", "hasExceptionHandling", "hasAsync"};
        
        for (String feature : booleanFeatures) {
            if (struct1.containsKey(feature) && struct2.containsKey(feature)) {
                Boolean val1 = (Boolean) struct1.get(feature);
                Boolean val2 = (Boolean) struct2.get(feature);
                
                if (val1.equals(val2)) {
                    similarity += 1.0;
                }
                comparisons++;
            }
        }
        
        return comparisons > 0 ? similarity / comparisons : 0.0;
    }
    
    private double calculateLexicalSimilarity(CodeRepresentation repr1, CodeRepresentation repr2) {
        Map<String, Integer> freq1 = repr1.getTokenFrequency();
        Map<String, Integer> freq2 = repr2.getTokenFrequency();
        
        // Calculate Jaccard similarity for tokens
        Set<String> allTokens = new HashSet<>(freq1.keySet());
        allTokens.addAll(freq2.keySet());
        
        int intersection = 0;
        for (String token : allTokens) {
            if (freq1.containsKey(token) && freq2.containsKey(token)) {
                intersection++;
            }
        }
        
        int union = allTokens.size();
        double jaccardSimilarity = union > 0 ? (double) intersection / union : 0.0;
        
        // Calculate cosine similarity for identifier overlap
        Set<String> identifiers1 = new HashSet<>(repr1.getIdentifiers());
        Set<String> identifiers2 = new HashSet<>(repr2.getIdentifiers());
        
        Set<String> commonIdentifiers = new HashSet<>(identifiers1);
        commonIdentifiers.retainAll(identifiers2);
        
        double identifierSimilarity = 0.0;
        if (!identifiers1.isEmpty() || !identifiers2.isEmpty()) {
            identifierSimilarity = (double) commonIdentifiers.size() /
                                  Math.max(identifiers1.size(), identifiers2.size());
        }
        
        return 0.6 * jaccardSimilarity + 0.4 * identifierSimilarity;
    }
    
    private double calculateSemanticSimilarity(CodeRepresentation repr1, CodeRepresentation repr2) {
        // Use embedding vectors for semantic similarity
        double[] emb1 = repr1.getEmbeddingVector();
        double[] emb2 = repr2.getEmbeddingVector();
        
        // Calculate cosine similarity
        double cosineSimilarity = calculateCosineSimilarity(emb1, emb2);
        
        // Combine with semantic feature similarity
        Map<String, Object> sem1 = repr1.getSemanticFeatures();
        Map<String, Object> sem2 = repr2.getSemanticFeatures();
        
        double featureSimilarity = calculateFeatureSimilarity(sem1, sem2);
        
        return 0.7 * cosineSimilarity + 0.3 * featureSimilarity;
    }
    
    private double calculateCosineSimilarity(double[] vec1, double[] vec2) {
        if (vec1.length != vec2.length) return 0.0;
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private double calculateFeatureSimilarity(Map<String, Object> features1, 
                                            Map<String, Object> features2) {
        Set<String> allKeys = new HashSet<>(features1.keySet());
        allKeys.addAll(features2.keySet());
        
        double similarity = 0.0;
        int comparisons = 0;
        
        for (String key : allKeys) {
            if (features1.containsKey(key) && features2.containsKey(key)) {
                Object val1 = features1.get(key);
                Object val2 = features2.get(key);
                
                if (val1.equals(val2)) {
                    similarity += 1.0;
                } else if (val1 instanceof Number && val2 instanceof Number) {
                    Number num1 = (Number) val1;
                    Number num2 = (Number) val2;
                    double ratio = Math.min(num1.doubleValue(), num2.doubleValue()) /
                                 Math.max(num1.doubleValue(), num2.doubleValue());
                    similarity += ratio;
                }
                comparisons++;
            }
        }
        
        return comparisons > 0 ? similarity / comparisons : 0.0;
    }
    
    private double calculateBehavioralSimilarity(CodeRepresentation repr1, CodeRepresentation repr2) {
        // Simplified behavioral similarity based on control structures and patterns
        Map<String, Object> struct1 = repr1.getStructuralFeatures();
        Map<String, Object> struct2 = repr2.getStructuralFeatures();
        
        double similarity = 0.0;
        int comparisons = 0;
        
        // Compare control flow patterns
        String[] behaviorFeatures = {"hasLoops", "hasConditionals", "hasExceptionHandling", "hasAsync"};
        
        for (String feature : behaviorFeatures) {
            if (struct1.containsKey(feature) && struct2.containsKey(feature)) {
                Boolean val1 = (Boolean) struct1.get(feature);
                Boolean val2 = (Boolean) struct2.get(feature);
                
                if (val1.equals(val2)) {
                    similarity += 1.0;
                }
                comparisons++;
            }
        }
        
        return comparisons > 0 ? similarity / comparisons : 0.0;
    }
    
    private double calculateControlFlowSimilarity(CodeRepresentation repr1, CodeRepresentation repr2) {
        // Simplified control flow similarity based on nesting and complexity
        Map<String, Object> struct1 = repr1.getStructuralFeatures();
        Map<String, Object> struct2 = repr2.getStructuralFeatures();
        
        if (struct1.containsKey("maxNestingDepth") && struct2.containsKey("maxNestingDepth")) {
            int depth1 = (Integer) struct1.get("maxNestingDepth");
            int depth2 = (Integer) struct2.get("maxNestingDepth");
            
            if (depth1 == 0 && depth2 == 0) return 1.0;
            
            return (double) Math.min(depth1, depth2) / Math.max(depth1, depth2);
        }
        
        return 0.0;
    }
    
    private double calculateDataFlowSimilarity(CodeRepresentation repr1, CodeRepresentation repr2) {
        // Simplified data flow similarity based on variable usage patterns
        List<String> identifiers1 = repr1.getIdentifiers();
        List<String> identifiers2 = repr2.getIdentifiers();
        
        if (identifiers1.isEmpty() && identifiers2.isEmpty()) return 1.0;
        if (identifiers1.isEmpty() || identifiers2.isEmpty()) return 0.0;
        
        Set<String> set1 = new HashSet<>(identifiers1);
        Set<String> set2 = new HashSet<>(identifiers2);
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }
    
    private double calculatePatternSimilarity(CodeRepresentation repr1, CodeRepresentation repr2) {
        Map<String, Object> sem1 = repr1.getSemanticFeatures();
        Map<String, Object> sem2 = repr2.getSemanticFeatures();
        
        String[] patterns = {"singletonPattern", "factoryPattern", "observerPattern", "builderPattern"};
        
        double similarity = 0.0;
        int comparisons = 0;
        
        for (String pattern : patterns) {
            if (sem1.containsKey(pattern) && sem2.containsKey(pattern)) {
                Boolean val1 = (Boolean) sem1.get(pattern);
                Boolean val2 = (Boolean) sem2.get(pattern);
                
                if (val1.equals(val2)) {
                    similarity += 1.0;
                }
                comparisons++;
            }
        }
        
        return comparisons > 0 ? similarity / comparisons : 0.0;
    }
    
    private double calculateComplexitySimilarity(CodeRepresentation repr1, CodeRepresentation repr2) {
        Map<String, Object> sem1 = repr1.getSemanticFeatures();
        Map<String, Object> sem2 = repr2.getSemanticFeatures();
        
        if (sem1.containsKey("cyclomaticComplexity") && sem2.containsKey("cyclomaticComplexity")) {
            int complexity1 = (Integer) sem1.get("cyclomaticComplexity");
            int complexity2 = (Integer) sem2.get("cyclomaticComplexity");
            
            if (complexity1 == 0 && complexity2 == 0) return 1.0;
            
            return (double) Math.min(complexity1, complexity2) / Math.max(complexity1, complexity2);
        }
        
        return 0.0;
    }
    
    private double calculateWeightedSimilarity(Map<SimilarityMetric, Double> metricScores) {
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        
        for (Map.Entry<SimilarityMetric, Double> entry : metricScores.entrySet()) {
            SimilarityMetric metric = entry.getKey();
            double score = entry.getValue();
            double weight = metricWeights.get(metric);
            
            weightedSum += score * weight;
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }
    
    private List<String> generateSimilarityReasons(Map<SimilarityMetric, Double> metricScores) {
        List<String> reasons = new ArrayList<>();
        
        for (Map.Entry<SimilarityMetric, Double> entry : metricScores.entrySet()) {
            SimilarityMetric metric = entry.getKey();
            double score = entry.getValue();
            
            if (score > 0.7) {
                reasons.add("High " + metric.name().toLowerCase().replace("_", " ") + 
                          " similarity (" + String.format("%.2f", score) + ")");
            }
        }
        
        return reasons;
    }
    
    private List<String> generateDifferences(CodeRepresentation repr1, CodeRepresentation repr2,
                                           Map<SimilarityMetric, Double> metricScores) {
        List<String> differences = new ArrayList<>();
        
        for (Map.Entry<SimilarityMetric, Double> entry : metricScores.entrySet()) {
            SimilarityMetric metric = entry.getKey();
            double score = entry.getValue();
            
            if (score < 0.3) {
                differences.add("Low " + metric.name().toLowerCase().replace("_", " ") + 
                              " similarity (" + String.format("%.2f", score) + ")");
            }
        }
        
        // Language-specific differences
        if (!repr1.getLanguage().equals(repr2.getLanguage())) {
            differences.add("Different programming languages: " + 
                          repr1.getLanguage() + " vs " + repr2.getLanguage());
        }
        
        return differences;
    }
    
    private double calculateConfidence(Map<SimilarityMetric, Double> metricScores) {
        // Calculate standard deviation of metric scores
        double mean = metricScores.values().stream()
                                          .mapToDouble(Double::doubleValue)
                                          .average()
                                          .orElse(0.0);
        
        double variance = metricScores.values().stream()
                                              .mapToDouble(score -> Math.pow(score - mean, 2))
                                              .average()
                                              .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        
        // Higher confidence when scores are consistent (low standard deviation)
        return Math.max(0.0, 1.0 - (stdDev * 2));
    }
    
    public void setMetricWeight(SimilarityMetric metric, double weight) {
        if (weight >= 0.0 && weight <= 1.0) {
            metricWeights.put(metric, weight);
            normalizeWeights();
        } else {
            throw new IllegalArgumentException("Weight must be between 0.0 and 1.0");
        }
    }
    
    private void normalizeWeights() {
        double totalWeight = metricWeights.values().stream()
                                          .mapToDouble(Double::doubleValue)
                                          .sum();
        
        if (totalWeight > 0) {
            for (SimilarityMetric metric : metricWeights.keySet()) {
                double normalizedWeight = metricWeights.get(metric) / totalWeight;
                metricWeights.put(metric, normalizedWeight);
            }
        }
    }
    
    public Map<SimilarityMetric, Double> getMetricWeights() {
        return new HashMap<>(metricWeights);
    }
    
    public void clearCache() {
        codeCache.clear();
        System.out.println("Semantic similarity cache cleared");
    }
    
    public int getCacheSize() {
        return codeCache.size();
    }
    
    public List<SimilarityResult> findSimilarCode(String queryCode, String queryLanguage,
                                                 Map<String, String> codeDatabase,
                                                 int topK) {
        List<SimilarityResult> results = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : codeDatabase.entrySet()) {
            String candidateCode = entry.getKey();
            String candidateLanguage = entry.getValue();
            
            SimilarityResult result = analyzeSimilarity(queryCode, queryLanguage,
                                                      candidateCode, candidateLanguage);
            results.add(result);
        }
        
        // Sort by overall similarity and return top K
        return results.stream()
                     .sorted((r1, r2) -> Double.compare(r2.getOverallSimilarity(), 
                                                       r1.getOverallSimilarity()))
                     .limit(topK)
                     .collect(Collectors.toList());
    }
    
    public void printSimilarityReport(SimilarityResult result) {
        System.out.println("\n=== SEMANTIC SIMILARITY ANALYSIS ===");
        System.out.println("Overall Similarity: " + 
                          String.format("%.2f%%", result.getOverallSimilarity() * 100));
        System.out.println("Confidence: " + String.format("%.2f%%", result.getConfidence() * 100));
        
        System.out.println("\nMetric Scores:");
        for (Map.Entry<SimilarityMetric, Double> entry : result.getMetricScores().entrySet()) {
            System.out.println("  " + entry.getKey().name().replace("_", " ") + ": " +
                              String.format("%.2f%%", entry.getValue() * 100));
        }
        
        System.out.println("\nSimilarity Reasons:");
        for (String reason : result.getSimilarityReasons()) {
            System.out.println("  + " + reason);
        }
        
        System.out.println("\nKey Differences:");
        for (String difference : result.getDifferences()) {
            System.out.println("  - " + difference);
        }
    }
}