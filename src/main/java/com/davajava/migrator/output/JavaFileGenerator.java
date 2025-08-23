package com.davajava.migrator.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class JavaFileGenerator {
    
    public void generateFile(Path outputPath, String javaCode, String packageName) throws IOException {
        // Ensure output directory exists
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        
        // Generate complete Java file with package declaration
        StringBuilder completeJavaFile = new StringBuilder();
        
        if (packageName != null && !packageName.isEmpty()) {
            completeJavaFile.append("package ").append(packageName).append(";\n\n");
        }
        
        completeJavaFile.append(javaCode);
        
        // Write file
        Files.writeString(outputPath, completeJavaFile.toString(), 
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}