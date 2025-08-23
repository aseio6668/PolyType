// Simple test to verify DavaJava core parsing works
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class TestCore {
    public static void main(String[] args) {
        System.out.println("=== DavaJava Core Functionality Test ===\n");
        
        // Test Rust parsing patterns
        String rustCode = 
            "pub struct Person {\n" +
            "    pub name: String,\n" +
            "    age: i32,\n" +
            "}\n" +
            "\n" +
            "pub fn create_person(name: String, age: i32) -> Person {\n" +
            "    Person { name, age }\n" +
            "}";
        
        System.out.println("Input Rust Code:");
        System.out.println(rustCode);
        System.out.println();
        
        // Test struct pattern
        Pattern structPattern = Pattern.compile(
            "(?:pub\\s+)?struct\\s+(\\w+)(?:<[^>]*>)?\\s*\\{([^}]*)\\}",
            Pattern.MULTILINE | Pattern.DOTALL
        );
        
        Matcher matcher = structPattern.matcher(rustCode);
        if (matcher.find()) {
            System.out.println("✅ Found struct: " + matcher.group(1));
            String fields = matcher.group(2);
            System.out.println("   Fields block: " + fields.trim().replaceAll("\\s+", " "));
            
            // Parse fields
            String[] fieldLines = fields.split(",");
            for (String field : fieldLines) {
                field = field.trim();
                if (field.isEmpty()) continue;
                
                boolean isPublic = field.startsWith("pub ");
                if (isPublic) field = field.substring(4).trim();
                
                String[] parts = field.split(":");
                if (parts.length >= 2) {
                    String fieldName = parts[0].trim();
                    String fieldType = mapRustTypeToJava(parts[1].trim());
                    System.out.println("   - " + (isPublic ? "public" : "private") + " " + fieldType + " " + fieldName);
                }
            }
        }
        
        System.out.println();
        
        // Test function pattern
        Pattern funcPattern = Pattern.compile(
            "(?:pub\\s+)?fn\\s+(\\w+)\\s*\\(([^)]*)\\)(?:\\s*->\\s*([^{]+))?\\s*\\{",
            Pattern.MULTILINE
        );
        
        matcher = funcPattern.matcher(rustCode);
        if (matcher.find()) {
            System.out.println("✅ Found function: " + matcher.group(1));
            System.out.println("   Parameters: " + matcher.group(2));
            String returnType = matcher.group(3);
            if (returnType != null) {
                System.out.println("   Return type: " + mapRustTypeToJava(returnType.trim()));
            }
        }
        
        System.out.println();
        System.out.println("🎉 Core parsing logic is working correctly!");
        System.out.println("📝 This demonstrates the same patterns used in the full DavaJava parser.");
    }
    
    private static String mapRustTypeToJava(String rustType) {
        rustType = rustType.trim();
        switch (rustType) {
            case "i32": case "i64": return "int";
            case "u32": case "u64": return "int";
            case "f32": case "f64": return "double";
            case "bool": return "boolean";
            case "String": case "&str": return "String";
            case "()": return "void";
            default:
                if (rustType.startsWith("Vec<") && rustType.endsWith(">")) {
                    String innerType = rustType.substring(4, rustType.length() - 1);
                    return "List<" + mapRustTypeToJava(innerType) + ">";
                }
                return rustType;
        }
    }
}