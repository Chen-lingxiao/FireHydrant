<script setup lang="ts">
import { ref } from 'vue'
const title = '用户信息管理列表'
const odata = ref({
  total: 3,
  current: 1,
  code: 200,
  pages: 1,
  data: [
    {
      id: 2,
      name: 'admin',
      sex: '男',
      birthDate: '1999-12-31T16:00:00.000+00:00',
      department: '测试部',
      telephone: '12345678900',
      email: '123456789@outlook.com',
      role: 'ADMIN',
      createTime: '2025-11-19T10:07:05.649+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
    {
      id: 3,
      name: 'test001',
      sex: null,
      birthDate: null,
      department: null,
      telephone: null,
      email: null,
      role: 'USER',
      createTime: '2025-11-19T10:07:49.832+00:00',
    },
  ],
  size: 5,
  message: '查询成功',
})
const data = odata.value.data.map((user) => ({
  ...user,
  birthDate: user.birthDate
    ? new Date(user.birthDate).toLocaleDateString('zh-CN')
    : null,
  createTime: user.createTime
    ? new Date(user.createTime).toLocaleString('zh-CN')
    : null,
}))
console.log(data)
</script>

<template>
  <table-component
    :columns="[
      { label: 'ID', prop: 'id', width: 80 },
      { label: '用户名', prop: 'name', width: 150 },
      { label: '角色', prop: 'role', width: 120 },
      { label: '性别', prop: 'sex', width: 80 },
      { label: '出生日期', prop: 'birthDate' },
      { label: '部门', prop: 'department' },
      { label: '电话', prop: 'telephone' },
      { label: '邮箱', prop: 'email' },
      { label: '创建时间', prop: 'createTime', width: 200 },
    ]"
    :title="title"
    :data="data"
    :total="odata.total"
    :page-size="odata.size"
    :current-page="odata.current"
    :search-keys="['name', 'role']"
  ></table-component>
</template>

<style scoped></style>
