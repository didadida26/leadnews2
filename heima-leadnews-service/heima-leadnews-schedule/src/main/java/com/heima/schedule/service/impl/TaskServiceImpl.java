package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {
    /**
     * 添加延迟任务
     *
     * @param task
     * @return
     */
    @Override
    public long addTask(Task task) {

        //1.添加任务到数据库中
        boolean success = addTask2Db(task);

        //2.添加任务到redis
        if (success) {
            addTask2Cache(task);
        }


        return task.getTaskId();
    }

    /**
     * 取消任务
     *
     * @param taskId
     * @return
     */
    @Override
    public boolean cancelTask(long taskId) {

        boolean flag = false; // 默认失败

        //删除任务，更新日志
        Task task = updateDb(taskId, ScheduleConstants.CANCELLED);

        //删除redis的数据
        if (task != null) {
            removeTaskFromCache(task);
            flag = true;
        }
        return flag;
    }

    /**
     * 按照类型和优先级拉取任务
     *
     * @param type
     * @param priority
     * @return
     */
    @Override
    public Task poll(int type, int priority) {

        Task task = null;

        try {
            String key = type + "_" + priority;

            // 从redis中拉取数据 pop
            String task_json = cacheService.lRightPop(ScheduleConstants.TOPIC + key);

            if (StringUtils.isNotBlank(task_json)) {
                task = JSON.parseObject(task_json, Task.class);
                // 修改数据库信息
                updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        } catch (Exception e) {
            log.error("poll task execption");
        }

        return task;
    }

    /**
     * 未来数据每分钟执行一次
     */
    @Scheduled(cron = "0 */1 * * * ?") // 每分钟执行一次
    public void refresh() {

        String token = cacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30); // 基于redis的setnx
        if (StringUtils.isNotBlank(token)) {
            log.info("未来数据定时刷新==定时任务");

            // 获取所有未来数据的集合key
            Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");

            for (String futureKey : futureKeys) {
                // 获取当前数据的key topic
                String topicKey = ScheduleConstants.TOPIC + futureKey.split(ScheduleConstants.FUTURE)[1];
                // 按照key和分值查询符合条件的数据
                Set<String> tasks = cacheService.zRangeByScore(futureKey, 0, System.currentTimeMillis());

                // 同步数据
                if (!tasks.isEmpty()) {
                    cacheService.refreshWithPipeline(futureKey, topicKey, tasks);
                    log.info("成功将" + futureKey + "刷新到了" + topicKey);
                }
            }
        }
    }

    /**
     * 数据库任务定时同步到redis
     * 每5分钟执行一次
     */
//    @PostConstruct // 初始化方法，微服务一启动方法就会执行
//    @Scheduled(cron = "0 */5 * * * ?")
    @Scheduled(cron = "0 */5 * * * ?")
    @PostConstruct
    public void reloadData() {

        // 清理缓存中的数据
        clearCache();

        // 查询符合条件的任务，小于未来5分钟的数据
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        Date date = calendar.getTime();// 未来5分钟
        List<Taskinfo> taskinfoList = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime, date));

        // 把任务添加到redis
        if (taskinfoList != null && taskinfoList.size() > 0) {
            for (Taskinfo taskinfo : taskinfoList) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo, task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTask2Cache(task);
            }
        }
        log.info("数据库任务同步到了redis");

    }

    /**
     * 清理缓存中的数据
     */
    public void clearCache() {
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*"); //获取"future_"开头的key
        Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*"); //获取"topic_"开头的key
        // 删除
        cacheService.delete(futureKeys);
        cacheService.delete(topicKeys);
    }

    /**
     * 删除redis的数据
     *
     * @param task
     */
    private void removeTaskFromCache(Task task) {
        String key = task.getTaskType() + "_" + task.getPriority();

        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            // 从list删除
            cacheService.lRemove(ScheduleConstants.TOPIC + key, 0, JSON.toJSONString(task));
        } else {
            //2.2 从zset删除
            cacheService.zRemove(ScheduleConstants.FUTURE + key, JSON.toJSONString(task));
        }

    }

    /**
     * 删除任务，更新日志
     *
     * @param taskId
     * @param status
     * @return
     */
    private Task updateDb(long taskId, int status) {
        Task task = null;
        try {
            // 删除任务
            taskinfoMapper.deleteById(taskId);

            // 更新任务日志
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            taskinfoLogs.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLogs);

            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs, task);
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        } catch (BeansException e) {
            log.error("task cancel exception taskId:{}", taskId);
        }

        return task;
    }


    @Autowired
    private CacheService cacheService;

    /**
     * 把任务添加到redis中
     *
     * @param task
     */
    private void addTask2Cache(Task task) {

        String key = task.getTaskType() + "_" + task.getPriority();

        // 获取5分钟后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        long nextScheduleTime = calendar.getTimeInMillis(); // 未来5分钟的时间


        //2.1 如果任务的执行时间小于等于当前时间，存入list
        // 判断任务执行时间与当前时间大小
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() <= nextScheduleTime) {
            //2.2 如果任务的执行时间大于当前时间 && 小于等于预设时间（未来5分钟） 存入zset中
            // zset 分值为任务执行时间
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());
        }

    }

    @Autowired
    private TaskinfoMapper taskinfoMapper;
    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;

    /**
     * 添加任务到数据库中
     *
     * @param task
     * @return
     */
    private boolean addTask2Db(Task task) {

        boolean flag = false;

        try {
            //保存任务表
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);

            // 设置taskID
            task.setTaskId(taskinfo.getTaskId());

            //保存任务日志数据
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogs.setVersion(1); // 乐观锁版本号
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
}
