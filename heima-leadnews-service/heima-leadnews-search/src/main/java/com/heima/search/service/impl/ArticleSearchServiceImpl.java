package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.service.ApUserSearchService;
import com.heima.search.service.ArticleSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ArticleSearchServiceImpl implements ArticleSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private ApUserSearchService apUserSearchService;

    /**
     * es文章分页检索
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) throws IOException {

        //1.检查参数
        if (dto == null || StringUtils.isBlank(dto.getSearchWords())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID); // 参数失效
        }

        long id = Thread.currentThread().getId();
        System.err.println("当前线程ID：" + id);


        ApUser user = AppThreadLocalUtil.getUser();

        // 异步调用 保存搜索记录
        if (user != null && dto.getFromIndex() == 0) { // 只保存一次
            apUserSearchService.insert(dto.getSearchWords(), user.getId());
        }


        //2.设置查询条件
        // 布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        SearchRequest searchRequest = new SearchRequest("app_info_article"); // 设置索引名称
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); // 设置参数 放入searchSourceBuilder

        //关键字的分词之后查询 去title和content域查询
        QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(dto.getSearchWords())
                .field("title").field("content").defaultOperator(Operator.OR);
        boolQueryBuilder.must(queryStringQueryBuilder); // 放入布尔查询

        //查询小于mindate的数据 范围查询 建议参考索引库的字段名称
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime().getTime());
        boolQueryBuilder.filter(rangeQueryBuilder); // 放入布尔查询里

        //分页查询
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(dto.getPageSize());

        //按照发布时间倒序查询
        searchSourceBuilder.sort("publishTime", SortOrder.DESC);

        //设置高亮  title
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title"); // 指定要高亮的字段
        highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>"); // 前缀
        highlightBuilder.postTags("</font>"); // 后缀
        searchSourceBuilder.highlighter(highlightBuilder);

        searchSourceBuilder.query(boolQueryBuilder); // 把布尔查询放入SearchSourceBuilder
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //3.结果封装返回
        List<Map> list = new ArrayList<>();

        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            Map map = JSON.parseObject(json, Map.class); // 解析成map
            // 处理高亮
            if (hit.getHighlightFields() != null && hit.getHighlightFields().size() > 0) {
                Text[] titles = hit.getHighlightFields().get("title").getFragments();
                String title = StringUtils.join(titles);
                map.put("h_title", title); // 把含有高亮的title放入map

            } else {
                map.put("h_title", map.get("title")); //原始标题
            }
            list.add(map);

        }
        return ResponseResult.okResult(list);
    }
}
