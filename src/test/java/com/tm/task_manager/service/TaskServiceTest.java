package com.tm.task_manager.service;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import com.tm.task_manager.model.User;
import com.tm.task_manager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private User ownerUser;
    private User otherUser;
    private Task sampleTask;

    @BeforeEach
    void setUp() {
        ownerUser = new User(1L, "admin", "hashed_pass", "ROLE_ADMIN");
        otherUser = new User(2L, "user1", "hashed_pass", "ROLE_USER");

        sampleTask = new Task();
        sampleTask.setId(10L);
        sampleTask.setTitle("Học Spring Security");
        sampleTask.setPriority(Priority.HIGH);
        sampleTask.setStatus(Status.TODO);
        sampleTask.setDueDate(LocalDate.now().plusDays(1));
        sampleTask.setUser(ownerUser);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTasksByUser_ShouldReturnPagedTasksForUser() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Task> mockPage = new PageImpl<>(List.of(sampleTask));
        when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(mockPage);

        Page<Task> result = taskService.getTasksByUser(ownerUser, "Spring", Status.TODO, Priority.HIGH, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Học Spring Security", result.getContent().get(0).getTitle());
    }

    @Test
    void getTaskByIdAndUser_WhenValidOwner_ShouldReturnTask() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

        Task found = taskService.getTaskByIdAndUser(10L, ownerUser);

        assertNotNull(found);
        assertEquals(10L, found.getId());
    }

    @Test
    void getTaskByIdAndUser_WhenUnauthorizedUser_ShouldThrowException() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskByIdAndUser(10L, otherUser));

        assertEquals("Bạn không có quyền truy cập công việc này!", exception.getMessage());
    }

    @Test
    void saveTaskForUser_WhenNewTask_ShouldAssignOwnerAndSave() {
        Task newTask = new Task();
        newTask.setTitle("Task mới");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task saved = taskService.saveTaskForUser(newTask, ownerUser);

        assertEquals(ownerUser, saved.getUser());
        verify(taskRepository, times(1)).save(newTask);
    }

    @Test
    void toggleStatusForUser_ShouldFlipStatusBetweenTodoAndDone() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task toggled = taskService.toggleStatusForUser(10L, ownerUser);
        assertEquals(Status.DONE, toggled.getStatus());

        Task toggledBack = taskService.toggleStatusForUser(10L, ownerUser);
        assertEquals(Status.TODO, toggledBack.getStatus());
    }

    @Test
    void deleteTaskForUser_WhenValidOwner_ShouldDeleteTask() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(sampleTask));

        taskService.deleteTaskForUser(10L, ownerUser);

        verify(taskRepository, times(1)).delete(sampleTask);
    }
}
