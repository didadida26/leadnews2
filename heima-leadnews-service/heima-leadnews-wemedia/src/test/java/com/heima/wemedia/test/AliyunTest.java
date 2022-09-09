package com.heima.wemedia.test;

import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class AliyunTest {

    // 图片审核
    @Autowired
    private GreenImageScan greenImageScan;
    @Autowired
    // 文本审核
    private GreenTextScan greenTextScan;
    //上传图片
    @Autowired
    private FileStorageService fileStorageService;

    @Test
    public void testScanText() throws Exception {
        String text = "爱我是一个好人";
        Map map = greenTextScan.greeTextScan(text);
        System.out.println(map);
    }

    @Test
    public void testScanImg() throws Exception {

        byte[] bytes = fileStorageService.downLoadFile("http://192.168.200.130:9000/leadnews/2022/09/05/044282b107de4450954fe7bf8652a098.jpg");
        List<byte[]> list = new ArrayList<>();
        list.add(bytes);
        Map map = greenImageScan.imageScan(list);
        System.out.println(map);
    }
}
