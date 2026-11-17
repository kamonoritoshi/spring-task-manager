package com.tm.task_manager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER";
}
