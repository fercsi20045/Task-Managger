package com.taskmanager.dto;

import com.taskmanager.model.Task;
import com.taskmanager.model.Task.TaskStatus;
import java.time.LocalDateTime;

public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
    private String username;

    public TaskResponse() {}

    /** Task entity-ből létrehoz egy TaskResponse-t */
    public static TaskResponse from(Task task) {
        TaskResponse r = new TaskResponse();
        r.id = task.getId();
        r.title = task.getTitle();
        r.description = task.getDescription();
        r.status = task.getStatus();
        r.createdAt = task.getCreatedAt();
        r.deadline = task.getDeadline();
        r.username = task.getUser().getUsername();
        return r;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeadline() { return deadline; }
    public String getUsername() { return username; }
}
