package com.polytype.migrator.cli;

import com.polytype.migrator.core.SourceLanguage;
import com.polytype.migrator.core.TargetLanguage;
import org.apache.commons.cli.*;

public class CommandLineInterface {
    private final Options options;

    public CommandLineInterface() {
        this.options = createOptions();
    }

    private Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show help message")
                .build());

        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArg()
                .argName("file/directory")
                .desc("Input file or directory to migrate")
                .required()
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .argName("directory")
                .desc("Output directory for generated Java files")
                .required()
                .build());

        options.addOption(Option.builder("l")
                .longOpt("language")
                .hasArg()
                .argName("language")
                .desc("Source language (rust, crystal, c, cpp, python, csharp, kotlin, scala, javascript, go, swift)")
                .build());
        
        options.addOption(Option.builder("t")
                .longOpt("target")
                .hasArg()
                .argName("target")
                .desc("Target language (java, cpp, python, javascript, typescript, csharp, go, rust, kotlin, swift)")
                .build());

        options.addOption(Option.builder("p")
                .longOpt("package")
                .hasArg()
                .argName("package")
                .desc("Package/namespace name for generated classes")
                .build());

        options.addOption(Option.builder("c")
                .longOpt("config")
                .hasArg()
                .argName("file")
                .desc("Configuration file path")
                .build());

        options.addOption(Option.builder("v")
                .longOpt("verbose")
                .desc("Enable verbose logging")
                .build());

        options.addOption(Option.builder("r")
                .longOpt("recursive")
                .desc("Process directories recursively")
                .build());

        options.addOption(Option.builder()
                .longOpt("preserve-comments")
                .desc("Preserve original comments")
                .build());

        options.addOption(Option.builder()
                .longOpt("generate-javadoc")
                .desc("Generate JavaDoc comments")
                .build());
        
        // Android-specific options
        options.addOption(Option.builder()
                .longOpt("apk-decompile")
                .desc("Decompile APK file to structured source code")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("android-to-web")
                .desc("Convert Android app to web application")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("android-project")
                .desc("Parse Android Studio project")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("framework")
                .hasArg()
                .argName("framework")
                .desc("Target web framework (react, vue, angular, vanilla)")
                .build());

        return options;
    }

    public MigrationCommand parseArguments(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                printHelp();
                return null;
            }

            MigrationCommand command = new MigrationCommand();
            command.setInputPath(cmd.getOptionValue("input"));
            command.setOutputPath(cmd.getOptionValue("output"));
            command.setPackageName(cmd.getOptionValue("package", "com.generated"));
            command.setConfigPath(cmd.getOptionValue("config"));
            command.setVerbose(cmd.hasOption("verbose"));
            command.setRecursive(cmd.hasOption("recursive"));
            command.setPreserveComments(cmd.hasOption("preserve-comments"));
            command.setGenerateJavaDoc(cmd.hasOption("generate-javadoc"));

            if (cmd.hasOption("language")) {
                String langStr = cmd.getOptionValue("language").toLowerCase();
                try {
                    SourceLanguage language = parseSourceLanguage(langStr);
                    command.setSourceLanguage(language);
                } catch (IllegalArgumentException e) {
                    throw new ParseException("Unsupported source language: " + langStr);
                }
            }
            
            if (cmd.hasOption("target")) {
                String targetStr = cmd.getOptionValue("target").toLowerCase();
                try {
                    TargetLanguage target = TargetLanguage.fromString(targetStr);
                    command.setTargetLanguage(target);
                } catch (IllegalArgumentException e) {
                    throw new ParseException("Unsupported target language: " + targetStr);
                }
            }
            
            // Android-specific options
            command.setApkDecompile(cmd.hasOption("apk-decompile"));
            command.setAndroidToWeb(cmd.hasOption("android-to-web"));
            command.setAndroidProject(cmd.hasOption("android-project"));
            command.setWebFramework(cmd.getOptionValue("framework", "react"));

            return command;

        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            printHelp();
            throw e;
        }
    }

    private SourceLanguage parseSourceLanguage(String langStr) {
        switch (langStr) {
            case "rust": return SourceLanguage.RUST;
            case "crystal": return SourceLanguage.CRYSTAL;
            case "c": return SourceLanguage.C;
            case "cpp": case "c++": return SourceLanguage.CPP;
            case "python": case "py": return SourceLanguage.PYTHON;
            case "csharp": case "c#": case "cs": return SourceLanguage.CSHARP;
            case "kotlin": case "kt": return SourceLanguage.KOTLIN;
            case "scala": return SourceLanguage.SCALA;
            case "javascript": case "js": return SourceLanguage.JAVASCRIPT;
            case "typescript": case "ts": return SourceLanguage.TYPESCRIPT;
            case "go": case "golang": return SourceLanguage.GO;
            case "swift": return SourceLanguage.SWIFT;
            default:
                throw new IllegalArgumentException("Unknown source language: " + langStr);
        }
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("polytype-migrator", 
            "PolyType Code Migrator - Translates source code between multiple programming languages",
            options,
            "\nSupported source languages: rust, crystal, c, cpp, python, csharp, kotlin, scala, javascript, typescript, go, swift\n" +
            "Supported target languages: java, cpp, python, javascript, typescript, csharp, go, rust, kotlin, swift\n" +
            "\nExamples:\n" +
            "  polytype-migrator -i myfile.rs -o output/ -l rust -t java\n" +
            "  polytype-migrator -i src/ -o cpp-src/ -l kotlin -t cpp -r -p com.myproject\n" +
            "  polytype-migrator --apk-decompile app.apk -o decompiled/\n" +
            "  polytype-migrator --android-to-web app.apk -o webapp/ --framework react\n");
    }
}