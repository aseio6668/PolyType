package com.polytype.migrator.ml;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Advanced feedback learning system for continuous improvement of translation quality.
 * Implements reinforcement learning and active learning techniques to adapt translation
 * models based on user feedback and translation outcomes.
 */
public class FeedbackLearningSystem {
    
    public enum FeedbackType {
        POSITIVE,        // Translation was correct/good
        NEGATIVE,        // Translation had issues
        CORRECTION,      // User provided correction
        QUALITY_RATING   // Numeric quality score
    }
    
    public enum LearningStrategy {
        REINFORCEMENT,   // Reward/penalty based learning
        ACTIVE,         // Query user for uncertain cases
        TRANSFER,       // Learn from similar patterns
        ENSEMBLE        // Combine multiple models
    }
    
    public static class FeedbackEntry {
        private final String sourceCode;
        private final String targetCode;
        private final String sourceLanguage;
        private final String targetLanguage;
        private final FeedbackType feedbackType;
        private final String userCorrection;
        private final double qualityScore;
        private final LocalDateTime timestamp;
        private final Map<String, Object> context;
        
        public FeedbackEntry(String sourceCode, String targetCode, String sourceLanguage, 
                           String targetLanguage, FeedbackType feedbackType, 
                           String userCorrection, double qualityScore, 
                           Map<String, Object> context) {
            this.sourceCode = sourceCode;
            this.targetCode = targetCode;
            this.sourceLanguage = sourceLanguage;
            this.targetLanguage = targetLanguage;
            this.feedbackType = feedbackType;
            this.userCorrection = userCorrection;
            this.qualityScore = qualityScore;
            this.timestamp = LocalDateTime.now();
            this.context = new HashMap<>(context);
        }
        
        // Getters
        public String getSourceCode() { return sourceCode; }
        public String getTargetCode() { return targetCode; }
        public String getSourceLanguage() { return sourceLanguage; }
        public String getTargetLanguage() { return targetLanguage; }
        public FeedbackType getFeedbackType() { return feedbackType; }
        public String getUserCorrection() { return userCorrection; }
        public double getQualityScore() { return qualityScore; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getContext() { return context; }
    }
    
    public static class LearningMetrics {
        private int totalFeedbackCount = 0;
        private int positiveFeedbackCount = 0;
        private int negativeFeedbackCount = 0;
        private double averageQualityScore = 0.0;
        private Map<String, Integer> languagePairCounts = new HashMap<>();
        private Map<String, Double> patternSuccessRates = new HashMap<>();
        private double modelAccuracyImprovement = 0.0;
        private int activeQueriesGenerated = 0;
        
        public void updateFromFeedback(FeedbackEntry feedback) {
            totalFeedbackCount++;
            
            if (feedback.getFeedbackType() == FeedbackType.POSITIVE) {
                positiveFeedbackCount++;
            } else if (feedback.getFeedbackType() == FeedbackType.NEGATIVE) {
                negativeFeedbackCount++;
            }
            
            if (feedback.getQualityScore() > 0) {
                averageQualityScore = (averageQualityScore * (totalFeedbackCount - 1) + 
                                     feedback.getQualityScore()) / totalFeedbackCount;
            }
            
            String languagePair = feedback.getSourceLanguage() + "->" + feedback.getTargetLanguage();
            languagePairCounts.merge(languagePair, 1, Integer::sum);
        }
        
        // Getters
        public double getPositiveFeedbackRatio() {
            return totalFeedbackCount > 0 ? (double) positiveFeedbackCount / totalFeedbackCount : 0.0;
        }
        
        public int getTotalFeedbackCount() { return totalFeedbackCount; }
        public double getAverageQualityScore() { return averageQualityScore; }
        public Map<String, Integer> getLanguagePairCounts() { return languagePairCounts; }
        public Map<String, Double> getPatternSuccessRates() { return patternSuccessRates; }
        public double getModelAccuracyImprovement() { return modelAccuracyImprovement; }
        public int getActiveQueriesGenerated() { return activeQueriesGenerated; }
    }
    
    private final Map<String, List<FeedbackEntry>> feedbackDatabase;
    private final LearningMetrics metrics;
    private final Map<String, Double> patternRewards;
    private final Map<String, Double> languagePairWeights;
    private final Queue<String> uncertainTranslations;
    private final Set<LearningStrategy> activeStrategies;
    
    // ML Model placeholders
    private Object reinforcementModel;
    private Object transferLearningModel;
    private Object uncertaintyModel;
    
    public FeedbackLearningSystem() {
        this.feedbackDatabase = new ConcurrentHashMap<>();
        this.metrics = new LearningMetrics();
        this.patternRewards = new ConcurrentHashMap<>();
        this.languagePairWeights = new ConcurrentHashMap<>();
        this.uncertainTranslations = new LinkedList<>();
        this.activeStrategies = EnumSet.of(
            LearningStrategy.REINFORCEMENT,
            LearningStrategy.ACTIVE,
            LearningStrategy.TRANSFER
        );
        
        initializeModels();
    }
    
    private void initializeModels() {
        // Initialize ML models for different learning strategies
        this.reinforcementModel = createReinforcementModel();
        this.transferLearningModel = createTransferLearningModel();
        this.uncertaintyModel = createUncertaintyModel();
        
        // Initialize default language pair weights
        initializeLanguagePairWeights();
    }
    
    private Object createReinforcementModel() {
        // Placeholder for reinforcement learning model
        // In practice, would initialize Q-learning or policy gradient model
        return new HashMap<String, Double>(); // Simple reward table
    }
    
    private Object createTransferLearningModel() {
        // Placeholder for transfer learning model
        // In practice, would load pre-trained transformer or fine-tune existing model
        return new HashMap<String, Object>(); // Feature transfer mappings
    }
    
    private Object createUncertaintyModel() {
        // Placeholder for uncertainty estimation model
        // In practice, would use Bayesian neural networks or ensemble methods
        return new HashMap<String, Double>(); // Uncertainty scores
    }
    
    private void initializeLanguagePairWeights() {
        // High-confidence language pairs (well-supported)
        languagePairWeights.put("Java->Python", 0.9);
        languagePairWeights.put("Python->Java", 0.85);
        languagePairWeights.put("JavaScript->TypeScript", 0.95);
        languagePairWeights.put("TypeScript->JavaScript", 0.9);
        
        // Medium-confidence pairs
        languagePairWeights.put("C++->Rust", 0.75);
        languagePairWeights.put("Java->Kotlin", 0.8);
        languagePairWeights.put("Python->Go", 0.7);
        
        // Lower-confidence pairs (need more data)
        languagePairWeights.put("Assembly->HighLevel", 0.4);
        languagePairWeights.put("COBOL->Modern", 0.3);
    }
    
    public void recordFeedback(String sourceCode, String targetCode, 
                             String sourceLanguage, String targetLanguage,
                             FeedbackType feedbackType, String userCorrection,
                             double qualityScore) {
        
        Map<String, Object> context = extractContext(sourceCode, targetCode);
        
        FeedbackEntry feedback = new FeedbackEntry(
            sourceCode, targetCode, sourceLanguage, targetLanguage,
            feedbackType, userCorrection, qualityScore, context
        );
        
        // Store feedback
        String key = sourceLanguage + "->" + targetLanguage;
        feedbackDatabase.computeIfAbsent(key, k -> new ArrayList<>()).add(feedback);
        
        // Update metrics
        metrics.updateFromFeedback(feedback);
        
        // Apply learning strategies
        applyReinforcementLearning(feedback);
        updateTransferLearning(feedback);
        assessUncertainty(feedback);
        
        System.out.println("Recorded feedback: " + feedbackType + 
                          " for " + sourceLanguage + "->" + targetLanguage +
                          " (Score: " + qualityScore + ")");
    }
    
    private Map<String, Object> extractContext(String sourceCode, String targetCode) {
        Map<String, Object> context = new HashMap<>();
        
        // Code complexity metrics
        context.put("sourceLength", sourceCode.length());
        context.put("targetLength", targetCode.length());
        context.put("lineCount", sourceCode.split("\n").length);
        
        // Pattern detection
        context.put("hasLoops", sourceCode.contains("for") || sourceCode.contains("while"));
        context.put("hasClasses", sourceCode.contains("class"));
        context.put("hasFunctions", sourceCode.contains("def") || sourceCode.contains("function"));
        context.put("hasAsync", sourceCode.contains("async") || sourceCode.contains("await"));
        
        // Language-specific features
        context.put("hasGenerics", sourceCode.contains("<") && sourceCode.contains(">"));
        context.put("hasLambdas", sourceCode.contains("->") || sourceCode.contains("lambda"));
        
        return context;
    }
    
    private void applyReinforcementLearning(FeedbackEntry feedback) {
        if (!activeStrategies.contains(LearningStrategy.REINFORCEMENT)) return;
        
        // Calculate reward based on feedback
        double reward = calculateReward(feedback);
        
        // Update pattern rewards
        String pattern = extractPattern(feedback.getSourceCode());
        patternRewards.merge(pattern, reward, (oldReward, newReward) -> 
            0.9 * oldReward + 0.1 * newReward); // Exponential moving average
        
        // Update language pair weights
        String languagePair = feedback.getSourceLanguage() + "->" + feedback.getTargetLanguage();
        languagePairWeights.merge(languagePair, reward, (oldWeight, newReward) ->
            Math.max(0.1, Math.min(1.0, oldWeight + 0.01 * newReward)));
    }
    
    private double calculateReward(FeedbackEntry feedback) {
        double reward = 0.0;
        
        switch (feedback.getFeedbackType()) {
            case POSITIVE:
                reward = 1.0;
                break;
            case NEGATIVE:
                reward = -0.5;
                break;
            case CORRECTION:
                // Reward based on similarity between original and corrected
                double similarity = calculateCodeSimilarity(
                    feedback.getTargetCode(), 
                    feedback.getUserCorrection()
                );
                reward = similarity - 0.5; // Range: -0.5 to 0.5
                break;
            case QUALITY_RATING:
                reward = (feedback.getQualityScore() - 0.5) * 2; // Normalize to [-1, 1]
                break;
        }
        
        return reward;
    }
    
    private double calculateCodeSimilarity(String code1, String code2) {
        if (code1 == null || code2 == null) return 0.0;
        
        // Simple Levenshtein distance-based similarity
        int distance = levenshteinDistance(code1, code2);
        int maxLength = Math.max(code1.length(), code2.length());
        
        return maxLength == 0 ? 1.0 : 1.0 - (double) distance / maxLength;
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i-1][j], dp[i][j-1]), dp[i-1][j-1]);
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    private String extractPattern(String sourceCode) {
        // Simple pattern extraction - in practice would use AST analysis
        if (sourceCode.contains("class") && sourceCode.contains("extends")) {
            return "inheritance";
        } else if (sourceCode.contains("interface") || sourceCode.contains("implements")) {
            return "interface";
        } else if (sourceCode.contains("async") || sourceCode.contains("await")) {
            return "async";
        } else if (sourceCode.contains("for") || sourceCode.contains("while")) {
            return "loop";
        } else if (sourceCode.contains("try") || sourceCode.contains("catch")) {
            return "error_handling";
        }
        return "general";
    }
    
    private void updateTransferLearning(FeedbackEntry feedback) {
        if (!activeStrategies.contains(LearningStrategy.TRANSFER)) return;
        
        // Find similar patterns in feedback history
        List<FeedbackEntry> similarFeedback = findSimilarFeedback(feedback);
        
        // Update transfer learning model based on similar cases
        for (FeedbackEntry similar : similarFeedback) {
            double transferWeight = calculateTransferWeight(feedback, similar);
            // Update model parameters (placeholder)
            updateTransferModel(feedback, similar, transferWeight);
        }
    }
    
    private List<FeedbackEntry> findSimilarFeedback(FeedbackEntry target) {
        return feedbackDatabase.values().stream()
            .flatMap(List::stream)
            .filter(feedback -> isSimilarPattern(target, feedback))
            .limit(10) // Top 10 similar cases
            .collect(Collectors.toList());
    }
    
    private boolean isSimilarPattern(FeedbackEntry feedback1, FeedbackEntry feedback2) {
        String pattern1 = extractPattern(feedback1.getSourceCode());
        String pattern2 = extractPattern(feedback2.getSourceCode());
        
        return pattern1.equals(pattern2) || 
               feedback1.getSourceLanguage().equals(feedback2.getSourceLanguage()) ||
               feedback1.getTargetLanguage().equals(feedback2.getTargetLanguage());
    }
    
    private double calculateTransferWeight(FeedbackEntry target, FeedbackEntry source) {
        double weight = 0.0;
        
        // Language similarity
        if (target.getSourceLanguage().equals(source.getSourceLanguage())) weight += 0.4;
        if (target.getTargetLanguage().equals(source.getTargetLanguage())) weight += 0.4;
        
        // Pattern similarity
        if (extractPattern(target.getSourceCode()).equals(extractPattern(source.getSourceCode()))) {
            weight += 0.3;
        }
        
        // Temporal decay (recent feedback is more relevant)
        long daysDiff = Math.abs(
            target.getTimestamp().toLocalDate().toEpochDay() -
            source.getTimestamp().toLocalDate().toEpochDay()
        );
        weight *= Math.exp(-daysDiff / 30.0); // Decay over 30 days
        
        return Math.min(1.0, weight);
    }
    
    private void updateTransferModel(FeedbackEntry target, FeedbackEntry source, double weight) {
        // Placeholder for transfer learning model update
        // In practice, would update neural network weights or feature mappings
        Map<String, Object> transferModel = (Map<String, Object>) this.transferLearningModel;
        
        String key = extractPattern(target.getSourceCode()) + "->" + 
                    target.getSourceLanguage() + "->" + target.getTargetLanguage();
        
        transferModel.put(key, weight);
    }
    
    private void assessUncertainty(FeedbackEntry feedback) {
        if (!activeStrategies.contains(LearningStrategy.ACTIVE)) return;
        
        // Calculate uncertainty for this translation
        double uncertainty = calculateTranslationUncertainty(feedback);
        
        // If uncertainty is high, add to active learning queue
        if (uncertainty > 0.7) {
            String translationKey = feedback.getSourceLanguage() + "->" + 
                                  feedback.getTargetLanguage() + ":" +
                                  extractPattern(feedback.getSourceCode());
            
            uncertainTranslations.offer(translationKey);
            metrics.activeQueriesGenerated++;
        }
    }
    
    private double calculateTranslationUncertainty(FeedbackEntry feedback) {
        // Simple uncertainty calculation based on historical data
        String languagePair = feedback.getSourceLanguage() + "->" + feedback.getTargetLanguage();
        String pattern = extractPattern(feedback.getSourceCode());
        
        // Base uncertainty from language pair confidence
        double baseUncertainty = 1.0 - languagePairWeights.getOrDefault(languagePair, 0.5);
        
        // Pattern-specific uncertainty
        double patternReward = patternRewards.getOrDefault(pattern, 0.0);
        double patternUncertainty = patternReward < 0 ? 0.8 : 0.2;
        
        // Combine uncertainties
        return Math.min(1.0, 0.6 * baseUncertainty + 0.4 * patternUncertainty);
    }
    
    public List<String> getHighUncertaintyTranslations(int limit) {
        return uncertainTranslations.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    public Map<String, Double> getPatternSuccessRates() {
        Map<String, Double> successRates = new HashMap<>();
        
        for (String pattern : patternRewards.keySet()) {
            double reward = patternRewards.get(pattern);
            double successRate = (reward + 1.0) / 2.0; // Convert [-1,1] to [0,1]
            successRates.put(pattern, successRate);
        }
        
        return successRates;
    }
    
    public LearningMetrics getMetrics() {
        // Update derived metrics
        metrics.patternSuccessRates.putAll(getPatternSuccessRates());
        
        // Calculate accuracy improvement (placeholder)
        double baselineAccuracy = 0.7; // Assumed baseline
        double currentAccuracy = metrics.getPositiveFeedbackRatio();
        metrics.modelAccuracyImprovement = Math.max(0, currentAccuracy - baselineAccuracy);
        
        return metrics;
    }
    
    public void enableLearningStrategy(LearningStrategy strategy) {
        activeStrategies.add(strategy);
        System.out.println("Enabled learning strategy: " + strategy);
    }
    
    public void disableLearningStrategy(LearningStrategy strategy) {
        activeStrategies.remove(strategy);
        System.out.println("Disabled learning strategy: " + strategy);
    }
    
    public void exportLearningData(String filePath) throws IOException {
        // Placeholder for exporting learning data
        System.out.println("Exporting learning data to: " + filePath);
        System.out.println("Total feedback entries: " + 
                          feedbackDatabase.values().stream().mapToInt(List::size).sum());
        System.out.println("Pattern rewards: " + patternRewards.size());
        System.out.println("Language pair weights: " + languagePairWeights.size());
    }
    
    public void importLearningData(String filePath) throws IOException {
        // Placeholder for importing learning data
        System.out.println("Importing learning data from: " + filePath);
    }
    
    public void printLearningReport() {
        LearningMetrics metrics = getMetrics();
        
        System.out.println("\n=== FEEDBACK LEARNING SYSTEM REPORT ===");
        System.out.println("Total Feedback: " + metrics.getTotalFeedbackCount());
        System.out.println("Positive Feedback Ratio: " + 
                          String.format("%.2f%%", metrics.getPositiveFeedbackRatio() * 100));
        System.out.println("Average Quality Score: " + 
                          String.format("%.2f", metrics.getAverageQualityScore()));
        System.out.println("Model Accuracy Improvement: " + 
                          String.format("%.2f%%", metrics.getModelAccuracyImprovement() * 100));
        System.out.println("Active Queries Generated: " + metrics.getActiveQueriesGenerated());
        
        System.out.println("\nTop Language Pairs by Feedback:");
        metrics.getLanguagePairCounts().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
        
        System.out.println("\nPattern Success Rates:");
        getPatternSuccessRates().entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + 
                                               String.format("%.2f%%", entry.getValue() * 100)));
        
        System.out.println("\nActive Learning Strategies: " + activeStrategies);
    }
}