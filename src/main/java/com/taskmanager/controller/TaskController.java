package com.taskmanager.controller;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.model.Task;
import com.taskmanager.model.Task.TaskStatus;
import com.taskmanager.model.User;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskController(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/tasks
     * Bejelentkezett felhasználó feladatainak listázása
     * Opcionális szűrés: ?status=TODO|IN_PROGRESS|DONE
     */
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(
            @RequestParam(required = false) TaskStatus status,
            Authentication auth) {

        User user = getUser(auth);
        List<Task> tasks;

        if (status != null) {
            tasks = taskRepository.findByUserIdAndStatus(user.getId(), status);
        } else {
            tasks = taskRepository.findByUserId(user.getId());
        }

        List<TaskResponse> response = tasks.stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/tasks/{id}
     * Egy feladat részleteinek megtekintése
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTask(@PathVariable Long id, Authentication auth) {
        User user = getUser(auth);
        Task task = taskRepository.findById(id).orElse(null);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        // Csak a saját feladatát nézheti meg
        if (!task.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Nincs hozzáférésed ehhez a feladathoz"));
        }

        return ResponseEntity.ok(TaskResponse.from(task));
    }

    /**
     * POST /api/tasks
     * Új feladat létrehozása
     */
    @PostMapping
    public ResponseEntity<?> createTask(@Valid @RequestBody TaskRequest request, Authentication auth) {
        User user = getUser(auth);

        Task task = new Task(request.getTitle(), request.getDescription(), user);
        task.setDeadline(request.getDeadline());

        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        Task saved = taskRepository.save(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(saved));
    }

    /**
     * PUT /api/tasks/{id}
     * Feladat módosítása (cím, leírás, státusz, határidő)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id,
                                        @Valid @RequestBody TaskRequest request,
                                        Authentication auth) {
        User user = getUser(auth);
        Task task = taskRepository.findById(id).orElse(null);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        if (!task.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Nincs hozzáférésed ehhez a feladathoz"));
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        task.setDeadline(request.getDeadline());

        return ResponseEntity.ok(TaskResponse.from(taskRepository.save(task)));
    }

    /**
     * PATCH /api/tasks/{id}/status
     * Csak a státusz módosítása
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          Authentication auth) {
        User user = getUser(auth);
        Task task = taskRepository.findById(id).orElse(null);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        if (!task.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Nincs hozzáférésed ehhez a feladathoz"));
        }

        try {
            task.setStatus(TaskStatus.valueOf(body.get("status")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Érvénytelen státusz"));
        }

        return ResponseEntity.ok(TaskResponse.from(taskRepository.save(task)));
    }

    /**
     * DELETE /api/tasks/{id}
     * Feladat törlése
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Authentication auth) {
        User user = getUser(auth);
        Task task = taskRepository.findById(id).orElse(null);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        if (!task.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Nincs hozzáférésed ehhez a feladathoz"));
        }

        taskRepository.delete(task);
        return ResponseEntity.ok(Map.of("message", "Feladat törölve"));
    }

    /** Segédmetódus: bejelentkezett felhasználó lekérdezése */
    private User getUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Felhasználó nem található"));
    }
}
