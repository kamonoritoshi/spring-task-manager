package com.tm.task_manager.repository;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
// import org.springframework.stereotype.Repository;

import java.util.List;

// @Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    // Lấy tất cả công việc, sắp xếp theo hạn chót tăng dần (việc gấp xếp trên)
    List<Task> findAllByOrderByDueDateAsc();

    // Lọc danh sách công việc theo trạng thái (TODO, IN_PROGRESS, DONE)
    List<Task> findByStatusOrderByDueDateAsc(Status status);

    // Lọc danh sách công việc theo mức độ ưu tiên (LOW, MEDIUM, HIGH)
    List<Task> findByPriorityOrderByDueDateAsc(Priority priority);

    // Lọc danh sách công việc theo cả trạng thái và mức độ ưu tiên
    List<Task> findByStatusAndPriorityOrderByDueDateAsc(Status status, Priority priority);

    // Tìm kiếm công việc có tiêu đề chứa từ khóa (không phân biệt hoa thường)
    List<Task> findByTitleContainingIgnoreCaseOrderByDueDateAsc(String keyword);

    // Đếm số lượng công việc theo từng trạng thái để làm thẻ thống kê nhanh
    long countByStatus(Status status);

    // đếm số lượng công việc theo từng User và Status
    long countByUserAndStatus(com.tm.task_manager.model.User user, Status status);
}
