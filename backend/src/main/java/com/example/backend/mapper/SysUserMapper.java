package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // 不需要写任何方法！
    // BaseMapper已经提供了增删改查的所有基本方法
    // 用户注册
    boolean register(SysUser user);
    // 用户登录
    SysUser login(String name, String password);
}
