package com.davajava.migrator;

import com.davajava.migrator.core.MigrationCommand;
import com.davajava.migrator.core.MigrationService;
import com.davajava.migrator.core.SourceLanguage;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            // Simple command-line parsing for basic functionality
            if (args.length < 3) {
                System.out.println("Usage: java -jar davajava-migrator.jar [OPTIONS] <input> <output> <language>");
                System.out.println("Options:");
                System.out.println("  -r, --recursive    Process directories recursively");
                System.out.println("Arguments:");
                System.out.println("  <input>            Input file or directory");
                System.out.println("  <output>           Output directory");
                System.out.println("  <language>         Source language (rust, python)");
                System.out.println("");
                System.out.println("Examples:");
                System.out.println("  java -jar davajava-migrator.jar file.rs output rust");
                System.out.println("  java -jar davajava-migrator.jar -r src_folder output rust");
                return;
            }

            // Parse arguments
            boolean recursive = false;
            int argIndex = 0;
            
            // Check for flags
            while (argIndex < args.length && args[argIndex].startsWith("-")) {
                String flag = args[argIndex];
                if ("-r".equals(flag) || "--recursive".equals(flag)) {
                    recursive = true;
                } else {
                    System.err.println("Unknown option: " + flag);
                    System.exit(1);
                }
                argIndex++;
            }
            
            // Need at least 3 more arguments after flags
            if (argIndex + 3 > args.length) {
                System.err.println("Missing required arguments");
                System.exit(1);
            }

            String inputPath = args[argIndex];
            String outputPath = args[argIndex + 1];
            String languageStr = args[argIndex + 2].toLowerCase();

            SourceLanguage language;
            switch (languageStr) {
                case "rust":
                    language = SourceLanguage.RUST;
                    break;
                case "python":
                    language = SourceLanguage.PYTHON;
                    break;
                default:
                    System.err.println("Unsupported language: " + languageStr);
                    System.err.println("Supported languages: rust, python");
                    System.exit(1);
                    return;
            }

            MigrationCommand command = new MigrationCommand(inputPath, outputPath, language, recursive, "com.migrated", true, false);
            MigrationService migrationService = new MigrationService();
            
            logger.info("Starting DavaJava migration...");
            migrationService.migrate(command);
            logger.info("Migration completed successfully!");
            
        } catch (Exception e) {
            logger.severe("Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}