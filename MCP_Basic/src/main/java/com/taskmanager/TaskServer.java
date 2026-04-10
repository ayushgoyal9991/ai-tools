package com.taskmanager;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

public class TaskServer {

    private final TaskStore store = new TaskStore();

    public McpSyncServer build() {
        var transport = new StdioServerTransportProvider();

        return McpServer.sync(transport)
                .serverInfo("task-manager", "1.0.0")
                .tools(addTaskTool(), listTasksTool(), deleteTaskTool())
                .build();
    }

    private McpServerFeatures.SyncToolSpecification addTaskTool() {
        var schema = new McpSchema.JsonSchema("object",
                Map.of("title", Map.of("type", "string", "description", "Task title")),
                List.of("title"), false, null, null);

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("add_task", "Add a new task", schema),
                (exchange, args) -> {
                    try {
                        String result = store.addTask((String) args.get("title"));
                        return McpSchema.CallToolResult.builder()
                                .content(List.of(new McpSchema.TextContent(result)))
                                .isError(false).build();
                    } catch (Exception e) {
                        return McpSchema.CallToolResult.builder()
                                .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                .isError(true).build();
                    }
                });
    }

    private McpServerFeatures.SyncToolSpecification listTasksTool() {
        var schema = new McpSchema.JsonSchema("object",
                Map.of(), List.of(), false, null, null);

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("list_tasks", "List all tasks", schema),
                (exchange, args) -> McpSchema.CallToolResult.builder()
                        .content(List.of(new McpSchema.TextContent(store.listTasks())))
                        .isError(false).build());
    }

    private McpServerFeatures.SyncToolSpecification deleteTaskTool() {
        var schema = new McpSchema.JsonSchema("object",
                Map.of("id", Map.of("type", "string", "description", "Task ID to delete")),
                List.of("id"), false, null, null);

        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("delete_task", "Delete a task by ID", schema),
                (exchange, args) -> {
                    try {
                        String result = store.deleteTask((String) args.get("id"));
                        return McpSchema.CallToolResult.builder()
                                .content(List.of(new McpSchema.TextContent(result)))
                                .isError(false).build();
                    } catch (Exception e) {
                        return McpSchema.CallToolResult.builder()
                                .content(List.of(new McpSchema.TextContent("Error: " + e.getMessage())))
                                .isError(true).build();
                    }
                });
    }
}