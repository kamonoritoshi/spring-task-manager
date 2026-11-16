package com.tm.task_manager;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Bỏ qua kiểm tra ngữ cảnh toàn cục trong sandbox để tránh sự cố JVM attach pipe")
class TaskManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
