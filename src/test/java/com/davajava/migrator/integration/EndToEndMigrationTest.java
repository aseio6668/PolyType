package com.davajava.migrator.integration;

import com.davajava.migrator.cli.MigrationCommand;
import com.davajava.migrator.core.MigrationService;
import com.davajava.migrator.core.SourceLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EndToEndMigrationTest {
    
    @TempDir
    Path tempDir;
    
    private MigrationService migrationService;
    
    @BeforeEach
    void setUp() {
        migrationService = new MigrationService();
    }

    @Test
    void testRustFunctionMigration() throws Exception {
        // Create a test Rust file
        Path rustFile = tempDir.resolve("test.rs");
        String rustCode = 
            "pub fn add(a: i32, b: i32) -> i32 {\n" +
            "    a + b\n" +
            "}\n" +
            "\n" +
            "fn multiply(x: f64, y: f64) -> f64 {\n" +
            "    x * y\n" +
            "}";
        
        Files.writeString(rustFile, rustCode);
        
        // Create output directory
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Set up migration command
        MigrationCommand command = new MigrationCommand();
        command.setInputPath(rustFile.toString());
        command.setOutputPath(outputDir.toString());
        command.setSourceLanguage(SourceLanguage.RUST);
        command.setPackageName("com.test");
        
        // Execute migration
        migrationService.migrate(command);
        
        // Verify output
        Path javaFile = outputDir.resolve("Test.java");
        assertTrue(Files.exists(javaFile), "Java file should be created");
        
        String javaCode = Files.readString(javaFile);
        assertTrue(javaCode.contains("package com.test;"));
        assertTrue(javaCode.contains("public int add(int a, int b)"));
        assertTrue(javaCode.contains("private double multiply(double x, double y)"));
        assertTrue(javaCode.contains("Generated from Rust"));
    }

    @Test
    void testRustStructMigration() throws Exception {
        // Create a test Rust struct file
        Path rustFile = tempDir.resolve("person.rs");
        String rustCode = 
            "pub struct Person {\n" +
            "    pub name: String,\n" +
            "    age: i32,\n" +
            "    email: String,\n" +
            "}\n" +
            "\n" +
            "pub fn create_person(name: String, age: i32) -> Person {\n" +
            "    Person { name, age, email: String::new() }\n" +
            "}";
        
        Files.writeString(rustFile, rustCode);
        
        // Create output directory
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Set up migration command
        MigrationCommand command = new MigrationCommand();
        command.setInputPath(rustFile.toString());
        command.setOutputPath(outputDir.toString());
        command.setSourceLanguage(SourceLanguage.RUST);
        command.setPackageName("com.test");
        
        // Execute migration
        migrationService.migrate(command);
        
        // Verify output
        Path javaFile = outputDir.resolve("Person.java");
        assertTrue(Files.exists(javaFile), "Java file should be created");
        
        String javaCode = Files.readString(javaFile);
        assertTrue(javaCode.contains("public class Person"));
        assertTrue(javaCode.contains("public String name;"));
        assertTrue(javaCode.contains("private int age;"));
        assertTrue(javaCode.contains("public Person("));
        assertTrue(javaCode.contains("public String getName()"));
        assertTrue(javaCode.contains("public int createPerson("));
    }

    @Test
    void testPythonFunctionMigration() throws Exception {
        // Create a test Python file
        Path pythonFile = tempDir.resolve("calculator.py");
        String pythonCode = 
            "def calculate_sum(a: int, b: int) -> int:\n" +
            "    return a + b\n" +
            "\n" +
            "def process_list(numbers: list[int]) -> int:\n" +
            "    return sum(numbers)\n" +
            "\n" +
            "class Calculator:\n" +
            "    def add(self, x: int, y: int) -> int:\n" +
            "        return x + y";
        
        Files.writeString(pythonFile, pythonCode);
        
        // Create output directory
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Set up migration command
        MigrationCommand command = new MigrationCommand();
        command.setInputPath(pythonFile.toString());
        command.setOutputPath(outputDir.toString());
        command.setSourceLanguage(SourceLanguage.PYTHON);
        command.setPackageName("com.test");
        
        // Execute migration
        migrationService.migrate(command);
        
        // Verify output
        Path javaFile = outputDir.resolve("Calculator.java");
        assertTrue(Files.exists(javaFile), "Java file should be created");
        
        String javaCode = Files.readString(javaFile);
        assertTrue(javaCode.contains("package com.test;"));
        assertTrue(javaCode.contains("Generated from Python"));
        assertTrue(javaCode.contains("import java.util.*"));
        assertTrue(javaCode.contains("public int calculateSum(int a, int b)"));
        assertTrue(javaCode.contains("public int processList(List<Int> numbers)"));
        assertTrue(javaCode.contains("public class Calculator"));
    }

    @Test
    void testDirectoryMigration() throws Exception {
        // Create multiple source files
        Path sourceDir = tempDir.resolve("src");
        Files.createDirectories(sourceDir);
        
        // Create Rust files
        Path rustFile1 = sourceDir.resolve("math.rs");
        Files.writeString(rustFile1, "pub fn add(a: i32, b: i32) -> i32 { a + b }");
        
        Path rustFile2 = sourceDir.resolve("utils.rs");
        Files.writeString(rustFile2, "fn helper() -> bool { true }");
        
        // Create Python file
        Path pythonFile = sourceDir.resolve("data.py");
        Files.writeString(pythonFile, "def process_data(items: list[str]) -> int:\n    return len(items)");
        
        // Create output directory
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Set up migration command for recursive processing
        MigrationCommand command = new MigrationCommand();
        command.setInputPath(sourceDir.toString());
        command.setOutputPath(outputDir.toString());
        command.setPackageName("com.test");
        command.setRecursive(true);
        
        // Execute migration
        migrationService.migrate(command);
        
        // Verify outputs
        assertTrue(Files.exists(outputDir.resolve("Math.java")));
        assertTrue(Files.exists(outputDir.resolve("Utils.java")));
        assertTrue(Files.exists(outputDir.resolve("Data.java")));
        
        // Check content of one file
        String mathJavaCode = Files.readString(outputDir.resolve("Math.java"));
        assertTrue(mathJavaCode.contains("package com.test;"));
        assertTrue(mathJavaCode.contains("public int add"));
    }
}