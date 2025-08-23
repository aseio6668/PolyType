# DavaJava Usage Guide

## Quick Start

### 1. Build the Project
```bash
# Linux/macOS
./build.sh

# Windows
build.bat
```

### 2. Basic Usage
```bash
# Migrate a single file
java -jar target/code-migrator-1.0.0.jar -i examples/example.rs -o output/ -l rust

# Migrate a directory recursively
java -jar target/code-migrator-1.0.0.jar -i src/ -o java-output/ -r

# Use custom package name
java -jar target/code-migrator-1.0.0.jar -i examples/ -o output/ -p com.mycompany.migrated -r
```

## Supported Languages

### ✅ Rust (Fully Functional)
- **Functions**: Parameter parsing, return types, visibility
- **Structs**: Fields, visibility, constructor generation
- **Type Mapping**: i32→int, String→String, Vec<T>→T[], etc.
- **Features**: Getters/setters generation, proper Java naming

**Example:**
```rust
// Input: example.rs
pub struct Person {
    pub name: String,
    age: i32,
}

pub fn create_person(name: String, age: i32) -> Person {
    Person { name, age }
}
```

```java
// Output: Person.java
package com.generated;

public class Person {
    public String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() { return name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}

public Person createPerson(String name, int age) {
    // TODO: Implement function body
    return null;
}
```

### ✅ Python (Basic Functionality)
- **Functions**: Type annotations, parameter parsing
- **Classes**: Basic class structure
- **Type Mapping**: int→int, list[T]→List<T>, dict→Map, etc.
- **Features**: Import generation, Java collection types

**Example:**
```python
# Input: calculator.py
def calculate_sum(a: int, b: int) -> int:
    return a + b

class Calculator:
    def multiply(self, x: float, y: float) -> float:
        return x * y
```

```java
// Output: Calculator.java
package com.generated;

import java.util.*;
import java.util.stream.*;

public int calculateSum(int a, int b) {
    // TODO: Implement method body from Python
    return 0;
}

public class Calculator {
    // TODO: Add fields and methods from Python class
}
```

### 🔧 Other Languages (Ready for Extension)
- **C/C++**: Stub implementations ready
- **C#**: Stub implementations ready  
- **Crystal**: Stub implementations ready

## Configuration

### Using Configuration Files
Create a `migration-config.json` file:

```json
{
  "general": {
    "default_package": "com.mycompany",
    "preserve_comments": true,
    "generate_javadoc": true,
    "verbose": true
  },
  "translation": {
    "indent_size": 2,
    "generate_constructors": true,
    "generate_getters_setters": true,
    "type_mappings": {
      "rust.i32": "int",
      "python.str": "String"
    }
  },
  "language_specific": {
    "rust": {
      "handle_ownership": true,
      "convert_traits": true
    },
    "python": {
      "generate_imports": true,
      "handle_duck_typing": true
    }
  }
}
```

Use with: `java -jar code-migrator.jar -c migration-config.json -i src/ -o output/`

### Command Line Options
- `-i, --input`: Input file or directory (required)
- `-o, --output`: Output directory (required)
- `-l, --language`: Source language (rust, crystal, c, cpp, python, csharp)
- `-p, --package`: Java package name
- `-c, --config`: Configuration file path
- `-r, --recursive`: Process directories recursively
- `-v, --verbose`: Enable verbose logging
- `--preserve-comments`: Preserve original comments
- `--generate-javadoc`: Generate JavaDoc comments
- `-h, --help`: Show help message

## Examples

### Example 1: Rust Struct Migration
```bash
java -jar code-migrator.jar -i examples/struct_example.rs -o output/ -l rust -p com.example
```

Input (struct_example.rs):
```rust
pub struct Point {
    pub x: f64,
    pub y: f64,
}

pub fn distance(p1: Point, p2: Point) -> f64 {
    let dx = p1.x - p2.x;
    let dy = p1.y - p2.y;
    (dx * dx + dy * dy).sqrt()
}
```

Output (Point.java):
```java
package com.example;

public class Point {
    public double x;
    public double y;
    
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    // Getters and setters...
}

public double distance(Point p1, Point p2) {
    // TODO: Implement function body
    return 0.0;
}
```

### Example 2: Python Function Migration
```bash
java -jar code-migrator.jar -i examples/example.py -o output/ -l python
```

### Example 3: Batch Directory Processing
```bash
java -jar code-migrator.jar -i multi-lang-project/ -o java-output/ -r -v
```

## Architecture Overview

### Core Components
1. **Parsers**: Language-specific code parsing (AST generation)
2. **Translators**: Java code generation from AST
3. **CLI**: Command-line interface and argument processing
4. **Configuration**: JSON-based configuration system
5. **Output**: File generation and package management

### Extension Points
- Add new `Parser` implementations for new languages
- Add new `Translator` implementations for different output formats
- Extend `SourceLanguage` enum for new language support
- Register parsers/translators in respective registries

## Troubleshooting

### Common Issues

1. **"No parser available for language"**
   - Ensure the language is supported
   - Check file extension matches expected format

2. **"Parse error at line X"**
   - Check source code syntax
   - Enable verbose mode with `-v` for detailed error info

3. **Empty output files**
   - Verify input files contain supported constructs
   - Check if parsing completed successfully

### Debug Mode
Enable verbose logging with `-v` flag:
```bash
java -jar code-migrator.jar -i example.rs -o output/ -l rust -v
```

### Logging
The tool uses SLF4J with Logback. Logs are written to console and can be configured via logback configuration.

## Future Enhancements

- Expression parsing for all languages
- Control flow statement translation
- Advanced type inference
- IDE plugin support
- GUI interface
- More comprehensive language support

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your parser/translator
4. Add comprehensive tests
5. Update documentation
6. Submit a pull request

See README.md for detailed contribution guidelines.