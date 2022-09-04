package com.heima.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional  // 涉及到article和content两张表联合查询
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    private final static Integer MAX_PAGE_SIZE = 50;

    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 加载文章列表
     *
     * @param dto
     * @param type 1 加载更多  2 加载最新
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {

        Integer size = dto.getSize();  // 每页条数
        //1.校验参数 分页条数校验
        if (dto.getSize() == 0 || dto.getSize() == null) {
            size = 10; // 如果size不存在 默认10条
        }

        //类型参数检验  分页值不超过50
        size = Math.min(MAX_PAGE_SIZE, size);
        dto.setSize( size);

        // 文章加载类型 如果type不等于1也不等于2 默认为1：加载更多
        if (!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)) {
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        // 文章频道校验 如果tag为空 默认加载默认频道
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        //时间校验 如果为空最大时间和最小时间设置
        if (dto.getMaxBehotTime() == null){
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime() == null){
            dto.setMinBehotTime(new Date());
        }

        //2.查询数据
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, type);

        //3.结果封装
        return ResponseResult.okResult(apArticles);
    }
}
