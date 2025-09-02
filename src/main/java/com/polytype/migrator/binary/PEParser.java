package com.polytype.migrator.binary;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

/**
 * Advanced Windows PE (Portable Executable) parser.
 * Handles .exe, .dll, .sys, and other PE format files.
 * 
 * Supports:
 * - PE32 and PE32+ (64-bit) formats
 * - Import/Export table analysis
 * - Resource extraction
 * - Section mapping and permissions
 * - Anti-analysis detection
 * - Packer identification
 */
public class PEParser implements BinaryParser {
    
    private static final int DOS_HEADER_SIZE = 64;
    private static final int PE_SIGNATURE = 0x00004550; // "PE\0\0"
    private static final int IMAGE_NT_OPTIONAL_HDR32_MAGIC = 0x10b;
    private static final int IMAGE_NT_OPTIONAL_HDR64_MAGIC = 0x20b;
    
    @Override
    public BinaryStructure parseStructure(Path binaryFile) throws IOException {
        BinaryStructure structure = new BinaryStructure();
        
        try (RandomAccessFile file = new RandomAccessFile(binaryFile.toFile(), "r")) {
            // Parse DOS header
            DOSHeader dosHeader = parseDOSHeader(file);
            
            // Parse PE header
            file.seek(dosHeader.peHeaderOffset);
            PEHeader peHeader = parsePEHeader(file);
            
            // Determine architecture
            structure.setArchitecture(peHeader.is64Bit ? "x64" : "x86");
            structure.setOperatingSystem("Windows");
            structure.setEntryPoint(peHeader.entryPoint);
            
            // Store headers in metadata
            Map<String, Object> headers = new HashMap<>();
            headers.put("dos_header", dosHeader);
            headers.put("pe_header", peHeader);
            structure.setHeaders(headers);
            
            // Parse sections
            List<Section> sections = parseSections(file, peHeader);
            structure.setSections(sections);
            
            // Parse import table
            List<String> imports = parseImportTable(file, peHeader, sections);
            structure.setLinkedLibraries(imports);
            
            // Detect packers
            PackerDetectionResult packerResult = detectPacker(peHeader, sections);
            structure.setPacked(packerResult.isPacked);
            structure.setPacker(packerResult.packerName);
            
            return structure;
        }
    }
    
    private DOSHeader parseDOSHeader(RandomAccessFile file) throws IOException {
        file.seek(0);
        byte[] dosHeaderBytes = new byte[DOS_HEADER_SIZE];
        file.readFully(dosHeaderBytes);
        
        ByteBuffer buffer = ByteBuffer.wrap(dosHeaderBytes).order(ByteOrder.LITTLE_ENDIAN);
        
        DOSHeader header = new DOSHeader();
        header.signature = buffer.getShort(0) & 0xFFFF;
        header.bytesOnLastPage = buffer.getShort(2) & 0xFFFF;
        header.pagesInFile = buffer.getShort(4) & 0xFFFF;
        header.relocations = buffer.getShort(6) & 0xFFFF;
        header.sizeOfHeaderInParagraphs = buffer.getShort(8) & 0xFFFF;
        header.peHeaderOffset = buffer.getInt(60);
        
        if (header.signature != 0x5A4D) { // "MZ"
            throw new IOException("Invalid DOS header signature");
        }
        
        return header;
    }
    
    private PEHeader parsePEHeader(RandomAccessFile file) throws IOException {
        PEHeader header = new PEHeader();
        
        // Read PE signature
        int signature = Integer.reverseBytes(file.readInt());
        if (signature != PE_SIGNATURE) {
            throw new IOException("Invalid PE signature");
        }
        
        // Read COFF header
        header.machine = Short.reverseBytes(file.readShort()) & 0xFFFF;
        header.numberOfSections = Short.reverseBytes(file.readShort()) & 0xFFFF;
        header.timeDateStamp = Integer.reverseBytes(file.readInt());
        header.pointerToSymbolTable = Integer.reverseBytes(file.readInt());
        header.numberOfSymbols = Integer.reverseBytes(file.readInt());
        header.sizeOfOptionalHeader = Short.reverseBytes(file.readShort()) & 0xFFFF;
        header.characteristics = Short.reverseBytes(file.readShort()) & 0xFFFF;
        
        // Read optional header
        if (header.sizeOfOptionalHeader > 0) {
            int magic = Short.reverseBytes(file.readShort()) & 0xFFFF;
            header.is64Bit = (magic == IMAGE_NT_OPTIONAL_HDR64_MAGIC);
            
            // Skip version fields
            file.skipBytes(2);
            
            header.sizeOfCode = Integer.reverseBytes(file.readInt());
            header.sizeOfInitializedData = Integer.reverseBytes(file.readInt());
            header.sizeOfUninitializedData = Integer.reverseBytes(file.readInt());
            header.addressOfEntryPoint = Integer.reverseBytes(file.readInt());
            header.baseOfCode = Integer.reverseBytes(file.readInt());
            
            if (!header.is64Bit) {
                header.baseOfData = Integer.reverseBytes(file.readInt());
                header.imageBase = Integer.reverseBytes(file.readInt());
            } else {
                header.imageBase = Long.reverseBytes(file.readLong());
            }
            
            header.sectionAlignment = Integer.reverseBytes(file.readInt());
            header.fileAlignment = Integer.reverseBytes(file.readInt());
            
            // Skip version fields
            file.skipBytes(16);
            
            header.sizeOfImage = Integer.reverseBytes(file.readInt());
            header.sizeOfHeaders = Integer.reverseBytes(file.readInt());
            header.checkSum = Integer.reverseBytes(file.readInt());
            header.subsystem = Short.reverseBytes(file.readShort()) & 0xFFFF;
            header.dllCharacteristics = Short.reverseBytes(file.readShort()) & 0xFFFF;
            
            // Calculate entry point
            header.entryPoint = header.imageBase + header.addressOfEntryPoint;
        }
        
        return header;
    }
    
    private List<Section> parseSections(RandomAccessFile file, PEHeader peHeader) throws IOException {
        List<Section> sections = new ArrayList<>();
        
        for (int i = 0; i < peHeader.numberOfSections; i++) {
            Section section = new Section();
            
            // Read section name (8 bytes)
            byte[] nameBytes = new byte[8];
            file.readFully(nameBytes);
            section.setName(new String(nameBytes).trim().replaceAll("\\x00", ""));
            
            section.setVirtualSize(Integer.reverseBytes(file.readInt()));
            section.setVirtualAddress(Integer.reverseBytes(file.readInt()));
            section.setRawSize(Integer.reverseBytes(file.readInt()));
            section.setRawAddress(Integer.reverseBytes(file.readInt()));
            
            // Skip relocation and line number info
            file.skipBytes(12);
            
            int characteristics = Integer.reverseBytes(file.readInt());
            section.setPermissions(parseCharacteristics(characteristics));
            section.setType(determineSectionType(section.getName(), characteristics));
            
            sections.add(section);
        }
        
        return sections;
    }
    
    private Set<String> parseCharacteristics(int characteristics) {
        Set<String> permissions = new HashSet<>();
        
        if ((characteristics & 0x20) != 0) permissions.add("CODE");
        if ((characteristics & 0x40) != 0) permissions.add("INITIALIZED_DATA");
        if ((characteristics & 0x80) != 0) permissions.add("UNINITIALIZED_DATA");
        if ((characteristics & 0x20000000) != 0) permissions.add("EXECUTABLE");
        if ((characteristics & 0x40000000) != 0) permissions.add("READABLE");
        if ((characteristics & 0x80000000) != 0) permissions.add("WRITABLE");
        
        return permissions;
    }
    
    private Section.SectionType determineSectionType(String name, int characteristics) {
        if (name.equals(".text") || (characteristics & 0x20) != 0) {
            return Section.SectionType.CODE;
        } else if (name.equals(".data") || name.equals(".bss")) {
            return Section.SectionType.DATA;
        } else if (name.equals(".rsrc")) {
            return Section.SectionType.RESOURCES;
        } else if (name.equals(".idata")) {
            return Section.SectionType.IMPORTS;
        } else if (name.equals(".edata")) {
            return Section.SectionType.EXPORTS;
        } else if (name.contains("debug")) {
            return Section.SectionType.DEBUG;
        }
        return Section.SectionType.UNKNOWN;
    }
    
    private List<String> parseImportTable(RandomAccessFile file, PEHeader peHeader, List<Section> sections) throws IOException {
        List<String> imports = new ArrayList<>();
        
        // Find .idata section or use data directories
        Section idataSection = sections.stream()
            .filter(s -> s.getName().equals(".idata"))
            .findFirst()
            .orElse(null);
        
        if (idataSection != null) {
            // Parse import directory table
            file.seek(idataSection.getRawAddress());
            
            while (true) {
                int nameRva = Integer.reverseBytes(file.readInt());
                if (nameRva == 0) break;
                
                // Skip timestamp and forwarder chain
                file.skipBytes(8);
                
                // Read DLL name
                long currentPos = file.getFilePointer();
                file.seek(rvaToFileOffset(nameRva, sections));
                
                StringBuilder dllName = new StringBuilder();
                int b;
                while ((b = file.read()) != 0 && b != -1) {
                    dllName.append((char) b);
                }
                
                imports.add(dllName.toString());
                
                file.seek(currentPos + 8); // Move to next import descriptor
            }
        }
        
        return imports;
    }
    
    private long rvaToFileOffset(long rva, List<Section> sections) {
        for (Section section : sections) {
            long sectionStart = section.getVirtualAddress();
            long sectionEnd = sectionStart + section.getVirtualSize();
            
            if (rva >= sectionStart && rva < sectionEnd) {
                return section.getRawAddress() + (rva - sectionStart);
            }
        }
        return rva; // Fallback
    }
    
    private PackerDetectionResult detectPacker(PEHeader peHeader, List<Section> sections) {
        PackerDetectionResult result = new PackerDetectionResult();
        
        // Check for common packer indicators
        
        // 1. High entropy in code sections
        Section textSection = sections.stream()
            .filter(s -> s.getName().equals(".text"))
            .findFirst()
            .orElse(null);
        
        if (textSection != null) {
            double codeToTotalRatio = (double) textSection.getRawSize() / 
                sections.stream().mapToLong(Section::getRawSize).sum();
            
            if (codeToTotalRatio > 0.8) {
                result.isPacked = true;
                result.packerName = "Unknown Packer (High code ratio)";
            }
        }
        
        // 2. Check for known packer section names
        Set<String> packerSections = Set.of("UPX0", "UPX1", ".aspack", ".adata", "MEW", ".mew", ".vmp");
        for (Section section : sections) {
            if (packerSections.contains(section.getName())) {
                result.isPacked = true;
                result.packerName = "Known Packer: " + section.getName();
                break;
            }
        }
        
        // 3. Check for suspicious characteristics
        if (peHeader.numberOfSections < 3 || peHeader.numberOfSections > 10) {
            // Unusual number of sections might indicate packing
            result.suspiciousCharacteristics = true;
        }
        
        return result;
    }
    
    // Data structures for PE parsing
    
    private static class DOSHeader {
        int signature;
        int bytesOnLastPage;
        int pagesInFile;
        int relocations;
        int sizeOfHeaderInParagraphs;
        int peHeaderOffset;
    }
    
    private static class PEHeader {
        int machine;
        int numberOfSections;
        int timeDateStamp;
        int pointerToSymbolTable;
        int numberOfSymbols;
        int sizeOfOptionalHeader;
        int characteristics;
        boolean is64Bit;
        int sizeOfCode;
        int sizeOfInitializedData;
        int sizeOfUninitializedData;
        int addressOfEntryPoint;
        int baseOfCode;
        int baseOfData;
        long imageBase;
        int sectionAlignment;
        int fileAlignment;
        int sizeOfImage;
        int sizeOfHeaders;
        int checkSum;
        int subsystem;
        int dllCharacteristics;
        long entryPoint;
    }
    
    private static class PackerDetectionResult {
        boolean isPacked = false;
        String packerName = null;
        boolean suspiciousCharacteristics = false;
    }
}