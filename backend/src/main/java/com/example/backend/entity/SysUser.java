package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
import java.util.Date;

// Lombok注解：自动生成getter、setter、toString等方法，不用手动写
//@Data
// MyBatis-Plus注解：指定对应的数据表名（必须和数据库表名一致）
@TableName("sys_user")
public class SysUser {
    // 主键ID：对应表中id字段（SERIAL自增）
    @TableId(type = IdType.AUTO) // 自增主键（和PostgreSQL的SERIAL对应）
    private Integer id;

    // 用户名：对应表中name字段
    private String name;

    // 密码：对应表中password字段
    private String password;

    // 性别：对应表中sex字段
    private String sex;

    // 出生日期：对应表中birth_date字段
    @TableField("birth_date")
    private Date birthDate; // 数据库字段birth_date → 实体类birthDate（下划线转驼峰）

    // 所属部门：对应表中department字段
    private String department;

    // 联系电话：对应表中telephone字段
    private String telephone;

    // 电子邮箱：对应表中email字段
    private String email;

    // 权限：对应表中role字段（ADMIN/USER）
    private String role;

    // 创建时间：对应表中create_time字段
    private Date createTime;
    @JsonIgnore // 忽略此字段，不返回给前端
    public String getPassword() {
        return password;
    }
    @JsonProperty // 设置此字段可返回给前端
    public void setPassword(String password) {
        this.password = password;
    }
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
