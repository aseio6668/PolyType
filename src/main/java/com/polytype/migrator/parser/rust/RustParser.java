package com.polytype.migrator.parser.rust;

import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.parser.base.Parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Rust source code that converts it to AST representation.
 * Supports Rust's ownership system, traits, enums, structs, and modern language features.
 */
public class RustParser implements Parser {
    
    private static final Pattern STRUCT_PATTERN = Pattern.compile(
        "(?:pub\\s+)?(?:#\\[derive\\([^)]+\\)\\]\\s*)?struct\\s+(\\w+)(?:<[^>]*>)?\\s*\\{"
    );
    
    private static final Pattern ENUM_PATTERN = Pattern.compile(
        "(?:pub\\s+)?(?:#\\[derive\\([^)]+\\)\\]\\s*)?enum\\s+(\\w+)(?:<[^>]*>)?\\s*\\{"
    );
    
    private static final Pattern IMPL_PATTERN = Pattern.compile(
        "impl(?:<[^>]*>)?\\s+(?:(\\w+)\\s+for\\s+)?(\\w+)(?:<[^>]*>)?\\s*\\{"
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
        "(?:pub\\s+)?(?:(async)\\s+)?(?:(unsafe)\\s+)?fn\\s+(\\w+)(?:<[^>]*>)?\\s*\\([^)]*\\)(?:\\s*->\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "(?:pub\\s+)?(\\w+):\\s*([^,}]+)(?:,|$)"
    );
    
    private static final Pattern USE_PATTERN = Pattern.compile(
        "use\\s+([^;]+);"
    );
    
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
        "(?:&(?:mut\\s+)?)?(?:mut\\s+)?(\\w+):\\s*([^,)]+)"
    );
    
    private static final Pattern TRAIT_PATTERN = Pattern.compile(
        "(?:pub\\s+)?trait\\s+(\\w+)(?:<[^>]*>)?\\s*(?::\\s*([^{]+))?\\s*\\{"
    );
    
    private static final Pattern MOD_PATTERN = Pattern.compile(
        "(?:pub\\s+)?mod\\s+(\\w+)\\s*(?:\\{|;)"
    );
    
    @Override
    public ProgramNode parse(String filePath) throws IOException {
        ProgramNode program = new ProgramNode(1, 1);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            parseLines(lines, program);
        }
        
        return program;
    }
    
    private void parseLines(List<String> lines, ProgramNode program) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*")) {
                continue;
            }
            
            // Parse use statements (imports)
            Matcher useMatcher = USE_PATTERN.matcher(line);
            if (useMatcher.find()) {
                // Handle imports - could be expanded for dependency tracking
                continue;
            }
            
            // Parse struct declarations
            Matcher structMatcher = STRUCT_PATTERN.matcher(line);
            if (structMatcher.find()) {
                ClassDeclarationNode structNode = parseStruct(lines, i, structMatcher);
                program.addChild(structNode);
                continue;
            }
            
            // Parse enum declarations
            Matcher enumMatcher = ENUM_PATTERN.matcher(line);
            if (enumMatcher.find()) {
                ClassDeclarationNode enumNode = parseEnum(lines, i, enumMatcher);
                program.addChild(enumNode);
                continue;
            }
            
            // Parse trait declarations
            Matcher traitMatcher = TRAIT_PATTERN.matcher(line);
            if (traitMatcher.find()) {
                ClassDeclarationNode traitNode = parseTrait(lines, i, traitMatcher);
                program.addChild(traitNode);
                continue;
            }
            
            // Parse impl blocks
            Matcher implMatcher = IMPL_PATTERN.matcher(line);
            if (implMatcher.find()) {
                parseImplBlock(lines, i, implMatcher, program);
                continue;
            }
            
            // Parse standalone functions
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            if (functionMatcher.find()) {
                FunctionDeclarationNode functionNode = parseFunction(lines, i, functionMatcher);
                program.addChild(functionNode);
                continue;
            }
        }
    }
    
    private ClassDeclarationNode parseStruct(List<String> lines, int startIndex, Matcher structMatcher) {
        String structName = structMatcher.group(1);
        ClassDeclarationNode structNode = new ClassDeclarationNode(structName, false, startIndex + 1, 1);
        
        // Find struct body
        int braceCount = 0;
        boolean inStruct = false;
        
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    inStruct = true;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && inStruct) {
                        return structNode; // End of struct
                    }
                }
            }
            
            if (inStruct && braceCount == 1) {
                // Parse struct fields
                parseStructField(line, structNode, i + 1);
            }
        }
        
        return structNode;
    }
    
    private void parseStructField(String line, ClassDeclarationNode structNode, int lineNumber) {
        Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
        if (fieldMatcher.find()) {
            String fieldName = fieldMatcher.group(1);
            String fieldType = fieldMatcher.group(2).trim();
            
            // Convert Rust types to more generic types
            String mappedType = mapRustType(fieldType);
            
            VariableDeclarationNode field = new VariableDeclarationNode(
                fieldName, mappedType, false, null, lineNumber, 1);
            
            structNode.addChild(field);
        }
    }
    
    private ClassDeclarationNode parseEnum(List<String> lines, int startIndex, Matcher enumMatcher) {
        String enumName = enumMatcher.group(1);
        ClassDeclarationNode enumNode = new ClassDeclarationNode(enumName, false, startIndex + 1, 1);
        enumNode.setEnum(true);
        
        // Parse enum variants - simplified for now
        return enumNode;
    }
    
    private ClassDeclarationNode parseTrait(List<String> lines, int startIndex, Matcher traitMatcher) {
        String traitName = traitMatcher.group(1);
        ClassDeclarationNode traitNode = new ClassDeclarationNode(traitName, false, startIndex + 1, 1);
        traitNode.setInterface(true);
        
        // Parse trait methods - simplified for now
        return traitNode;
    }
    
    private void parseImplBlock(List<String> lines, int startIndex, Matcher implMatcher, ProgramNode program) {
        String traitName = implMatcher.group(1); // trait name if impl Trait for Type
        String typeName = implMatcher.group(2);   // type name
        
        // Find implementation methods
        int braceCount = 0;
        boolean inImpl = false;
        
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    inImpl = true;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && inImpl) {
                        return; // End of impl block
                    }
                }
            }
            
            if (inImpl && braceCount == 1) {
                // Parse methods in impl block
                Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
                if (functionMatcher.find()) {
                    FunctionDeclarationNode method = parseFunction(lines, i, functionMatcher);
                    method.setDeclaringClass(typeName);
                    
                    // Add method to the program or find corresponding class
                    // For simplicity, adding to program - could be enhanced to find target class
                    program.addChild(method);
                }
            }
        }
    }
    
    private FunctionDeclarationNode parseFunction(List<String> lines, int startIndex, Matcher functionMatcher) {
        String asyncKeyword = functionMatcher.group(1);
        String unsafeKeyword = functionMatcher.group(2);
        String functionName = functionMatcher.group(3);
        String returnType = functionMatcher.group(4);
        
        boolean isAsync = asyncKeyword != null;
        boolean isUnsafe = unsafeKeyword != null;
        
        if (returnType == null || returnType.trim().isEmpty()) {
            returnType = "()"; // Unit type in Rust
        } else {
            returnType = returnType.trim();
        }
        
        String mappedReturnType = mapRustType(returnType);
        List<ParameterNode> parameters = parseParameters(lines.get(startIndex));
        
        FunctionDeclarationNode function = new FunctionDeclarationNode(
            functionName, mappedReturnType, parameters, false, false, startIndex + 1, 1);
        function.setAsync(isAsync);
        // Could add unsafe flag as custom property
        
        return function;
    }
    
    private List<ParameterNode> parseParameters(String line) {
        List<ParameterNode> parameters = new ArrayList<>();
        
        // Extract parameter list from parentheses
        int start = line.indexOf('(');
        int end = line.lastIndexOf(')');
        
        if (start >= 0 && end > start) {
            String paramString = line.substring(start + 1, end).trim();
            if (!paramString.isEmpty()) {
                String[] params = paramString.split(",");
                
                for (String param : params) {
                    param = param.trim();
                    
                    // Handle self parameters
                    if (param.equals("self") || param.equals("&self") || param.equals("&mut self")) {
                        // Skip self parameters for now
                        continue;
                    }
                    
                    Matcher paramMatcher = PARAMETER_PATTERN.matcher(param);
                    if (paramMatcher.find()) {
                        String paramName = paramMatcher.group(1);
                        String paramType = paramMatcher.group(2).trim();
                        
                        String mappedType = mapRustType(paramType);
                        ParameterNode parameter = new ParameterNode(paramName, mappedType, false, 0, 0);
                        parameters.add(parameter);
                    }
                }
            }
        }
        
        return parameters;
    }
    
    private String mapRustType(String rustType) {
        if (rustType == null || rustType.trim().isEmpty()) {
            return "Object";
        }
        
        rustType = rustType.trim();
        
        // Remove common Rust-specific prefixes/suffixes
        rustType = rustType.replaceAll("^&(?:mut\\s+)?", ""); // Remove references
        rustType = rustType.replaceAll("^mut\\s+", "");        // Remove mutability
        
        switch (rustType) {
            case "()": return "void";
            case "bool": return "Boolean";
            case "i8": case "i16": case "i32": return "Integer";
            case "i64": case "i128": return "Long";
            case "u8": case "u16": case "u32": return "Integer";
            case "u64": case "u128": return "Long";
            case "f32": return "Float";
            case "f64": return "Double";
            case "char": return "Character";
            case "str": case "String": return "String";
            case "usize": case "isize": return "Long";
            default:
                // Handle generic types
                if (rustType.startsWith("Vec<")) {
                    return "List";
                }
                if (rustType.startsWith("HashMap<") || rustType.startsWith("BTreeMap<")) {
                    return "Map";
                }
                if (rustType.startsWith("HashSet<") || rustType.startsWith("BTreeSet<")) {
                    return "Set";
                }
                if (rustType.startsWith("Option<")) {
                    return "Optional";
                }
                if (rustType.startsWith("Result<")) {
                    return "Object"; // Could be enhanced to handle Result type
                }
                if (rustType.startsWith("Box<") || rustType.startsWith("Rc<") || rustType.startsWith("Arc<")) {
                    // Smart pointers - extract inner type
                    int start = rustType.indexOf('<');
                    int end = rustType.lastIndexOf('>');
                    if (start > 0 && end > start) {
                        return mapRustType(rustType.substring(start + 1, end));
                    }
                }
                
                return rustType; // Return as-is for custom types
        }
    }
    
    @Override
    public boolean supportsFile(String fileName) {
        return fileName.toLowerCase().endsWith(".rs");
    }
    
    @Override
    public String getLanguageName() {
        return "Rust";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{".rs"};
    }
}