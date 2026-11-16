package com.tm.task_manager.service;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
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

    /**
     * Lấy danh sách công việc có phân trang và sắp xếp, hỗ trợ kết hợp động các bộ lọc keyword, status, priority.
     */
    @Transactional(readOnly = true)
    public Page<Task> getTasks(String keyword, Status status, Priority priority, Pageable pageable) {
        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

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

    /**
     * Lấy danh sách công việc có hỗ trợ lọc theo status, priority hoặc tìm kiếm theo keyword.
     * Nếu không truyền điều kiện lọc, trả về toàn bộ công việc sắp xếp theo dueDate tăng dần.
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks(Status status, Priority priority, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return taskRepository.findByTitleContainingIgnoreCaseOrderByDueDateAsc(keyword.trim());
        }
        if (status != null && priority != null) {
            return taskRepository.findByStatusAndPriorityOrderByDueDateAsc(status, priority);
        }
        if (status != null) {
            return taskRepository.findByStatusOrderByDueDateAsc(status);
        }
        if (priority != null) {
            return taskRepository.findByPriorityOrderByDueDateAsc(priority);
        }
        return taskRepository.findAllByOrderByDueDateAsc();
    }

    /**
     * Lấy toàn bộ danh sách công việc sắp xếp theo hạn chót tăng dần.
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return taskRepository.findAllByOrderByDueDateAsc();
    }

    /**
     * Lấy chi tiết 1 công việc theo id.
     * Ném RuntimeException với thông báo tiếng Việt nếu không tìm thấy.
     */
    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));
    }

    /**
     * Lưu mới hoặc cập nhật công việc.
     */
    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    /**
     * Đổi nhanh trạng thái: nếu đang là DONE thì chuyển về TODO, ngược lại chuyển sang DONE.
     */
    public Task toggleStatus(Long id) {
        Task task = getTaskById(id);
        if (task.getStatus() == Status.DONE) {
            task.setStatus(Status.TODO);
        } else {
            task.setStatus(Status.DONE);
        }
        return taskRepository.save(task);
    }

    /**
     * Xóa công việc theo id.
     * Ném RuntimeException nếu không tìm thấy công việc.
     */
    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }

    /**
     * Thống kê số lượng task theo từng trạng thái (Map với key là Enum Status).
     */
    @Transactional(readOnly = true)
    public Map<Status, Long> getTaskCountByStatus() {
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            counts.put(status, taskRepository.countByStatus(status));
        }
        return counts;
    }

    /**
     * Thống kê số lượng task theo từng trạng thái (Map với key là String: "TODO", "IN_PROGRESS", "DONE", "TOTAL").
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getTaskStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("TODO", taskRepository.countByStatus(Status.TODO));
        stats.put("IN_PROGRESS", taskRepository.countByStatus(Status.IN_PROGRESS));
        stats.put("DONE", taskRepository.countByStatus(Status.DONE));
        stats.put("TOTAL", taskRepository.count());
        return stats;
    }

    /**
     * Đếm số lượng task theo trạng thái cụ thể.
     */
    @Transactional(readOnly = true)
    public long countByStatus(Status status) {
        return taskRepository.countByStatus(status);
    }
}
