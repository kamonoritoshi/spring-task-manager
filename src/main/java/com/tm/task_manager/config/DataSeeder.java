package com.tm.task_manager.config;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import com.tm.task_manager.model.User;
import com.tm.task_manager.repository.TaskRepository;
import com.tm.task_manager.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Tạo tài khoản mẫu nếu bảng users đang trống
        User adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRole("ROLE_ADMIN");
            return userRepository.save(admin);
        });

        if (!userRepository.existsByUsername("user1")) {
            User user1 = new User();
            user1.setUsername("user1");
            user1.setPassword(passwordEncoder.encode("123456"));
            user1.setRole("ROLE_USER");
            userRepository.save(user1);
        }

        // 2. Chèn 5 công việc mẫu cho tài khoản admin nếu bảng tasks đang trống
        if (taskRepository.count() == 0) {
            log.info("Bảng tasks đang trống, tiến hành chèn 5 công việc mẫu cho user 'admin'...");

            Task task1 = new Task();
            task1.setTitle("Nộp báo cáo tiến độ dự án quý 4");
            task1.setDescription(
                    "Tổng hợp các tính năng đã hoàn thiện và báo cáo các đầu việc còn tồn đọng cho Project Manager.");
            task1.setPriority(Priority.HIGH);
            task1.setStatus(Status.TODO);
            task1.setDueDate(LocalDate.now().minusDays(3));
            task1.setUser(adminUser);

            Task task2 = new Task();
            task2.setTitle("Họp review kiến trúc cơ sở dữ liệu");
            task2.setDescription("Thảo luận về thiết kế bảng, quan hệ giữa các entity và tối ưu chỉ mục index.");
            task2.setPriority(Priority.HIGH);
            task2.setStatus(Status.IN_PROGRESS);
            task2.setDueDate(LocalDate.now());
            task2.setUser(adminUser);

            Task task3 = new Task();
            task3.setTitle("Thiết kế giao diện Thymeleaf và Bootstrap");
            task3.setDescription(
                    "Xây dựng layout chuẩn, bảng hiển thị danh sách công việc và form nhập liệu trực quan.");
            task3.setPriority(Priority.MEDIUM);
            task3.setStatus(Status.IN_PROGRESS);
            task3.setDueDate(LocalDate.now().plusDays(2));
            task3.setUser(adminUser);

            Task task4 = new Task();
            task4.setTitle("Viết tài liệu hướng dẫn sử dụng");
            task4.setDescription(
                    "Soạn thảo tài liệu README chi tiết kèm hình ảnh minh họa cho các chức năng của ứng dụng.");
            task4.setPriority(Priority.LOW);
            task4.setStatus(Status.TODO);
            task4.setDueDate(LocalDate.now().plusDays(7));
            task4.setUser(adminUser);

            Task task5 = new Task();
            task5.setTitle("Khởi tạo dự án Spring Boot và cấu hình MySQL");
            task5.setDescription("Tạo khung dự án Spring Boot, tích hợp Spring Data JPA, Spring Security và MySQL.");
            task5.setPriority(Priority.MEDIUM);
            task5.setStatus(Status.DONE);
            task5.setDueDate(LocalDate.now().minusDays(1));
            task5.setUser(adminUser);

            taskRepository.saveAll(List.of(task1, task2, task3, task4, task5));
            log.info("Đã khởi tạo thành công 5 công việc mẫu vào MySQL.");
        }
    }
}
