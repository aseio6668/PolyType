package com.polytype.migrator.core.testing;

import com.polytype.migrator.core.logging.PolyTypeLogger;
import com.polytype.migrator.core.config.PolyTypeConfig;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Comprehensive testing framework for PolyType.
 * Supports unit tests, integration tests, performance tests, and regression tests.
 */
public class PolyTypeTestFramework {
    
    public enum TestType {
        UNIT,           // Unit tests for individual components
        INTEGRATION,    // Integration tests for component interactions
        PERFORMANCE,    // Performance and load tests
        REGRESSION,     // Regression tests for bug prevention
        FUNCTIONAL,     // End-to-end functional tests
        SECURITY,       // Security tests
        ML_VALIDATION   // ML model validation tests
    }
    
    public enum TestResult {
        PASSED,
        FAILED,
        SKIPPED,
        ERROR
    }
    
    public static class TestCase {
        private final String id;
        private final String name;
        private final String description;
        private final TestType type;
        private final int timeoutSeconds;
        private final Map<String, Object> parameters;
        private final Runnable testMethod;
        private final List<String> tags;
        private final String category;
        
        public TestCase(String id, String name, String description, TestType type,
                       int timeoutSeconds, Map<String, Object> parameters,
                       Runnable testMethod, List<String> tags, String category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.timeoutSeconds = timeoutSeconds;
            this.parameters = new HashMap<>(parameters != null ? parameters : Collections.emptyMap());
            this.testMethod = testMethod;
            this.tags = new ArrayList<>(tags != null ? tags : Collections.emptyList());
            this.category = category;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public TestType getType() { return type; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
        public Runnable getTestMethod() { return testMethod; }
        public List<String> getTags() { return new ArrayList<>(tags); }
        public String getCategory() { return category; }
    }
    
    public static class TestExecution {
        private final TestCase testCase;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private TestResult result;
        private String errorMessage;
        private Throwable exception;
        private final Map<String, Object> metrics;
        private final List<String> logs;
        
        public TestExecution(TestCase testCase) {
            this.testCase = testCase;
            this.startTime = LocalDateTime.now();
            this.metrics = new ConcurrentHashMap<>();
            this.logs = Collections.synchronizedList(new ArrayList<>());
        }
        
        public void complete(TestResult result) {
            complete(result, null, null);
        }
        
        public void complete(TestResult result, String errorMessage, Throwable exception) {
            this.endTime = LocalDateTime.now();
            this.result = result;
            this.errorMessage = errorMessage;
            this.exception = exception;
        }
        
        public void addMetric(String name, Object value) {
            metrics.put(name, value);
        }
        
        public void addLog(String message) {
            logs.add(LocalDateTime.now() + ": " + message);
        }
        
        public Duration getDuration() {
            return endTime != null ? Duration.between(startTime, endTime) : Duration.ZERO;
        }
        
        // Getters
        public TestCase getTestCase() { return testCase; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public TestResult getResult() { return result; }
        public String getErrorMessage() { return errorMessage; }
        public Throwable getException() { return exception; }
        public Map<String, Object> getMetrics() { return new HashMap<>(metrics); }
        public List<String> getLogs() { return new ArrayList<>(logs); }
    }
    
    public static class TestSuite {
        private final String name;
        private final String description;
        private final List<TestCase> testCases;
        private final Map<String, Object> configuration;
        private final List<String> setupMethods;
        private final List<String> teardownMethods;
        
        public TestSuite(String name, String description, List<TestCase> testCases,
                        Map<String, Object> configuration, List<String> setupMethods,
                        List<String> teardownMethods) {
            this.name = name;
            this.description = description;
            this.testCases = new ArrayList<>(testCases != null ? testCases : Collections.emptyList());
            this.configuration = new HashMap<>(configuration != null ? configuration : Collections.emptyMap());
            this.setupMethods = new ArrayList<>(setupMethods != null ? setupMethods : Collections.emptyList());
            this.teardownMethods = new ArrayList<>(teardownMethods != null ? teardownMethods : Collections.emptyList());
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<TestCase> getTestCases() { return new ArrayList<>(testCases); }
        public Map<String, Object> getConfiguration() { return new HashMap<>(configuration); }
        public List<String> getSetupMethods() { return new ArrayList<>(setupMethods); }
        public List<String> getTeardownMethods() { return new ArrayList<>(teardownMethods); }
    }
    
    public static class TestReport {
        private final String name;
        private final LocalDateTime generatedAt;
        private final List<TestExecution> executions;
        private final Map<String, Object> summary;
        
        public TestReport(String name, List<TestExecution> executions) {
            this.name = name;
            this.generatedAt = LocalDateTime.now();
            this.executions = new ArrayList<>(executions);
            this.summary = generateSummary();
        }
        
        private Map<String, Object> generateSummary() {
            Map<String, Object> summary = new HashMap<>();
            
            int total = executions.size();
            int passed = 0;
            int failed = 0;
            int skipped = 0;
            int errors = 0;
            long totalDuration = 0;
            
            Map<TestType, Integer> typeDistribution = new HashMap<>();
            Map<String, Integer> categoryDistribution = new HashMap<>();
            
            for (TestExecution execution : executions) {
                switch (execution.getResult()) {
                    case PASSED: passed++; break;
                    case FAILED: failed++; break;
                    case SKIPPED: skipped++; break;
                    case ERROR: errors++; break;
                }
                
                totalDuration += execution.getDuration().toMillis();
                
                TestType type = execution.getTestCase().getType();
                typeDistribution.merge(type, 1, Integer::sum);
                
                String category = execution.getTestCase().getCategory();
                if (category != null) {
                    categoryDistribution.merge(category, 1, Integer::sum);
                }
            }
            
            summary.put("total", total);
            summary.put("passed", passed);
            summary.put("failed", failed);
            summary.put("skipped", skipped);
            summary.put("errors", errors);
            summary.put("passRate", total > 0 ? (double) passed / total : 0.0);
            summary.put("totalDurationMs", totalDuration);
            summary.put("averageDurationMs", total > 0 ? totalDuration / total : 0);
            summary.put("typeDistribution", typeDistribution);
            summary.put("categoryDistribution", categoryDistribution);
            
            return summary;
        }
        
        public String getName() { return name; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public List<TestExecution> getExecutions() { return new ArrayList<>(executions); }
        public Map<String, Object> getSummary() { return new HashMap<>(summary); }
    }
    
    // Test framework instance
    private final Map<String, TestSuite> testSuites = new ConcurrentHashMap<>();
    private final List<TestExecution> executionHistory = Collections.synchronizedList(new ArrayList<>());
    private final PolyTypeLogger logger = PolyTypeLogger.getLogger(PolyTypeTestFramework.class);
    private final PolyTypeConfig config = PolyTypeConfig.getInstance();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "PolyTypeTest-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });
    
    // Configuration
    private boolean parallelExecution = true;
    private int maxConcurrentTests = 4;
    private boolean stopOnFirstFailure = false;
    private boolean generateReports = true;
    private String reportDirectory = "test-reports";
    
    public PolyTypeTestFramework() {
        // Ensure report directory exists
        new File(reportDirectory).mkdirs();
        registerBuiltinTestSuites();
    }
    
    private void registerBuiltinTestSuites() {
        // Register core translation test suite
        registerTranslationTestSuite();
        
        // Register ML test suite
        registerMLTestSuite();
        
        // Register binary analysis test suite
        registerBinaryAnalysisTestSuite();
        
        // Register performance test suite
        registerPerformanceTestSuite();
    }
    
    private void registerTranslationTestSuite() {
        List<TestCase> testCases = new ArrayList<>();
        
        // Java to Python translation test
        testCases.add(new TestCase(
            "trans-java-python-001",
            "Java to Python Basic Class Translation",
            "Test basic Java class translation to Python",
            TestType.FUNCTIONAL,
            30,
            Map.of("source", "Java", "target", "Python"),
            this::testJavaToPythonTranslation,
            Arrays.asList("translation", "java", "python"),
            "core-translation"
        ));
        
        // Python to Java translation test
        testCases.add(new TestCase(
            "trans-python-java-001", 
            "Python to Java Basic Class Translation",
            "Test basic Python class translation to Java",
            TestType.FUNCTIONAL,
            30,
            Map.of("source", "Python", "target", "Java"),
            this::testPythonToJavaTranslation,
            Arrays.asList("translation", "python", "java"),
            "core-translation"
        ));
        
        TestSuite translationSuite = new TestSuite(
            "Translation Tests",
            "Core translation functionality tests",
            testCases,
            Map.of("timeout", 60),
            Collections.emptyList(),
            Collections.emptyList()
        );
        
        testSuites.put("translation", translationSuite);
    }
    
    private void registerMLTestSuite() {
        List<TestCase> testCases = new ArrayList<>();
        
        // Pattern recognition test
        testCases.add(new TestCase(
            "ml-pattern-001",
            "Pattern Recognition Accuracy Test",
            "Test ML pattern recognition accuracy",
            TestType.ML_VALIDATION,
            60,
            Map.of("confidence_threshold", 0.8),
            this::testPatternRecognition,
            Arrays.asList("ml", "pattern", "recognition"),
            "ml-validation"
        ));
        
        // Similarity analysis test
        testCases.add(new TestCase(
            "ml-similarity-001",
            "Semantic Similarity Analysis Test",
            "Test semantic similarity analysis accuracy",
            TestType.ML_VALIDATION,
            45,
            Map.of("similarity_threshold", 0.7),
            this::testSemanticSimilarity,
            Arrays.asList("ml", "similarity", "semantic"),
            "ml-validation"
        ));
        
        TestSuite mlSuite = new TestSuite(
            "ML Validation Tests",
            "Machine learning functionality validation",
            testCases,
            Map.of("ml_enabled", true),
            Collections.emptyList(),
            Collections.emptyList()
        );
        
        testSuites.put("ml", mlSuite);
    }
    
    private void registerBinaryAnalysisTestSuite() {
        List<TestCase> testCases = new ArrayList<>();
        
        // PE analysis test
        testCases.add(new TestCase(
            "binary-pe-001",
            "PE File Analysis Test",
            "Test Windows PE file analysis",
            TestType.FUNCTIONAL,
            30,
            Collections.emptyMap(),
            this::testPEAnalysis,
            Arrays.asList("binary", "pe", "windows"),
            "binary-analysis"
        ));
        
        TestSuite binarySuite = new TestSuite(
            "Binary Analysis Tests",
            "Binary file analysis tests",
            testCases,
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList()
        );
        
        testSuites.put("binary", binarySuite);
    }
    
    private void registerPerformanceTestSuite() {
        List<TestCase> testCases = new ArrayList<>();
        
        // Translation performance test
        testCases.add(new TestCase(
            "perf-translation-001",
            "Translation Performance Test",
            "Test translation performance under load",
            TestType.PERFORMANCE,
            300,
            Map.of("iterations", 100, "concurrency", 4),
            this::testTranslationPerformance,
            Arrays.asList("performance", "translation", "load"),
            "performance"
        ));
        
        TestSuite perfSuite = new TestSuite(
            "Performance Tests",
            "Performance and load testing",
            testCases,
            Map.of("warm_up", true),
            Collections.emptyList(),
            Collections.emptyList()
        );
        
        testSuites.put("performance", perfSuite);
    }
    
    // Test execution methods
    public TestExecution runTest(TestCase testCase) {
        logger.info(PolyTypeLogger.LogCategory.GENERAL, 
                   "Running test: " + testCase.getName());
        
        TestExecution execution = new TestExecution(testCase);
        
        try {
            // Run test with timeout
            Future<Void> future = executor.submit(() -> {
                try {
                    testCase.getTestMethod().run();
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            future.get(testCase.getTimeoutSeconds(), TimeUnit.SECONDS);
            
            execution.complete(TestResult.PASSED);
            logger.info(PolyTypeLogger.LogCategory.GENERAL, 
                       "Test passed: " + testCase.getName());
            
        } catch (TimeoutException e) {
            execution.complete(TestResult.ERROR, "Test timed out", e);
            logger.error(PolyTypeLogger.LogCategory.GENERAL, 
                        "Test timeout: " + testCase.getName(), e);
        } catch (Exception e) {
            execution.complete(TestResult.FAILED, e.getMessage(), e);
            logger.error(PolyTypeLogger.LogCategory.GENERAL, 
                        "Test failed: " + testCase.getName(), e);
        }
        
        executionHistory.add(execution);
        return execution;
    }
    
    public List<TestExecution> runTestSuite(String suiteName) {
        TestSuite suite = testSuites.get(suiteName);
        if (suite == null) {
            throw new IllegalArgumentException("Test suite not found: " + suiteName);
        }
        
        logger.info(PolyTypeLogger.LogCategory.GENERAL, 
                   "Running test suite: " + suite.getName());
        
        List<TestExecution> executions = new ArrayList<>();
        
        if (parallelExecution) {
            // Run tests in parallel
            List<Future<TestExecution>> futures = new ArrayList<>();
            
            for (TestCase testCase : suite.getTestCases()) {
                Future<TestExecution> future = executor.submit(() -> runTest(testCase));
                futures.add(future);
            }
            
            for (Future<TestExecution> future : futures) {
                try {
                    TestExecution execution = future.get();
                    executions.add(execution);
                    
                    if (stopOnFirstFailure && execution.getResult() == TestResult.FAILED) {
                        // Cancel remaining tests
                        futures.forEach(f -> f.cancel(true));
                        break;
                    }
                } catch (Exception e) {
                    logger.error(PolyTypeLogger.LogCategory.GENERAL, 
                                "Error getting test result", e);
                }
            }
        } else {
            // Run tests sequentially
            for (TestCase testCase : suite.getTestCases()) {
                TestExecution execution = runTest(testCase);
                executions.add(execution);
                
                if (stopOnFirstFailure && execution.getResult() == TestResult.FAILED) {
                    break;
                }
            }
        }
        
        logger.info(PolyTypeLogger.LogCategory.GENERAL, 
                   "Test suite completed: " + suite.getName() + 
                   " (" + executions.size() + " tests)");
        
        return executions;
    }
    
    public TestReport runAllTests() {
        logger.info(PolyTypeLogger.LogCategory.GENERAL, "Running all test suites");
        
        List<TestExecution> allExecutions = new ArrayList<>();
        
        for (String suiteName : testSuites.keySet()) {
            List<TestExecution> executions = runTestSuite(suiteName);
            allExecutions.addAll(executions);
        }
        
        TestReport report = new TestReport("All Tests", allExecutions);
        
        if (generateReports) {
            generateHtmlReport(report);
            generateJsonReport(report);
        }
        
        logger.info(PolyTypeLogger.LogCategory.GENERAL, 
                   "All tests completed. Report generated.");
        
        return report;
    }
    
    // Individual test implementations
    private void testJavaToPythonTranslation() {
        String javaCode = "public class Calculator { private int value; public int getValue() { return value; } }";
        // Mock translation logic
        String pythonCode = translateCode(javaCode, "Java", "Python");
        
        if (pythonCode == null || pythonCode.isEmpty()) {
            throw new AssertionError("Translation result is empty");
        }
        
        if (!pythonCode.contains("class Calculator")) {
            throw new AssertionError("Expected class definition not found in translation");
        }
    }
    
    private void testPythonToJavaTranslation() {
        String pythonCode = "class Calculator:\\n    def __init__(self):\\n        self.value = 0\\n    def get_value(self):\\n        return self.value";
        // Mock translation logic
        String javaCode = translateCode(pythonCode, "Python", "Java");
        
        if (javaCode == null || javaCode.isEmpty()) {
            throw new AssertionError("Translation result is empty");
        }
        
        if (!javaCode.contains("class Calculator")) {
            throw new AssertionError("Expected class definition not found in translation");
        }
    }
    
    private void testPatternRecognition() {
        String code = "public enum Singleton { INSTANCE; }";
        // Mock ML pattern recognition
        Map<String, Double> patterns = recognizePatterns(code);
        
        if (!patterns.containsKey("Singleton")) {
            throw new AssertionError("Singleton pattern not recognized");
        }
        
        double confidence = patterns.get("Singleton");
        if (confidence < 0.8) {
            throw new AssertionError("Singleton pattern confidence too low: " + confidence);
        }
    }
    
    private void testSemanticSimilarity() {
        String code1 = "class Calculator { int add(int a, int b) { return a + b; } }";
        String code2 = "class Calc { int sum(int x, int y) { return x + y; } }";
        
        // Mock similarity analysis
        double similarity = calculateSimilarity(code1, code2);
        
        if (similarity < 0.7) {
            throw new AssertionError("Similarity too low for functionally equivalent code: " + similarity);
        }
    }
    
    private void testPEAnalysis() {
        // Mock PE analysis test
        String mockPEFile = "mock-pe-file.exe";
        Map<String, Object> analysis = analyzeBinary(mockPEFile);
        
        if (analysis.isEmpty()) {
            throw new AssertionError("Binary analysis returned no results");
        }
    }
    
    private void testTranslationPerformance() {
        String code = "public class Test { public void method() { System.out.println(\"Hello\"); } }";
        int iterations = 100;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            String result = translateCode(code, "Java", "Python");
            if (result == null) {
                throw new AssertionError("Translation failed at iteration " + i);
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        double avgTime = (double) duration / iterations;
        
        if (avgTime > 1000) { // 1 second per translation
            throw new AssertionError("Translation performance too slow: " + avgTime + "ms average");
        }
        
        logger.info(PolyTypeLogger.LogCategory.PERFORMANCE, 
                   "Translation performance: " + avgTime + "ms average");
    }
    
    // Mock methods for testing (would be replaced with actual implementations)
    private String translateCode(String code, String fromLang, String toLang) {
        // Mock implementation
        return "# Translated from " + fromLang + " to " + toLang + "\\nclass MockTranslation: pass";
    }
    
    private Map<String, Double> recognizePatterns(String code) {
        // Mock implementation
        Map<String, Double> patterns = new HashMap<>();
        if (code.contains("enum") && code.contains("INSTANCE")) {
            patterns.put("Singleton", 0.95);
        }
        return patterns;
    }
    
    private double calculateSimilarity(String code1, String code2) {
        // Mock implementation
        return 0.85;
    }
    
    private Map<String, Object> analyzeBinary(String filePath) {
        // Mock implementation
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("fileType", "PE");
        analysis.put("architecture", "x64");
        analysis.put("functions", Arrays.asList("main", "init"));
        return analysis;
    }
    
    // Report generation
    private void generateHtmlReport(TestReport report) {
        try {
            String fileName = reportDirectory + "/test-report-" + 
                            System.currentTimeMillis() + ".html";
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                writer.println("<!DOCTYPE html>");
                writer.println("<html><head><title>PolyType Test Report</title>");
                writer.println("<style>");
                writer.println("body { font-family: Arial, sans-serif; margin: 20px; }");
                writer.println(".passed { color: green; }");
                writer.println(".failed { color: red; }");
                writer.println(".error { color: orange; }");
                writer.println(".skipped { color: gray; }");
                writer.println("table { border-collapse: collapse; width: 100%; }");
                writer.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
                writer.println("th { background-color: #f2f2f2; }");
                writer.println("</style></head><body>");
                
                writer.println("<h1>PolyType Test Report</h1>");
                writer.println("<p>Generated: " + report.getGeneratedAt() + "</p>");
                
                // Summary
                Map<String, Object> summary = report.getSummary();
                writer.println("<h2>Summary</h2>");
                writer.println("<table>");
                writer.println("<tr><th>Metric</th><th>Value</th></tr>");
                writer.println("<tr><td>Total Tests</td><td>" + summary.get("total") + "</td></tr>");
                writer.println("<tr><td>Passed</td><td class='passed'>" + summary.get("passed") + "</td></tr>");
                writer.println("<tr><td>Failed</td><td class='failed'>" + summary.get("failed") + "</td></tr>");
                writer.println("<tr><td>Errors</td><td class='error'>" + summary.get("errors") + "</td></tr>");
                writer.println("<tr><td>Skipped</td><td class='skipped'>" + summary.get("skipped") + "</td></tr>");
                writer.println("<tr><td>Pass Rate</td><td>" + 
                              String.format("%.1f%%", (Double) summary.get("passRate") * 100) + "</td></tr>");
                writer.println("<tr><td>Total Duration</td><td>" + summary.get("totalDurationMs") + " ms</td></tr>");
                writer.println("</table>");
                
                // Test Results
                writer.println("<h2>Test Results</h2>");
                writer.println("<table>");
                writer.println("<tr><th>Test</th><th>Type</th><th>Result</th><th>Duration</th><th>Error</th></tr>");
                
                for (TestExecution execution : report.getExecutions()) {
                    TestCase testCase = execution.getTestCase();
                    String resultClass = execution.getResult().name().toLowerCase();
                    
                    writer.println("<tr>");
                    writer.println("<td>" + testCase.getName() + "</td>");
                    writer.println("<td>" + testCase.getType() + "</td>");
                    writer.println("<td class='" + resultClass + "'>" + execution.getResult() + "</td>");
                    writer.println("<td>" + execution.getDuration().toMillis() + " ms</td>");
                    writer.println("<td>" + (execution.getErrorMessage() != null ? execution.getErrorMessage() : "") + "</td>");
                    writer.println("</tr>");
                }
                
                writer.println("</table>");
                writer.println("</body></html>");
            }
            
            logger.info(PolyTypeLogger.LogCategory.GENERAL, 
                       "HTML report generated: " + fileName);
            
        } catch (IOException e) {
            logger.error(PolyTypeLogger.LogCategory.GENERAL, 
                        "Failed to generate HTML report", e);
        }
    }
    
    private void generateJsonReport(TestReport report) {
        try {
            String fileName = reportDirectory + "/test-report-" + 
                            System.currentTimeMillis() + ".json";
            
            // Simple JSON generation (in a real implementation, would use a JSON library)
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                writer.println("{");
                writer.println("  \"name\": \"" + report.getName() + "\",");
                writer.println("  \"generatedAt\": \"" + report.getGeneratedAt() + "\",");
                writer.println("  \"summary\": " + formatMapAsJson(report.getSummary()) + ",");
                writer.println("  \"executions\": [");
                
                List<TestExecution> executions = report.getExecutions();
                for (int i = 0; i < executions.size(); i++) {
                    TestExecution execution = executions.get(i);
                    writer.println("    {");
                    writer.println("      \"testName\": \"" + execution.getTestCase().getName() + "\",");
                    writer.println("      \"testType\": \"" + execution.getTestCase().getType() + "\",");
                    writer.println("      \"result\": \"" + execution.getResult() + "\",");
                    writer.println("      \"duration\": " + execution.getDuration().toMillis() + ",");
                    writer.println("      \"error\": \"" + (execution.getErrorMessage() != null ? execution.getErrorMessage() : "") + "\"");
                    writer.print("    }");
                    if (i < executions.size() - 1) writer.println(",");
                    else writer.println();
                }
                
                writer.println("  ]");
                writer.println("}");
            }
            
            logger.info(PolyTypeLogger.LogCategory.GENERAL, 
                       "JSON report generated: " + fileName);
            
        } catch (IOException e) {
            logger.error(PolyTypeLogger.LogCategory.GENERAL, 
                        "Failed to generate JSON report", e);
        }
    }
    
    private String formatMapAsJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append("\"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    // Configuration and management
    public void setParallelExecution(boolean parallel) {
        this.parallelExecution = parallel;
    }
    
    public void setMaxConcurrentTests(int maxConcurrent) {
        this.maxConcurrentTests = maxConcurrent;
    }
    
    public void setStopOnFirstFailure(boolean stopOnFailure) {
        this.stopOnFirstFailure = stopOnFailure;
    }
    
    public void setGenerateReports(boolean generateReports) {
        this.generateReports = generateReports;
    }
    
    public void setReportDirectory(String reportDirectory) {
        this.reportDirectory = reportDirectory;
        new File(reportDirectory).mkdirs();
    }
    
    // Query methods
    public Set<String> getAvailableTestSuites() {
        return new HashSet<>(testSuites.keySet());
    }
    
    public TestSuite getTestSuite(String name) {
        return testSuites.get(name);
    }
    
    public List<TestExecution> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }
    
    public void printSummary() {
        System.out.println("\\n=== PolyType Test Framework Summary ===");
        System.out.println("Test Suites: " + testSuites.size());
        System.out.println("Total Executions: " + executionHistory.size());
        System.out.println("Parallel Execution: " + parallelExecution);
        System.out.println("Max Concurrent: " + maxConcurrentTests);
        System.out.println("Report Generation: " + generateReports);
        System.out.println("Report Directory: " + reportDirectory);
        System.out.println("==========================================\\n");
    }
    
    // Cleanup
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        logger.info(PolyTypeLogger.LogCategory.GENERAL, "Test framework shutdown complete");
    }
}