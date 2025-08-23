# DavaJava Code Migrator

A robust, extensible multi-language to Java code migration tool that can translate source code from various programming languages into executable Java code.

## Supported Languages

- **Rust** (.rs) - Basic function parsing and translation
- **Crystal** (.cr) - Stub implementation ready for extension  
- **C** (.c) - Stub implementation ready for extension
- **C++** (.cpp, .cc, .cxx) - Stub implementation ready for extension
- **Python** (.py) - Stub implementation ready for extension
- **C#** (.cs) - Stub implementation ready for extension

## Features

- **Extensible Architecture**: Plugin-based system for adding new language support
- **CLI Interface**: Robust command-line interface with comprehensive options
- **AST-based Translation**: Uses Abstract Syntax Trees for accurate code analysis
- **Configurable Output**: Customizable Java package names and code formatting
- **Recursive Processing**: Can process entire directory trees
- **Type Mapping**: Intelligent type mapping between source languages and Java

## Quick Start

### Prerequisites
- Java JDK 11+ (run `verify-java.bat` to check)

### Building the Project

**Full Build (Recommended):**
```cmd
build.bat
```

**Quick Test (Core Only):**
```cmd
quick-build.bat
```

### Basic Usage

```cmd
# Migrate a single Rust file
java -jar davajava-migrator.jar -i example.rs -o output/ -l rust

# Migrate all files in a directory recursively
java -jar davajava-migrator.jar -i src/ -o java-src/ -r -p com.myproject

# Show help
java -jar davajava-migrator.jar --help
```

### Command Line Options

- `-i, --input`: Input file or directory to migrate (required)
- `-o, --output`: Output directory for generated Java files (required)  
- `-l, --language`: Source language (rust, crystal, c, cpp, python, csharp)
- `-p, --package`: Java package name for generated classes
- `-r, --recursive`: Process directories recursively
- `-v, --verbose`: Enable verbose logging
- `--preserve-comments`: Preserve original comments
- `--generate-javadoc`: Generate JavaDoc comments
- `-c, --config`: Configuration file path

## Architecture

### Core Components

- **Parser Interface**: Extensible parsing system for different languages
- **AST Nodes**: Common Abstract Syntax Tree representation
- **Translator Interface**: Pluggable translation system to Java
- **CLI Framework**: Apache Commons CLI-based command line interface
- **File Generator**: Java source code generation and output management

### Adding New Languages

1. Implement the `Parser` interface for your language
2. Implement the `Translator` interface for Java translation
3. Register your parser and translator in their respective registries
4. Add language enum entry to `SourceLanguage`

Example:
```java
// In ParserRegistry.java
parsers.put(SourceLanguage.YOUR_LANGUAGE, new YourLanguageParser());

// In TranslatorRegistry.java  
translators.put(SourceLanguage.YOUR_LANGUAGE, new YourLanguageToJavaTranslator());
```

## Current Implementation Status

### Rust (Most Complete)
- ✅ Function declaration parsing with parameters and return types
- ✅ Basic type mapping (i32→int, String→String, etc.)
- ✅ Public/private visibility handling
- ✅ Parameter mutation detection
- ⚠️ Basic Java code generation (function stubs)

### Other Languages
- ⚠️ Stub implementations that generate placeholder Java classes
- 🔧 Ready for extension with proper parsing logic

## Example Input/Output

### Rust Input (`example.rs`):
```rust
pub fn calculate_sum(a: i32, b: i32) -> i32 {
    a + b
}

fn private_helper(data: &str) -> bool {
    !data.is_empty()
}
```

### Java Output (`CalculateSum.java`):
```java
package com.generated;

// Generated from Rust source code
// Migrated using DavaJava Code Migrator

public int calculateSum(int a, int b) {
    // TODO: Implement function body
    return 0;
}

private boolean privateHelper(String data) {
    // TODO: Implement function body
    return false;
}
```

## Development

### Project Structure
```
src/main/java/com/davajava/migrator/
├── Main.java                           # Application entry point
├── cli/                               # Command line interface
├── core/                              # Core interfaces and services
│   ├── ast/                          # Abstract Syntax Tree nodes
│   └── MigrationService.java         # Main migration orchestrator
├── parser/                           # Language parsers
│   ├── rust/, crystal/, c/, cpp/, python/, csharp/
│   └── ParserRegistry.java
├── translator/                       # Java translators  
│   ├── rust/, crystal/, c/, cpp/, python/, csharp/
│   └── TranslatorRegistry.java
└── output/                           # Java code generation
```

### Testing

```bash
mvn test
```

### Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your language parser/translator
4. Add comprehensive tests
5. Update documentation
6. Submit a pull request

## Future Enhancements

- Full expression and statement parsing
- Advanced type inference
- Memory management pattern translation
- Object-oriented construct mapping
- Error handling translation
- Standard library mapping
- Configuration file support
- GUI interface
- IDE plugin support

## License

MIT License - See LICENSE file for details.

## Authors

DavaJava Development Team