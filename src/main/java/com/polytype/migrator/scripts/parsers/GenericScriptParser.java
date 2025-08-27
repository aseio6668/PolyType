package com.polytype.migrator.scripts.parsers;

import com.polytype.migrator.scripts.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic script parser that can handle unknown script types.
 * Provides basic analysis for any script file.
 */
public class GenericScriptParser implements ScriptParser {
    
    private static final Pattern SHEBANG_PATTERN = Pattern.compile("^#!(.+)$");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*[#;](.+)$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(.+)$");
    
    @Override
    public ScriptAnalysisResult parseScript(Path scriptFile) throws IOException {
        ScriptAnalysisResult result = new ScriptAnalysisResult();
        result.setScriptType(ScriptAnalyzer.ScriptType.UNKNOWN);
        result.setFilePath(scriptFile.toString());
        result.setPurpose(ScriptAnalysisResult.ScriptPurpose.UNKNOWN);
        
        List<String> lines = Files.readAllLines(scriptFile);
        
        if (!lines.isEmpty()) {
            // Check shebang line
            String firstLine = lines.get(0);
            Matcher shebangMatcher = SHEBANG_PATTERN.matcher(firstLine);
            if (shebangMatcher.find()) {
                String interpreter = shebangMatcher.group(1);
                result.addMetadata("interpreter", interpreter);
                result.setLanguage(detectLanguageFromShebang(interpreter));
                result.addEmbeddedLanguage(result.getLanguage());
            }
            
            // Basic analysis
            for (String line : lines) {
                String trimmedLine = line.trim();
                
                // Skip empty lines and comments
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith(";")) {
                    continue;
                }
                
                // Look for variable assignments
                Matcher varMatcher = VARIABLE_PATTERN.matcher(trimmedLine);
                if (varMatcher.find()) {
                    String varName = varMatcher.group(1);
                    String varValue = varMatcher.group(2);
                    result.addVariable(varName, varValue);
                }
                
                // Look for common command patterns
                if (trimmedLine.contains("make") || trimmedLine.contains("build")) {
                    result.setPurpose(ScriptAnalysisResult.ScriptPurpose.BUILD);
                } else if (trimmedLine.contains("test")) {
                    result.setPurpose(ScriptAnalysisResult.ScriptPurpose.TESTING);
                } else if (trimmedLine.contains("install") || trimmedLine.contains("setup")) {
                    result.setPurpose(ScriptAnalysisResult.ScriptPurpose.SETUP);
                }
            }
        }
        
        // If language not detected from shebang, try file extension
        if (result.getLanguage() == null) {
            result.setLanguage(detectLanguageFromExtension(scriptFile));
        }
        
        return result;
    }
    
    private String detectLanguageFromShebang(String interpreter) {
        if (interpreter.contains("bash") || interpreter.contains("sh")) {
            return "Shell";
        } else if (interpreter.contains("python")) {
            return "Python";
        } else if (interpreter.contains("node")) {
            return "JavaScript";
        } else if (interpreter.contains("ruby")) {
            return "Ruby";
        } else if (interpreter.contains("perl")) {
            return "Perl";
        }
        return "Unknown";
    }
    
    private String detectLanguageFromExtension(Path scriptFile) {
        String filename = scriptFile.getFileName().toString();
        String extension = "";
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = filename.substring(lastDotIndex);
        }
        
        switch (extension) {
            case ".sh": case ".bash": case ".zsh": return "Shell";
            case ".bat": case ".cmd": return "Batch";
            case ".ps1": return "PowerShell";
            case ".py": return "Python";
            case ".js": return "JavaScript";
            case ".rb": return "Ruby";
            case ".pl": return "Perl";
            case ".php": return "PHP";
            default: return "Unknown";
        }
    }
    
    @Override
    public ScriptAnalyzer.ScriptType getSupportedType() {
        return ScriptAnalyzer.ScriptType.UNKNOWN;
    }
    
    @Override
    public boolean canHandle(Path scriptFile) {
        return true; // Generic parser can handle any file
    }
}

// Placeholder implementations for other parsers referenced in ScriptAnalyzer
class CMakeParser extends CMakeListsParser {
    // Use the full CMakeListsParser implementation
}

class PomXmlParser extends PomXmlParserImpl {
    // Use the full PomXmlParserImpl implementation
}

class GradleParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.BUILD_GRADLE; }
}

class CargoTomlParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.CARGO_TOML; }
}

class SetupPyParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.SETUP_PY; }
}

class ShellScriptParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.SHELL_SCRIPT; }
}

class BatchScriptParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.BATCH_SCRIPT; }
}

class ComposerJsonParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.COMPOSER_JSON; }
}

class GemfileParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.GEMFILE; }
}

class GitHubWorkflowParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.GITHUB_WORKFLOW; }
}

class GitLabCIParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.GITLAB_CI; }
}

class DockerfileParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.DOCKERFILE; }
}

class JenkinsfileParser extends GenericScriptParser {
    @Override public ScriptAnalyzer.ScriptType getSupportedType() { return ScriptAnalyzer.ScriptType.JENKINS_FILE; }
}