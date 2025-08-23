package com.davajava.migrator.core;

public class ParseException extends Exception {
    private final int lineNumber;
    private final int columnNumber;

    public ParseException(String message) {
        super(message);
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    public ParseException(String message, int lineNumber, int columnNumber) {
        super(String.format("Parse error at line %d, column %d: %s", lineNumber, columnNumber, message));
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }
}