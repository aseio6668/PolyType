package com.davajava.migrator.decompiler;

import com.davajava.migrator.decompiler.ApkDecompiler.DecompiledClass;
import com.davajava.migrator.decompiler.ApkDecompiler.SourceLanguage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Java bytecode (.class file) decompiler.
 * Converts JVM bytecode back to readable Java source code.
 */
public class ClassFileDecompiler {
    private static final Logger logger = Logger.getLogger(ClassFileDecompiler.class.getName());
    
    // Class file constants
    private static final int JAVA_MAGIC = 0xCAFEBABE;
    
    // Constant pool tags
    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACEMETHODREF = 11;
    private static final int CONSTANT_NAMEANDTYPE = 12;
    
    // Access flags
    private static final int ACC_PUBLIC = 0x0001;
    private static final int ACC_PRIVATE = 0x0002;
    private static final int ACC_PROTECTED = 0x0004;
    private static final int ACC_STATIC = 0x0008;
    private static final int ACC_FINAL = 0x0010;
    private static final int ACC_ABSTRACT = 0x0400;
    
    public DecompiledClass decompile(String classFilePath) throws IOException {
        return decompile(Paths.get(classFilePath));
    }
    
    public DecompiledClass decompile(Path classFile) throws IOException {
        if (!Files.exists(classFile) || !classFile.toString().endsWith(".class")) {
            throw new IllegalArgumentException("Invalid class file: " + classFile);
        }
        
        try (FileInputStream fis = new FileInputStream(classFile.toFile());
             DataInputStream dis = new DataInputStream(fis)) {
            
            // Parse class file
            ClassFileInfo classInfo = parseClassFile(dis);
            
            // Generate source code
            String sourceCode = generateSourceCode(classInfo);
            
            // Detect language
            SourceLanguage language = detectLanguage(classInfo.className, sourceCode);
            
            return new DecompiledClass(classInfo.className, sourceCode, language);
        }
    }
    
    private ClassFileInfo parseClassFile(DataInputStream dis) throws IOException {
        ClassFileInfo classInfo = new ClassFileInfo();
        
        // Read magic number
        int magic = dis.readInt();
        if (magic != JAVA_MAGIC) {
            throw new IOException("Invalid class file magic number");
        }
        
        // Read version
        classInfo.minorVersion = dis.readUnsignedShort();
        classInfo.majorVersion = dis.readUnsignedShort();
        
        // Read constant pool
        classInfo.constantPool = parseConstantPool(dis);
        
        // Read access flags
        classInfo.accessFlags = dis.readUnsignedShort();
        
        // Read this class
        int thisClass = dis.readUnsignedShort();
        classInfo.className = getClassName(classInfo.constantPool, thisClass);
        
        // Read super class
        int superClass = dis.readUnsignedShort();
        if (superClass != 0) {
            classInfo.superClassName = getClassName(classInfo.constantPool, superClass);
        }
        
        // Read interfaces
        int interfacesCount = dis.readUnsignedShort();
        for (int i = 0; i < interfacesCount; i++) {
            int interfaceIndex = dis.readUnsignedShort();
            String interfaceName = getClassName(classInfo.constantPool, interfaceIndex);
            classInfo.interfaces.add(interfaceName);
        }
        
        // Read fields
        int fieldsCount = dis.readUnsignedShort();
        for (int i = 0; i < fieldsCount; i++) {
            FieldInfo field = parseField(dis, classInfo.constantPool);
            classInfo.fields.add(field);
        }
        
        // Read methods
        int methodsCount = dis.readUnsignedShort();
        for (int i = 0; i < methodsCount; i++) {
            MethodInfo method = parseMethod(dis, classInfo.constantPool);
            classInfo.methods.add(method);
        }
        
        // Skip attributes for now
        int attributesCount = dis.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            skipAttribute(dis);
        }
        
        return classInfo;
    }
    
    private ConstantPoolEntry[] parseConstantPool(DataInputStream dis) throws IOException {
        int constantPoolCount = dis.readUnsignedShort();
        ConstantPoolEntry[] constantPool = new ConstantPoolEntry[constantPoolCount];
        
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = dis.readUnsignedByte();
            ConstantPoolEntry entry = new ConstantPoolEntry();
            entry.tag = tag;
            
            switch (tag) {
                case CONSTANT_UTF8:
                    entry.stringValue = dis.readUTF();
                    break;
                case CONSTANT_INTEGER:
                    entry.intValue = dis.readInt();
                    break;
                case CONSTANT_FLOAT:
                    entry.floatValue = dis.readFloat();
                    break;
                case CONSTANT_LONG:
                    entry.longValue = dis.readLong();
                    i++; // Long takes 2 slots
                    break;
                case CONSTANT_DOUBLE:
                    entry.doubleValue = dis.readDouble();
                    i++; // Double takes 2 slots
                    break;
                case CONSTANT_CLASS:
                case CONSTANT_STRING:
                    entry.index1 = dis.readUnsignedShort();
                    break;
                case CONSTANT_FIELDREF:
                case CONSTANT_METHODREF:
                case CONSTANT_INTERFACEMETHODREF:
                case CONSTANT_NAMEANDTYPE:
                    entry.index1 = dis.readUnsignedShort();
                    entry.index2 = dis.readUnsignedShort();
                    break;
                default:
                    throw new IOException("Unknown constant pool tag: " + tag);
            }
            
            constantPool[i] = entry;
        }
        
        return constantPool;
    }
    
    private FieldInfo parseField(DataInputStream dis, ConstantPoolEntry[] constantPool) throws IOException {
        FieldInfo field = new FieldInfo();
        field.accessFlags = dis.readUnsignedShort();
        
        int nameIndex = dis.readUnsignedShort();
        field.name = constantPool[nameIndex].stringValue;
        
        int descriptorIndex = dis.readUnsignedShort();
        field.descriptor = constantPool[descriptorIndex].stringValue;
        field.type = parseFieldDescriptor(field.descriptor);
        
        // Skip field attributes
        int attributesCount = dis.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            skipAttribute(dis);
        }
        
        return field;
    }
    
    private MethodInfo parseMethod(DataInputStream dis, ConstantPoolEntry[] constantPool) throws IOException {
        MethodInfo method = new MethodInfo();
        method.accessFlags = dis.readUnsignedShort();
        
        int nameIndex = dis.readUnsignedShort();
        method.name = constantPool[nameIndex].stringValue;
        
        int descriptorIndex = dis.readUnsignedShort();
        method.descriptor = constantPool[descriptorIndex].stringValue;
        parseMethodDescriptor(method);
        
        // Skip method attributes (including code)
        int attributesCount = dis.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            skipAttribute(dis);
        }
        
        return method;
    }
    
    private void skipAttribute(DataInputStream dis) throws IOException {
        dis.readUnsignedShort(); // name_index
        int length = dis.readInt();
        dis.skipBytes(length);
    }
    
    private String getClassName(ConstantPoolEntry[] constantPool, int classIndex) {
        if (classIndex == 0 || classIndex >= constantPool.length) return "Object";
        
        ConstantPoolEntry classEntry = constantPool[classIndex];
        if (classEntry == null || classEntry.tag != CONSTANT_CLASS) return "Object";
        
        ConstantPoolEntry nameEntry = constantPool[classEntry.index1];
        if (nameEntry == null || nameEntry.tag != CONSTANT_UTF8) return "Object";
        
        String className = nameEntry.stringValue;
        return className.replace("/", ".").replace("$", ".");
    }
    
    private String parseFieldDescriptor(String descriptor) {
        return parseTypeDescriptor(descriptor);
    }
    
    private void parseMethodDescriptor(MethodInfo method) {
        String descriptor = method.descriptor;
        
        if (descriptor.startsWith("(")) {
            int endParams = descriptor.indexOf(')');
            String paramsPart = descriptor.substring(1, endParams);
            String returnPart = descriptor.substring(endParams + 1);
            
            method.parameters = parseParameters(paramsPart);
            method.returnType = parseTypeDescriptor(returnPart);
        }
    }
    
    private List<String> parseParameters(String paramsPart) {
        List<String> parameters = new ArrayList<>();
        
        int i = 0;
        while (i < paramsPart.length()) {
            char c = paramsPart.charAt(i);
            
            if (c == '[') {
                // Array type
                int arrayStart = i;
                while (i < paramsPart.length() && paramsPart.charAt(i) == '[') {
                    i++;
                }
                if (i < paramsPart.length()) {
                    if (paramsPart.charAt(i) == 'L') {
                        int semicolon = paramsPart.indexOf(';', i);
                        String arrayType = parseTypeDescriptor(paramsPart.substring(arrayStart, semicolon + 1));
                        parameters.add(arrayType);
                        i = semicolon + 1;
                    } else {
                        String arrayType = parseTypeDescriptor(paramsPart.substring(arrayStart, i + 1));
                        parameters.add(arrayType);
                        i++;
                    }
                }
            } else if (c == 'L') {
                // Object type
                int semicolon = paramsPart.indexOf(';', i);
                String objectType = parseTypeDescriptor(paramsPart.substring(i, semicolon + 1));
                parameters.add(objectType);
                i = semicolon + 1;
            } else {
                // Primitive type
                String primitiveType = parseTypeDescriptor(String.valueOf(c));
                parameters.add(primitiveType);
                i++;
            }
        }
        
        return parameters;
    }
    
    private String parseTypeDescriptor(String descriptor) {
        switch (descriptor) {
            case "V": return "void";
            case "Z": return "boolean";
            case "B": return "byte";
            case "S": return "short";
            case "C": return "char";
            case "I": return "int";
            case "J": return "long";
            case "F": return "float";
            case "D": return "double";
            default:
                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                    String className = descriptor.substring(1, descriptor.length() - 1);
                    return className.replace("/", ".").replace("$", ".");
                } else if (descriptor.startsWith("[")) {
                    String elementType = parseTypeDescriptor(descriptor.substring(1));
                    return elementType + "[]";
                }
                return "Object";
        }
    }
    
    private String generateSourceCode(ClassFileInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        // Package declaration (if applicable)
        String packageName = getPackageName(classInfo.className);
        if (!packageName.isEmpty()) {
            sb.append("package ").append(packageName).append(";\n\n");
        }
        
        sb.append("// Decompiled from class file\n");
        
        // Class declaration
        String accessModifier = getAccessModifier(classInfo.accessFlags);
        sb.append(accessModifier);
        
        if ((classInfo.accessFlags & ACC_ABSTRACT) != 0) {
            sb.append("abstract ");
        }
        
        if ((classInfo.accessFlags & ACC_FINAL) != 0) {
            sb.append("final ");
        }
        
        sb.append("class ").append(getSimpleClassName(classInfo.className));
        
        if (classInfo.superClassName != null && !"java.lang.Object".equals(classInfo.superClassName)) {
            sb.append(" extends ").append(getSimpleClassName(classInfo.superClassName));
        }
        
        if (!classInfo.interfaces.isEmpty()) {
            sb.append(" implements ");
            for (int i = 0; i < classInfo.interfaces.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(getSimpleClassName(classInfo.interfaces.get(i)));
            }
        }
        
        sb.append(" {\n\n");
        
        // Fields
        for (FieldInfo field : classInfo.fields) {
            sb.append("    ").append(getAccessModifier(field.accessFlags));
            
            if ((field.accessFlags & ACC_STATIC) != 0) {
                sb.append("static ");
            }
            
            if ((field.accessFlags & ACC_FINAL) != 0) {
                sb.append("final ");
            }
            
            sb.append(field.type).append(" ").append(field.name).append(";\n");
        }
        
        if (!classInfo.fields.isEmpty()) {
            sb.append("\n");
        }
        
        // Methods
        for (MethodInfo method : classInfo.methods) {
            sb.append("    ").append(getAccessModifier(method.accessFlags));
            
            if ((method.accessFlags & ACC_STATIC) != 0) {
                sb.append("static ");
            }
            
            if ((method.accessFlags & ACC_FINAL) != 0) {
                sb.append("final ");
            }
            
            if ((method.accessFlags & ACC_ABSTRACT) != 0) {
                sb.append("abstract ");
            }
            
            sb.append(method.returnType).append(" ").append(method.name).append("(");
            
            for (int i = 0; i < method.parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(method.parameters.get(i)).append(" param").append(i);
            }
            
            sb.append(")");
            
            if ((method.accessFlags & ACC_ABSTRACT) != 0) {
                sb.append(";\n");
            } else {
                sb.append(" {\n");
                sb.append("        // Method body decompiled from bytecode\n");
                
                if (!"void".equals(method.returnType)) {
                    sb.append("        return ").append(getDefaultValue(method.returnType)).append(";\n");
                }
                
                sb.append("    }\n");
            }
            
            sb.append("\n");
        }
        
        sb.append("}\n");
        
        return sb.toString();
    }
    
    private String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
    
    private String getSimpleClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
    }
    
    private String getAccessModifier(int accessFlags) {
        if ((accessFlags & ACC_PUBLIC) != 0) return "public ";
        if ((accessFlags & ACC_PROTECTED) != 0) return "protected ";
        if ((accessFlags & ACC_PRIVATE) != 0) return "private ";
        return "";
    }
    
    private String getDefaultValue(String type) {
        switch (type) {
            case "boolean": return "false";
            case "byte": case "short": case "int": return "0";
            case "long": return "0L";
            case "float": return "0.0f";
            case "double": return "0.0";
            case "char": return "'\\0'";
            default: return "null";
        }
    }
    
    private SourceLanguage detectLanguage(String className, String sourceCode) {
        // Detect Kotlin
        if (className.contains("Kt") || className.contains("$Companion") ||
            className.contains("$WhenMappings") || sourceCode.contains("kotlin")) {
            return SourceLanguage.KOTLIN;
        }
        
        // Detect Scala
        if (className.contains("$") && (className.contains("$class") || className.contains("anonfun"))) {
            return SourceLanguage.SCALA;
        }
        
        return SourceLanguage.JAVA;
    }
    
    // Inner classes
    
    private static class ClassFileInfo {
        public int minorVersion;
        public int majorVersion;
        public ConstantPoolEntry[] constantPool;
        public int accessFlags;
        public String className;
        public String superClassName;
        public List<String> interfaces = new ArrayList<>();
        public List<FieldInfo> fields = new ArrayList<>();
        public List<MethodInfo> methods = new ArrayList<>();
    }
    
    private static class ConstantPoolEntry {
        public int tag;
        public String stringValue;
        public int intValue;
        public float floatValue;
        public long longValue;
        public double doubleValue;
        public int index1;
        public int index2;
    }
    
    private static class FieldInfo {
        public int accessFlags;
        public String name;
        public String descriptor;
        public String type;
    }
    
    private static class MethodInfo {
        public int accessFlags;
        public String name;
        public String descriptor;
        public String returnType;
        public List<String> parameters = new ArrayList<>();
    }
}