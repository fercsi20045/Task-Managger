package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.Task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    // Felhasználó összes feladata
    List<Task> findByUserId(Long userId);

    // Felhasználó feladatai státusz szerint szűrve
    List<Task> findByUserIdAndStatus(Long userId, TaskStatus status);
}
