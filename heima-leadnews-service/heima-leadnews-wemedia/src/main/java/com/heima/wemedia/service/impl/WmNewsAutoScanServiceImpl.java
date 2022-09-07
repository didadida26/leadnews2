package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {

    @Autowired
    private WmNewsMapper wmNewsMapper;

    /**
     * 自媒体文章审核
     *
     * @param id 自媒体文章ID
     */
    @Override
    @Async // 当前方法异步
    public void autoScanWmNews(Integer id) {
        // 查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(id);
        if (wmNews == null) {
            throw new RuntimeException("WmNewsAutoScanServiceImpl 文章不存在");
        }


        //审核图文内容
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            // 从内容中提取纯文本内容和图片
            Map<String, Object> textAndImages = handleTextAndImages(wmNews);

            // 自管理的敏感词过滤
            boolean isSensitive = handleSensitiveScan((String) textAndImages.get("content"), wmNews);
            if (!isSensitive) {
                return;
            }

            // 审核文本 如果isTextScan为true不需要执行后面的动作了 审核成功 true 失败 FALSE
            boolean isTextScan = handleTextScan((String) textAndImages.get("content"), wmNews);

            if (!isTextScan) {
                return;
            }

            // 审核图片
            boolean isImageScan = handleImageScan((List<String>) textAndImages.get("images"), wmNews);
            if (!isImageScan) {
                return;
            }

            // 审核成功，保存app端相关文章数据
            ResponseResult responseResult = saveAppArticle(wmNews);
            if (!responseResult.getCode().equals(200)) {
                throw new RuntimeException("WmNewsAutoScanServiceImpl 文章审核，保存app端相关文章失败");
            }

            // 回填article_id
            wmNews.setArticleId((Long) responseResult.getData());
            updateWmNews(wmNews, (short)9, "审核成功");

        }

    }

    @Autowired
    private WmSensitiveMapper wmSensitiveMapper;

    /**
     * 自管理敏感词审核
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleSensitiveScan(String content, WmNews wmNews) {

        boolean flag = true;

        // 获取所有敏感词 只查询sensitive一个字段
        List<WmSensitive> wmSensitives = wmSensitiveMapper.selectList(Wrappers.<WmSensitive>lambdaQuery().select(WmSensitive::getSensitives));
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());

        // 初始化敏感词库
        SensitiveWordUtil.initMap(sensitiveList);

        // 查看文章中是否有敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content + wmNews.getTitle());
        if (map.size() > 0) {
            // 存在敏感词
            updateWmNews(wmNews, 2, "存在违规" + map);
            flag = false;
        }

        return flag;
    }

    @Autowired
    private IArticleClient articleClient;
    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 保存app端相关文章数据
     * @param wmNews
     */
    private ResponseResult saveAppArticle(WmNews wmNews) {

        ArticleDto dto = new ArticleDto();
        // 拷贝属性
        BeanUtils.copyProperties(wmNews, dto);
        dto.setLayout(wmNews.getType()); // 布局
        // 根据频道ID查询频道名字
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null) {
            dto.setChannelName(wmChannel.getName());
        }

        // 作者
        dto.setAuthorId(wmNews.getUserId().longValue());  // 设置作者ID
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmNews != null) {
            dto.setAuthorName(wmUser.getName()); // 设置作者名字
        }

        // 设置文章ID
        if (wmNews.getArticleId() != null){
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());

        ResponseResult responseResult = articleClient.saveArticle(dto); // 包含文章ID
        return responseResult;
    }


    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private GreenImageScan greenImageScan;
    @Autowired
    private Tess4jClient tess4jClient;

    /**
     * 审核图片
     *
     * @param images
     * @param wmNews
     * @return
     */
    private boolean handleImageScan(List<String> images, WmNews wmNews) {

        boolean flag = true; // 默认为true

        if (images == null || images.size() == 0) {
            return flag;
        }

        List<byte[]> imageList = new ArrayList<>();

        // 去除重复图片链接
        images = images.stream().distinct().collect(Collectors.toList());

        try {
            //1. 下载图片
            for (String image : images) {
                byte[] bytes = fileStorageService.downLoadFile(image);

                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage bufferedImage = ImageIO.read(in);
                // 图片识别
                String result = tess4jClient.doOCR(bufferedImage);
                // 敏感词过滤
                boolean isSensitive = handleSensitiveScan(result, wmNews);
                if (!isSensitive) {
                    return false;
                }

                imageList.add(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TesseractException e) {
            e.printStackTrace();
        }

        // 审核图片
        try {
            Map map = greenImageScan.imageScan(imageList);
            if (map != null) {
                if (map.get("suggestion").equals("block")) {
                    // 审核失败
                    flag = false;
                    updateWmNews(wmNews, 2, "当前文章存在违规内容");
                }

                if (map.get("suggestion").equals("review")) {
                    // 审核失败 需要人工审核
                    flag = false;
                    updateWmNews(wmNews, 3, "当前文章需要进一步审核");
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;
    }


    @Autowired
    private GreenTextScan greenTextScan;


    /**
     * 审核纯文本内容
     *
     * @param content
     * @param wmNews
     * @return
     */
    private boolean handleTextScan(String content, WmNews wmNews) {

        boolean flag = true; // 默认审核通过

        if (wmNews.getTitle().length() == 0) {
            return flag;
        }
        if (content.length() == 0) {
            return flag;
        }

        try {
            Map map = greenTextScan.greeTextScan(wmNews.getTitle() + "-" + content); // 审核标题和文本
            if (map != null) {
                if (map.get("suggestion").equals("block")) {
                    // 审核失败
                    flag = false;
                    updateWmNews(wmNews, 2, "当前文章存在违规内容");
                }

                if (map.get("suggestion").equals("review")) {
                    // 审核失败 需要人工审核
                    flag = false;
                    updateWmNews(wmNews, 3, "当前文章需要进一步审核");
                }
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 修改文章内容
     *
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, int status, String reason) {
        wmNews.setStatus((short) status); //
        wmNews.setReason(reason);
        wmNewsMapper.updateById(wmNews);
    }

    /**
     * 从自媒体文章内容中提取文本和内容
     * 提取文章封面图片
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {

        StringBuilder stringBuilder = new StringBuilder(); // 存储纯文本内容
        List<String> images = new ArrayList<>();


        if (StringUtils.isNotBlank(wmNews.getContent())) {
            List<Map> maps = JSON.parseArray(wmNews.getContent(), Map.class);
            for (Map map : maps) {
                if (map.get("type").equals("text")) {
                    // 文本内容
                    stringBuilder.append(map.get("value"));
                }
                if (map.get("type").equals("image")) {
                    // 提取图片
                    images.add((String) map.get("value"));
                }
            }
        }

        // 提取文章封面图片
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split)); // 装入图片集合
        }

        // 封装为map返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("content", stringBuilder.toString());
        resultMap.put("images", images);

        return resultMap;
    }
}
