package com.davajava.migrator.core;

import com.davajava.migrator.core.MigrationCommand;
import com.davajava.migrator.core.ast.ASTNode;
import com.davajava.migrator.output.JavaFileGenerator;
import com.davajava.migrator.parser.ParserRegistry;
import com.davajava.migrator.translator.TranslatorRegistry;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MigrationService {
    private static final Logger logger = Logger.getLogger(MigrationService.class.getName());
    
    private final ParserRegistry parserRegistry;
    private final TranslatorRegistry translatorRegistry;
    private final JavaFileGenerator fileGenerator;

    public MigrationService() {
        this.parserRegistry = new ParserRegistry();
        this.translatorRegistry = new TranslatorRegistry();
        this.fileGenerator = new JavaFileGenerator();
    }

    public void migrate(MigrationCommand command) throws MigrationException {
        try {
            logger.info("Starting migration from " + command.getInputPath() + " to " + command.getOutputPath());
            
            List<File> filesToMigrate = collectFiles(command);
            logger.info("Found " + filesToMigrate.size() + " files to migrate");

            TranslationOptions options = createTranslationOptions(command);
            
            for (File file : filesToMigrate) {
                migrateFile(file, command, options);
            }
            
            logger.info("Migration completed successfully");
            
        } catch (Exception e) {
            throw new MigrationException("Migration failed: " + e.getMessage(), e);
        }
    }

    private List<File> collectFiles(MigrationCommand command) throws IOException {
        List<File> files = new ArrayList<>();
        Path inputPath = Paths.get(command.getInputPath());
        
        if (Files.isRegularFile(inputPath)) {
            files.add(inputPath.toFile());
        } else if (Files.isDirectory(inputPath)) {
            if (command.isRecursive()) {
                try (Stream<Path> paths = Files.walk(inputPath)) {
                    paths.filter(Files::isRegularFile)
                         .filter(this::isSupportedFile)
                         .map(Path::toFile)
                         .forEach(files::add);
                }
            } else {
                try (Stream<Path> paths = Files.list(inputPath)) {
                    paths.filter(Files::isRegularFile)
                         .filter(this::isSupportedFile)
                         .map(Path::toFile)
                         .forEach(files::add);
                }
            }
        }
        
        return files;
    }

    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString();
        try {
            SourceLanguage.fromFileExtension(fileName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void migrateFile(File file, MigrationCommand command, TranslationOptions options) 
            throws MigrationException {
        try {
            logger.fine("Migrating file: " + file.getPath());
            
            SourceLanguage language = determineLanguage(file, command);
            Parser parser = parserRegistry.getParser(language);
            Translator translator = translatorRegistry.getTranslator(language);
            
            if (parser == null) {
                throw new MigrationException("No parser available for language: " + language);
            }
            
            if (translator == null) {
                throw new MigrationException("No translator available for language: " + language);
            }

            ASTNode ast = parser.parseFile(file.getPath());
            String javaCode = translator.translate(ast, options);
            
            String outputFileName = generateOutputFileName(file, command.getPackageName());
            Path outputPath = Paths.get(command.getOutputPath(), outputFileName);
            
            fileGenerator.generateFile(outputPath, javaCode, command.getPackageName());
            
            logger.fine("Successfully migrated " + file.getPath() + " to " + outputPath);
            
        } catch (Exception e) {
            throw new MigrationException("Failed to migrate file: " + file.getPath(), e);
        }
    }

    private SourceLanguage determineLanguage(File file, MigrationCommand command) {
        if (command.getSourceLanguage() != null) {
            return command.getSourceLanguage();
        }
        
        return SourceLanguage.fromFileExtension(file.getName());
    }

    private String generateOutputFileName(File inputFile, String packageName) {
        String baseName = inputFile.getName();
        int lastDotIndex = baseName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            baseName = baseName.substring(0, lastDotIndex);
        }
        
        // Convert to Java class naming convention
        String className = toPascalCase(baseName);
        return className + ".java";
    }

    private String toPascalCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(Character.toLowerCase(c));
                }
            } else {
                capitalizeNext = true;
            }
        }
        
        return result.toString();
    }

    private TranslationOptions createTranslationOptions(MigrationCommand command) {
        TranslationOptions options = TranslationOptions.defaultOptions();
        options.setOption("generateComments", command.isPreserveComments());
        options.setOption("generateJavaDoc", command.isGenerateJavaDoc());
        return options;
    }
}