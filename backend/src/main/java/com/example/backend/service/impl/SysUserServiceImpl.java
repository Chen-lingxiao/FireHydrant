package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.SysUser;
import com.example.backend.mapper.SysUserMapper;
import com.example.backend.service.SysUserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 * 实际处理业务逻辑的地方
 */
@Service  // 告诉Spring这是一个业务服务组件
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {                   // 实现我们的接口
    // 目前不需要写额外代码，通用功能已满足需求
    //注册接口
    @Override // 重写父类的方法
    public boolean register(SysUser user) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>(); // 创建查询条件
        queryWrapper.eq("name", user.getName()); // 设置查询条件为用户名
        SysUser existUser = baseMapper.selectOne(queryWrapper); // 查询用户
        if (existUser != null) {
            return false; // 用户名已存在
        }
        return save(user); // 保存用户信息
    }
    // 登录接口
    @Override
    public SysUser login(String name, String password) {
        QueryWrapper< SysUser> queryWrapper = new QueryWrapper<>(); // 创建查询条件
        queryWrapper.eq("name", name);
        queryWrapper.eq("password", password);
        return baseMapper.selectOne(queryWrapper); // 查询用户
    }
}
