package com.polytype.migrator.scripts;

import java.util.*;

/**
 * Recommendations for project structure in the target language.
 */
public class ProjectStructureRecommendation {
    
    private List<String> directories;
    private Map<String, String> files;
    private Map<String, String> descriptions;
    
    public ProjectStructureRecommendation() {
        this.directories = new ArrayList<>();
        this.files = new HashMap<>();
        this.descriptions = new HashMap<>();
    }
    
    public void addDirectory(String path) {
        directories.add(path);
    }
    
    public void addFile(String path, String description) {
        files.put(path, description);
        descriptions.put(path, description);
    }
    
    public List<String> getDirectories() { return directories; }
    public Map<String, String> getFiles() { return files; }
    public Map<String, String> getDescriptions() { return descriptions; }
    
    public String generateMarkdown() {
        StringBuilder md = new StringBuilder();
        md.append("# Project Structure Recommendations\n\n");
        
        md.append("## Recommended Directory Structure\n\n");
        md.append("```\n");
        md.append("project/\n");
        
        for (String dir : directories) {
            md.append("├── ").append(dir).append("/\n");
        }
        
        for (String file : files.keySet()) {
            md.append("├── ").append(file).append("\n");
        }
        
        md.append("```\n\n");
        
        md.append("## File Descriptions\n\n");
        for (Map.Entry<String, String> entry : descriptions.entrySet()) {
            md.append("- **").append(entry.getKey()).append("**: ").append(entry.getValue()).append("\n");
        }
        
        return md.toString();
    }
}