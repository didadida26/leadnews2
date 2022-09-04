package com.heima.minio.test;

import com.heima.file.service.FileStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest
public class MinIOTest {

    // MinIO 的上传图片，下载图片，删除图片，上传HTML等
    @Autowired
    private FileStorageService fileStorageService;

    // 如果上传后MinIO文件不能访问，需要设置bucket权限 read and write
    @Test
    public void test(){

        try {
            // 文件输入流
            FileInputStream fileInputStream = new FileInputStream("H:\\1\\js\\index.js");

            //1.创建minio链接客户端
            // MinIO 凭证 accessKey：用户名 secretKey：密码
            MinioClient minioClient = MinioClient.builder().credentials("minio", "minio123")
                    .endpoint("http://192.168.200.130:9000/").build();

            //2.上传
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                            .object("plugins/js/index.js") // 文件名称
                            .contentType("text/js") // 文件类型
                            .bucket("leadnews") // 文件夹名称
                            .stream(fileInputStream,  fileInputStream.available(), -1).build();
            minioClient.putObject(putObjectArgs);

            // 访问路径
//            System.out.println("http://192.168.200.130:9000/leadnews/list10.html");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 引入 heima-file-starter MinIO File
    @Test
    public void test2(){
        try {
            FileInputStream fileInputStream = new FileInputStream("D:\\list10.html");
            String path = fileStorageService.uploadHtmlFile("", "list101.html", fileInputStream);
            System.out.println(path);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
