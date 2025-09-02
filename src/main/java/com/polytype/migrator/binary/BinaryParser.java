package com.polytype.migrator.binary;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for parsing different binary formats.
 */
public interface BinaryParser {
    
    /**
     * Parse the structure of a binary file.
     * 
     * @param binaryFile Path to the binary file
     * @return BinaryStructure containing parsed information
     * @throws IOException if file cannot be read or parsed
     */
    BinaryStructure parseStructure(Path binaryFile) throws IOException;
}