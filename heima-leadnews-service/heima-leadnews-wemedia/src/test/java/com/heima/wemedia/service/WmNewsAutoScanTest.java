package com.heima.wemedia.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class WmNewsAutoScanTest {

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    @Test
    public void  test(){
        wmNewsAutoScanService.autoScanWmNews(6239);
    }
}
