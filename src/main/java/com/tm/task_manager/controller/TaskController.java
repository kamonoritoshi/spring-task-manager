package com.tm.task_manager.controller;

import com.tm.task_manager.model.Priority;
import com.tm.task_manager.model.Status;
import com.tm.task_manager.model.Task;
import com.tm.task_manager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /**
     * Chuyển hướng trang chủ về danh sách công việc.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/tasks";
    }

    /**
     * Hiển thị danh sách công việc có phân trang, sắp xếp và lọc.
     */
    @GetMapping("/tasks")
    public String listTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "dueDate") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {

        // Giới hạn trường sắp xếp an toàn giữa dueDate và createdAt
        String validSortField = "createdAt".equalsIgnoreCase(sortField) ? "createdAt" : "dueDate";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, validSortField);
        Pageable pageable = PageRequest.of(Math.max(0, page), size > 0 ? size : 5, sort);

        Page<Task> taskPage = taskService.getTasks(keyword, status, priority, pageable);

        model.addAttribute("taskPage", taskPage);
        model.addAttribute("tasks", taskPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", taskPage.getTotalPages());
        model.addAttribute("totalItems", taskPage.getTotalElements());
        model.addAttribute("sortField", validSortField);
        model.addAttribute("sortDir", direction.name().toLowerCase());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");

        model.addAttribute("todoCount", taskService.countByStatus(Status.TODO));
        model.addAttribute("inProgressCount", taskService.countByStatus(Status.IN_PROGRESS));
        model.addAttribute("doneCount", taskService.countByStatus(Status.DONE));
        model.addAttribute("statuses", Status.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("today", LocalDate.now());

        // Đảm bảo luôn có đối tượng task cho Modal nếu chưa có từ flash attributes
        if (!model.containsAttribute("task")) {
            model.addAttribute("task", new Task());
        }
        boolean openModal = Boolean.TRUE.equals(model.asMap().get("openModal"));
        model.addAttribute("openModal", openModal);

        // Giữ lại các giá trị lọc trên thanh tìm kiếm
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);

        return "tasks/list";
    }

    /**
     * Lưu mới hoặc cập nhật công việc từ Modal.
     */
    @PostMapping("/tasks/save")
    public String saveTask(
            @Valid @ModelAttribute("task") Task task,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("task", task);
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "task", bindingResult);
            redirectAttributes.addFlashAttribute("openModal", true);
            redirectAttributes.addFlashAttribute("message", "Vui lòng kiểm tra lại thông tin công việc!");
            redirectAttributes.addFlashAttribute("alertType", "danger");
            return "redirect:/tasks";
        }

        boolean isNew = (task.getId() == null);
        taskService.saveTask(task);
        redirectAttributes.addFlashAttribute("message",
                isNew ? "Thêm mới công việc thành công!" : "Cập nhật công việc thành công!");
        redirectAttributes.addFlashAttribute("alertType", "success");
        return "redirect:/tasks";
    }

    /**
     * Đổi nhanh trạng thái (TODO <-> DONE).
     */
    @GetMapping("/tasks/toggle/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Task updatedTask = taskService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("message",
                "Đã đổi trạng thái công việc sang: " + updatedTask.getStatus().getDisplayName());
        redirectAttributes.addFlashAttribute("alertType", "info");
        return "redirect:/tasks";
    }

    /**
     * Xóa công việc theo ID.
     */
    @GetMapping("/tasks/delete/{id}")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        taskService.deleteTask(id);
        redirectAttributes.addFlashAttribute("message", "Đã xóa công việc thành công!");
        redirectAttributes.addFlashAttribute("alertType", "success");
        return "redirect:/tasks";
    }

    /**
     * Bắt ngoại lệ nếu không tìm thấy task theo ID và chuyển hướng về trang danh sách.
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", ex.getMessage());
        redirectAttributes.addFlashAttribute("alertType", "danger");
        return "redirect:/tasks";
    }
}
