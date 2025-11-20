package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
// 暂时不需要额外功能，基础功能已足够
// 注册接口
    boolean register(SysUser user);
    // 登录接口
    SysUser login(String name, String password);
}
