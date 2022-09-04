package com.heima.article.test;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.ArticleApplication;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成文章的静态模板文件，然后上传到MinIO
 */
@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
public class ArticleFreemarkerTest {

    // 查询文章内容的Mapper
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;
    // freemarker
    @Autowired
    private Configuration configuration;
    // MinIO 文件上传
    @Autowired
    private FileStorageService fileStorageService;
    // 保存文章
    @Autowired
    private ApArticleService apArticleService;


    @Test
    public void createStaticUrlTest() throws Exception{
        // 根据文章ID查询
        //1.获取文章内容
        ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, "1383827787629252610L"));

        if (apArticleContent != null && StringUtils.isNotBlank(apArticleContent.getContent())) {
            //2.文章内容通过freemarker生成html文件
            Template template = configuration.getTemplate("article.ftl");

            // 准备数据模型
            HashMap<String, Object> content = new HashMap<>();
            content.put("content", JSON.parseArray(apArticleContent.getContent()));
            // 输出流
            StringWriter out = new StringWriter();
            template.process(content, out);

            //3.把html文件上传到minio中
            // 从out获取数据转换成inputstream
            InputStream in = new ByteArrayInputStream(out.toString().getBytes(StandardCharsets.UTF_8));
            // 模板文件在MinIO 的访问路径
            String path = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", in);

            //4.修改ap_article表，保存static_url字段
            ApArticle article = new ApArticle();
            article.setId(apArticleContent.getArticleId());
            article.setStaticUrl(path);
            apArticleService.updateById(article);

        }

    }

    @Test
    public void test3() throws Exception{
        FileInputStream in = new FileInputStream("D:\\1.jpg");
        String path = fileStorageService.uploadImgFile("", "a.jpg", in);
        System.out.println(path);
    }
}
