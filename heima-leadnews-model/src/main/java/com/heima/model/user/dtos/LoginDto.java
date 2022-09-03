package com.heima.model.user.dtos;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class LoginDto {
    /**
     * 手机号
     */
    @ApiModelProperty(value = "手机号", required = true) // swagger
    private String phone;
    /**
     * 密码
     */
    @ApiModelProperty(value = "密码", required = true) // swagger
    private String password;
}
