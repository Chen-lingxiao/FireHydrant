package com.example.backend.controller;
import com.example.backend.entity.SysUser;
import com.example.backend.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
/**
 * 用户控制器
 * 就像"餐厅的前台"，接收客户请求并返回结果
 */
@RestController         // 表示这是处理HTTP请求的组件
@RequestMapping("/api/users")  // 所有接口都以/api/users开头
@CrossOrigin  // 允许前端跨域访问
public class SysUserController {

    @Autowired  // 自动注入用户服务（Spring自动帮我们创建实例）
    private SysUserService sysUserService;
    /**
     * 🔍 根据ID查询用户
     * GET http://localhost:8080/api/users/1
     */
    @GetMapping("/{id}")
    public Map<String, Object> getUserById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            SysUser user = sysUserService.getById(id);
            if (user != null) {
                result.put("code", 200);
                result.put("message", "查询成功");
                result.put("data", user);
            } else {
                result.put("code", 404);
                result.put("message", "用户不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

}
