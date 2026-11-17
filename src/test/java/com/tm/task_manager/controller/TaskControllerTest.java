package com.tm.task_manager.controller;

import com.tm.task_manager.config.SecurityConfig;
import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import com.tm.task_manager.model.User;
import com.tm.task_manager.repository.UserRepository;
import com.tm.task_manager.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private UserRepository userRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User(1L, "admin", "encoded", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));
    }

    @Test
    void listTasks_WhenUnauthenticated_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void listTasks_WhenAuthenticated_ShouldReturnListView() throws Exception {
        when(taskService.getTasksByUser(eq(mockUser), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(taskService.countByUserAndStatus(eq(mockUser), any(Status.class))).thenReturn(0L);

        mockMvc.perform(get("/tasks")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/list"))
                .andExpect(model().attributeExists("tasks", "currentUsername", "todoCount"));
    }

    @Test
    void saveTask_WithValidDataAndCsrf_ShouldRedirectToTasks() throws Exception {
        mockMvc.perform(post("/tasks/save")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("title", "Kiểm thử Spring Security")
                .param("priority", Priority.HIGH.name())
                .param("status", Status.TODO.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attribute("alertType", "success"));
    }

    @Test
    void saveTask_WithBlankTitle_ShouldRedirectWithOpenModalFlag() throws Exception {
        mockMvc.perform(post("/tasks/save")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("title", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attribute("openModal", true))
                .andExpect(flash().attribute("alertType", "danger"));
    }

    @Test
    void toggleStatus_ShouldRedirectToTasks() throws Exception {
        Task toggled = new Task();
        toggled.setStatus(Status.DONE);
        when(taskService.toggleStatusForUser(1L, mockUser)).thenReturn(toggled);

        mockMvc.perform(get("/tasks/toggle/1")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));
    }
}