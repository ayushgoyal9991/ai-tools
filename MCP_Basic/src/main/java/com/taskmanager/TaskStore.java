package com.taskmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.UUID;

public class TaskStore {
    private static final String FILE = "tasks.json";
    private final ObjectMapper mapper = new ObjectMapper();

    private ArrayNode load() {
        try {
            File f = new File(FILE);
            if (!f.exists()) return mapper.createArrayNode();
            return (ArrayNode) mapper.readTree(f);
        } catch (Exception e) {
            return mapper.createArrayNode();
        }
    }

    private void save(ArrayNode tasks) throws Exception {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), tasks);
    }

    public String addTask(String title) throws Exception {
        ArrayNode tasks = load();
        if (title == null || title.isBlank()) return "Error: title is required";
        ObjectNode task = mapper.createObjectNode();
        String id = UUID.randomUUID().toString().substring(0, 8);
        task.put("id", id);
        task.put("title", title);
        task.put("done", false);
        tasks.add(task);
        save(tasks);
        return "Task added with ID: " + id;
    }

    public String listTasks() {
        ArrayNode tasks = load();
        if (tasks.isEmpty()) return "No tasks found.";
        StringBuilder sb = new StringBuilder();
        tasks.forEach(t -> sb.append(String.format("[%s] %s - %s%n",
                t.get("id").asText(),
                t.get("title").asText(),
                t.get("done").asBoolean() ? "✓ done" : "pending")));
        return sb.toString();
    }

    public String deleteTask(String id) throws Exception {
        ArrayNode tasks = load();
        ArrayNode updated = mapper.createArrayNode();
        boolean found = false;
        for (var task : tasks) {
            if (task.get("id").asText().equals(id)) { found = true; }
            else updated.add(task);
        }
        if (!found) return "Task not found: " + id;
        save(updated);
        return "Task deleted: " + id;
    }
}