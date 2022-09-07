package com.heima.schedule.service.impl;

import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootTest
public class TaskServiceImplTest {

    @Autowired
    private TaskService taskService;

    @Test
    public void test(){

        for (int i = 0; i<5 ; i++) {
            Task task = new Task();
            task.setTaskType(100 + i);
            task.setPriority(50);
            task.setParameters("task taa".getBytes(StandardCharsets.UTF_8));
            task.setExecuteTime(new Date().getTime() + 500*i);

            taskService.addTask(task);
        }


    }

    @Test
    public void cancelTask(){
        boolean cancelTask = taskService.cancelTask(1567311095239122945L);
        System.out.println(cancelTask);
    }

    @Test
    public void pollTask(){
        Task task = taskService.poll(100, 50);
        System.out.println(task);
    }
}
