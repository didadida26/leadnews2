package com.heima.model.wemedia.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;

/**
 * 素材图片dto
 */
@Data
public class WmMaterialDto extends PageRequestDto {
    /**
     * 是否收藏
     * 1 收藏
     * 0 未收藏
     */
    private Short isCollection;
}
