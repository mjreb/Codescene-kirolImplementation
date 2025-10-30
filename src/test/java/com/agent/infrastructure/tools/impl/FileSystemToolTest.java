package com.agent.infrastructure.tools.impl;

import com.agent.domain.model.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemToolTest {
    
    private FileSystemTool fileSystemTool;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        fileSystemTool = new FileSystemTool();
    }
    
    @Test
    void testWriteAndReadFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        
        // Write file
        Map<String, Object> writeParams = new HashMap<>();
        writeParams.put("operation", "write");
        writeParams.put("path", testFile.toString());
        writeParams.put("content", "Hello, World!");
        
        ToolResult writeResult = fileSystemTool.execute(writeParams);
        assertTrue(writeResult.isSuccess());
        
        // Read file
        Map<String, Object> readParams = new HashMap<>();
        readParams.put("operation", "read");
        readParams.put("path", testFile.toString());
        
        ToolResult readResult = fileSystemTool.execute(readParams);
        assertTrue(readResult.isSuccess());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) readResult.getResult();
        assertEquals("Hello, World!", result.get("content"));
        assertEquals(testFile.toString(), result.get("path"));
    }
    
    @Test
    void testAppendToFile() throws IOException {
        Path testFile = tempDir.resolve("append_test.txt");
        
        // Write initial content
        Map<String, Object> writeParams = new HashMap<>();
        writeParams.put("operation", "write");
        writeParams.put("path", testFile.toString());
        writeParams.put("content", "Line 1\n");
        
        ToolResult writeResult = fileSystemTool.execute(writeParams);
        assertTrue(writeResult.isSuccess());
        
        // Append content
        Map<String, Object> appendParams = new HashMap<>();
        appendParams.put("operation", "write");
        appendParams.put("path", testFile.toString());
        appendParams.put("content", "Line 2\n");
        appendParams.put("append", true);
        
        ToolResult appendResult = fileSystemTool.execute(appendParams);
        assertTrue(appendResult.isSuccess());
        
        // Read and verify
        Map<String, Object> readParams = new HashMap<>();
        readParams.put("operation", "read");
        readParams.put("path", testFile.toString());
        
        ToolResult readResult = fileSystemTool.execute(readParams);
        assertTrue(readResult.isSuccess());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) readResult.getResult();
        assertEquals("Line 1\nLine 2\n", result.get("content"));
    }
    
    @Test
    void testListDirectory() throws IOException {
        // Create some test files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.md"));
        Files.createDirectories(tempDir.resolve("subdir"));
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "list");
        params.put("path", tempDir.toString());
        
        ToolResult result = fileSystemTool.execute(params);
        assertTrue(result.isSuccess());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> resultData = (Map<String, Object>) result.getResult();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resultData.get("items");
        
        assertEquals(3, items.size());
        assertEquals(tempDir.toString(), resultData.get("path"));
        
        // Check that directory comes first (sorted)
        assertEquals("subdir", items.get(0).get("name"));
        assertTrue((Boolean) items.get(0).get("is_directory"));
    }
    
    @Test
    void testFileExists() throws IOException {
        Path existingFile = tempDir.resolve("existing.txt");
        Files.createFile(existingFile);
        
        Path nonExistingFile = tempDir.resolve("nonexisting.txt");
        
        // Test existing file
        Map<String, Object> existsParams = new HashMap<>();
        existsParams.put("operation", "exists");
        existsParams.put("path", existingFile.toString());
        
        ToolResult existsResult = fileSystemTool.execute(existsParams);
        assertTrue(existsResult.isSuccess());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) existsResult.getResult();
        assertTrue((Boolean) result.get("exists"));
        assertTrue((Boolean) result.get("is_file"));
        assertFalse((Boolean) result.get("is_directory"));
        
        // Test non-existing file
        Map<String, Object> notExistsParams = new HashMap<>();
        notExistsParams.put("operation", "exists");
        notExistsParams.put("path", nonExistingFile.toString());
        
        ToolResult notExistsResult = fileSystemTool.execute(notExistsParams);
        assertTrue(notExistsResult.isSuccess());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> notExistsResultData = (Map<String, Object>) notExistsResult.getResult();
        assertFalse((Boolean) notExistsResultData.get("exists"));
    }
    
    @Test
    void testGetFileInfo() throws IOException {
        Path testFile = tempDir.resolve("info_test.txt");
        Files.write(testFile, "test content".getBytes());
        
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "info");
        params.put("path", testFile.toString());
        
        ToolResult result = fileSystemTool.execute(params);
        assertTrue(result.isSuccess());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) result.getResult();
        assertEquals("info_test.txt", info.get("name"));
        assertTrue((Boolean) info.get("is_file"));
        assertFalse((Boolean) info.get("is_directory"));
        assertNotNull(info.get("creation_time"));
        assertNotNull(info.get("last_modified"));
    }
    
    @Test
    void testReadNonExistentFile() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "read");
        params.put("path", tempDir.resolve("nonexistent.txt").toString());
        
        ToolResult result = fileSystemTool.execute(params);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("does not exist"));
    }
    
    @Test
    void testInvalidOperation() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "invalid_operation");
        params.put("path", tempDir.toString());
        
        ToolResult result = fileSystemTool.execute(params);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Unknown operation"));
    }
    
    @Test
    void testMissingRequiredParameters() {
        // Missing operation
        Map<String, Object> params1 = new HashMap<>();
        params1.put("path", tempDir.toString());
        
        ToolResult result1 = fileSystemTool.execute(params1);
        assertFalse(result1.isSuccess());
        
        // Missing path
        Map<String, Object> params2 = new HashMap<>();
        params2.put("operation", "read");
        
        ToolResult result2 = fileSystemTool.execute(params2);
        assertFalse(result2.isSuccess());
    }
    
    @Test
    void testWriteWithoutContent() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "write");
        params.put("path", tempDir.resolve("test.txt").toString());
        // Missing content parameter
        
        ToolResult result = fileSystemTool.execute(params);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Content parameter is required"));
    }
    
    @Test
    void testSecurityRestrictions() {
        // Test that system paths are blocked
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "read");
        params.put("path", "/etc/passwd");
        
        ToolResult result = fileSystemTool.execute(params);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("not allowed for security reasons"));
    }
    
    @Test
    void testDirectoryTraversalPrevention() {
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "read");
        params.put("path", "../../../etc/passwd");
        
        ToolResult result = fileSystemTool.execute(params);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("not allowed for security reasons"));
    }
    
    @Test
    void testToolDefinition() {
        assertNotNull(fileSystemTool.getDefinition());
        assertEquals("file_system", fileSystemTool.getDefinition().getName());
        assertNotNull(fileSystemTool.getDefinition().getDescription());
        assertFalse(fileSystemTool.getDefinition().isAsync());
        
        // Should have required parameters
        assertTrue(fileSystemTool.getDefinition().getParameters().containsKey("operation"));
        assertTrue(fileSystemTool.getDefinition().getParameters().containsKey("path"));
        assertTrue(fileSystemTool.getDefinition().getParameters().containsKey("content"));
        assertTrue(fileSystemTool.getDefinition().getParameters().containsKey("append"));
        
        // Operation and path should be required
        assertTrue(fileSystemTool.getDefinition().getParameters().get("operation").isRequired());
        assertTrue(fileSystemTool.getDefinition().getParameters().get("path").isRequired());
        
        // Content and append should be optional
        assertFalse(fileSystemTool.getDefinition().getParameters().get("content").isRequired());
        assertFalse(fileSystemTool.getDefinition().getParameters().get("append").isRequired());
    }
}