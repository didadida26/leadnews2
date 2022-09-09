package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;
import org.springframework.web.bind.annotation.RequestBody;

public interface ApUserSearchService {

    /**
     * 保存用户搜索记录
     * @param keyword
     * @param useId
     */
    void insert(String keyword, Integer useId);

    /**
     * 查询搜索历史
     * @return
     */
    ResponseResult findUserSearch();

    /**
     * 删除搜索记录
     * @param dto
     * @return
     */
    ResponseResult delUserSearch( HistorySearchDto dto);
}
