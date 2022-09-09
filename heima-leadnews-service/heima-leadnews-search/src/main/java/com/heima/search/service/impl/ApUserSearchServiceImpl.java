package com.heima.search.service.impl;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.pojos.ApUserSearch;
import com.heima.search.service.ApUserSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ApUserSearchServiceImpl implements ApUserSearchService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存用户搜索记录
     *
     * @param keyword
     * @param useId
     */
    @Override
    @Async
    public void insert(String keyword, Integer useId) {
        // 查询当前用户的搜索关键词
        Query query = Query.query(Criteria.where("userId").is(useId).and("keyword").is(keyword));
        ApUserSearch apUserSearch = mongoTemplate.findOne(query, ApUserSearch.class);

        // 存在，更新创建时间
        if (apUserSearch != null) {
            apUserSearch.setCreatedTime(new Date());
            mongoTemplate.save(apUserSearch);
            return;
        }

        // 不存在，判断当前历史记录总数量是否超过10
        apUserSearch = new ApUserSearch();
        apUserSearch.setUserId(useId);
        apUserSearch.setKeyword(keyword);
        apUserSearch.setCreatedTime(new Date());

        Query query1 = Query.query(Criteria.where("userId").is(useId));
//        query1.with(Sort.by(Sort.Order.desc("createdTime")));
        query1.with(Sort.by(Sort.Direction.DESC,"createdTime"));
        List<ApUserSearch> apUserSearchList = mongoTemplate.find(query1, ApUserSearch.class);

        // 判断当前历史记录总数量是否超过10
        if (apUserSearchList == null || apUserSearchList.size() < 10) {
            mongoTemplate.save(apUserSearch);
        } else {
            ApUserSearch lastUserSearch = apUserSearchList.get(apUserSearchList.size() - 1); // 用户的最后一条搜索记录
            // 替换最后一条记录
            mongoTemplate.findAndReplace(Query.query(Criteria.where("id").is(lastUserSearch.getId())), apUserSearch);
        }
    }

    /**
     * 查询搜索历史
     *
     * @return
     */
    @Override
    public ResponseResult findUserSearch() {

        // 获取当前用户
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //根据用户查询数据，按照时间倒序
        List<ApUserSearch> apUserSearches = mongoTemplate.find(Query.query(Criteria.where("userId").is(user.getId())).with(Sort.by(Sort.Direction.DESC, "createdTime")), ApUserSearch.class);

        return ResponseResult.okResult(apUserSearches);
    }

    /**
     * 删除搜索记录
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult delUserSearch(HistorySearchDto dto) {

        //1.检查参数
        if (dto.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.判断是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if (user == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

         //3.删除
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(user.getId()).and("id").is(dto.getId())), ApUserSearch.class);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
