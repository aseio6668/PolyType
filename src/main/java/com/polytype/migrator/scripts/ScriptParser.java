package com.polytype.migrator.scripts;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for parsing different types of script files.
 */
public interface ScriptParser {
    
    /**
     * Parse a script file and extract relevant information.
     * 
     * @param scriptFile Path to the script file
     * @return ScriptAnalysisResult containing parsed information
     * @throws IOException if file cannot be read
     */
    ScriptAnalysisResult parseScript(Path scriptFile) throws IOException;
    
    /**
     * Get the script type this parser handles.
     */
    ScriptAnalyzer.ScriptType getSupportedType();
    
    /**
     * Check if this parser can handle the given file.
     */
    boolean canHandle(Path scriptFile);
}