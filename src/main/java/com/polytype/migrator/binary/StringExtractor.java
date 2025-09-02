package com.polytype.migrator.binary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Advanced string extraction from binary files.
 * Extracts ASCII, Unicode, and obfuscated strings.
 */
public class StringExtractor {
    
    private static final int MIN_STRING_LENGTH = 4;
    private static final int MAX_STRING_LENGTH = 1000;
    private static final Pattern PRINTABLE_ASCII = Pattern.compile("^[ -~]+$");
    
    /**
     * Extract strings from a binary file.
     */
    public List<String> extractStrings(Path binaryFile, BinaryAnalyzer.BinaryFormat format) throws IOException {
        List<String> strings = new ArrayList<>();
        
        try (RandomAccessFile file = new RandomAccessFile(binaryFile.toFile(), "r")) {
            byte[] buffer = new byte[(int) file.length()];
            file.readFully(buffer);
            
            // Extract ASCII strings
            strings.addAll(extractAsciiStrings(buffer));
            
            // Extract Unicode strings
            strings.addAll(extractUnicodeStrings(buffer));
            
            // Extract format-specific strings
            switch (format) {
                case PE:
                    strings.addAll(extractPEStrings(file));
                    break;
                case ELF:
                    strings.addAll(extractELFStrings(file));
                    break;
            }
        }
        
        // Deduplicate and filter
        return strings.stream()
                .distinct()
                .filter(this::isValidString)
                .sorted()
                .toList();
    }
    
    private List<String> extractAsciiStrings(byte[] data) {
        List<String> strings = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (byte b : data) {
            if (b >= 32 && b <= 126) { // Printable ASCII
                current.append((char) b);
            } else {
                if (current.length() >= MIN_STRING_LENGTH) {
                    strings.add(current.toString());
                }
                current.setLength(0);
            }
        }
        
        // Add final string if valid
        if (current.length() >= MIN_STRING_LENGTH) {
            strings.add(current.toString());
        }
        
        return strings;
    }
    
    private List<String> extractUnicodeStrings(byte[] data) {
        List<String> strings = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < data.length - 1; i += 2) {
            // Little-endian UTF-16
            int codePoint = (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8);
            
            if (codePoint >= 32 && codePoint <= 126) { // Printable ASCII range in Unicode
                current.append((char) codePoint);
            } else if (codePoint == 0 && current.length() >= MIN_STRING_LENGTH) {
                strings.add(current.toString());
                current.setLength(0);
            } else if (codePoint < 32 || codePoint > 0xFFFF) {
                if (current.length() >= MIN_STRING_LENGTH) {
                    strings.add(current.toString());
                }
                current.setLength(0);
            }
        }
        
        // Add final string if valid
        if (current.length() >= MIN_STRING_LENGTH) {
            strings.add(current.toString());
        }
        
        return strings;
    }
    
    private List<String> extractPEStrings(RandomAccessFile file) throws IOException {
        List<String> strings = new ArrayList<>();
        
        // Extract strings from resource section
        try {
            file.seek(0);
            
            // Read DOS header
            file.seek(0x3C);
            int peOffset = Integer.reverseBytes(file.readInt());
            
            // Read PE header
            file.seek(peOffset + 4); // Skip PE signature
            file.skipBytes(20); // Skip COFF header
            
            int optionalHeaderSize = Short.reverseBytes(file.readShort());
            file.skipBytes(2); // Skip characteristics
            
            // Skip optional header
            file.skipBytes(optionalHeaderSize);
            
            // Read section headers
            file.seek(peOffset + 4 + 20 + 2 + optionalHeaderSize);
            int numberOfSections = Short.reverseBytes(file.readShort());
            
            for (int i = 0; i < numberOfSections; i++) {
                byte[] sectionName = new byte[8];
                file.readFully(sectionName);
                String name = new String(sectionName).trim();
                
                file.skipBytes(4); // Virtual size
                file.skipBytes(4); // Virtual address
                int rawDataSize = Integer.reverseBytes(file.readInt());
                int rawDataPointer = Integer.reverseBytes(file.readInt());
                file.skipBytes(16); // Skip remaining section header fields
                
                // Extract strings from .rsrc (resource) and .rdata sections
                if (name.startsWith(".rsrc") || name.startsWith(".rdata")) {
                    long currentPos = file.getFilePointer();
                    
                    file.seek(rawDataPointer);
                    byte[] sectionData = new byte[rawDataSize];
                    file.readFully(sectionData);
                    
                    strings.addAll(extractAsciiStrings(sectionData));
                    strings.addAll(extractUnicodeStrings(sectionData));
                    
                    file.seek(currentPos);
                }
            }
        } catch (Exception e) {
            // Ignore errors in resource extraction
        }
        
        return strings;
    }
    
    private List<String> extractELFStrings(RandomAccessFile file) throws IOException {
        List<String> strings = new ArrayList<>();
        
        // Extract strings from .rodata and .data sections
        try {
            file.seek(0);
            
            // Check ELF magic
            byte[] magic = new byte[4];
            file.readFully(magic);
            if (magic[0] != 0x7F || magic[1] != 'E' || magic[2] != 'L' || magic[3] != 'F') {
                return strings;
            }
            
            // Read ELF header fields
            int elfClass = file.readByte(); // 1 = 32-bit, 2 = 64-bit
            file.skipBytes(11); // Skip remaining e_ident
            
            file.skipBytes(2); // e_type
            file.skipBytes(2); // e_machine
            file.skipBytes(4); // e_version
            
            // Skip entry point and program header info
            if (elfClass == 1) { // 32-bit
                file.skipBytes(12);
            } else { // 64-bit
                file.skipBytes(24);
            }
            
            // Read section header info
            long shoff = (elfClass == 1) ? Integer.reverseBytes(file.readInt()) : Long.reverseBytes(file.readLong());
            file.skipBytes(4); // e_flags
            file.skipBytes(2); // e_ehsize
            file.skipBytes(2); // e_phentsize
            file.skipBytes(2); // e_phnum
            int shentsize = Short.reverseBytes(file.readShort());
            int shnum = Short.reverseBytes(file.readShort());
            int shstrndx = Short.reverseBytes(file.readShort());
            
            // Read section headers to find string sections
            for (int i = 0; i < shnum; i++) {
                long sectionHeaderOffset = shoff + (i * shentsize);
                file.seek(sectionHeaderOffset);
                
                file.skipBytes(4); // sh_name
                file.skipBytes(4); // sh_type
                file.skipBytes(elfClass == 1 ? 8 : 16); // sh_flags and sh_addr
                
                long shOffset = (elfClass == 1) ? Integer.reverseBytes(file.readInt()) : Long.reverseBytes(file.readLong());
                long shSize = (elfClass == 1) ? Integer.reverseBytes(file.readInt()) : Long.reverseBytes(file.readLong());
                
                // Extract strings from sections that typically contain string data
                if (shSize > 0 && shSize < 10000000) { // Reasonable size limit
                    long currentPos = file.getFilePointer();
                    
                    file.seek(shOffset);
                    byte[] sectionData = new byte[(int) shSize];
                    file.readFully(sectionData);
                    
                    strings.addAll(extractAsciiStrings(sectionData));
                    
                    file.seek(currentPos);
                }
            }
        } catch (Exception e) {
            // Ignore errors in section extraction
        }
        
        return strings;
    }
    
    private boolean isValidString(String str) {
        if (str.length() < MIN_STRING_LENGTH || str.length() > MAX_STRING_LENGTH) {
            return false;
        }
        
        // Filter out obvious non-strings
        if (str.matches("^[\\x00-\\x1F]+$")) { // All control characters
            return false;
        }
        
        if (str.matches("^[\\xFF]+$")) { // All 0xFF bytes
            return false;
        }
        
        // Check for reasonable character distribution
        long printableChars = str.chars().filter(c -> c >= 32 && c <= 126).count();
        double printableRatio = (double) printableChars / str.length();
        
        if (printableRatio < 0.7) { // At least 70% printable characters
            return false;
        }
        
        // Filter common patterns that aren't meaningful strings
        if (str.matches("^[A-Fa-f0-9]{8,}$")) { // Looks like hex data
            return false;
        }
        
        if (str.matches("^[01]{8,}$")) { // Looks like binary data
            return false;
        }
        
        return true;
    }
    
    /**
     * Extract strings with context information.
     */
    public List<ExtractedString> extractStringsWithContext(Path binaryFile, BinaryAnalyzer.BinaryFormat format) throws IOException {
        List<ExtractedString> strings = new ArrayList<>();
        
        try (RandomAccessFile file = new RandomAccessFile(binaryFile.toFile(), "r")) {
            byte[] buffer = new byte[(int) file.length()];
            file.readFully(buffer);
            
            strings.addAll(extractAsciiStringsWithContext(buffer));
            strings.addAll(extractUnicodeStringsWithContext(buffer));
        }
        
        return strings.stream()
                .filter(s -> isValidString(s.getValue()))
                .sorted(Comparator.comparing(ExtractedString::getOffset))
                .toList();
    }
    
    private List<ExtractedString> extractAsciiStringsWithContext(byte[] data) {
        List<ExtractedString> strings = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int startOffset = -1;
        
        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            if (b >= 32 && b <= 126) { // Printable ASCII
                if (current.length() == 0) {
                    startOffset = i;
                }
                current.append((char) b);
            } else {
                if (current.length() >= MIN_STRING_LENGTH) {
                    ExtractedString str = new ExtractedString();
                    str.setValue(current.toString());
                    str.setOffset(startOffset);
                    str.setLength(current.length());
                    str.setEncoding(StringEncoding.ASCII);
                    str.setContext(analyzeStringContext(data, startOffset));
                    strings.add(str);
                }
                current.setLength(0);
                startOffset = -1;
            }
        }
        
        // Add final string if valid
        if (current.length() >= MIN_STRING_LENGTH) {
            ExtractedString str = new ExtractedString();
            str.setValue(current.toString());
            str.setOffset(startOffset);
            str.setLength(current.length());
            str.setEncoding(StringEncoding.ASCII);
            str.setContext(analyzeStringContext(data, startOffset));
            strings.add(str);
        }
        
        return strings;
    }
    
    private List<ExtractedString> extractUnicodeStringsWithContext(byte[] data) {
        List<ExtractedString> strings = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int startOffset = -1;
        
        for (int i = 0; i < data.length - 1; i += 2) {
            int codePoint = (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8);
            
            if (codePoint >= 32 && codePoint <= 126) {
                if (current.length() == 0) {
                    startOffset = i;
                }
                current.append((char) codePoint);
            } else if (codePoint == 0 && current.length() >= MIN_STRING_LENGTH) {
                ExtractedString str = new ExtractedString();
                str.setValue(current.toString());
                str.setOffset(startOffset);
                str.setLength(current.length() * 2);
                str.setEncoding(StringEncoding.UTF16LE);
                str.setContext(analyzeStringContext(data, startOffset));
                strings.add(str);
                current.setLength(0);
                startOffset = -1;
            } else if (codePoint < 32 || codePoint > 0xFFFF) {
                if (current.length() >= MIN_STRING_LENGTH) {
                    ExtractedString str = new ExtractedString();
                    str.setValue(current.toString());
                    str.setOffset(startOffset);
                    str.setLength(current.length() * 2);
                    str.setEncoding(StringEncoding.UTF16LE);
                    str.setContext(analyzeStringContext(data, startOffset));
                    strings.add(str);
                }
                current.setLength(0);
                startOffset = -1;
            }
        }
        
        return strings;
    }
    
    private StringContext analyzeStringContext(byte[] data, int offset) {
        StringContext context = new StringContext();
        
        // Check surrounding bytes for patterns
        int contextStart = Math.max(0, offset - 16);
        int contextEnd = Math.min(data.length, offset + 64);
        
        // Look for null-terminated string patterns
        if (offset > 0 && data[offset - 1] == 0) {
            context.addProperty("null_prefixed", true);
        }
        
        // Look for reference patterns (e.g., push instructions referencing this string)
        for (int i = contextStart; i < offset; i++) {
            if (data[i] == (byte) 0x68) { // PUSH immediate on x86
                // Check if the next 4 bytes could be an address pointing to our string
                context.addProperty("possible_reference", i);
                break;
            }
        }
        
        // Classify string type based on content patterns
        return context;
    }
}

/**
 * Represents an extracted string with metadata.
 */
class ExtractedString {
    private String value;
    private long offset;
    private int length;
    private StringEncoding encoding;
    private StringContext context;
    private StringType type;
    
    public ExtractedString() {
        this.context = new StringContext();
        this.type = StringType.UNKNOWN;
    }
    
    // Getters and setters
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public long getOffset() { return offset; }
    public void setOffset(long offset) { this.offset = offset; }
    
    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    
    public StringEncoding getEncoding() { return encoding; }
    public void setEncoding(StringEncoding encoding) { this.encoding = encoding; }
    
    public StringContext getContext() { return context; }
    public void setContext(StringContext context) { this.context = context; }
    
    public StringType getType() { return type; }
    public void setType(StringType type) { this.type = type; }
}

/**
 * Context information for an extracted string.
 */
class StringContext {
    private Map<String, Object> properties;
    
    public StringContext() {
        this.properties = new HashMap<>();
    }
    
    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
}

enum StringEncoding {
    ASCII, UTF8, UTF16LE, UTF16BE, UTF32
}

enum StringType {
    ERROR_MESSAGE, DEBUG_STRING, FILE_PATH, URL, 
    REGISTRY_KEY, API_NAME, CLASS_NAME, UNKNOWN
}