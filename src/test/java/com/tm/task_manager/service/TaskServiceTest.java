package com.tm.task_manager.service;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import com.tm.task_manager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    private final Map<Long, Task> database = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private TaskRepository taskRepository;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        database.clear();
        idGenerator.set(1);

        taskRepository = (TaskRepository) Proxy.newProxyInstance(
                TaskRepository.class.getClassLoader(),
                new Class<?>[] { TaskRepository.class },
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    switch (methodName) {
                        case "findAllByOrderByDueDateAsc":
                            return database.values().stream()
                                    .sorted(Comparator.comparing(Task::getDueDate,
                                            Comparator.nullsLast(Comparator.naturalOrder())))
                                    .collect(Collectors.toList());

                        case "findByStatusOrderByDueDateAsc":
                            Status status = (Status) args[0];
                            return database.values().stream()
                                    .filter(t -> t.getStatus() == status)
                                    .sorted(Comparator.comparing(Task::getDueDate,
                                            Comparator.nullsLast(Comparator.naturalOrder())))
                                    .collect(Collectors.toList());

                        case "findByPriorityOrderByDueDateAsc":
                            Priority priorityFilter = (Priority) args[0];
                            return database.values().stream()
                                    .filter(t -> t.getPriority() == priorityFilter)
                                    .sorted(Comparator.comparing(Task::getDueDate,
                                            Comparator.nullsLast(Comparator.naturalOrder())))
                                    .collect(Collectors.toList());

                        case "findByStatusAndPriorityOrderByDueDateAsc":
                            Status s = (Status) args[0];
                            Priority priorityParam = (Priority) args[1];
                            return database.values().stream()
                                    .filter(t -> t.getStatus() == s && t.getPriority() == priorityParam)
                                    .sorted(Comparator.comparing(Task::getDueDate,
                                            Comparator.nullsLast(Comparator.naturalOrder())))
                                    .collect(Collectors.toList());

                        case "findByTitleContainingIgnoreCaseOrderByDueDateAsc":
                            String kw = ((String) args[0]).toLowerCase();
                            return database.values().stream()
                                    .filter(t -> t.getTitle() != null && t.getTitle().toLowerCase().contains(kw))
                                    .sorted(Comparator.comparing(Task::getDueDate,
                                            Comparator.nullsLast(Comparator.naturalOrder())))
                                    .collect(Collectors.toList());

                        case "countByStatus":
                            Status countStatus = (Status) args[0];
                            return database.values().stream()
                                    .filter(t -> t.getStatus() == countStatus)
                                    .count();

                        case "count":
                            return (long) database.size();

                        case "findAll":
                            if (args.length == 2 && args[1] instanceof org.springframework.data.domain.Pageable) {
                                org.springframework.data.domain.Pageable pageable = (org.springframework.data.domain.Pageable) args[1];
                                List<Task> list = new ArrayList<>(database.values());
                                int start = (int) pageable.getOffset();
                                int end = Math.min((start + pageable.getPageSize()), list.size());
                                List<Task> subList = start < list.size() ? list.subList(start, end)
                                        : Collections.emptyList();
                                return new org.springframework.data.domain.PageImpl<>(subList, pageable, list.size());
                            }
                            return new ArrayList<>(database.values());

                        case "findById":
                            Long id = (Long) args[0];
                            return Optional.ofNullable(database.get(id));

                        case "save":
                            Task taskToSave = (Task) args[0];
                            if (taskToSave.getId() == null) {
                                taskToSave.setId(idGenerator.getAndIncrement());
                            }
                            database.put(taskToSave.getId(), taskToSave);
                            return taskToSave;

                        case "delete":
                            Task taskToDelete = (Task) args[0];
                            if (taskToDelete != null && taskToDelete.getId() != null) {
                                database.remove(taskToDelete.getId());
                            }
                            return null;

                        case "deleteById":
                            Long deleteId = (Long) args[0];
                            database.remove(deleteId);
                            return null;

                        default:
                            return null;
                    }
                });

        taskService = new TaskService(taskRepository);

        // Chèn 3 task mẫu để kiểm thử
        Task t1 = new Task();
        t1.setTitle("Học Spring Boot");
        t1.setDescription("Học Service và Repository");
        t1.setPriority(Priority.HIGH);
        t1.setStatus(Status.TODO);
        t1.setDueDate(LocalDate.now().plusDays(3));
        taskService.saveTask(t1);

        Task t2 = new Task();
        t2.setTitle("Làm bài tập Java");
        t2.setDescription("Thực hành Spring Data JPA");
        t2.setPriority(Priority.MEDIUM);
        t2.setStatus(Status.IN_PROGRESS);
        t2.setDueDate(LocalDate.now().plusDays(1));
        taskService.saveTask(t2);

        Task t3 = new Task();
        t3.setTitle("Nộp báo cáo");
        t3.setDescription("Báo cáo tuần");
        t3.setPriority(Priority.LOW);
        t3.setStatus(Status.DONE);
        t3.setDueDate(LocalDate.now().minusDays(2));
        taskService.saveTask(t3);
    }

    @Test
    @DisplayName("Lấy tất cả công việc mặc định sắp xếp theo dueDate tăng dần")
    void testGetAllTasksDefault() {
        List<Task> tasks = taskService.getAllTasks();
        assertEquals(3, tasks.size());
        assertEquals("Nộp báo cáo", tasks.get(0).getTitle());
        assertEquals("Làm bài tập Java", tasks.get(1).getTitle());
        assertEquals("Học Spring Boot", tasks.get(2).getTitle());
    }

    @Test
    @DisplayName("Lấy tất cả công việc khi truyền tham số null")
    void testGetAllTasksWithNullFilters() {
        List<Task> tasks = taskService.getAllTasks(null, null, null);
        assertEquals(3, tasks.size());
        assertEquals("Nộp báo cáo", tasks.get(0).getTitle());
    }

    @Test
    @DisplayName("Lọc công việc theo trạng thái")
    void testFilterByStatus() {
        List<Task> todoTasks = taskService.getAllTasks(Status.TODO, null, null);
        assertEquals(1, todoTasks.size());
        assertEquals("Học Spring Boot", todoTasks.get(0).getTitle());

        List<Task> inProgressTasks = taskService.getAllTasks(Status.IN_PROGRESS, null, null);
        assertEquals(1, inProgressTasks.size());
        assertEquals("Làm bài tập Java", inProgressTasks.get(0).getTitle());
    }

    @Test
    @DisplayName("Lọc công việc theo mức độ ưu tiên")
    void testFilterByPriority() {
        List<Task> highTasks = taskService.getAllTasks(null, Priority.HIGH, null);
        assertEquals(1, highTasks.size());
        assertEquals("Học Spring Boot", highTasks.get(0).getTitle());
    }

    @Test
    @DisplayName("Lọc công việc theo cả trạng thái và mức độ ưu tiên")
    void testFilterByStatusAndPriority() {
        List<Task> result = taskService.getAllTasks(Status.TODO, Priority.HIGH, null);
        assertEquals(1, result.size());
        assertEquals("Học Spring Boot", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Tìm kiếm công việc theo từ khóa tiêu đề")
    void testSearchByKeyword() {
        List<Task> result = taskService.getAllTasks(null, null, "spring");
        assertEquals(1, result.size());
        assertEquals("Học Spring Boot", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Lấy chi tiết task theo id thành công")
    void testGetTaskByIdSuccess() {
        Task task = taskService.getTaskById(1L);
        assertNotNull(task);
        assertEquals("Học Spring Boot", task.getTitle());
    }

    @Test
    @DisplayName("Lấy chi tiết task theo id không tồn tại ném RuntimeException tiếng Việt")
    void testGetTaskByIdNotFound() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.getTaskById(999L));
        assertTrue(ex.getMessage().contains("Không tìm thấy công việc với ID: 999"));
    }

    @Test
    @DisplayName("Lưu mới và cập nhật task")
    void testSaveTask() {
        Task newTask = new Task();
        newTask.setTitle("Công việc mới");
        newTask.setPriority(Priority.HIGH);
        newTask.setStatus(Status.TODO);
        newTask.setDueDate(LocalDate.now().plusDays(5));

        Task saved = taskService.saveTask(newTask);
        assertNotNull(saved.getId());
        assertEquals(4L, saved.getId());

        saved.setTitle("Công việc đã sửa");
        Task updated = taskService.saveTask(saved);
        assertEquals("Công việc đã sửa", updated.getTitle());
    }

    @Test
    @DisplayName("toggleStatus đổi từ TODO sang DONE và từ DONE về TODO")
    void testToggleStatus() {
        Task task = taskService.getTaskById(1L);
        assertEquals(Status.TODO, task.getStatus());

        Task toggledToDone = taskService.toggleStatus(1L);
        assertEquals(Status.DONE, toggledToDone.getStatus());

        Task toggledBackToTodo = taskService.toggleStatus(1L);
        assertEquals(Status.TODO, toggledBackToTodo.getStatus());
    }

    @Test
    @DisplayName("Xóa task theo id")
    void testDeleteTask() {
        taskService.deleteTask(1L);
        assertEquals(2, taskService.getAllTasks().size());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.getTaskById(1L));
        assertTrue(ex.getMessage().contains("Không tìm thấy công việc với ID: 1"));
    }

    @Test
    @DisplayName("Thống kê số lượng task theo từng trạng thái")
    void testStatistics() {
        Map<Status, Long> counts = taskService.getTaskCountByStatus();
        assertEquals(1L, counts.get(Status.TODO));
        assertEquals(1L, counts.get(Status.IN_PROGRESS));
        assertEquals(1L, counts.get(Status.DONE));

        Map<String, Long> stats = taskService.getTaskStatistics();
        assertEquals(1L, stats.get("TODO"));
        assertEquals(1L, stats.get("IN_PROGRESS"));
        assertEquals(1L, stats.get("DONE"));
        assertEquals(3L, stats.get("TOTAL"));

        assertEquals(1L, taskService.countByStatus(Status.TODO));
        assertEquals(1L, taskService.countByStatus(Status.IN_PROGRESS));
        assertEquals(1L, taskService.countByStatus(Status.DONE));
    }
}
