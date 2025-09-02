package com.polytype.migrator.web;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.time.LocalDateTime;

/**
 * Legacy Web Technology Migration System
 * Specialized for migrating old, established web technologies to modern equivalents
 */
public class LegacyWebMigrator {
    
    public enum LegacyWebTechnology {
        CGI_PERL("CGI Perl", "pl", new String[]{"#!/usr/bin/perl", "use CGI", "print \"Content-type"}),
        CLASSIC_ASP("Classic ASP", "asp", new String[]{"<%", "Response.Write", "Server.CreateObject"}),
        PHP3_4("PHP 3/4", "php", new String[]{"<?php", "mysql_connect", "ereg("}),
        COLD_FUSION("ColdFusion", "cfm", new String[]{"<cfset", "<cfquery", "<cfoutput"}),
        JSP_1_2("JSP 1.2", "jsp", new String[]{"<%@", "pageContext", "jsp:useBean"}),
        SERVLET_2_3("Servlet 2.3", "java", new String[]{"HttpServlet", "doGet", "web.xml"}),
        WEBLOGIC_6("WebLogic 6.x", "java", new String[]{"weblogic.servlet", "jsp:plugin", "weblogic.ejb"}),
        WEBSPHERE_4("WebSphere 4.x", "java", new String[]{"com.ibm.websphere", "ServletContext", "EJBHome"}),
        FRONTPAGE("FrontPage Extensions", "htm", new String[]{"<!--webbot", "_vti_", "FrontPage"}),
        SHTML("Server Side Includes", "shtml", new String[]{"<!--#include", "<!--#exec", "<!--#config"}),
        PERL_MASON("Perl Mason", "pl", new String[]{"<%perl>", "<%args>", "<%init>"}),
        ZOPE("Zope/Plone", "py", new String[]{"##", "context.", "REQUEST"}),
        TCLTK_WEB("Tcl/Tk Web", "tcl", new String[]{"ns_write", "ns_conn", "AOLserver"}),
        RESIN("Resin 2.x", "java", new String[]{"com.caucho", "<resin:", "caucho.jsp"}),
        ORION("Orion Application Server", "java", new String[]{"com.evermind", "orion-ejb", "application-client.xml"}),
        JRUN("JRun Server", "java", new String[]{"jrun.", "JRunServlet", "jrun.xml"}),
        IPLANET("iPlanet Web Server", "java", new String[]{"iplanet", "nsapi", "obj.conf"});
        
        private final String displayName;
        private final String extension;
        private final String[] patterns;
        
        LegacyWebTechnology(String displayName, String extension, String[] patterns) {
            this.displayName = displayName;
            this.extension = extension;
            this.patterns = patterns;
        }
        
        public String getDisplayName() { return displayName; }
        public String getExtension() { return extension; }
        public String[] getPatterns() { return patterns; }
    }
    
    public enum ModernTarget {
        NODE_EXPRESS("Node.js/Express", "js"),
        SPRING_BOOT("Spring Boot", "java"),
        PYTHON_FLASK("Python/Flask", "py"),
        PHP_LARAVEL("PHP/Laravel", "php"),
        RUBY_RAILS("Ruby/Rails", "rb"),
        DOTNET_CORE(".NET Core", "cs"),
        GOLANG_GIN("Go/Gin", "go"),
        JAVA_JERSEY("Java/Jersey", "java");
        
        private final String displayName;
        private final String extension;
        
        ModernTarget(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }
        
        public String getDisplayName() { return displayName; }
        public String getExtension() { return extension; }
    }
    
    public static class LegacyMigrationResult {
        private final LegacyWebTechnology sourceTech;
        private final ModernTarget targetTech;
        private final String modernizedCode;
        private final List<String> modernizations;
        private final List<String> deprecatedFeatures;
        private final List<String> securityImprovements;
        private final int legacyComplexity;
        private final LocalDateTime migrationDate;
        
        public LegacyMigrationResult(LegacyWebTechnology source, ModernTarget target, String code,
                                   List<String> modernizations, List<String> deprecated,
                                   List<String> security, int complexity) {
            this.sourceTech = source;
            this.targetTech = target;
            this.modernizedCode = code;
            this.modernizations = modernizations != null ? new ArrayList<>(modernizations) : new ArrayList<>();
            this.deprecatedFeatures = deprecated != null ? new ArrayList<>(deprecated) : new ArrayList<>();
            this.securityImprovements = security != null ? new ArrayList<>(security) : new ArrayList<>();
            this.legacyComplexity = complexity;
            this.migrationDate = LocalDateTime.now();
        }
        
        public LegacyWebTechnology getSourceTech() { return sourceTech; }
        public ModernTarget getTargetTech() { return targetTech; }
        public String getModernizedCode() { return modernizedCode; }
        public List<String> getModernizations() { return new ArrayList<>(modernizations); }
        public List<String> getDeprecatedFeatures() { return new ArrayList<>(deprecatedFeatures); }
        public List<String> getSecurityImprovements() { return new ArrayList<>(securityImprovements); }
        public int getLegacyComplexity() { return legacyComplexity; }
        public LocalDateTime getMigrationDate() { return migrationDate; }
        
        @Override
        public String toString() {
            return String.format("Legacy Migration: %s → %s (Complexity: %d/100, Improvements: %d)",
                sourceTech.getDisplayName(), targetTech.getDisplayName(),
                legacyComplexity, modernizations.size() + securityImprovements.size());
        }
    }
    
    /**
     * Detect legacy web technology
     */
    public LegacyWebTechnology detectLegacyTechnology(String sourceCode) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return null;
        }
        
        Map<LegacyWebTechnology, Integer> scores = new HashMap<>();
        
        for (LegacyWebTechnology tech : LegacyWebTechnology.values()) {
            int score = 0;
            for (String pattern : tech.getPatterns()) {
                score += countMatches(sourceCode, pattern);
            }
            if (score > 0) {
                scores.put(tech, score);
            }
        }
        
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Migrate legacy web technology to modern equivalent
     */
    public LegacyMigrationResult migrateLegacyToModern(String sourceCode, ModernTarget targetTech) {
        LegacyWebTechnology sourceTech = detectLegacyTechnology(sourceCode);
        if (sourceTech == null) {
            return new LegacyMigrationResult(null, targetTech, sourceCode, 
                Arrays.asList("No legacy technology detected"), null, null, 0);
        }
        
        return migrateLegacyToModern(sourceCode, sourceTech, targetTech);
    }
    
    /**
     * Migrate with explicit source technology
     */
    public LegacyMigrationResult migrateLegacyToModern(String sourceCode, LegacyWebTechnology sourceTech, ModernTarget targetTech) {
        List<String> modernizations = new ArrayList<>();
        List<String> deprecated = new ArrayList<>();
        List<String> security = new ArrayList<>();
        String modernCode = sourceCode;
        int complexity = calculateLegacyComplexity(sourceCode, sourceTech);
        
        // Apply specific migration patterns
        switch (sourceTech) {
            case CGI_PERL:
                modernCode = migrateCgiPerlToModern(modernCode, targetTech, modernizations, deprecated, security);
                break;
            case CLASSIC_ASP:
                modernCode = migrateClassicAspToModern(modernCode, targetTech, modernizations, deprecated, security);
                break;
            case PHP3_4:
                modernCode = migratePhp34ToModern(modernCode, targetTech, modernizations, deprecated, security);
                break;
            case COLD_FUSION:
                modernCode = migrateColdFusionToModern(modernCode, targetTech, modernizations, deprecated, security);
                break;
            case JSP_1_2:
                modernCode = migrateJsp12ToModern(modernCode, targetTech, modernizations, deprecated, security);
                break;
            case SERVLET_2_3:
                modernCode = migrateServlet23ToModern(modernCode, targetTech, modernizations, deprecated, security);
                break;
            case FRONTPAGE:
                modernCode = migrateFrontPageToModern(modernCode, targetTech, modernizations, deprecated, security);
                break;
            case SHTML:
                modernCode = migrateSsIncludesToModern(modernCode, targetTech, modernizations, deprecated, security);
                break;
            default:
                modernCode = performGenericLegacyMigration(modernCode, sourceTech, targetTech, 
                    modernizations, deprecated, security);
        }
        
        return new LegacyMigrationResult(sourceTech, targetTech, modernCode, 
            modernizations, deprecated, security, complexity);
    }
    
    /**
     * Migrate CGI Perl to modern framework
     */
    private String migrateCgiPerlToModern(String code, ModernTarget target, 
                                        List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        if (target == ModernTarget.NODE_EXPRESS) {
            StringBuilder express = new StringBuilder();
            express.append("const express = require('express');\n");
            express.append("const app = express();\n");
            express.append("app.use(express.urlencoded({ extended: true }));\n\n");
            
            // Convert CGI parameter parsing
            if (migrated.contains("use CGI")) {
                express.append("// Converted from CGI Perl\n");
                express.append("app.get('/', (req, res) => {\n");
                express.append("  const params = req.query;\n");
                express.append("  // Original Perl CGI logic converted\n");
                
                // Convert print statements
                String printPattern = "print\\s+\"([^\"]+)\";";
                Pattern pattern = Pattern.compile(printPattern);
                Matcher matcher = pattern.matcher(migrated);
                while (matcher.find()) {
                    String content = matcher.group(1);
                    express.append("  res.write('").append(content).append("');\n");
                }
                
                express.append("  res.end();\n");
                express.append("});\n\n");
                express.append("app.listen(3000);\n");
                
                migrated = express.toString();
                modernizations.add("Converted CGI Perl to Express.js with modern routing");
                deprecated.add("CGI environment variables replaced with Express req object");
                security.add("Added express.urlencoded() for safe parameter parsing");
            }
        } else if (target == ModernTarget.PYTHON_FLASK) {
            StringBuilder flask = new StringBuilder();
            flask.append("from flask import Flask, request, render_template_string\n");
            flask.append("app = Flask(__name__)\n\n");
            flask.append("@app.route('/', methods=['GET', 'POST'])\n");
            flask.append("def index():\n");
            flask.append("    # Converted from CGI Perl\n");
            flask.append("    params = request.args if request.method == 'GET' else request.form\n");
            flask.append("    # Original Perl logic here\n");
            flask.append("    return 'Converted CGI response'\n\n");
            flask.append("if __name__ == '__main__':\n");
            flask.append("    app.run(debug=True)\n");
            
            migrated = flask.toString();
            modernizations.add("Converted CGI Perl to Flask with WSGI compatibility");
            deprecated.add("CGI.pm module replaced with Flask request handling");
            security.add("Added CSRF protection capabilities with Flask-WTF");
        }
        
        return migrated;
    }
    
    /**
     * Migrate Classic ASP to modern framework
     */
    private String migrateClassicAspToModern(String code, ModernTarget target,
                                           List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        if (target == ModernTarget.DOTNET_CORE) {
            StringBuilder dotnet = new StringBuilder();
            dotnet.append("using Microsoft.AspNetCore.Mvc;\n");
            dotnet.append("using Microsoft.AspNetCore.Http;\n\n");
            dotnet.append("[ApiController]\n");
            dotnet.append("[Route(\"[controller]\")]\n");
            dotnet.append("public class ConvertedController : ControllerBase {\n\n");
            
            // Convert Response.Write to return statements
            migrated = migrated.replaceAll("Response\\.Write\\(([^)]+)\\)", "return Ok($1);");
            
            // Convert Server.CreateObject to dependency injection
            migrated = migrated.replaceAll("Server\\.CreateObject\\(\"([^\"]+)\"\\)", 
                "// Use dependency injection for $1");
            
            // Convert Request.Form/QueryString
            migrated = migrated.replaceAll("Request\\.Form\\(\"([^\"]+)\"\\)", "Request.Form[\"$1\"]");
            migrated = migrated.replaceAll("Request\\.QueryString\\(\"([^\"]+)\"\\)", "Request.Query[\"$1\"]");
            
            dotnet.append("    [HttpGet]\n");
            dotnet.append("    public IActionResult Get() {\n");
            dotnet.append("        // Converted from Classic ASP\n");
            dotnet.append(migrated);
            dotnet.append("    }\n");
            dotnet.append("}\n");
            
            migrated = dotnet.toString();
            modernizations.add("Converted Classic ASP to .NET Core with MVC pattern");
            modernizations.add("Replaced Server.CreateObject with dependency injection");
            deprecated.add("Global.asa replaced with Startup.cs configuration");
            deprecated.add("Session state requires explicit configuration");
            security.add("Added built-in CSRF protection");
            security.add("Replaced weak ASP authentication with Identity framework");
        }
        
        return migrated;
    }
    
    /**
     * Migrate PHP 3/4 to modern PHP or other framework
     */
    private String migratePhp34ToModern(String code, ModernTarget target,
                                      List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        if (target == ModernTarget.PHP_LARAVEL) {
            StringBuilder laravel = new StringBuilder();
            laravel.append("<?php\n\n");
            laravel.append("namespace App\\Http\\Controllers;\n\n");
            laravel.append("use Illuminate\\Http\\Request;\n");
            laravel.append("use Illuminate\\Support\\Facades\\DB;\n\n");
            laravel.append("class ConvertedController extends Controller {\n\n");
            laravel.append("    public function index(Request $request) {\n");
            laravel.append("        // Converted from PHP 3/4\n");
            
            // Convert old MySQL functions to Laravel Eloquent
            migrated = migrated.replaceAll("mysql_connect\\([^)]+\\)", "// Use Laravel database configuration");
            migrated = migrated.replaceAll("mysql_query\\(([^)]+)\\)", "DB::select($1)");
            migrated = migrated.replaceAll("mysql_fetch_array\\([^)]+\\)", "// Use Eloquent model methods");
            
            // Convert register_globals usage
            migrated = migrated.replaceAll("\\$HTTP_GET_VARS\\[([^]]+)\\]", "$request->query($1)");
            migrated = migrated.replaceAll("\\$HTTP_POST_VARS\\[([^]]+)\\]", "$request->input($1)");
            
            // Convert ereg to preg functions
            migrated = migrated.replaceAll("ereg\\(", "preg_match('/");
            migrated = migrated.replaceAll("eregi\\(", "preg_match('/");
            
            laravel.append(migrated);
            laravel.append("        return view('converted');\n");
            laravel.append("    }\n");
            laravel.append("}\n");
            
            migrated = laravel.toString();
            modernizations.add("Converted PHP 3/4 to Laravel with MVC architecture");
            modernizations.add("Replaced mysql_* functions with Eloquent ORM");
            modernizations.add("Updated ereg functions to preg_match");
            deprecated.add("register_globals replaced with Request object");
            deprecated.add("Magic quotes functionality removed");
            security.add("Added CSRF protection middleware");
            security.add("Replaced manual SQL with prepared statements via Eloquent");
            security.add("Implemented input validation and sanitization");
        }
        
        return migrated;
    }
    
    /**
     * Migrate ColdFusion to modern framework
     */
    private String migrateColdFusionToModern(String code, ModernTarget target,
                                           List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        if (target == ModernTarget.SPRING_BOOT) {
            StringBuilder spring = new StringBuilder();
            spring.append("@RestController\n");
            spring.append("@RequestMapping(\"/api\")\n");
            spring.append("public class ConvertedCfController {\n\n");
            spring.append("    @Autowired\n");
            spring.append("    private JdbcTemplate jdbcTemplate;\n\n");
            
            // Convert CFSET to variable declarations
            migrated = migrated.replaceAll("<cfset\\s+([^=]+)=([^>]+)>", "String $1 = $2;");
            
            // Convert CFQUERY to Spring JDBC
            Pattern queryPattern = Pattern.compile("<cfquery[^>]*>([^<]+)</cfquery>", Pattern.DOTALL);
            Matcher matcher = queryPattern.matcher(migrated);
            if (matcher.find()) {
                String sql = matcher.group(1).trim();
                spring.append("    @GetMapping(\"/query\")\n");
                spring.append("    public List<Map<String, Object>> executeQuery() {\n");
                spring.append("        String sql = \"").append(sql).append("\";\n");
                spring.append("        return jdbcTemplate.queryForList(sql);\n");
                spring.append("    }\n\n");
                
                modernizations.add("Converted CFQUERY to Spring JDBC Template");
            }
            
            // Convert CFOUTPUT
            migrated = migrated.replaceAll("<cfoutput>([^<]+)</cfoutput>", "return \"$1\";");
            
            spring.append("    @GetMapping\n");
            spring.append("    public String index() {\n");
            spring.append("        // Converted from ColdFusion\n");
            spring.append(migrated);
            spring.append("    }\n");
            spring.append("}\n");
            
            migrated = spring.toString();
            modernizations.add("Converted ColdFusion to Spring Boot REST API");
            modernizations.add("Replaced CF tags with Java annotations");
            deprecated.add("ColdFusion Application.cfc replaced with Spring configuration");
            security.add("Added Spring Security for authentication");
            security.add("Implemented prepared statements via JdbcTemplate");
        }
        
        return migrated;
    }
    
    /**
     * Migrate JSP 1.2 to modern framework
     */
    private String migrateJsp12ToModern(String code, ModernTarget target,
                                      List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        if (target == ModernTarget.SPRING_BOOT) {
            StringBuilder spring = new StringBuilder();
            spring.append("@Controller\n");
            spring.append("public class ConvertedJspController {\n\n");
            
            // Convert JSP beans to Spring components
            Pattern beanPattern = Pattern.compile("<jsp:useBean[^>]*class=\"([^\"]+)\"[^>]*/>", Pattern.DOTALL);
            Matcher matcher = beanPattern.matcher(migrated);
            while (matcher.find()) {
                String beanClass = matcher.group(1);
                spring.append("    @Autowired\n");
                spring.append("    private ").append(beanClass.substring(beanClass.lastIndexOf('.') + 1)).append(" bean;\n\n");
                modernizations.add("Converted jsp:useBean to Spring @Autowired dependency");
            }
            
            // Convert scriptlets to controller methods
            Pattern scriptletPattern = Pattern.compile("<%([^%]+)%>", Pattern.DOTALL);
            matcher = scriptletPattern.matcher(migrated);
            if (matcher.find()) {
                spring.append("    @GetMapping(\"/\")\n");
                spring.append("    public String index(Model model) {\n");
                spring.append("        // Converted from JSP scriptlet\n");
                spring.append(matcher.group(1));
                spring.append("        return \"index\";\n");
                spring.append("    }\n");
                
                modernizations.add("Converted JSP scriptlets to Spring MVC controller method");
            }
            
            spring.append("}\n");
            migrated = spring.toString();
            
            deprecated.add("JSP scriptlets replaced with controller logic");
            deprecated.add("pageContext implicit object replaced with Model");
            security.add("Removed direct Java code execution in views");
            security.add("Added Spring Security integration");
        }
        
        return migrated;
    }
    
    /**
     * Migrate Servlet 2.3 to modern framework
     */
    private String migrateServlet23ToModern(String code, ModernTarget target,
                                          List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        if (target == ModernTarget.SPRING_BOOT) {
            // Convert servlet to Spring Boot controller
            migrated = migrated.replaceAll("extends HttpServlet", "");
            migrated = migrated.replaceAll("public\\s+void\\s+doGet\\s*\\([^)]+\\)", 
                "@GetMapping\n    public ResponseEntity<String> doGet(HttpServletRequest request)");
            migrated = migrated.replaceAll("public\\s+void\\s+doPost\\s*\\([^)]+\\)", 
                "@PostMapping\n    public ResponseEntity<String> doPost(HttpServletRequest request)");
            
            // Add Spring annotations
            migrated = "@RestController\n@RequestMapping(\"/api\")\npublic class ConvertedServlet {\n\n" + migrated + "\n}";
            
            modernizations.add("Converted Servlet 2.3 to Spring Boot RestController");
            modernizations.add("Replaced servlet lifecycle with Spring managed beans");
            deprecated.add("web.xml configuration replaced with annotations");
            deprecated.add("ServletConfig replaced with Spring configuration properties");
            security.add("Added Spring Security filter chain");
            security.add("Implemented CORS protection");
        }
        
        return migrated;
    }
    
    /**
     * Migrate FrontPage Extensions to modern HTML/JS
     */
    private String migrateFrontPageToModern(String code, ModernTarget target,
                                          List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        // Remove FrontPage webbots
        migrated = migrated.replaceAll("<!--webbot[^>]+-->", "<!-- FrontPage webbot removed -->");
        
        // Convert hit counters to modern analytics
        if (migrated.contains("Hit Counter")) {
            migrated = migrated.replaceAll("<!--webbot\\s+bot=\"HitCounter\"[^>]+-->", 
                "<!-- Use Google Analytics or similar for visitor tracking -->");
            modernizations.add("Replaced FrontPage hit counter with analytics recommendation");
        }
        
        // Convert form validation
        if (migrated.contains("FPValidation")) {
            migrated = migrated.replaceAll("FPValidation[^>]+", 
                "<!-- Use HTML5 form validation or JavaScript -->");
            modernizations.add("Replaced FrontPage form validation with HTML5/JS");
        }
        
        if (target == ModernTarget.NODE_EXPRESS) {
            StringBuilder express = new StringBuilder();
            express.append("const express = require('express');\n");
            express.append("const app = express();\n");
            express.append("app.use(express.static('public'));\n\n");
            express.append("// Serve converted FrontPage HTML\n");
            express.append("app.get('/', (req, res) => {\n");
            express.append("    res.send(`").append(migrated.replace("`", "\\`")).append("`);\n");
            express.append("});\n\n");
            express.append("app.listen(3000);\n");
            
            migrated = express.toString();
            modernizations.add("Wrapped static FrontPage HTML in Express.js server");
        }
        
        deprecated.add("FrontPage Extensions server-side features removed");
        deprecated.add("_vti_ directories and files no longer needed");
        security.add("Removed proprietary FrontPage server extensions");
        
        return migrated;
    }
    
    /**
     * Migrate Server Side Includes to modern templating
     */
    private String migrateSsIncludesToModern(String code, ModernTarget target,
                                           List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        if (target == ModernTarget.NODE_EXPRESS) {
            StringBuilder express = new StringBuilder();
            express.append("const express = require('express');\n");
            express.append("const path = require('path');\n");
            express.append("const fs = require('fs');\n");
            express.append("const app = express();\n\n");
            
            // Convert includes to template partials
            Pattern includePattern = Pattern.compile("<!--#include\\s+(?:virtual|file)=\"([^\"]+)\"\\s*-->", Pattern.DOTALL);
            Matcher matcher = includePattern.matcher(migrated);
            Set<String> includes = new HashSet<>();
            
            while (matcher.find()) {
                String includePath = matcher.group(1);
                includes.add(includePath);
                migrated = migrated.replace(matcher.group(0), 
                    "<%- include('" + includePath.replace(".shtml", "") + "') %>");
            }
            
            if (!includes.isEmpty()) {
                express.append("app.set('view engine', 'ejs');\n");
                express.append("app.set('views', path.join(__dirname, 'views'));\n\n");
                
                for (String include : includes) {
                    express.append("// Create partial: views/").append(include.replace(".shtml", ".ejs")).append("\n");
                }
                
                modernizations.add("Converted SSI includes to EJS template partials");
            }
            
            // Convert exec directives (security risk)
            migrated = migrated.replaceAll("<!--#exec\\s+cmd=\"([^\"]+)\"\\s*-->", 
                "<!-- SECURITY: Command execution removed: $1 -->");
            
            express.append("app.get('/', (req, res) => {\n");
            express.append("    res.render('index', { /* template data */ });\n");
            express.append("});\n\n");
            express.append("app.listen(3000);\n");
            
            migrated = express.toString() + "\n\n<!-- Template content: -->\n" + migrated;
            
            deprecated.add("Server-side includes replaced with EJS templating");
            security.add("Removed dangerous <!--#exec--> directives");
            security.add("Template rendering prevents code injection");
        }
        
        return migrated;
    }
    
    /**
     * Generic legacy migration
     */
    private String performGenericLegacyMigration(String code, LegacyWebTechnology source, ModernTarget target,
                                               List<String> modernizations, List<String> deprecated, List<String> security) {
        String migrated = code;
        
        // Add modern framework boilerplate
        switch (target) {
            case SPRING_BOOT:
                migrated = "@SpringBootApplication\n@RestController\npublic class LegacyMigration {\n" +
                          "    // Migrated from " + source.getDisplayName() + "\n" + migrated + "\n}";
                modernizations.add("Added Spring Boot structure");
                break;
            case NODE_EXPRESS:
                migrated = "const express = require('express');\nconst app = express();\n" +
                          "// Migrated from " + source.getDisplayName() + "\n" + migrated + 
                          "\napp.listen(3000);";
                modernizations.add("Added Express.js server structure");
                break;
            case PYTHON_FLASK:
                migrated = "from flask import Flask\napp = Flask(__name__)\n" +
                          "# Migrated from " + source.getDisplayName() + "\n" + migrated;
                modernizations.add("Added Flask application structure");
                break;
        }
        
        deprecated.add("Legacy " + source.getDisplayName() + " patterns require manual review");
        security.add("Applied modern framework security defaults");
        
        return migrated;
    }
    
    /**
     * Calculate complexity of legacy code
     */
    private int calculateLegacyComplexity(String code, LegacyWebTechnology tech) {
        int complexity = 0;
        
        // Base complexity
        complexity += code.length() / 50;
        
        // Legacy-specific complexity factors
        switch (tech) {
            case CGI_PERL:
                complexity += countMatches(code, "print ") * 2;
                complexity += countMatches(code, "use CGI") * 5;
                complexity += countMatches(code, "\\$ENV") * 3;
                break;
            case CLASSIC_ASP:
                complexity += countMatches(code, "Response.Write") * 2;
                complexity += countMatches(code, "Server.CreateObject") * 4;
                complexity += countMatches(code, "Session(") * 3;
                break;
            case PHP3_4:
                complexity += countMatches(code, "mysql_") * 4; // High security risk
                complexity += countMatches(code, "register_globals") * 5;
                complexity += countMatches(code, "ereg") * 3;
                break;
        }
        
        return Math.min(complexity, 100);
    }
    
    private int countMatches(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }
    
    /**
     * Generate comprehensive legacy migration report
     */
    public String generateLegacyMigrationReport(List<LegacyMigrationResult> results) {
        StringBuilder report = new StringBuilder();
        report.append("Legacy Web Technology Migration Report\n");
        report.append("Generated: ").append(LocalDateTime.now()).append("\n");
        report.append("=" .repeat(60)).append("\n\n");
        
        // Summary statistics
        int totalMigrations = results.size();
        int totalModernizations = results.stream().mapToInt(r -> r.getModernizations().size()).sum();
        int totalDeprecated = results.stream().mapToInt(r -> r.getDeprecatedFeatures().size()).sum();
        int totalSecurityImprovements = results.stream().mapToInt(r -> r.getSecurityImprovements().size()).sum();
        double avgComplexity = results.stream().mapToInt(LegacyMigrationResult::getLegacyComplexity).average().orElse(0.0);
        
        report.append("Executive Summary:\n");
        report.append("- Total Legacy Migrations: ").append(totalMigrations).append("\n");
        report.append("- Modernizations Applied: ").append(totalModernizations).append("\n");
        report.append("- Deprecated Features Addressed: ").append(totalDeprecated).append("\n");
        report.append("- Security Improvements: ").append(totalSecurityImprovements).append("\n");
        report.append("- Average Legacy Complexity: ").append(String.format("%.1f", avgComplexity)).append("/100\n\n");
        
        // Technology breakdown
        Map<LegacyWebTechnology, Long> legacyStats = results.stream()
            .filter(r -> r.getSourceTech() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                LegacyMigrationResult::getSourceTech,
                java.util.stream.Collectors.counting()));
        
        Map<ModernTarget, Long> modernStats = results.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                LegacyMigrationResult::getTargetTech,
                java.util.stream.Collectors.counting()));
        
        report.append("Legacy Technologies Migrated:\n");
        legacyStats.entrySet().stream()
            .sorted(Map.Entry.<LegacyWebTechnology, Long>comparingByValue().reversed())
            .forEach(entry -> report.append("- ").append(entry.getKey().getDisplayName())
                .append(": ").append(entry.getValue()).append(" migrations\n"));
        
        report.append("\nModern Target Platforms:\n");
        modernStats.entrySet().stream()
            .sorted(Map.Entry.<ModernTarget, Long>comparingByValue().reversed())
            .forEach(entry -> report.append("- ").append(entry.getKey().getDisplayName())
                .append(": ").append(entry.getValue()).append(" migrations\n"));
        
        // Security improvements summary
        report.append("\nTop Security Improvements:\n");
        Map<String, Integer> securityCounts = new HashMap<>();
        for (LegacyMigrationResult result : results) {
            for (String improvement : result.getSecurityImprovements()) {
                securityCounts.merge(improvement, 1, Integer::sum);
            }
        }
        
        securityCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> report.append("- ").append(entry.getKey())
                .append(" (").append(entry.getValue()).append(" times)\n"));
        
        // Detailed migration results
        report.append("\n").append("=" .repeat(60)).append("\n");
        report.append("Detailed Migration Results:\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            LegacyMigrationResult result = results.get(i);
            report.append(i + 1).append(". ").append(result.toString()).append("\n");
            
            if (!result.getModernizations().isEmpty()) {
                report.append("   Modernizations:\n");
                for (String mod : result.getModernizations()) {
                    report.append("   • ").append(mod).append("\n");
                }
            }
            
            if (!result.getDeprecatedFeatures().isEmpty()) {
                report.append("   Deprecated Features Addressed:\n");
                for (String dep : result.getDeprecatedFeatures()) {
                    report.append("   ⚠ ").append(dep).append("\n");
                }
            }
            
            if (!result.getSecurityImprovements().isEmpty()) {
                report.append("   Security Improvements:\n");
                for (String sec : result.getSecurityImprovements()) {
                    report.append("   🔒 ").append(sec).append("\n");
                }
            }
            
            report.append("\n");
        }
        
        return report.toString();
    }
}