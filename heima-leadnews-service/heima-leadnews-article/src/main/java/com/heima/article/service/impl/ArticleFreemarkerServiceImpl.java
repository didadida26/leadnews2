package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.search.vos.SearchArticleVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Service
@Slf4j
@Transactional
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {


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

    /**
     * 生成静态文件到minio中
     *
     * @param apArticle
     * @param content
     */
    @Override
    @Async  // 异步调用
    public void buildArticle2MinIO(ApArticle apArticle, String content) {

        // 根据文章ID查询
        //1.获取文章内容

        if (StringUtils.isNotBlank(content)) {
            //2.文章内容通过freemarker生成html文件
            Template template = null;
            // 输出流
            StringWriter out = new StringWriter();
            try {
                template = configuration.getTemplate("article.ftl");

                // 准备数据模型
                HashMap<String, Object> contentDataModel = new HashMap<>();
                contentDataModel.put("content", JSON.parseArray(content));

                template.process(contentDataModel, out);

            } catch (Exception e) {
                e.printStackTrace();
            }

            //3.把html文件上传到minio中
            // 从out获取数据转换成inputstream
            InputStream in = new ByteArrayInputStream(out.toString().getBytes(StandardCharsets.UTF_8));
            // 模板文件在MinIO 的访问路径
            String path = fileStorageService.uploadHtmlFile("", apArticle.getId() + ".html", in);

            //4.修改ap_article表，保存static_url字段
            ApArticle article = new ApArticle();
            article.setId(apArticle.getId());
            article.setStaticUrl(path);
            apArticleService.updateById(article);

            // 发送消息创建索引
            createArticleESIndex(apArticle, content, path);

        }
    }

    @Autowired
    private KafkaTemplate<String,  String> kafkaTemplate;

    /**
     *发送消息，创建索引
     * @param apArticle
     * @param content
     * @param path
     */
    private void createArticleESIndex(ApArticle apArticle, String content, String path) {
        SearchArticleVo vo = new SearchArticleVo();
        BeanUtils.copyProperties(apArticle, vo);

        vo.setContent(content);
        vo.setStaticUrl(path);

        kafkaTemplate.send(ArticleConstants.ARTICLE_ES_SYNC_TOPIC, JSON.toJSONString(vo));
    }
}
