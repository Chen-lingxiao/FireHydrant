package com.example.backend.service.impl;

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
}
