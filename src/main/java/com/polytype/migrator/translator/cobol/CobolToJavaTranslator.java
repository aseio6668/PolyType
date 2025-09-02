package com.polytype.migrator.translator.cobol;

import com.polytype.migrator.core.logging.PolyTypeLogger;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * COBOL to Java translator for legacy enterprise system modernization.
 * Handles COBOL data structures, procedures, and business logic translation.
 */
public class CobolToJavaTranslator {
    
    private final PolyTypeLogger logger = PolyTypeLogger.getLogger(CobolToJavaTranslator.class);
    
    // COBOL language patterns
    private static final Pattern IDENTIFICATION_DIVISION = Pattern.compile("IDENTIFICATION\\s+DIVISION", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRAM_ID = Pattern.compile("PROGRAM-ID\\.\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_DIVISION = Pattern.compile("DATA\\s+DIVISION", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORKING_STORAGE = Pattern.compile("WORKING-STORAGE\\s+SECTION", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROCEDURE_DIVISION = Pattern.compile("PROCEDURE\\s+DIVISION", Pattern.CASE_INSENSITIVE);
    private static final Pattern PIC_CLAUSE = Pattern.compile("(\\d{2})\\s+(\\w+)\\s+PIC\\s+([X9V\\(\\)\\-\\.S]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERFORM_STATEMENT = Pattern.compile("PERFORM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOVE_STATEMENT = Pattern.compile("MOVE\\s+([\\w\\-\\'\"\\s]+)\\s+TO\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_STATEMENT = Pattern.compile("IF\\s+([^\\n]+)\\s+THEN", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPUTE_STATEMENT = Pattern.compile("COMPUTE\\s+(\\w+)\\s+=\\s+([^\\n]+)", Pattern.CASE_INSENSITIVE);
    
    public static class CobolField {
        private final String name;
        private final String level;
        private final String picture;
        private final String javaType;
        private final int length;
        private final boolean isNumeric;
        
        public CobolField(String name, String level, String picture) {
            this.name = name;
            this.level = level;
            this.picture = picture;
            this.javaType = determineJavaType(picture);
            this.length = calculateLength(picture);
            this.isNumeric = isNumericPicture(picture);
        }
        
        private String determineJavaType(String pic) {
            pic = pic.toUpperCase();
            
            if (pic.contains("9")) {
                if (pic.contains("V") || pic.contains(".")) {
                    return "BigDecimal"; // Decimal numbers
                } else if (pic.length() <= 9 || (pic.contains("(") && extractLength(pic) <= 9)) {
                    return "int";
                } else {
                    return "long";
                }
            } else if (pic.contains("X")) {
                return "String";
            } else if (pic.contains("S")) {
                return "int"; // Signed numeric
            }
            
            return "String"; // Default fallback
        }
        
        private int calculateLength(String pic) {
            if (pic.contains("(") && pic.contains(")")) {
                return extractLength(pic);
            }
            return pic.replaceAll("[^X9VS]", "").length();
        }
        
        private int extractLength(String pic) {
            Pattern lengthPattern = Pattern.compile("\\((\\d+)\\)");
            Matcher matcher = lengthPattern.matcher(pic);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
            return 1;
        }
        
        private boolean isNumericPicture(String pic) {
            return pic.toUpperCase().matches(".*[9VS].*");
        }
        
        // Getters
        public String getName() { return name; }
        public String getLevel() { return level; }
        public String getPicture() { return picture; }
        public String getJavaType() { return javaType; }
        public int getLength() { return length; }
        public boolean isNumeric() { return isNumeric; }
    }
    
    public String translateCobolToJava(String cobolCode) {
        logger.info(PolyTypeLogger.LogCategory.TRANSLATION, "Starting COBOL to Java translation");
        
        StringBuilder javaCode = new StringBuilder();
        
        try {
            // Parse COBOL structure
            String programName = extractProgramName(cobolCode);
            List<CobolField> dataFields = parseWorkingStorage(cobolCode);
            List<String> procedures = parseProcedures(cobolCode);
            
            // Generate Java class structure
            javaCode.append("import java.math.BigDecimal;\n");
            javaCode.append("import java.util.*;\n");
            javaCode.append("import java.io.*;\n\n");
            
            javaCode.append("/**\n");
            javaCode.append(" * Translated from COBOL program: ").append(programName).append("\n");
            javaCode.append(" * Generated by PolyType COBOL-to-Java Translator\n");
            javaCode.append(" */\n");
            javaCode.append("public class ").append(capitalize(programName)).append(" {\n\n");
            
            // Generate data fields as class members
            generateDataFields(javaCode, dataFields);
            
            // Generate constructor
            generateConstructor(javaCode, programName, dataFields);
            
            // Generate main method
            generateMainMethod(javaCode);
            
            // Generate business logic methods
            generateProcedureMethods(javaCode, procedures);
            
            // Generate utility methods
            generateUtilityMethods(javaCode);
            
            javaCode.append("}\n");
            
            logger.info(PolyTypeLogger.LogCategory.TRANSLATION, 
                       "COBOL translation completed successfully", 
                       Map.of("program_name", programName, "fields", dataFields.size(), "procedures", procedures.size()));
            
        } catch (Exception e) {
            logger.error(PolyTypeLogger.LogCategory.TRANSLATION, "COBOL translation failed", e);
            throw new RuntimeException("COBOL translation failed: " + e.getMessage(), e);
        }
        
        return javaCode.toString();
    }
    
    private String extractProgramName(String cobolCode) {
        Matcher matcher = PROGRAM_ID.matcher(cobolCode);
        if (matcher.find()) {
            return sanitizeJavaIdentifier(matcher.group(1));
        }
        return "CobolProgram";
    }
    
    private List<CobolField> parseWorkingStorage(String cobolCode) {
        List<CobolField> fields = new ArrayList<>();
        
        // Find WORKING-STORAGE SECTION
        String[] lines = cobolCode.split("\\n");
        boolean inWorkingStorage = false;
        
        for (String line : lines) {
            line = line.trim();
            
            if (WORKING_STORAGE.matcher(line).find()) {
                inWorkingStorage = true;
                continue;
            }
            
            if (inWorkingStorage && PROCEDURE_DIVISION.matcher(line).find()) {
                break; // End of WORKING-STORAGE
            }
            
            if (inWorkingStorage && !line.isEmpty() && !line.startsWith("*")) {
                CobolField field = parseDataField(line);
                if (field != null) {
                    fields.add(field);
                }
            }
        }
        
        return fields;
    }
    
    private CobolField parseDataField(String line) {
        Matcher matcher = PIC_CLAUSE.matcher(line);
        if (matcher.find()) {
            String level = matcher.group(1);
            String name = sanitizeJavaIdentifier(matcher.group(2));
            String picture = matcher.group(3);
            
            return new CobolField(name, level, picture);
        }
        return null;
    }
    
    private List<String> parseProcedures(String cobolCode) {
        List<String> procedures = new ArrayList<>();
        
        String[] lines = cobolCode.split("\\n");
        boolean inProcedureDiv = false;
        StringBuilder currentProcedure = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            
            if (PROCEDURE_DIVISION.matcher(line).find()) {
                inProcedureDiv = true;
                continue;
            }
            
            if (inProcedureDiv && !line.isEmpty() && !line.startsWith("*")) {
                if (line.endsWith(".") && !line.startsWith(" ")) {
                    // End of current procedure
                    if (currentProcedure.length() > 0) {
                        procedures.add(currentProcedure.toString());
                        currentProcedure = new StringBuilder();
                    }
                    currentProcedure.append(line);
                } else {
                    currentProcedure.append(" ").append(line);
                }
            }
        }
        
        // Add last procedure if any
        if (currentProcedure.length() > 0) {
            procedures.add(currentProcedure.toString());
        }
        
        return procedures;
    }
    
    private void generateDataFields(StringBuilder java, List<CobolField> fields) {
        java.append("    // COBOL WORKING-STORAGE fields translated to Java\n");
        
        for (CobolField field : fields) {
            java.append("    private ").append(field.getJavaType()).append(" ");
            java.append(field.getName().toLowerCase());
            
            // Initialize with appropriate default values
            if (field.getJavaType().equals("String")) {
                java.append(" = \"\"");
            } else if (field.getJavaType().equals("int") || field.getJavaType().equals("long")) {
                java.append(" = 0");
            } else if (field.getJavaType().equals("BigDecimal")) {
                java.append(" = BigDecimal.ZERO");
            }
            
            java.append("; // COBOL: ").append(field.getLevel()).append(" ");
            java.append(field.getName()).append(" PIC ").append(field.getPicture()).append("\n");
        }
        java.append("\n");
    }
    
    private void generateConstructor(StringBuilder java, String programName, List<CobolField> fields) {
        java.append("    public ").append(capitalize(programName)).append("() {\n");
        java.append("        // Initialize COBOL program state\n");
        
        for (CobolField field : fields) {
            if (field.getJavaType().equals("String")) {
                java.append("        this.").append(field.getName().toLowerCase());
                java.append(" = String.format(\"%").append(field.getLength()).append("s\", \"\").replace(' ', ' ');\n");
            }
        }
        
        java.append("    }\n\n");
    }
    
    private void generateMainMethod(StringBuilder java) {
        java.append("    public static void main(String[] args) {\n");
        java.append("        // COBOL program entry point\n");
        java.append("        var program = new ").append("CobolProgram").append("();\n");
        java.append("        program.executeMainLogic();\n");
        java.append("    }\n\n");
    }
    
    private void generateProcedureMethods(StringBuilder java, List<String> procedures) {
        java.append("    // COBOL PROCEDURE DIVISION methods\n");
        
        java.append("    public void executeMainLogic() {\n");
        java.append("        // Main COBOL program logic\n");
        
        for (String procedure : procedures) {
            String methodCall = translateProcedureToMethodCall(procedure);
            java.append("        ").append(methodCall).append("\n");
        }
        
        java.append("    }\n\n");
        
        // Generate individual procedure methods
        for (int i = 0; i < procedures.size(); i++) {
            String procedure = procedures.get(i);
            generateProcedureMethod(java, procedure, "procedure" + (i + 1));
        }
    }
    
    private String translateProcedureToMethodCall(String procedure) {
        // Translate COBOL statements to Java method calls
        procedure = procedure.trim();
        
        // PERFORM statements
        Matcher performMatcher = PERFORM_STATEMENT.matcher(procedure);
        if (performMatcher.find()) {
            String paragraphName = performMatcher.group(1);
            return "perform" + capitalize(sanitizeJavaIdentifier(paragraphName)) + "();";
        }
        
        // MOVE statements
        Matcher moveMatcher = MOVE_STATEMENT.matcher(procedure);
        if (moveMatcher.find()) {
            String source = moveMatcher.group(1).trim();
            String target = sanitizeJavaIdentifier(moveMatcher.group(2).trim());
            return "move(\"" + source + "\", \"" + target + "\");";
        }
        
        // COMPUTE statements
        Matcher computeMatcher = COMPUTE_STATEMENT.matcher(procedure);
        if (computeMatcher.find()) {
            String target = sanitizeJavaIdentifier(computeMatcher.group(1));
            String expression = computeMatcher.group(2);
            return "compute(\"" + target + "\", \"" + expression + "\");";
        }
        
        // Default: create a comment
        return "// TODO: Translate COBOL statement: " + procedure;
    }
    
    private void generateProcedureMethod(StringBuilder java, String procedure, String methodName) {
        java.append("    private void ").append(methodName).append("() {\n");
        java.append("        // Translated from COBOL: ").append(procedure.substring(0, Math.min(50, procedure.length()))).append("...\n");
        
        // Basic COBOL statement translation
        if (procedure.toUpperCase().contains("DISPLAY")) {
            java.append("        System.out.println(\"COBOL Display: [Translated]\");\n");
        } else if (procedure.toUpperCase().contains("ACCEPT")) {
            java.append("        // TODO: Implement ACCEPT statement (user input)\n");
        } else if (procedure.toUpperCase().contains("OPEN")) {
            java.append("        // TODO: Implement file OPEN statement\n");
        } else if (procedure.toUpperCase().contains("READ")) {
            java.append("        // TODO: Implement file READ statement\n");
        } else if (procedure.toUpperCase().contains("WRITE")) {
            java.append("        // TODO: Implement file WRITE statement\n");
        } else {
            java.append("        // TODO: Implement COBOL logic: ").append(procedure).append("\n");
        }
        
        java.append("    }\n\n");
    }
    
    private void generateUtilityMethods(StringBuilder java) {
        java.append("    // COBOL utility methods\n");
        
        // MOVE operation
        java.append("    private void move(String source, String targetField) {\n");
        java.append("        // COBOL MOVE statement implementation\n");
        java.append("        try {\n");
        java.append("            java.lang.reflect.Field field = this.getClass().getDeclaredField(targetField.toLowerCase());\n");
        java.append("            field.setAccessible(true);\n");
        java.append("            \n");
        java.append("            if (field.getType() == String.class) {\n");
        java.append("                field.set(this, source);\n");
        java.append("            } else if (field.getType() == int.class) {\n");
        java.append("                field.set(this, Integer.parseInt(source.trim()));\n");
        java.append("            } else if (field.getType() == BigDecimal.class) {\n");
        java.append("                field.set(this, new BigDecimal(source.trim()));\n");
        java.append("            }\n");
        java.append("        } catch (Exception e) {\n");
        java.append("            System.err.println(\"MOVE operation failed: \" + e.getMessage());\n");
        java.append("        }\n");
        java.append("    }\n\n");
        
        // COMPUTE operation
        java.append("    private void compute(String targetField, String expression) {\n");
        java.append("        // COBOL COMPUTE statement implementation\n");
        java.append("        // TODO: Implement mathematical expression evaluation\n");
        java.append("        System.out.println(\"COMPUTE \" + targetField + \" = \" + expression);\n");
        java.append("    }\n\n");
        
        // Display formatting for COBOL compatibility
        java.append("    private void displayCobolFormat(String fieldName, Object value) {\n");
        java.append("        // Format output similar to COBOL DISPLAY\n");
        java.append("        System.out.printf(\"%-20s: %s%n\", fieldName, value);\n");
        java.append("    }\n\n");
    }
    
    private String sanitizeJavaIdentifier(String cobolName) {
        if (cobolName == null) return "field";
        
        // Convert COBOL naming to Java camelCase
        String name = cobolName.replace("-", "_").toLowerCase();
        
        // Ensure it starts with a letter
        if (!Character.isLetter(name.charAt(0))) {
            name = "field_" + name;
        }
        
        // Convert to camelCase
        StringBuilder camelCase = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (char c : name.toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                camelCase.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                camelCase.append(c);
            }
        }
        
        return camelCase.toString();
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    // Demo method for testing
    public void demonstrateCobolTranslation() {
        String sampleCobol = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. PAYROLL.
            
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 EMPLOYEE-RECORD.
               05 EMP-NAME         PIC X(30).
               05 EMP-ID           PIC 9(6).
               05 HOURLY-RATE      PIC 9(3)V99.
               05 HOURS-WORKED     PIC 9(3).
               05 GROSS-PAY        PIC 9(5)V99.
            
            PROCEDURE DIVISION.
            MAIN-LOGIC.
                PERFORM CALCULATE-PAY.
                PERFORM DISPLAY-RESULTS.
                STOP RUN.
            
            CALCULATE-PAY.
                COMPUTE GROSS-PAY = HOURLY-RATE * HOURS-WORKED.
            
            DISPLAY-RESULTS.
                DISPLAY 'Employee: ' EMP-NAME.
                DISPLAY 'Gross Pay: ' GROSS-PAY.
            """;
        
        System.out.println("COBOL TO JAVA TRANSLATION DEMO");
        System.out.println("===============================");
        System.out.println("Original COBOL Program:");
        System.out.println(sampleCobol);
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        String javaResult = translateCobolToJava(sampleCobol);
        System.out.println("Translated Java Program:");
        System.out.println(javaResult);
    }
}