package com.tm.task_manager.controller;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import com.tm.task_manager.repository.TaskRepository;
import com.tm.task_manager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest {

    private final Map<Long, Task> database = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        database.clear();
        idGenerator.set(1);

        TaskRepository fakeRepo = (TaskRepository) Proxy.newProxyInstance(
                TaskRepository.class.getClassLoader(),
                new Class<?>[] { TaskRepository.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "findAll":
                            if (args.length == 2 && args[1] instanceof Pageable) {
                                Pageable p = (Pageable) args[1];
                                List<Task> list = new ArrayList<>(database.values());
                                int start = (int) p.getOffset();
                                int end = Math.min((start + p.getPageSize()), list.size());
                                List<Task> subList = start < list.size() ? list.subList(start, end)
                                        : Collections.emptyList();
                                return new PageImpl<>(subList, p, list.size());
                            }
                            return new ArrayList<>(database.values());
                        case "findAllByOrderByDueDateAsc":
                            return new ArrayList<>(database.values());
                        case "countByStatus":
                            Status st = (Status) args[0];
                            return database.values().stream().filter(t -> t.getStatus() == st).count();
                        case "findById":
                            Long id = (Long) args[0];
                            return Optional.ofNullable(database.get(id));
                        case "save":
                            Task t = (Task) args[0];
                            if (t.getId() == null) {
                                t.setId(idGenerator.getAndIncrement());
                            }
                            database.put(t.getId(), t);
                            return t;
                        case "delete":
                            Task delTask = (Task) args[0];
                            if (delTask != null && delTask.getId() != null) {
                                database.remove(delTask.getId());
                            }
                            return null;
                        default:
                            return null;
                    }
                });

        TaskService taskService = new TaskService(fakeRepo);
        TaskController taskController = new TaskController(taskService);

        // Cấu hình Thymeleaf ViewResolver để kiểm thử toàn diện việc render template
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setViewResolvers(viewResolver)
                .build();

        // Seed 2 task mẫu với các trường hợp null và ngày tháng
        Task task1 = new Task();
        task1.setTitle("Task test 1");
        task1.setDescription("Mô tả công việc");
        task1.setStatus(Status.TODO);
        task1.setPriority(Priority.HIGH);
        task1.setDueDate(LocalDate.now().plusDays(2));
        taskService.saveTask(task1);

        Task task2 = new Task();
        task2.setTitle("Task test 2 null dueDate và description");
        task2.setDescription(null);
        task2.setStatus(Status.DONE);
        task2.setPriority(Priority.LOW);
        task2.setDueDate(null);
        taskService.saveTask(task2);
    }

    @Test
    @DisplayName("GET / -> chuyển hướng đến /tasks")
    void testIndexRedirect() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));
    }

    @Test
    @DisplayName("GET /tasks -> render hoàn chỉnh tasks/list.html có phân trang và sắp xếp")
    void testListTasks() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/list"))
                .andExpect(model().attributeExists("tasks", "taskPage", "currentPage", "totalPages", "totalItems",
                        "sortField", "sortDir", "reverseSortDir", "todoCount", "inProgressCount", "doneCount",
                        "statuses", "priorities", "today", "task", "openModal"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Task Manager")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openCreateModal")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openEditModal")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("const shouldOpen = false;")));
    }

    @Test
    @DisplayName("POST /tasks/save hợp lệ -> lưu task, thêm flash message và chuyển hướng đến /tasks")
    void testSaveTaskSuccess() throws Exception {
        mockMvc.perform(post("/tasks/save")
                .param("title", "Công việc mới hợp lệ")
                .param("priority", "HIGH")
                .param("status", "TODO")
                .param("dueDate", "2026-12-31"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attributeExists("message"))
                .andExpect(flash().attribute("alertType", "success"));

        assertEquals(3, database.size());
    }

    @Test
    @DisplayName("POST /tasks/save tiêu đề rỗng -> validation error, thêm flash attributes openModal=true và chuyển hướng /tasks")
    void testSaveTaskValidationError() throws Exception {
        mockMvc.perform(post("/tasks/save")
                .param("title", "") // Title trống vi phạm @NotBlank
                .param("priority", "HIGH")
                .param("status", "TODO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attribute("openModal", true))
                .andExpect(flash().attributeExists("task"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.task"))
                .andExpect(flash().attribute("alertType", "danger"));
    }

    @Test
    @DisplayName("GET /tasks/toggle/{id} -> đổi trạng thái và chuyển hướng /tasks")
    void testToggleStatus() throws Exception {
        mockMvc.perform(get("/tasks/toggle/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attributeExists("message"));

        assertEquals(Status.DONE, database.get(1L).getStatus());
    }

    @Test
    @DisplayName("GET /tasks/delete/{id} -> xóa task và chuyển hướng /tasks")
    void testDeleteTask() throws Exception {
        mockMvc.perform(get("/tasks/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attributeExists("message"));

        assertEquals(1, database.size());
    }
}
