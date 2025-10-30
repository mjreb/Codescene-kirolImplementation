package com.agent.infrastructure.tools.impl;

import com.agent.domain.model.ParameterDefinition;
import com.agent.domain.model.ToolDefinition;
import com.agent.domain.model.ToolResult;
import com.agent.infrastructure.tools.BaseTool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * File system tool for basic file operations.
 * Provides safe file system access with security restrictions.
 */
@Component
public class FileSystemTool extends BaseTool {
    
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".txt", ".md", ".json", ".xml", ".csv", ".log", ".properties", ".yml", ".yaml"
    );
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_LIST_ITEMS = 100;
    
    public FileSystemTool() {
        super(createDefinition());
    }
    
    private static ToolDefinition createDefinition() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName("file_system");
        definition.setDescription("Performs basic file system operations like reading, writing, and listing files");
        definition.setCategory("utility");
        definition.setAsync(false);
        
        Map<String, ParameterDefinition> parameters = new HashMap<>();
        
        // Operation parameter
        ParameterDefinition operation = new ParameterDefinition();
        operation.setName("operation");
        operation.setType("string");
        operation.setDescription("Operation to perform: 'read', 'write', 'list', 'exists', 'info'");
        operation.setRequired(true);
        parameters.put("operation", operation);
        
        // Path parameter
        ParameterDefinition path = new ParameterDefinition();
        path.setName("path");
        path.setType("string");
        path.setDescription("File or directory path");
        path.setRequired(true);
        parameters.put("path", path);
        
        // Content parameter (for write operations)
        ParameterDefinition content = new ParameterDefinition();
        content.setName("content");
        content.setType("string");
        content.setDescription("Content to write to file (required for write operation)");
        content.setRequired(false);
        parameters.put("content", content);
        
        // Append parameter (for write operations)
        ParameterDefinition append = new ParameterDefinition();
        append.setName("append");
        append.setType("boolean");
        append.setDescription("Whether to append to file instead of overwriting (default: false)");
        append.setRequired(false);
        append.setDefaultValue(false);
        parameters.put("append", append);
        
        definition.setParameters(parameters);
        return definition;
    }
    
    @Override
    protected ToolResult executeInternal(Map<String, Object> parameters) {
        String operation = getRequiredParameter(parameters, "operation", String.class);
        String pathStr = getRequiredParameter(parameters, "path", String.class);
        
        try {
            Path path = Paths.get(pathStr).normalize();
            
            // Security check: prevent access to system directories
            if (!isPathAllowed(path)) {
                return createErrorResult("Access to this path is not allowed for security reasons");
            }
            
            return switch (operation.toLowerCase()) {
                case "read" -> readFile(path);
                case "write" -> writeFile(path, parameters);
                case "list" -> listDirectory(path);
                case "exists" -> checkExists(path);
                case "info" -> getFileInfo(path);
                default -> createErrorResult("Unknown operation: " + operation + 
                    ". Supported operations: read, write, list, exists, info");
            };
            
        } catch (Exception e) {
            logger.error("File system operation '{}' failed for path '{}': {}", operation, pathStr, e.getMessage());
            return createErrorResult("File system operation failed: " + e.getMessage());
        }
    }
    
    private boolean isPathAllowed(Path path) {
        String pathStr = path.toString().toLowerCase();
        
        // Prevent access to system directories
        String[] forbiddenPaths = {
            "/etc", "/sys", "/proc", "/dev", "/boot", "/root",
            "c:\\windows", "c:\\program files", "c:\\users\\administrator"
        };
        
        for (String forbidden : forbiddenPaths) {
            if (pathStr.startsWith(forbidden)) {
                return false;
            }
        }
        
        // Prevent directory traversal attacks
        if (pathStr.contains("..")) {
            return false;
        }
        
        return true;
    }
    
    private ToolResult readFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            return createErrorResult("File does not exist: " + path);
        }
        
        if (!Files.isRegularFile(path)) {
            return createErrorResult("Path is not a regular file: " + path);
        }
        
        // Check file size
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            return createErrorResult("File is too large to read (max " + (MAX_FILE_SIZE / 1024 / 1024) + "MB): " + size + " bytes");
        }
        
        // Check file extension
        String fileName = path.getFileName().toString().toLowerCase();
        boolean allowedExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
        
        if (!allowedExtension) {
            return createErrorResult("File type not allowed for reading. Allowed extensions: " + ALLOWED_EXTENSIONS);
        }
        
        try {
            String content = Files.readString(path);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", path.toString());
            result.put("content", content);
            result.put("size", size);
            result.put("encoding", "UTF-8");
            
            return createSuccessResult(result);
            
        } catch (IOException e) {
            return createErrorResult("Failed to read file: " + e.getMessage());
        }
    }
    
    private ToolResult writeFile(Path path, Map<String, Object> parameters) throws IOException {
        String content = getParameter(parameters, "content", String.class);
        Boolean append = getParameter(parameters, "append", Boolean.class);
        
        if (content == null) {
            return createErrorResult("Content parameter is required for write operation");
        }
        
        if (append == null) {
            append = false;
        }
        
        // Check file extension
        String fileName = path.getFileName().toString().toLowerCase();
        boolean allowedExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
        
        if (!allowedExtension) {
            return createErrorResult("File type not allowed for writing. Allowed extensions: " + ALLOWED_EXTENSIONS);
        }
        
        try {
            // Create parent directories if they don't exist
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            
            if (append) {
                Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", path.toString());
            result.put("operation", append ? "append" : "write");
            result.put("bytes_written", content.getBytes().length);
            result.put("success", true);
            
            return createSuccessResult(result);
            
        } catch (IOException e) {
            return createErrorResult("Failed to write file: " + e.getMessage());
        }
    }
    
    private ToolResult listDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return createErrorResult("Directory does not exist: " + path);
        }
        
        if (!Files.isDirectory(path)) {
            return createErrorResult("Path is not a directory: " + path);
        }
        
        try (Stream<Path> stream = Files.list(path)) {
            List<Map<String, Object>> items = stream
                    .limit(MAX_LIST_ITEMS)
                    .map(this::createFileInfo)
                    .sorted((a, b) -> {
                        // Directories first, then files, both alphabetically
                        boolean aIsDir = (Boolean) a.get("is_directory");
                        boolean bIsDir = (Boolean) b.get("is_directory");
                        
                        if (aIsDir != bIsDir) {
                            return aIsDir ? -1 : 1;
                        }
                        
                        return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
                    })
                    .toList();
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", path.toString());
            result.put("items", items);
            result.put("total_items", items.size());
            result.put("max_items_shown", MAX_LIST_ITEMS);
            
            return createSuccessResult(result);
            
        } catch (IOException e) {
            return createErrorResult("Failed to list directory: " + e.getMessage());
        }
    }
    
    private ToolResult checkExists(Path path) {
        Map<String, Object> result = new HashMap<>();
        result.put("path", path.toString());
        result.put("exists", Files.exists(path));
        result.put("is_file", Files.isRegularFile(path));
        result.put("is_directory", Files.isDirectory(path));
        result.put("is_readable", Files.isReadable(path));
        result.put("is_writable", Files.isWritable(path));
        
        return createSuccessResult(result);
    }
    
    private ToolResult getFileInfo(Path path) throws IOException {
        if (!Files.exists(path)) {
            return createErrorResult("Path does not exist: " + path);
        }
        
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            
            Map<String, Object> result = createFileInfo(path);
            result.put("creation_time", formatTime(attrs.creationTime().toInstant()));
            result.put("last_modified", formatTime(attrs.lastModifiedTime().toInstant()));
            result.put("last_accessed", formatTime(attrs.lastAccessTime().toInstant()));
            
            return createSuccessResult(result);
            
        } catch (IOException e) {
            return createErrorResult("Failed to get file info: " + e.getMessage());
        }
    }
    
    private Map<String, Object> createFileInfo(Path path) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", path.getFileName().toString());
        info.put("path", path.toString());
        info.put("is_directory", Files.isDirectory(path));
        info.put("is_file", Files.isRegularFile(path));
        info.put("is_readable", Files.isReadable(path));
        info.put("is_writable", Files.isWritable(path));
        
        try {
            if (Files.isRegularFile(path)) {
                info.put("size", Files.size(path));
            }
        } catch (IOException e) {
            info.put("size", "unknown");
        }
        
        return info;
    }
    
    private String formatTime(Instant instant) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}