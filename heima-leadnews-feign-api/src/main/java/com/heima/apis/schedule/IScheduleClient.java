package com.heima.apis.schedule;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.schedule.dtos.Task;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("leadnews-schedule") // 定义feign的远程接口
public interface IScheduleClient {

    /**
     * 添加延迟任务 return long
     * @param task
     * @return
     */
    @PostMapping("/api/v1/task/add")
    ResponseResult addTask(@RequestBody Task task);

    /**
     * 取消任务 return 布尔值
     * @param taskId
     * @return
     */
    @GetMapping("/api/v1/task/{taskId}")
    ResponseResult cancelTask(@PathVariable("taskId") long taskId);

    /**
     * 按照类型和优先级拉取任务 return Task
     * @param type
     * @param priority
     * @return
     */
    @GetMapping("/api/v1/task/{type}/{priority}")
    ResponseResult poll(@PathVariable("type") int type, @PathVariable("priority") int priority);

}
