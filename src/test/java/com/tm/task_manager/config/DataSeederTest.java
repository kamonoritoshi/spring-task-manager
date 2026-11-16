package com.tm.task_manager.config;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import com.tm.task_manager.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataSeederTest {

    @Test
    @DisplayName("DataSeeder tự động chèn 5 công việc mẫu khi database đang trống")
    void testDataSeederWhenDatabaseIsEmpty() {
        List<Task> savedTasks = new ArrayList<>();

        TaskRepository fakeRepo = (TaskRepository) Proxy.newProxyInstance(
                TaskRepository.class.getClassLoader(),
                new Class<?>[]{TaskRepository.class},
                (proxy, method, args) -> {
                    if ("count".equals(method.getName())) {
                        return (long) savedTasks.size();
                    }
                    if ("saveAll".equals(method.getName())) {
                        @SuppressWarnings("unchecked")
                        Iterable<Task> entities = (Iterable<Task>) args[0];
                        for (Task t : entities) {
                            savedTasks.add(t);
                        }
                        return savedTasks;
                    }
                    return null;
                }
        );

        DataSeeder seeder = new DataSeeder(fakeRepo);
        seeder.run();

        assertEquals(5, savedTasks.size(), "Phải chèn đúng 5 công việc mẫu");

        // Kiểm tra ít nhất 1 việc quá hạn (dueDate < LocalDate.now())
        boolean hasOverdue = savedTasks.stream()
                .anyMatch(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()));
        assertTrue(hasOverdue, "Phải có ít nhất 1 công việc quá hạn");

        // Kiểm tra các mức độ ưu tiên
        boolean hasHigh = savedTasks.stream().anyMatch(t -> t.getPriority() == Priority.HIGH);
        boolean hasMedium = savedTasks.stream().anyMatch(t -> t.getPriority() == Priority.MEDIUM);
        boolean hasLow = savedTasks.stream().anyMatch(t -> t.getPriority() == Priority.LOW);
        assertTrue(hasHigh && hasMedium && hasLow, "Phải có đầy đủ các mức ưu tiên HIGH, MEDIUM, LOW");

        // Kiểm tra các trạng thái
        boolean hasTodo = savedTasks.stream().anyMatch(t -> t.getStatus() == Status.TODO);
        boolean hasInProgress = savedTasks.stream().anyMatch(t -> t.getStatus() == Status.IN_PROGRESS);
        boolean hasDone = savedTasks.stream().anyMatch(t -> t.getStatus() == Status.DONE);
        assertTrue(hasTodo && hasInProgress && hasDone, "Phải có đầy đủ các trạng thái TODO, IN_PROGRESS, DONE");
    }

    @Test
    @DisplayName("DataSeeder không chèn dữ liệu nếu database đã có sẵn dữ liệu")
    void testDataSeederWhenDatabaseHasData() {
        List<Task> savedTasks = new ArrayList<>();
        savedTasks.add(new Task()); // Giả lập đã có 1 task trong DB

        TaskRepository fakeRepo = (TaskRepository) Proxy.newProxyInstance(
                TaskRepository.class.getClassLoader(),
                new Class<?>[]{TaskRepository.class},
                (proxy, method, args) -> {
                    if ("count".equals(method.getName())) {
                        return (long) savedTasks.size();
                    }
                    if ("saveAll".equals(method.getName())) {
                        fail("Không được gọi saveAll khi database đã có dữ liệu");
                    }
                    return null;
                }
        );

        DataSeeder seeder = new DataSeeder(fakeRepo);
        seeder.run();

        assertEquals(1, savedTasks.size(), "Không được chèn thêm task nào");
    }
}
