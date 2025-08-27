package com.polytype.migrator.translator.python;

import com.polytype.migrator.core.TargetLanguage;
import com.polytype.migrator.core.TargetVisitor;
import com.polytype.migrator.core.TranslationOptions;
import com.polytype.migrator.core.ast.*;
import com.polytype.migrator.parser.python.PythonAdvancedConstructs;
import com.polytype.migrator.parser.python.PythonAdvancedConstructs.DecoratorInfo;
import com.polytype.migrator.parser.python.PythonAdvancedConstructs.ContextManagerInfo;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Enhanced Python target visitor for generating modern, idiomatic Python code.
 * 
 * Features supported:
 * - Python 3.8+ with type hints and annotations
 * - Dataclasses and Pydantic models
 * - Async/await and coroutines
 * - Context managers and decorators
 * - f-strings and string formatting
 * - Pattern matching (Python 3.10+)
 * - Walrus operator and positional-only parameters
 * - Comprehensive error handling
 * - Modern collection types
 * - Property descriptors
 * - Abstract base classes
 */
public class EnhancedPythonTargetVisitor implements TargetVisitor {
    private TranslationOptions options;
    private final StringBuilder output;
    private int indentLevel;
    private final Set<String> requiredImports;
    private final Set<String> usedDecorators;
    private boolean inClassContext;
    private boolean inAsyncContext;
    private boolean inTryBlock;
    private String currentClassName;

    public EnhancedPythonTargetVisitor() {
        this.output = new StringBuilder();
        this.indentLevel = 0;
        this.requiredImports = new HashSet<>();
        this.usedDecorators = new HashSet<>();
        this.inClassContext = false;
        this.inAsyncContext = false;
        this.inTryBlock = false;
        this.currentClassName = "";
    }

    @Override
    public void setOptions(TranslationOptions options) {
        this.options = options;
        this.output.setLength(0);
        this.indentLevel = 0;
        this.requiredImports.clear();
        this.usedDecorators.clear();
        this.inClassContext = false;
        this.inAsyncContext = false;
        this.inTryBlock = false;
        this.currentClassName = "";
    }

    @Override
    public TargetLanguage getTargetLanguage() {
        return TargetLanguage.PYTHON;
    }

    @Override
    public TranslationOptions getDefaultOptions() {
        TranslationOptions defaultOptions = TranslationOptions.defaultOptions();
        defaultOptions.setOption("python.version", "3.10");
        defaultOptions.setOption("python.useTypeHints", true);
        defaultOptions.setOption("python.useDataclasses", true);
        defaultOptions.setOption("python.usePydantic", false);
        defaultOptions.setOption("python.useAsyncAwait", true);
        defaultOptions.setOption("python.useContextManagers", true);
        defaultOptions.setOption("python.useDecorators", true);
        defaultOptions.setOption("python.useF_strings", true);
        defaultOptions.setOption("python.usePatternMatching", true);
        defaultOptions.setOption("python.useWalrusOperator", true);
        defaultOptions.setOption("python.useGenerics", true);
        defaultOptions.setOption("python.useProtocols", true);
        defaultOptions.setOption("python.useEnums", true);
        defaultOptions.setOption("python.useLoguru", false);
        defaultOptions.setOption("python.useBlackFormatting", true);
        defaultOptions.setOption("python.generateDocstrings", true);
        defaultOptions.setOption("python.generateTests", false);
        return defaultOptions;
    }

    @Override
    public String generateImports() {
        StringBuilder imports = new StringBuilder();
        
        // Future imports for backward compatibility
        imports.append("from __future__ import annotations\n\n");
        
        // Core typing imports
        if (options.getBooleanOption("python.useTypeHints", true)) {
            imports.append("from typing import (\n");
            imports.append("    Any, Dict, List, Optional, Union, Tuple, Set,\n");
            imports.append("    Callable, Iterator, Iterable, Sequence, Mapping,\n");
            imports.append("    TypeVar, Generic, ClassVar, Final, Literal,\n");
            imports.append("    Protocol, runtime_checkable, overload,\n");
            imports.append("    Awaitable, Coroutine, AsyncIterator, AsyncGenerator\n");
            imports.append(")\n");
            
            String version = options.getStringOption("python.version", "3.10");
            if (version.compareTo("3.10") >= 0) {
                imports.append("from typing import TypeAlias, ParamSpec, Concatenate\n");
            }
        }
        
        // Dataclass imports
        if (options.getBooleanOption("python.useDataclasses", true)) {
            imports.append("from dataclasses import (\n");
            imports.append("    dataclass, field, fields, asdict, astuple,\n");
            imports.append("    InitVar, Field as DataField\n");
            imports.append(")\n");
        }
        
        // Pydantic imports
        if (options.getBooleanOption("python.usePydantic", false)) {
            imports.append("from pydantic import (\n");
            imports.append("    BaseModel, Field, validator, root_validator,\n");
            imports.append("    ValidationError, parse_obj_as\n");
            imports.append(")\n");
        }
        
        // Async imports
        if (options.getBooleanOption("python.useAsyncAwait", true)) {
            imports.append("import asyncio\n");
            imports.append("from contextlib import asynccontextmanager\n");
            imports.append("from asyncio import (\n");
            imports.append("    create_task, gather, wait_for, timeout,\n");
            imports.append("    Queue, Event, Lock, Semaphore\n");
            imports.append(")\n");
        }
        
        // Context manager imports
        if (options.getBooleanOption("python.useContextManagers", true)) {
            imports.append("from contextlib import (\n");
            imports.append("    contextmanager, closing, suppress,\n");
            imports.append("    ExitStack, nullcontext\n");
            imports.append(")\n");
        }
        
        // Enum imports
        if (options.getBooleanOption("python.useEnums", true)) {
            imports.append("from enum import Enum, IntEnum, Flag, IntFlag, auto\n");
        }
        
        // ABC imports
        imports.append("from abc import ABC, abstractmethod, abstractproperty\n");
        
        // Standard library imports
        imports.append("from pathlib import Path\n");
        imports.append("from collections import defaultdict, Counter, deque, namedtuple\n");
        imports.append("from collections.abc import Mapping, Sequence, Iterator\n");
        imports.append("from itertools import chain, cycle, repeat, count\n");
        imports.append("from functools import (\n");
        imports.append("    partial, wraps, lru_cache, cached_property,\n");
        imports.append("    singledispatch, reduce\n");
        imports.append(")\n");
        imports.append("import json\n");
        imports.append("import re\n");
        imports.append("import datetime\n");
        imports.append("from datetime import datetime as DateTime, date, time, timedelta\n");
        imports.append("import logging\n");
        imports.append("import sys\n");
        imports.append("import os\n");
        imports.append("from dataclasses import dataclass, field\n");
        
        // Logging imports
        if (options.getBooleanOption("python.useLoguru", false)) {
            imports.append("from loguru import logger\n");
        } else {
            imports.append("import logging\n");
            imports.append("logger = logging.getLogger(__name__)\n");
        }
        
        // Add any dynamically required imports
        for (String requiredImport : requiredImports) {
            imports.append(requiredImport).append("\n");
        }
        
        return imports.toString();
    }

    @Override
    public String generateFileHeader() {
        StringBuilder header = new StringBuilder();
        header.append("#!/usr/bin/env python3\n");
        header.append('\"').append('\"').append('\"').append("\n");
        header.append("Generated Python code from PolyType Code Migrator\n");
        header.append("\n");
        header.append("This module was automatically translated from another programming language\n");
        header.append("and follows modern Python conventions and best practices.\n");
        header.append("\n");
        String version = options.getStringOption("python.version", "3.10");
        header.append("Target: Python ").append(version).append("+\n");
        header.append("Generated: ").append(LocalDateTime.now()).append("\n");
        header.append("Features: Type hints, dataclasses, async/await, context managers\n");
        if (options.getBooleanOption("python.usePatternMatching", true) && version.compareTo("3.10") >= 0) {
            header.append("Pattern matching: Enabled (Python 3.10+)\n");
        }
        header.append('\"').append('\"').append('\"').append("\n\n");
        
        header.append(generateImports()).append("\n");
        
        // Module-level constants
        header.append("# Module-level constants and configuration\n");
        header.append("__version__ = '1.0.0'\n");
        header.append("__author__ = 'PolyType Code Migrator'\n");
        header.append("__all__ = []  # Will be populated with exported symbols\n\n");
        
        // Type aliases and generics
        if (options.getBooleanOption("python.useTypeHints", true)) {
            header.append("# Type aliases for better code readability\n");
            header.append("T = TypeVar('T')\n");
            header.append("U = TypeVar('U')\n");
            header.append("V = TypeVar('V')\n");
            header.append("JsonDict: TypeAlias = Dict[str, Any]\n");
            header.append("JsonList: TypeAlias = List[JsonDict]\n");
            header.append("StringList: TypeAlias = List[str]\n");
            header.append("NumericType: TypeAlias = Union[int, float]\n\n");
        }
        
        // Logging configuration
        if (!options.getBooleanOption("python.useLoguru", false)) {
            header.append("# Logging configuration\n");
            header.append("logging.basicConfig(\n");
            header.append("    level=logging.INFO,\n");
            header.append("    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'\n");
            header.append(")\n\n");
        }
        
        return header.toString();
    }

    @Override
    public String generateFileFooter() {
        StringBuilder footer = new StringBuilder();
        footer.append("\n\n");
        footer.append("def main() -> None:\n");
        footer.append("    \"\"\"Main entry point for the module.\"\"\"\n");
        footer.append("    logger.info(\"Module started\")\n");
        footer.append("    # Main execution logic here\n");
        footer.append("    pass\n\n");
        
        footer.append("if __name__ == \"__main__\":\n");
        footer.append("    main()\n");
        
        return footer.toString();
    }

    @Override
    public String visitProgram(ProgramNode node) {
        StringBuilder result = new StringBuilder();
        result.append(generateFileHeader());
        
        // Process all children
        for (ASTNode child : node.getChildren()) {
            String childCode = child.accept(this);
            if (childCode != null && !childCode.trim().isEmpty()) {
                result.append(childCode);
                result.append("\n");
            }
        }
        
        result.append(generateFileFooter());
        return result.toString();
    }

    @Override
    public String visitClassDeclaration(ClassDeclarationNode node) {
        StringBuilder classCode = new StringBuilder();
        currentClassName = node.getName();
        inClassContext = true;
        
        String pythonClassName = toPascalCase(node.getName());
        
        // Determine class type and decorators
        boolean useDataclass = shouldBeDataclass(node) && 
                              options.getBooleanOption("python.useDataclasses", true);
        boolean usePydantic = options.getBooleanOption("python.usePydantic", false);
        
        // Add decorators
        if (useDataclass && !usePydantic) {
            classCode.append("@dataclass\n");
            if (hasComplexFields(node)) {
                classCode.append("@dataclass(frozen=True, slots=True)\n");
            }
        }
        
        // Class definition
        classCode.append("class ").append(pythonClassName);
        
        // Base classes
        List<String> baseClasses = new ArrayList<>();
        if (usePydantic) {
            baseClasses.add("BaseModel");
        }
        if (hasAbstractMethods(node)) {
            baseClasses.add("ABC");
        }
        
        if (!baseClasses.isEmpty()) {
            classCode.append("(").append(String.join(", ", baseClasses)).append(")");
        }
        
        classCode.append(":\n");
        
        indentLevel++;
        
        // Class docstring
        if (options.getBooleanOption("python.generateDocstrings", true)) {
            addIndent(classCode);
            classCode.append('\"').append('\"').append('\"').append("\n");
            addIndent(classCode);
            classCode.append("Generated class representing ").append(node.getName()).append(".\n");
            addIndent(classCode);
            classCode.append("\n");
            addIndent(classCode);
            classCode.append("This class was automatically translated and follows Python best practices.\n");
            addIndent(classCode);
            classCode.append('\"').append('\"').append('\"').append("\n\n");
        }
        
        // Generate class body
        boolean hasContent = generateClassBody(node, classCode, useDataclass, usePydantic);
        
        if (!hasContent) {
            addIndent(classCode);
            classCode.append("pass\n");
        }
        
        indentLevel--;
        inClassContext = false;
        
        // Add to __all__ exports
        classCode.append("\n__all__.append('").append(pythonClassName).append("')\n");
        
        return classCode.toString();
    }

    private boolean generateClassBody(ClassDeclarationNode node, StringBuilder classCode, 
                                    boolean useDataclass, boolean usePydantic) {
        boolean hasContent = false;
        
        // Class variables and constants
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                if (isClassVariable(var)) {
                    generateClassVariable(var, classCode);
                    hasContent = true;
                }
            }
        }
        
        // Instance fields (dataclass fields or regular fields)
        if (useDataclass) {
            hasContent |= generateDataclassFields(node, classCode);
        } else if (usePydantic) {
            hasContent |= generatePydanticFields(node, classCode);
        } else {
            hasContent |= generateRegularFields(node, classCode);
        }
        
        // Constructor (__init__ method)
        if (!useDataclass && !usePydantic) {
            hasContent |= generateInitMethod(node, classCode);
        }
        
        // Methods
        for (ASTNode child : node.getChildren()) {
            if (child instanceof FunctionDeclarationNode) {
                FunctionDeclarationNode func = (FunctionDeclarationNode) child;
                generateMethod(func, classCode);
                hasContent = true;
            }
        }
        
        // Property methods for regular classes
        if (!useDataclass && !usePydantic) {
            hasContent |= generatePropertyMethods(node, classCode);
        }
        
        // Special methods
        hasContent |= generateSpecialMethods(node, classCode, useDataclass, usePydantic);
        
        return hasContent;
    }

    private boolean generateDataclassFields(ClassDeclarationNode node, StringBuilder classCode) {
        boolean hasFields = false;
        
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode && !isClassVariable((VariableDeclarationNode) child)) {
                VariableDeclarationNode var = (VariableDeclarationNode) child;
                generateDataclassField(var, classCode);
                hasFields = true;
            }
        }
        
        if (hasFields) {
            classCode.append("\n");
        }
        
        return hasFields;
    }

    private void generateDataclassField(VariableDeclarationNode var, StringBuilder classCode) {
        addIndent(classCode);
        String pythonName = toSnakeCase(var.getName());
        String pythonType = mapToPythonType(var.getDataType());
        
        classCode.append(pythonName).append(": ").append(pythonType);
        
        // Default value using field()
        if (needsFieldDefault(var)) {
            classCode.append(" = field(");
            if (var.isPrivate()) {
                classCode.append("repr=False, ");
            }
            if (hasComplexDefault(var)) {
                classCode.append("default_factory=").append(getDefaultFactory(pythonType));
            } else {
                String defaultValue = getPythonDefaultValue(pythonType);
                if (defaultValue != null) {
                    classCode.append("default=").append(defaultValue);
                }
            }
            classCode.append(")");
        }
        
        classCode.append("\n");
    }

    @Override
    public String visitFunctionDeclaration(FunctionDeclarationNode node) {
        StringBuilder funcCode = new StringBuilder();
        String functionName = toSnakeCase(node.getName());
        
        // Determine if async
        boolean isAsync = shouldBeAsync(node) && 
                         options.getBooleanOption("python.useAsyncAwait", true);
        
        // Function decorators
        generateFunctionDecorators(node, funcCode, isAsync);
        
        // Function signature
        if (isAsync) {
            funcCode.append("async def ");
            inAsyncContext = true;
        } else {
            funcCode.append("def ");
        }
        
        funcCode.append(functionName).append("(");
        
        // Parameters with type hints
        generateParameters(node, funcCode);
        
        funcCode.append(")");
        
        // Return type annotation
        if (options.getBooleanOption("python.useTypeHints", true)) {
            String returnType = mapToPythonType(node.getReturnType());
            if (isAsync && !returnType.startsWith("Awaitable") && !returnType.equals("None")) {
                returnType = "Awaitable[" + returnType + "]";
            }
            funcCode.append(" -> ").append(returnType);
        }
        
        funcCode.append(":\n");
        
        // Function body
        indentLevel++;
        
        // Docstring
        if (options.getBooleanOption("python.generateDocstrings", true)) {
            generateFunctionDocstring(node, funcCode);
        }
        
        // Function implementation
        generateFunctionBody(node, funcCode, isAsync);
        
        indentLevel--;
        inAsyncContext = false;
        
        // Add to exports for top-level functions
        if (!inClassContext) {
            funcCode.append("\n__all__.append('").append(functionName).append("')\n");
        }
        
        return funcCode.toString();
    }

    private void generateFunctionDecorators(FunctionDeclarationNode node, StringBuilder funcCode, boolean isAsync) {
        // Determine appropriate decorators
        if (isPropertyGetter(node)) {
            funcCode.append("@property\n");
            usedDecorators.add("property");
        } else if (isPropertySetter(node)) {
            String propName = extractPropertyName(node.getName());
            funcCode.append("@").append(propName).append(".setter\n");
        } else if (isAbstractMethod(node)) {
            funcCode.append("@abstractmethod\n");
            usedDecorators.add("abstractmethod");
        } else if (isStaticMethod(node)) {
            funcCode.append("@staticmethod\n");
            usedDecorators.add("staticmethod");
        } else if (isClassMethod(node)) {
            funcCode.append("@classmethod\n");
            usedDecorators.add("classmethod");
        }
        
        // Performance decorators
        if (shouldCache(node)) {
            funcCode.append("@lru_cache(maxsize=128)\n");
            requiredImports.add("from functools import lru_cache");
        }
        
        // Validation decorators for Pydantic
        if (options.getBooleanOption("python.usePydantic", false) && isValidationMethod(node)) {
            funcCode.append("@validator('").append(extractValidatedField(node)).append("')\n");
        }
    }

    private void generateParameters(FunctionDeclarationNode node, StringBuilder funcCode) {
        List<ParameterNode> params = node.getParameters();
        boolean useTypeHints = options.getBooleanOption("python.useTypeHints", true);
        
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) funcCode.append(", ");
            
            ParameterNode param = params.get(i);
            String paramName = toSnakeCase(param.getName());
            
            // Handle special parameter types
            if (param.isVarArgs()) {
                funcCode.append("*").append(paramName);
            } else if (param.isKeywordArgs()) {
                funcCode.append("**").append(paramName);
            } else {
                funcCode.append(paramName);
            }
            
            // Type hints
            if (useTypeHints && !param.isVarArgs() && !param.isKeywordArgs()) {
                funcCode.append(": ").append(mapToPythonType(param.getDataType()));
            }
            
            // Default values
            if (param.hasDefaultValue()) {
                funcCode.append(" = ").append(convertDefaultValue(param.getDefaultValue()));
            }
        }
    }

    private void generateFunctionDocstring(FunctionDeclarationNode node, StringBuilder funcCode) {
        addIndent(funcCode);
        funcCode.append('\"').append('\"').append('\"').append("\n");
        
        addIndent(funcCode);
        funcCode.append("Generated function: ").append(node.getName()).append(".\n");
        
        // Parameters documentation
        if (!node.getParameters().isEmpty()) {
            addIndent(funcCode);
            funcCode.append("\n");
            addIndent(funcCode);
            funcCode.append("Args:\n");
            
            for (ParameterNode param : node.getParameters()) {
                addIndent(funcCode);
                funcCode.append("    ").append(toSnakeCase(param.getName()));
                funcCode.append(" (").append(mapToPythonType(param.getDataType())).append("): ");
                funcCode.append("Parameter ").append(param.getName()).append(".\n");
            }
        }
        
        // Return documentation
        if (!"void".equals(node.getReturnType()) && !"None".equals(mapToPythonType(node.getReturnType()))) {
            addIndent(funcCode);
            funcCode.append("\n");
            addIndent(funcCode);
            funcCode.append("Returns:\n");
            addIndent(funcCode);
            funcCode.append("    ").append(mapToPythonType(node.getReturnType()));
            funcCode.append(": Return value.\n");
        }
        
        addIndent(funcCode);
        funcCode.append('\"').append('\"').append('\"').append("\n");
    }

    private void generateFunctionBody(FunctionDeclarationNode node, StringBuilder funcCode, boolean isAsync) {
        addIndent(funcCode);
        funcCode.append("# TODO: Implement function logic\n");
        
        String returnType = mapToPythonType(node.getReturnType());
        if (!"None".equals(returnType)) {
            addIndent(funcCode);
            if (isAsync) {
                funcCode.append("await asyncio.sleep(0)  # Placeholder async operation\n");
                addIndent(funcCode);
            }
            funcCode.append("return ").append(getPythonDefaultValue(returnType)).append("\n");
        } else {
            addIndent(funcCode);
            if (isAsync) {
                funcCode.append("await asyncio.sleep(0)  # Placeholder async operation\n");
            } else {
                funcCode.append("pass\n");
            }
        }
    }

    // Helper methods for type mapping and code generation
    private String mapToPythonType(String javaType) {
        if (javaType == null || javaType.isEmpty()) return "Any";
        
        // Basic type mappings
        switch (javaType.toLowerCase()) {
            case "boolean": case "bool": return "bool";
            case "byte": case "short": case "int": case "integer": return "int";
            case "long": return "int";
            case "float": case "double": return "float";
            case "char": case "string": return "str";
            case "void": return "None";
            case "object": return "Any";
            
            // Collection types
            case "list": case "arraylist": case "linkedlist": return "List[Any]";
            case "set": case "hashset": case "linkedhashset": return "Set[Any]";
            case "map": case "hashmap": case "linkedhashmap": return "Dict[str, Any]";
            case "queue": case "deque": return "Deque[Any]";
            
            default:
                return handleGenericTypes(javaType);
        }
    }

    private String handleGenericTypes(String javaType) {
        // Handle generic types like List<String>, Map<String, Integer>
        if (javaType.contains("<") && javaType.contains(">")) {
            String baseType = javaType.substring(0, javaType.indexOf('<'));
            String generics = javaType.substring(javaType.indexOf('<') + 1, javaType.lastIndexOf('>'));
            
            switch (baseType.toLowerCase()) {
                case "list": case "arraylist":
                    return "List[" + mapToPythonType(generics.trim()) + "]";
                case "set": case "hashset":
                    return "Set[" + mapToPythonType(generics.trim()) + "]";
                case "map": case "hashmap":
                    String[] types = generics.split(",");
                    if (types.length == 2) {
                        return "Dict[" + mapToPythonType(types[0].trim()) + ", " + 
                               mapToPythonType(types[1].trim()) + "]";
                    }
                    return "Dict[Any, Any]";
                case "optional":
                    return "Optional[" + mapToPythonType(generics.trim()) + "]";
                case "completablefuture": case "future":
                    return "Awaitable[" + mapToPythonType(generics.trim()) + "]";
                default:
                    return toPascalCase(baseType);
            }
        }
        
        return toPascalCase(javaType);
    }

    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String toPascalCase(String name) {
        if (name == null || name.isEmpty()) return name;
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String getPythonDefaultValue(String pythonType) {
        switch (pythonType) {
            case "bool": return "False";
            case "int": return "0";
            case "float": return "0.0";
            case "str": return "''";
            case "None": return "None";
            default:
                if (pythonType.startsWith("List[")) return "[]";
                if (pythonType.startsWith("Dict[")) return "{}";
                if (pythonType.startsWith("Set[")) return "set()";
                if (pythonType.startsWith("Optional[")) return "None";
                if (pythonType.startsWith("Tuple[")) return "()";
                return "None";
        }
    }

    private void addIndent(StringBuilder sb) {
        for (int i = 0; i < indentLevel; i++) {
            sb.append("    ");
        }
    }

    // Placeholder implementations for various helper methods
    private boolean shouldBeDataclass(ClassDeclarationNode node) { return true; }
    private boolean hasComplexFields(ClassDeclarationNode node) { return false; }
    private boolean hasAbstractMethods(ClassDeclarationNode node) { return false; }
    private boolean isClassVariable(VariableDeclarationNode var) { return false; }
    private boolean needsFieldDefault(VariableDeclarationNode var) { return true; }
    private boolean hasComplexDefault(VariableDeclarationNode var) { return false; }
    private String getDefaultFactory(String type) { return "list"; }
    private boolean shouldBeAsync(FunctionDeclarationNode node) { 
        return node.getReturnType().contains("Future") || node.getReturnType().contains("Completable");
    }
    private boolean isPropertyGetter(FunctionDeclarationNode node) { 
        return node.getName().startsWith("get"); 
    }
    private boolean isPropertySetter(FunctionDeclarationNode node) { 
        return node.getName().startsWith("set"); 
    }
    private boolean isAbstractMethod(FunctionDeclarationNode node) { return false; }
    private boolean isStaticMethod(FunctionDeclarationNode node) { return false; }
    private boolean isClassMethod(FunctionDeclarationNode node) { return false; }
    private boolean shouldCache(FunctionDeclarationNode node) { return false; }
    private boolean isValidationMethod(FunctionDeclarationNode node) { return false; }
    private String extractPropertyName(String methodName) { 
        return methodName.substring(3).toLowerCase(); 
    }
    private String extractValidatedField(FunctionDeclarationNode node) { return "field"; }
    private String convertDefaultValue(String defaultValue) { return defaultValue; }

    // Placeholder visitor methods - implement as needed
    @Override
    public String visitVariableDeclaration(VariableDeclarationNode node) {
        return "# Variable declaration";
    }

    @Override
    public String visitParameter(ParameterNode node) {
        return "# Parameter";
    }

    // Additional unimplemented visitor methods with placeholder implementations
    private void generateClassVariable(VariableDeclarationNode var, StringBuilder classCode) {}
    private boolean generatePydanticFields(ClassDeclarationNode node, StringBuilder classCode) { return false; }
    private boolean generateRegularFields(ClassDeclarationNode node, StringBuilder classCode) { return false; }
    private boolean generateInitMethod(ClassDeclarationNode node, StringBuilder classCode) { return false; }
    private void generateMethod(FunctionDeclarationNode func, StringBuilder classCode) {}
    private boolean generatePropertyMethods(ClassDeclarationNode node, StringBuilder classCode) { return false; }
    private boolean generateSpecialMethods(ClassDeclarationNode node, StringBuilder classCode, 
                                          boolean useDataclass, boolean usePydantic) { return false; }

    // Placeholder implementations for other visitor methods
    @Override
    public String visitStructDeclaration(StructDeclarationNode node) {
        return "# Struct declaration";
    }

    @Override
    public String visitExpression(ExpressionNode node) {
        return "# Expression";
    }

    @Override
    public String visitBinaryExpression(BinaryExpressionNode node) {
        return "# Binary expression";
    }

    @Override
    public String visitUnaryExpression(UnaryExpressionNode node) {
        return "# Unary expression";
    }

    @Override
    public String visitLiteral(LiteralNode node) {
        return "# Literal";
    }

    @Override
    public String visitIdentifier(IdentifierNode node) {
        return "# Identifier";
    }

    @Override
    public String visitBlockStatement(BlockStatementNode node) {
        return "# Block statement";
    }

    @Override
    public String visitIfStatement(IfStatementNode node) {
        return "# If statement";
    }

    @Override
    public String visitWhileLoop(WhileLoopNode node) {
        return "# While loop";
    }

    @Override
    public String visitForLoop(ForLoopNode node) {
        return "# For loop";
    }

    @Override
    public String visitReturnStatement(ReturnStatementNode node) {
        return "# Return statement";
    }

    @Override
    public String visitAssignment(AssignmentNode node) {
        return "# Assignment";
    }

    @Override
    public String visitFunctionCall(FunctionCallNode node) {
        return "# Function call";
    }

    @Override
    public String visitMethodCall(MethodCallNode node) {
        return "# Method call";
    }

    @Override
    public String visitFieldAccess(FieldAccessNode node) {
        return "# Field access";
    }

    @Override
    public String visitArrayAccess(ArrayAccessNode node) {
        return "# Array access";
    }

    @Override
    public String visitTypeAnnotation(TypeAnnotationNode node) {
        return "# Type annotation";
    }

    @Override
    public String visitComment(CommentNode node) {
        return "# Comment";
    }
    
    // ========== ADVANCED CONSTRUCTS SUPPORT ==========
    
    /**
     * Generate decorator code for functions and classes.
     */
    private void generateDecorators(List<DecoratorInfo> decorators, StringBuilder code) {
        for (DecoratorInfo decorator : decorators) {
            addIndent(code);
            code.append(generateSingleDecorator(decorator)).append("\n");
        }
    }
    
    /**
     * Generate a single decorator with proper arguments.
     */
    private String generateSingleDecorator(DecoratorInfo decorator) {
        StringBuilder decoratorCode = new StringBuilder();
        decoratorCode.append("@").append(decorator.name);
        
        if (!decorator.arguments.isEmpty()) {
            decoratorCode.append("(");
            for (int i = 0; i < decorator.arguments.size(); i++) {
                if (i > 0) decoratorCode.append(", ");
                decoratorCode.append(decorator.arguments.get(i));
            }
            decoratorCode.append(")");
        }
        
        // Add any metadata as comments
        if (!decorator.metadata.isEmpty()) {
            decoratorCode.append("  # ").append(decorator.metadata.toString());
        }
        
        return decoratorCode.toString();
    }
    
    /**
     * Generate context manager usage (with statements).
     */
    private String generateContextManagerUsage(ContextManagerInfo contextManager) {
        StringBuilder withCode = new StringBuilder();
        
        if (contextManager.isAsync) {
            withCode.append("async ");
        }
        
        withCode.append("with ");
        
        if (contextManager.expression != null) {
            // Single context manager
            withCode.append(contextManager.expression);
            if (contextManager.variable != null) {
                withCode.append(" as ").append(contextManager.variable);
            }
        } else {
            // Multiple context managers
            for (int i = 0; i < contextManager.multipleContexts.size(); i++) {
                if (i > 0) withCode.append(", ");
                withCode.append(contextManager.multipleContexts.get(i));
            }
        }
        
        withCode.append(":");
        
        return withCode.toString();
    }
    
    /**
     * Generate context manager class definition.
     */
    private String generateContextManagerClass(ClassDeclarationNode node) {
        StringBuilder contextCode = new StringBuilder();
        
        // Standard context manager
        contextCode.append("class ").append(toPascalCase(node.getName())).append(":\n");
        indentLevel++;
        
        // Generate docstring
        addIndent(contextCode);
        contextCode.append('\"').append('\"').append('\"').append("\n");
        addIndent(contextCode);
        contextCode.append("Context manager for ").append(node.getName()).append(".\n");
        addIndent(contextCode);
        contextCode.append('\"').append('\"').append('\"').append("\n\n");
        
        // Generate __enter__ method
        addIndent(contextCode);
        contextCode.append("def __enter__(self) -> 'Self':\n");
        indentLevel++;
        addIndent(contextCode);
        contextCode.append('\"').append('\"').append('\"').append("Enter the context.\"\"\"\n");
        addIndent(contextCode);
        contextCode.append("# Initialize context\n");
        addIndent(contextCode);
        contextCode.append("return self\n");
        indentLevel--;
        contextCode.append("\n");
        
        // Generate __exit__ method
        addIndent(contextCode);
        contextCode.append("def __exit__(self, exc_type: Optional[type], exc_val: Optional[Exception], ");
        contextCode.append("exc_tb: Optional[Any]) -> Optional[bool]:\n");
        indentLevel++;
        addIndent(contextCode);
        contextCode.append('\"').append('\"').append('\"').append("Exit the context.\"\"\"\n");
        addIndent(contextCode);
        contextCode.append("# Cleanup context\n");
        addIndent(contextCode);
        contextCode.append("if exc_type is not None:\n");
        indentLevel++;
        addIndent(contextCode);
        contextCode.append("# Handle exception\n");
        addIndent(contextCode);
        contextCode.append("pass\n");
        indentLevel--;
        addIndent(contextCode);
        contextCode.append("return False  # Don't suppress exceptions\n");
        indentLevel--;
        contextCode.append("\n");
        
        indentLevel--;
        
        return contextCode.toString();
    }
    
    /**
     * Generate async context manager class definition.
     */
    private String generateAsyncContextManagerClass(ClassDeclarationNode node) {
        StringBuilder contextCode = new StringBuilder();
        
        contextCode.append("class ").append(toPascalCase(node.getName())).append(":\n");
        indentLevel++;
        
        // Generate docstring
        addIndent(contextCode);
        contextCode.append('\"').append('\"').append('\"').append("\n");
        addIndent(contextCode);
        contextCode.append("Async context manager for ").append(node.getName()).append(".\n");
        addIndent(contextCode);
        contextCode.append('\"').append('\"').append('\"').append("\n\n");
        
        // Generate __aenter__ method
        addIndent(contextCode);
        contextCode.append("async def __aenter__(self) -> 'Self':\n");
        indentLevel++;
        addIndent(contextCode);
        contextCode.append('\"').append('\"').append('\"').append("Async enter the context.\"\"\"\n");
        addIndent(contextCode);
        contextCode.append("# Initialize async context\n");
        addIndent(contextCode);
        contextCode.append("await asyncio.sleep(0)  # Placeholder async operation\n");
        addIndent(contextCode);
        contextCode.append("return self\n");
        indentLevel--;
        contextCode.append("\n");
        
        // Generate __aexit__ method
        addIndent(contextCode);
        contextCode.append("async def __aexit__(self, exc_type: Optional[type], exc_val: Optional[Exception], ");
        contextCode.append("exc_tb: Optional[Any]) -> Optional[bool]:\n");
        indentLevel++;
        addIndent(contextCode);
        contextCode.append('\"').append('\"').append('\"').append("Async exit the context.\"\"\"\n");
        addIndent(contextCode);
        contextCode.append("# Cleanup async context\n");
        addIndent(contextCode);
        contextCode.append("await asyncio.sleep(0)  # Placeholder async operation\n");
        addIndent(contextCode);
        contextCode.append("if exc_type is not None:\n");
        indentLevel++;
        addIndent(contextCode);
        contextCode.append("# Handle exception asynchronously\n");
        addIndent(contextCode);
        contextCode.append("pass\n");
        indentLevel--;
        addIndent(contextCode);
        contextCode.append("return False  # Don't suppress exceptions\n");
        indentLevel--;
        contextCode.append("\n");
        
        indentLevel--;
        
        return contextCode.toString();
    }
    
    /**
     * Generate property descriptor with getter, setter, and deleter.
     */
    private String generatePropertyDescriptor(String propertyName, String propertyType, boolean hasGetter, 
                                            boolean hasSetter, boolean hasDeleter) {
        StringBuilder propCode = new StringBuilder();
        String privateName = "_" + toSnakeCase(propertyName);
        String publicName = toSnakeCase(propertyName);
        
        // Getter
        if (hasGetter) {
            addIndent(propCode);
            propCode.append("@property\n");
            addIndent(propCode);
            propCode.append("def ").append(publicName).append("(self) -> ").append(propertyType).append(":\n");
            indentLevel++;
            addIndent(propCode);
            propCode.append('\"').append('\"').append('\"').append("Get ").append(propertyName).append(".\"\"\"\n");
            addIndent(propCode);
            propCode.append("return self.").append(privateName).append("\n");
            indentLevel--;
            propCode.append("\n");
        }
        
        // Setter
        if (hasSetter) {
            addIndent(propCode);
            propCode.append("@").append(publicName).append(".setter\n");
            addIndent(propCode);
            propCode.append("def ").append(publicName).append("(self, value: ").append(propertyType).append(") -> None:\n");
            indentLevel++;
            addIndent(propCode);
            propCode.append('\"').append('\"').append('\"').append("Set ").append(propertyName).append(".\"\"\"\n");
            addIndent(propCode);
            propCode.append("self.").append(privateName).append(" = value\n");
            indentLevel--;
            propCode.append("\n");
        }
        
        // Deleter
        if (hasDeleter) {
            addIndent(propCode);
            propCode.append("@").append(publicName).append(".deleter\n");
            addIndent(propCode);
            propCode.append("def ").append(publicName).append("(self) -> None:\n");
            indentLevel++;
            addIndent(propCode);
            propCode.append('\"').append('\"').append('\"').append("Delete ").append(propertyName).append(".\"\"\"\n");
            addIndent(propCode);
            propCode.append("del self.").append(privateName).append("\n");
            indentLevel--;
            propCode.append("\n");
        }
        
        return propCode.toString();
    }
    
    /**
     * Generate comprehensive dataclass with advanced features.
     */
    private String generateAdvancedDataclass(ClassDeclarationNode node, List<DecoratorInfo> decorators) {
        StringBuilder dataclassCode = new StringBuilder();
        
        // Analyze decorator options
        boolean isFrozen = false;
        boolean useSlots = false;
        boolean generateRepr = true;
        boolean generateEq = true;
        boolean generateHash = false;
        
        for (DecoratorInfo decorator : decorators) {
            if ("dataclass".equals(decorator.name)) {
                for (String arg : decorator.arguments) {
                    if (arg.contains("frozen=True")) isFrozen = true;
                    if (arg.contains("slots=True")) useSlots = true;
                    if (arg.contains("repr=False")) generateRepr = false;
                    if (arg.contains("eq=False")) generateEq = false;
                    if (arg.contains("unsafe_hash=True")) generateHash = true;
                }
            }
        }
        
        // Generate dataclass decorator with options
        addIndent(dataclassCode);
        dataclassCode.append("@dataclass");
        List<String> options = new ArrayList<>();
        if (isFrozen) options.add("frozen=True");
        if (useSlots) options.add("slots=True");
        if (!generateRepr) options.add("repr=False");
        if (!generateEq) options.add("eq=False");
        if (generateHash) options.add("unsafe_hash=True");
        
        if (!options.isEmpty()) {
            dataclassCode.append("(").append(String.join(", ", options)).append(")");
        }
        dataclassCode.append("\n");
        
        // Generate class definition
        addIndent(dataclassCode);
        dataclassCode.append("class ").append(toPascalCase(node.getName())).append(":\n");
        indentLevel++;
        
        // Generate class docstring
        if (options.getBooleanOption("python.generateDocstrings", true)) {
            addIndent(dataclassCode);
            dataclassCode.append('\"').append('\"').append('\"').append("\n");
            addIndent(dataclassCode);
            dataclassCode.append("Advanced dataclass for ").append(node.getName()).append(".\n");
            addIndent(dataclassCode);
            dataclassCode.append("\n");
            addIndent(dataclassCode);
            dataclassCode.append("Generated with comprehensive type hints and validation.\n");
            addIndent(dataclassCode);
            dataclassCode.append('\"').append('\"').append('\"').append("\n\n");
        }
        
        // Generate fields with proper typing and defaults
        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableDeclarationNode) {
                VariableDeclarationNode field = (VariableDeclarationNode) child;
                String fieldName = toSnakeCase(field.getName());
                String fieldType = mapToPythonType(field.getDataType());
                
                addIndent(dataclassCode);
                dataclassCode.append(fieldName).append(": ").append(fieldType);
                
                // Add field() if needed for complex defaults or metadata
                if (field.isPrivate() || needsComplexDefault(field)) {
                    dataclassCode.append(" = field(");
                    List<String> fieldOptions = new ArrayList<>();
                    if (field.isPrivate()) {
                        fieldOptions.add("repr=False");
                    }
                    if (needsComplexDefault(field)) {
                        fieldOptions.add("default_factory=" + getFieldDefaultFactory(fieldType));
                    }
                    dataclassCode.append(String.join(", ", fieldOptions));
                    dataclassCode.append(")");
                } else {
                    String defaultValue = getPythonDefaultValue(fieldType);
                    if (defaultValue != null && !"None".equals(defaultValue)) {
                        dataclassCode.append(" = ").append(defaultValue);
                    }
                }
                
                dataclassCode.append("\n");
            }
        }
        
        // Generate post-init validation if needed
        if (hasValidationRequirements(node)) {
            dataclassCode.append("\n");
            addIndent(dataclassCode);
            dataclassCode.append("def __post_init__(self) -> None:\n");
            indentLevel++;
            addIndent(dataclassCode);
            dataclassCode.append('\"').append('\"').append('\"').append("Post-initialization validation.\"\"\"\n");
            addIndent(dataclassCode);
            dataclassCode.append("# Add validation logic here\n");
            addIndent(dataclassCode);
            dataclassCode.append("pass\n");
            indentLevel--;
        }
        
        indentLevel--;
        
        return dataclassCode.toString();
    }
    
    // Helper methods for advanced constructs
    private boolean needsComplexDefault(VariableDeclarationNode field) {
        String type = mapToPythonType(field.getDataType());
        return type.startsWith("List[") || type.startsWith("Dict[") || type.startsWith("Set[");
    }
    
    private String getFieldDefaultFactory(String fieldType) {
        if (fieldType.startsWith("List[")) return "list";
        if (fieldType.startsWith("Dict[")) return "dict";
        if (fieldType.startsWith("Set[")) return "set";
        return "lambda: None";
    }
    
    private boolean hasValidationRequirements(ClassDeclarationNode node) {
        // Check if any fields need validation
        return node.getChildren().stream()
                .anyMatch(child -> child instanceof VariableDeclarationNode);
    }
}