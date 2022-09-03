package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional // 事务
@Slf4j
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {
    /**
     * APP端登录功能
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult login(LoginDto dto) {
        //1.正常登录（手机号+密码登录）
        // StringUtils.isNotBlank作用：不为null 长度不为0 不包含空白字符
        if (StringUtils.isNotBlank(dto.getPhone()) && StringUtils.isNotBlank(dto.getPassword())) {
            // 手机号和密码都不为空

            //1.1查询用户
            ApUser dbUser = getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, dto.getPhone())); // 从数据库查询出来的用户
            // 判断用户是否存在
            if (dbUser == null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在");
            }
            //1.2 比对密码
            String salt = dbUser.getSalt(); // 盐
            String password = dto.getPassword(); // 密码
            // 前端传过来的密码 + 数据库用户中的盐 做MD5加密：得到pswd
            String pswd = DigestUtils.md5DigestAsHex((password + salt).getBytes(StandardCharsets.UTF_8)); //

            if (!dbUser.getPassword().equals(pswd)) {
                // 数据库中的password 与pswd 不相等 登陆失败
                return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR, "密码错误");
            }
            // 逻辑走到这里说明密码正确
            //1.3 返回数据  jwt 生成token
            String token = AppJwtUtil.getToken(dbUser.getId().longValue());
            // 把User和token封装为map返回
            Map<String, Object> map = new HashMap<>();
            // 盐和password 设置为null 不需要返回
            dbUser.setPassword("");
            dbUser.setSalt("");

            map.put("user", dbUser);
            map.put("token",token);

            return ResponseResult.okResult(map);
        } else {
            //2.游客  同样返回token  id = 0
            String token = AppJwtUtil.getToken(0L);
            Map<String, Object> map = new HashMap<>();
            map.put("token", token);
            return ResponseResult.okResult(map);
        }
    }
}
