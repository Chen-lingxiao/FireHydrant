package com.example.backend.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.entity.SysUser;
import com.example.backend.service.SysUserService;
import com.example.backend.utils.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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
    @Autowired // 自动注入
    private JwtUtils jwtUtils; // 注入JWT工具类
    @Autowired  // 自动注入用户服务（Spring自动帮我们创建实例）
    private SysUserService sysUserService;

    /**
     * 注册接口
     * Post http://localhost:8080/api/users/register  请求体：{ "name": "admin", "password": "123456" }
     * @param sysUser 用户信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody SysUser sysUser) {
        Map<String, Object> result = new HashMap<>(); // 创建结果对象
        try {
            boolean success = sysUserService.register(sysUser);
            if (success) {
                result.put("code", 200);
                result.put("message", "注册成功");
            } else {
                result.put("code", 400);
                result.put("message", "用户名已存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "注册失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 登录接口
     * Post http://localhost:8080/api/users/login  请求体：{ "name": "admin", "password": "123456" }
     * @param loginInfo 登录信息
     * @param response 响应对象
     * @return 登录结果
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginInfo, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            String name = loginInfo.get("name");
            String password = loginInfo.get("password");
            SysUser user = sysUserService.login(name, password);
            if (user != null) {
                // 生成Token
                String token = jwtUtils.generateToken(name);
                // 使用httpOnlyCookie()方法将Token保存在Cookie中，并设置过期时间为7天
                Cookie cookie = new Cookie("token", token);
                cookie.setHttpOnly( true); // 设置Cookie为HttpOnly
                cookie.setMaxAge(7200); // 设置Cookie的过期时间为2小时
                cookie.setPath("/"); // 设置Cookie的生效路径为根目录
                response.addCookie(cookie); // 写入响应
                result.put("code", 200);
                result.put("message", "登录成功");
                result.put("data", user);

                //result.put("token", token); // 返回Token给前端
            }
            else {
                result.put("code", 401);
                result.put("message", "用户名或密码错误");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "登录失败：" + e.getMessage());
        }
        return result;
    }


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
    /**
     * 根据id删除用户
     * DELETE http://localhost:8080/api/users/del/1
     */
    @DeleteMapping("/del/{id}")
    public Map<String, Object> deleteUser(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = sysUserService.removeById(id);
            if (success) {
                result.put("code", 200);
                result.put("message", "删除成功");
            }
            else {
                result.put("code", 500);
                result.put("message", "删除失败,用户不存在");
            }
        }
        catch (Exception e) {
            result.put("code", 500);
        }
        return result;
    }
    /**
     * 退出登录接口
     * Post http://localhost:8080/api/users/logout
     * @param response 响应对象
     * @return 退出结果
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        // 清除Cookie（设置过期时间为0）
        Cookie cookie = new Cookie("jwt-token", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        result.put("code", 200);
        result.put("message", "退出成功");
        return result;
    }

    /**
     * 分页查询用户
     * GET http://localhost:8080/api/users/page?pageNum=1&pageSize=10
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/page")
    public Map<String, Object> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        try {
            Page<SysUser> page = new Page<>(pageNum, pageSize);
            IPage<SysUser> userPage = sysUserService.getUserPage(page);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", userPage.getRecords()); // 当前页数据
            result.put("total", userPage.getTotal()); // 总条数
            result.put("pages", userPage.getPages()); // 总页数
            result.put("current", userPage.getCurrent()); // 当前页码
            result.put("size", userPage.getSize()); // 每页条数
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "分页查询失败：" + e.getMessage());
        }
        return result;
    }
    /**
     * 根据id修改用户信息
     * PUT http://localhost:8080/api/users/update
     * @param user 用户信息
     * @return 修改结果
     */
    @PutMapping("/update")
    public Map<String, Object> updateUser(@RequestBody SysUser user) {
        Map<String, Object> result = new HashMap<>();
        if (user == null || user.getId() == null) {
            result.put("code", 400);
            result.put("message", "参数错误");
            return result;
        }
        try {
            boolean success = sysUserService.updateById(user);
            if (success) {
                result.put("code", 200);
                result.put("message", "更新成功");
            }
            else {
                result.put("code", 500);
                result.put("message", "更新失败");
            }
        }
        catch (Exception e) {
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                result.put("code", 400);
                result.put("message", "更新失败：用户名已存在");
            } else {
                result.put("code", 500);
                result.put("message", "更新失败：" + e.getMessage());
            }
        }
        return result;
    }
}
