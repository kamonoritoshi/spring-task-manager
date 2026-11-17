package com.tm.task_manager.service;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import com.tm.task_manager.model.User;
import com.tm.task_manager.repository.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    // 1. Lấy danh sách công việc CHỈ CỦA USER HIỆN TẠI
    @Transactional(readOnly = true)
    public Page<Task> getTasksByUser(User user, String keyword, Status status, Priority priority, Pageable pageable) {
        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Điều kiện bắt buộc: task phải thuộc về user đang đăng nhập
            predicates.add(cb.equal(root.get("user"), user));

            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return taskRepository.findAll(spec, pageable);
    }

    // 2. Lấy chi tiết task kèm kiểm tra quyền sở hữu
    @Transactional(readOnly = true)
    public Task getTaskByIdAndUser(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));
        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền truy cập công việc này!");
        }
        return task;
    }

    // 3. Lưu hoặc cập nhật task cho User
    public Task saveTaskForUser(Task task, User user) {
        if (task.getId() != null) {
            // Nếu là cập nhật, kiểm tra xem task có đúng của user này không và giữ nguyên
            // createdAt
            Task existingTask = getTaskByIdAndUser(task.getId(), user);
            existingTask.setTitle(task.getTitle());
            existingTask.setDescription(task.getDescription());
            existingTask.setPriority(task.getPriority());
            existingTask.setStatus(task.getStatus());
            existingTask.setDueDate(task.getDueDate());
            return taskRepository.save(existingTask);
        }
        task.setUser(user);
        return taskRepository.save(task);
    }

    // 4. Đổi trạng thái & Xóa có kiểm tra quyền sở hữu
    public Task toggleStatusForUser(Long id, User user) {
        Task task = getTaskByIdAndUser(id, user);
        task.setStatus(task.getStatus() == Status.DONE ? Status.TODO : Status.DONE);
        return taskRepository.save(task);
    }

    public void deleteTaskForUser(Long id, User user) {
        Task task = getTaskByIdAndUser(id, user);
        taskRepository.delete(task);
    }

    // 5. Đếm số lượng theo User & Status
    @Transactional(readOnly = true)
    public long countByUserAndStatus(User user, Status status) {
        return taskRepository.countByUserAndStatus(user, status);
    }
}
