package com.tm.task_manager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tiêu đề công việc không được để trống")
    @Size(max = 150, message = "Tiêu đề tối đa 150 ký tự")
    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.TODO;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Tự động gán thời gian tạo và giá trị mặc định trước khi lưu xuống DB
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.priority == null) {
            this.priority = Priority.MEDIUM;
        }
        if (this.status == null) {
            this.status = Status.TODO;
        }
    }
}
