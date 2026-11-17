package com.tm.task_manager.config;

import com.tm.task_manager.model.User;
import com.tm.task_manager.repository.TaskRepository;
import com.tm.task_manager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void run_WhenDatabaseEmpty_ShouldSeedUsersAndTasks() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("user1")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.count()).thenReturn(0L);

        dataSeeder.run();

        verify(userRepository, times(2)).save(any(User.class));
        verify(taskRepository, times(1)).saveAll(anyList());
    }

    @Test
    void run_WhenTasksAlreadyExist_ShouldNotSeedTasks() {
        User existingAdmin = new User(1L, "admin", "encoded", "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.existsByUsername("user1")).thenReturn(true);
        when(taskRepository.count()).thenReturn(5L);

        dataSeeder.run();

        verify(userRepository, never()).save(any(User.class));
        verify(taskRepository, never()).saveAll(anyList());
    }
}
