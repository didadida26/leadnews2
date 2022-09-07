package com.heima.wemedia.service;

import java.util.Date;

public interface WmNewsTaskService {

    /**
     * 添加任务到延迟队列中
     * @param id 文章ID
     * @param publishTime 文章发布时间 可以作为任务的执行时间
     */
    void addNews2Task(Integer id, Date publishTime);

    /**
     * 消费任务审核文章
     */
    void scanNewsByTask();

}
