package com.davajava.migrator.cli;

import com.davajava.migrator.core.SourceLanguage;
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
                .desc("Source language (rust, crystal, c, cpp, python, csharp)")
                .build());

        options.addOption(Option.builder("p")
                .longOpt("package")
                .hasArg()
                .argName("package")
                .desc("Java package name for generated classes")
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
                    throw new ParseException("Unsupported language: " + langStr);
                }
            }

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
            default:
                throw new IllegalArgumentException("Unknown language: " + langStr);
        }
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("davajava-migrator", 
            "Migrates source code from various languages to Java",
            options,
            "\nSupported languages: rust, crystal, c, cpp, python, csharp\n" +
            "Examples:\n" +
            "  davajava-migrator -i myfile.rs -o output/ -l rust\n" +
            "  davajava-migrator -i src/ -o java-src/ -r -p com.myproject\n");
    }
}