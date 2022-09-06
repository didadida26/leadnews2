package com.heima.apis.article;

import com.heima.apis.article.fallback.IArticleClientFallback;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(value = "leadnews-article", fallback = IArticleClientFallback.class) // 熔断降级
public interface IArticleClient {
    /**
     * 保存文章
     * @param dto
     * @return
     */
    @PostMapping("/api/v1/article/save")
    ResponseResult saveArticle(ArticleDto dto);

}
