package com.polytype.migrator.web;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.time.LocalDateTime;

/**
 * Comprehensive Web Framework Migration System
 * Handles migration between established web frameworks and patterns
 */
public class WebFrameworkMigrator {
    
    public enum WebFramework {
        JSP("JavaServer Pages", "jsp", new String[]{"<%", "%>", "jsp:"}),
        SPRING_BOOT("Spring Boot", "java", new String[]{"@RestController", "@SpringBootApplication"}),
        ASP_NET_WEBFORMS("ASP.NET WebForms", "aspx", new String[]{"<%@", "runat=\"server\"", "Page_Load"}),
        ASP_NET_MVC("ASP.NET MVC", "cs", new String[]{"ActionResult", "[HttpGet]", "Controller"}),
        RAILS("Ruby on Rails", "rb", new String[]{"def index", "render", "params"}),
        EXPRESS_JS("Express.js", "js", new String[]{"app.get", "res.send", "require('express')"}),
        DJANGO("Django", "py", new String[]{"def view", "HttpResponse", "from django"}),
        FLASK("Flask", "py", new String[]{"@app.route", "from flask", "return render_template"}),
        STRUTS("Apache Struts", "java", new String[]{"Action", "execute()", "struts-config"}),
        LARAVEL("Laravel", "php", new String[]{"Route::", "Eloquent", "artisan"}),
        CODEIGNITER("CodeIgniter", "php", new String[]{"$this->load", "CI_Controller", "system/core"}),
        SYMFONY("Symfony", "php", new String[]{"use Symfony", "@Route", "Controller"}),
        ANGULAR_JS("AngularJS 1.x", "js", new String[]{"angular.module", "ng-", "$scope"}),
        JQUERY_UI("jQuery UI", "js", new String[]{"$(document).ready", "$.ajax", "jQuery"}),
        BACKBONE_JS("Backbone.js", "js", new String[]{"Backbone.Model", "extend", "initialize"}),
        GRAILS("Grails", "groovy", new String[]{"def index()", "render", "grails-app"}),
        PLAY_FRAMEWORK("Play Framework", "scala", new String[]{"def index", "Action", "play.api"}),
        WICKET("Apache Wicket", "java", new String[]{"WebPage", "wicket:", "onSubmit"}),
        VAADIN("Vaadin", "java", new String[]{"@Route", "VerticalLayout", "Component"}),
        TAPESTRY("Apache Tapestry", "java", new String[]{"@Component", "onActivate", ".tml"});
        
        private final String displayName;
        private final String primaryExtension;
        private final String[] identifiers;
        
        WebFramework(String displayName, String primaryExtension, String[] identifiers) {
            this.displayName = displayName;
            this.primaryExtension = primaryExtension;
            this.identifiers = identifiers;
        }
        
        public String getDisplayName() { return displayName; }
        public String getPrimaryExtension() { return primaryExtension; }
        public String[] getIdentifiers() { return identifiers; }
    }
    
    public static class MigrationResult {
        private final WebFramework sourceFramework;
        private final WebFramework targetFramework;
        private final String migratedCode;
        private final List<String> changesApplied;
        private final List<String> warnings;
        private final int complexityScore;
        private final LocalDateTime timestamp;
        
        public MigrationResult(WebFramework source, WebFramework target, String migratedCode,
                             List<String> changes, List<String> warnings, int complexity) {
            this.sourceFramework = source;
            this.targetFramework = target;
            this.migratedCode = migratedCode;
            this.changesApplied = changes != null ? new ArrayList<>(changes) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
            this.complexityScore = complexity;
            this.timestamp = LocalDateTime.now();
        }
        
        // Getters
        public WebFramework getSourceFramework() { return sourceFramework; }
        public WebFramework getTargetFramework() { return targetFramework; }
        public String getMigratedCode() { return migratedCode; }
        public List<String> getChangesApplied() { return new ArrayList<>(changesApplied); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public int getComplexityScore() { return complexityScore; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Migration: ").append(sourceFramework.getDisplayName())
              .append(" → ").append(targetFramework.getDisplayName()).append("\n");
            sb.append("Complexity: ").append(complexityScore).append("/100\n");
            sb.append("Changes Applied: ").append(changesApplied.size()).append("\n");
            if (!warnings.isEmpty()) {
                sb.append("Warnings: ").append(warnings.size()).append("\n");
            }
            sb.append("Timestamp: ").append(timestamp).append("\n");
            return sb.toString();
        }
    }
    
    /**
     * Detect web framework from source code
     */
    public WebFramework detectFramework(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return null;
        }
        
        // Count matches for each framework
        Map<WebFramework, Integer> scores = new HashMap<>();
        
        for (WebFramework framework : WebFramework.values()) {
            int score = 0;
            for (String identifier : framework.getIdentifiers()) {
                score += countOccurrences(sourceCode, identifier);
            }
            if (score > 0) {
                scores.put(framework, score);
            }
        }
        
        // Return framework with highest score
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Migrate between web frameworks
     */
    public MigrationResult migrate(String sourceCode, WebFramework targetFramework) {
        WebFramework sourceFramework = detectFramework(sourceCode);
        if (sourceFramework == null) {
            List<String> warnings = Arrays.asList("Could not detect source framework");
            return new MigrationResult(null, targetFramework, sourceCode, null, warnings, 0);
        }
        
        return migrate(sourceCode, sourceFramework, targetFramework);
    }
    
    /**
     * Migrate with explicit source framework
     */
    public MigrationResult migrate(String sourceCode, WebFramework sourceFramework, WebFramework targetFramework) {
        if (sourceFramework == targetFramework) {
            List<String> warnings = Arrays.asList("Source and target frameworks are the same");
            return new MigrationResult(sourceFramework, targetFramework, sourceCode, null, warnings, 0);
        }
        
        List<String> changes = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String migratedCode = sourceCode;
        int complexityScore = calculateComplexity(sourceCode);
        
        // Apply specific migration patterns
        if (sourceFramework == WebFramework.JSP && targetFramework == WebFramework.SPRING_BOOT) {
            migratedCode = migrateJspToSpringBoot(migratedCode, changes, warnings);
        } else if (sourceFramework == WebFramework.ASP_NET_WEBFORMS && targetFramework == WebFramework.ASP_NET_MVC) {
            migratedCode = migrateWebFormsToMvc(migratedCode, changes, warnings);
        } else if (sourceFramework == WebFramework.RAILS && targetFramework == WebFramework.EXPRESS_JS) {
            migratedCode = migrateRailsToExpress(migratedCode, changes, warnings);
        } else if (sourceFramework == WebFramework.ANGULAR_JS && targetFramework == WebFramework.EXPRESS_JS) {
            migratedCode = migrateAngularJsToExpress(migratedCode, changes, warnings);
        } else if (sourceFramework == WebFramework.STRUTS && targetFramework == WebFramework.SPRING_BOOT) {
            migratedCode = migrateStrutsToSpringBoot(migratedCode, changes, warnings);
        } else if (sourceFramework == WebFramework.JQUERY_UI && targetFramework == WebFramework.EXPRESS_JS) {
            migratedCode = migrateJQueryToExpress(migratedCode, changes, warnings);
        } else {
            // Generic migration
            migratedCode = performGenericMigration(migratedCode, sourceFramework, targetFramework, changes, warnings);
        }
        
        return new MigrationResult(sourceFramework, targetFramework, migratedCode, changes, warnings, complexityScore);
    }
    
    /**
     * JSP to Spring Boot Migration
     */
    private String migrateJspToSpringBoot(String code, List<String> changes, List<String> warnings) {
        String migrated = code;
        
        // Convert JSP scriptlets to controller methods
        Pattern scriptletPattern = Pattern.compile("<%([^%]+)%>", Pattern.DOTALL);
        Matcher matcher = scriptletPattern.matcher(migrated);
        if (matcher.find()) {
            StringBuilder controller = new StringBuilder();
            controller.append("@RestController\n");
            controller.append("@RequestMapping(\"/api\")\n");
            controller.append("public class MigratedController {\n\n");
            
            int methodCount = 1;
            StringBuffer result = new StringBuffer();
            matcher.reset();
            
            while (matcher.find()) {
                String scriptletContent = matcher.group(1).trim();
                if (!scriptletContent.isEmpty()) {
                    String methodName = "method" + methodCount++;
                    
                    controller.append("    @GetMapping(\"/").append(methodName).append("\")\n");
                    controller.append("    public ResponseEntity<String> ").append(methodName).append("() {\n");
                    controller.append("        // Converted from JSP scriptlet\n");
                    controller.append("        ").append(scriptletContent).append("\n");
                    controller.append("        return ResponseEntity.ok(\"result\");\n");
                    controller.append("    }\n\n");
                    
                    matcher.appendReplacement(result, "<!-- Moved to controller method: " + methodName + " -->");
                    changes.add("Converted JSP scriptlet to controller method: " + methodName);
                }
            }
            matcher.appendTail(result);
            controller.append("}\n");
            
            migrated = controller.toString() + "\n\n" + result.toString();
        }
        
        // Convert JSP directives to Spring Boot annotations
        migrated = migrated.replaceAll("<%@\\s*page\\s+[^>]*%>", "// Page directive converted to Spring Boot configuration");
        if (!code.equals(migrated)) {
            changes.add("Converted JSP page directives to Spring Boot configuration");
        }
        
        // Convert JSP includes
        migrated = migrated.replaceAll("<%@\\s*include\\s+file=\"([^\"]+)\"\\s*%>", 
            "// Include converted: consider using @ComponentScan for $1");
        if (migrated.contains("Include converted")) {
            changes.add("Converted JSP includes to Spring Boot component suggestions");
            warnings.add("Manual review needed for JSP includes - consider Spring Boot component architecture");
        }
        
        return migrated;
    }
    
    /**
     * ASP.NET WebForms to MVC Migration
     */
    private String migrateWebFormsToMvc(String code, List<String> changes, List<String> warnings) {
        String migrated = code;
        
        // Convert Page_Load to Controller Action
        Pattern pageLoadPattern = Pattern.compile("protected\\s+void\\s+Page_Load\\s*\\([^)]*\\)\\s*\\{([^}]+)\\}", Pattern.DOTALL);
        Matcher matcher = pageLoadPattern.matcher(migrated);
        if (matcher.find()) {
            String pageLoadContent = matcher.group(1);
            
            StringBuilder controller = new StringBuilder();
            controller.append("public class HomeController : Controller {\n");
            controller.append("    public ActionResult Index() {\n");
            controller.append("        // Converted from Page_Load\n");
            controller.append(pageLoadContent);
            controller.append("        return View();\n");
            controller.append("    }\n");
            controller.append("}\n");
            
            migrated = matcher.replaceAll(controller.toString());
            changes.add("Converted Page_Load to MVC Controller Action");
        }
        
        // Convert server controls to Razor syntax
        migrated = migrated.replaceAll("<asp:Label[^>]*Text=\"([^\"]+)\"[^>]*/>", "@Html.Raw(\"$1\")");
        migrated = migrated.replaceAll("<asp:TextBox[^>]*ID=\"([^\"]+)\"[^>]*/>", "@Html.TextBox(\"$1\")");
        migrated = migrated.replaceAll("<asp:Button[^>]*Text=\"([^\"]+)\"[^>]*/>", "<input type=\"submit\" value=\"$1\" />");
        
        if (!code.equals(migrated)) {
            changes.add("Converted ASP.NET server controls to Razor syntax");
        }
        
        // Convert postback events
        if (migrated.contains("OnClick=\"")) {
            migrated = migrated.replaceAll("OnClick=\"([^\"]+)\"", "onclick=\"return handleClick();\"");
            changes.add("Converted server-side OnClick to client-side handling");
            warnings.add("Server-side event handlers need manual conversion to MVC action methods");
        }
        
        return migrated;
    }
    
    /**
     * Rails to Express.js Migration
     */
    private String migrateRailsToExpress(String code, List<String> changes, List<String> warnings) {
        String migrated = code;
        
        // Convert Rails controller actions to Express routes
        Pattern controllerPattern = Pattern.compile("def\\s+(\\w+)([^e]*)end", Pattern.DOTALL);
        Matcher matcher = controllerPattern.matcher(migrated);
        
        StringBuilder express = new StringBuilder();
        express.append("const express = require('express');\n");
        express.append("const app = express();\n\n");
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String actionName = matcher.group(1);
            String actionBody = matcher.group(2);
            
            String httpMethod = "get";
            if (actionName.equals("create")) httpMethod = "post";
            else if (actionName.equals("update")) httpMethod = "put";
            else if (actionName.equals("destroy")) httpMethod = "delete";
            
            express.append("app.").append(httpMethod).append("('/").append(actionName).append("', (req, res) => {\n");
            express.append("  // Converted from Rails action\n");
            
            // Convert Rails-specific syntax
            String convertedBody = actionBody
                .replaceAll("render\\s+:?(\\w+)", "res.render('$1')")
                .replaceAll("redirect_to\\s+(.+)", "res.redirect($1)")
                .replaceAll("params\\[(\\w+)\\]", "req.params.$1")
                .replaceAll("@(\\w+)\\s*=", "const $1 =");
            
            express.append(convertedBody);
            express.append("});\n\n");
            
            matcher.appendReplacement(result, "// Converted to Express route: " + actionName);
            changes.add("Converted Rails action '" + actionName + "' to Express route");
        }
        matcher.appendTail(result);
        
        express.append("module.exports = app;\n");
        migrated = express.toString() + "\n" + result.toString();
        
        if (migrated.contains("render")) {
            warnings.add("Rails view rendering may need adjustment for Express template engines");
        }
        
        return migrated;
    }
    
    /**
     * AngularJS to Express.js Migration (backend extraction)
     */
    private String migrateAngularJsToExpress(String code, List<String> changes, List<String> warnings) {
        String migrated = code;
        StringBuilder express = new StringBuilder();
        
        express.append("const express = require('express');\n");
        express.append("const app = express();\n");
        express.append("app.use(express.json());\n\n");
        
        // Extract $http calls and convert to API endpoints
        Pattern httpPattern = Pattern.compile("\\$http\\.([a-z]+)\\(['\"]([^'\"]+)['\"]([^)]*)\\)", Pattern.DOTALL);
        Matcher matcher = httpPattern.matcher(migrated);
        
        Set<String> extractedRoutes = new HashSet<>();
        while (matcher.find()) {
            String method = matcher.group(1);
            String url = matcher.group(2);
            
            if (!extractedRoutes.contains(method + ":" + url)) {
                express.append("app.").append(method).append("('").append(url).append("', (req, res) => {\n");
                express.append("  // Extracted from AngularJS $http.").append(method).append("\n");
                express.append("  res.json({ message: 'API endpoint for ").append(url).append("' });\n");
                express.append("});\n\n");
                
                extractedRoutes.add(method + ":" + url);
                changes.add("Extracted API endpoint: " + method.toUpperCase() + " " + url);
            }
        }
        
        express.append("const PORT = process.env.PORT || 3000;\n");
        express.append("app.listen(PORT, () => {\n");
        express.append("  console.log(`Server running on port ${PORT}`);\n");
        express.append("});\n");
        
        if (!extractedRoutes.isEmpty()) {
            migrated = express.toString() + "\n\n// Original AngularJS code (client-side):\n" + migrated;
            warnings.add("AngularJS frontend code remains - consider modern frontend framework");
            warnings.add("Extracted API endpoints need proper implementation");
        }
        
        return migrated;
    }
    
    /**
     * Struts to Spring Boot Migration
     */
    private String migrateStrutsToSpringBoot(String code, List<String> changes, List<String> warnings) {
        String migrated = code;
        
        // Convert Struts Action to Spring Boot Controller
        Pattern actionPattern = Pattern.compile("public\\s+class\\s+(\\w+)\\s+extends\\s+Action", Pattern.DOTALL);
        Matcher matcher = actionPattern.matcher(migrated);
        if (matcher.find()) {
            String className = matcher.group(1);
            migrated = migrated.replace("extends Action", "");
            migrated = "@RestController\n@RequestMapping(\"/api\")\n" + migrated;
            changes.add("Converted Struts Action to Spring Boot RestController");
        }
        
        // Convert execute method
        migrated = migrated.replaceAll("public\\s+ActionForward\\s+execute\\s*\\([^)]+\\)", 
            "@PostMapping(\"/execute\")\n    public ResponseEntity<String> execute(HttpServletRequest request)");
        if (!code.equals(migrated)) {
            changes.add("Converted execute() method to Spring Boot endpoint");
        }
        
        // Convert ActionForward returns
        migrated = migrated.replaceAll("return\\s+mapping\\.findForward\\([^)]+\\)", 
            "return ResponseEntity.ok(\"success\")");
        if (migrated.contains("ResponseEntity.ok")) {
            changes.add("Converted ActionForward returns to ResponseEntity");
            warnings.add("Action forward mappings need manual conversion to Spring Boot routing");
        }
        
        return migrated;
    }
    
    /**
     * jQuery UI to Express.js Migration
     */
    private String migrateJQueryToExpress(String code, List<String> changes, List<String> warnings) {
        String migrated = code;
        StringBuilder express = new StringBuilder();
        
        express.append("const express = require('express');\n");
        express.append("const app = express();\n");
        express.append("app.use(express.static('public'));\n");
        express.append("app.use(express.json());\n\n");
        
        // Extract AJAX calls and convert to API endpoints
        Pattern ajaxPattern = Pattern.compile("\\$.ajax\\(\\{[^}]*url:\\s*['\"]([^'\"]+)['\"][^}]*\\}", Pattern.DOTALL);
        Matcher matcher = ajaxPattern.matcher(migrated);
        
        Set<String> extractedEndpoints = new HashSet<>();
        while (matcher.find()) {
            String url = matcher.group(1);
            if (!extractedEndpoints.contains(url)) {
                express.append("app.post('").append(url).append("', (req, res) => {\n");
                express.append("  // Extracted from jQuery AJAX call\n");
                express.append("  res.json({ success: true, data: req.body });\n");
                express.append("});\n\n");
                
                extractedEndpoints.add(url);
                changes.add("Extracted API endpoint from AJAX call: " + url);
            }
        }
        
        // Convert document ready to server startup
        if (migrated.contains("$(document).ready")) {
            express.append("// Server startup equivalent of $(document).ready\n");
            express.append("app.listen(3000, () => {\n");
            express.append("  console.log('Server started - equivalent of document ready');\n");
            express.append("});\n");
            changes.add("Converted $(document).ready to server startup");
        }
        
        if (!extractedEndpoints.isEmpty()) {
            migrated = express.toString() + "\n\n// Original jQuery code (client-side):\n" + migrated;
            warnings.add("jQuery client-side code remains - consider modern frontend approach");
        }
        
        return migrated;
    }
    
    /**
     * Generic migration for unsupported combinations
     */
    private String performGenericMigration(String code, WebFramework source, WebFramework target, 
                                         List<String> changes, List<String> warnings) {
        String migrated = code;
        
        // Add target framework boilerplate
        StringBuilder boilerplate = new StringBuilder();
        
        switch (target) {
            case SPRING_BOOT:
                boilerplate.append("@SpringBootApplication\n");
                boilerplate.append("@RestController\n");
                boilerplate.append("public class MigratedApplication {\n");
                boilerplate.append("    public static void main(String[] args) {\n");
                boilerplate.append("        SpringApplication.run(MigratedApplication.class, args);\n");
                boilerplate.append("    }\n");
                boilerplate.append("}\n\n");
                break;
            case EXPRESS_JS:
                boilerplate.append("const express = require('express');\n");
                boilerplate.append("const app = express();\n");
                boilerplate.append("app.listen(3000);\n\n");
                break;
            case FLASK:
                boilerplate.append("from flask import Flask\n");
                boilerplate.append("app = Flask(__name__)\n\n");
                break;
            case DJANGO:
                boilerplate.append("from django.http import HttpResponse\n");
                boilerplate.append("from django.shortcuts import render\n\n");
                break;
        }
        
        if (boilerplate.length() > 0) {
            migrated = boilerplate.toString() + "// Migrated from " + source.getDisplayName() + "\n" + migrated;
            changes.add("Added " + target.getDisplayName() + " boilerplate");
        }
        
        warnings.add("Generic migration applied - manual review recommended");
        warnings.add("Specific " + source.getDisplayName() + " to " + target.getDisplayName() + " patterns not implemented");
        
        return migrated;
    }
    
    /**
     * Calculate migration complexity
     */
    private int calculateComplexity(String code) {
        int complexity = 0;
        
        // Base complexity on code length
        complexity += Math.min(code.length() / 100, 30);
        
        // Add complexity for various patterns
        complexity += countOccurrences(code, "function") * 2;
        complexity += countOccurrences(code, "class") * 3;
        complexity += countOccurrences(code, "if") * 1;
        complexity += countOccurrences(code, "for") * 1;
        complexity += countOccurrences(code, "while") * 1;
        complexity += countOccurrences(code, "try") * 2;
        complexity += countOccurrences(code, "catch") * 2;
        
        return Math.min(complexity, 100);
    }
    
    private int countOccurrences(String text, String pattern) {
        return text.split(Pattern.quote(pattern), -1).length - 1;
    }
    
    /**
     * Generate migration report
     */
    public String generateMigrationReport(List<MigrationResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("Web Framework Migration Report\n");
        report.append("Generated: ").append(LocalDateTime.now()).append("\n");
        report.append("=" .repeat(50)).append("\n\n");
        
        int totalMigrations = results.size();
        int totalChanges = results.stream().mapToInt(r -> r.getChangesApplied().size()).sum();
        int totalWarnings = results.stream().mapToInt(r -> r.getWarnings().size()).sum();
        double avgComplexity = results.stream().mapToInt(MigrationResult::getComplexityScore).average().orElse(0.0);
        
        report.append("Summary:\n");
        report.append("- Total Migrations: ").append(totalMigrations).append("\n");
        report.append("- Total Changes Applied: ").append(totalChanges).append("\n");
        report.append("- Total Warnings: ").append(totalWarnings).append("\n");
        report.append("- Average Complexity: ").append(String.format("%.1f", avgComplexity)).append("/100\n\n");
        
        // Framework usage statistics
        Map<WebFramework, Integer> sourceStats = new HashMap<>();
        Map<WebFramework, Integer> targetStats = new HashMap<>();
        
        for (MigrationResult result : results) {
            if (result.getSourceFramework() != null) {
                sourceStats.merge(result.getSourceFramework(), 1, Integer::sum);
            }
            targetStats.merge(result.getTargetFramework(), 1, Integer::sum);
        }
        
        report.append("Most Migrated From:\n");
        sourceStats.entrySet().stream()
            .sorted(Map.Entry.<WebFramework, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> report.append("- ").append(entry.getKey().getDisplayName())
                .append(": ").append(entry.getValue()).append("\n"));
        
        report.append("\nMost Migrated To:\n");
        targetStats.entrySet().stream()
            .sorted(Map.Entry.<WebFramework, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> report.append("- ").append(entry.getKey().getDisplayName())
                .append(": ").append(entry.getValue()).append("\n"));
        
        report.append("\nDetailed Results:\n");
        report.append("-" .repeat(30)).append("\n");
        
        for (int i = 0; i < results.size(); i++) {
            MigrationResult result = results.get(i);
            report.append("\n").append(i + 1).append(". ").append(result.toString());
            
            if (!result.getChangesApplied().isEmpty()) {
                report.append("Changes:\n");
                for (String change : result.getChangesApplied()) {
                    report.append("  • ").append(change).append("\n");
                }
            }
            
            if (!result.getWarnings().isEmpty()) {
                report.append("Warnings:\n");
                for (String warning : result.getWarnings()) {
                    report.append("  ⚠ ").append(warning).append("\n");
                }
            }
        }
        
        return report.toString();
    }
    
    /**
     * Batch migrate multiple files
     */
    public List<MigrationResult> batchMigrate(Map<String, String> sourceFiles, WebFramework targetFramework) {
        List<MigrationResult> results = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : sourceFiles.entrySet()) {
            String filename = entry.getKey();
            String content = entry.getValue();
            
            try {
                MigrationResult result = migrate(content, targetFramework);
                results.add(result);
            } catch (Exception e) {
                List<String> errors = Arrays.asList("Migration failed: " + e.getMessage());
                results.add(new MigrationResult(null, targetFramework, content, null, errors, 100));
            }
        }
        
        return results;
    }
}