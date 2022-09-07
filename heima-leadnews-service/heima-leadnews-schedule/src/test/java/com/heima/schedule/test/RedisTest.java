package com.heima.schedule.test;

import com.alibaba.fastjson.JSON;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.sun.istack.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;

import java.util.Date;
import java.util.List;
import java.util.Set;

@SpringBootTest
public class RedisTest {

    @Autowired
    private CacheService cacheService;

    @Test
    public void testList() {
        //在list的左边添加元素
//        cacheService.lLeftPush("list_001", "hello,redis");

        //在list的右边获取元素，并删除
        String rightPop = cacheService.lRightPop("list_001");
        System.out.println(rightPop);
    }

    @Test
    public void testZset() {

        //添加数据到zset中  分值
//        cacheService.zAdd("zset_key_001","hello zset 001",1000);
//        cacheService.zAdd("zset_key_001","hello zset 002",8888);
//        cacheService.zAdd("zset_key_001","hello zset 003",7777);
//        cacheService.zAdd("zset_key_001","hello zset 004",999999);

        //按照分值获取数据
        Set<String> zset_key_001 = cacheService.zRangeByScore("zset_key_001", 0, 8888);
        System.out.println(zset_key_001);

    }

    @Test
    public void testKeys() {
//        Set<String> set = cacheService.keys("future_*"); 性能低，不用

        Set<String> scan = cacheService.scan("future_*");
        System.out.println(scan);
    }


    //耗时6151
    @Test
    public void testPiple1() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            Task task = new Task();
            task.setTaskType(1001);
            task.setPriority(1);
            task.setExecuteTime(new Date().getTime());
            cacheService.lLeftPush("1001_1", JSON.toJSONString(task));
        }
        System.out.println("耗时" + (System.currentTimeMillis() - start));
    }


    @Test
    public void testPiple2() {
        long start = System.currentTimeMillis();
        //使用管道技术
        List<Object> objectList = cacheService.getstringRedisTemplate().executePipelined((RedisCallback<Object>) redisConnection -> {
            for (int i = 0; i < 10000; i++) {
                Task task = new Task();
                task.setTaskType(1001);
                task.setPriority(1);
                task.setExecuteTime(new Date().getTime());
                redisConnection.lPush("1001_1".getBytes(), JSON.toJSONString(task).getBytes());
            }
            return null;
        });
        System.out.println("使用管道技术执行10000次自增操作共耗时:" + (System.currentTimeMillis() - start) + "毫秒");
    }

}
