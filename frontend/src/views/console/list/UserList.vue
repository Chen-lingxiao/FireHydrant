<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getUserPagingQuery,
  deleteUser,
  updateUser,
  getUserByid,
} from '@/api/user'
// 表格配置
const tableConfig = ref({
  title: '用户信息管理列表',
  columns: [
    { label: 'ID', prop: 'id', width: 80 },
    { label: '用户名', prop: 'name', width: 150 },
    { label: '权限', prop: 'role', width: 120 },
    { label: '性别', prop: 'sex', width: 80 },
    { label: '出生日期', prop: 'birthDate' },
    { label: '部门', prop: 'department' },
    { label: '电话', prop: 'telephone' },
    { label: '邮箱', prop: 'email' },
    { label: '创建时间', prop: 'createTime', width: 200 },
  ],
  data: [],
  total: 0, // 总条数
  currentPage: 1, // 当前页码
  pageSize: 5, // 每页条数
})
//用户信息接口
interface User {
  id: number
  name: string
  sex: string
  birthDate: string
  department: string
  telephone: string
  email: string
  role: string
  createTime: string
}
const fetchUserList = async (pageNum: number = 1, pageSize: number = 10) => {
  try {
    const response = await getUserPagingQuery({ pageNum, pageSize })
    console.log('用户列表数据：', response.data)
    const data = response.data.data.map((user: User) => ({
      ...user,
      birthDate: user.birthDate
        ? new Date(user.birthDate).toLocaleDateString('zh-CN')
        : null,
      createTime: user.createTime
        ? new Date(user.createTime).toLocaleString('zh-CN')
        : null,
    }))
    tableConfig.value.data = data // 更新表格数据
    tableConfig.value.total = response.data.total // 更新总条数
    tableConfig.value.currentPage = response.data.current // 获取当前页码
    tableConfig.value.pageSize = response.data.size // 获取每页条数
    console.log('用户列表数据：', response.data) // 调试输出
  } catch (error) {
    console.error('获取用户列表失败：', error)
  }
}
// 处理分页变化事件
const handlePageChange = (page: number, pageSize: number) => {
  console.log('页码变化：', page, pageSize)
  fetchUserList(page, pageSize)
}
// 处理删除事件
const handleDeleteUser = async (row: User) => {
  console.log('删除用户:', row.id, row)
  // 弹出对话框确认
  await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      // 确认删除
      try {
        await deleteUser(row.id)
        fetchUserList() // 刷新列表
        ElMessage({
          type: 'success',
          message: '用户删除成功',
        })
      } catch (error) {
        console.error('删除用户失败：', error)
      }
    })
    .catch(() => {
      // 取消删除
      ElMessage({
        type: 'info',
        message: '已取消删除',
      })
    })
}

// 编辑功能相关
// 编辑表单相关变量
const dialogVisible = ref(false) // 控制编辑对话框显示/隐藏
const editFormRef = ref() // 编辑表单引用
// 编辑表单数据
const editForm = ref({
  id: 0,
  name: '',
  sex: '',
  birthDate: '',
  department: '',
  telephone: '',
  email: '',
  role: '',
  createTime: '',
})
// 编辑表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      min: 5,
      max: 10,
      message: '用户名长度在 5 到 10 个字符',
      trigger: 'blur',
    },
  ],
  sex: [], // 性别字段为可选
  birthDate: [], // 出生日期字段为可选
  department: [], // 部门字段为可选
  telephone: [
    // 电话号码格式验证（如果填写了的话）
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的电话号码',
      trigger: 'blur',
    },
  ],
  email: [
    // 邮箱格式验证（如果填写了的话）
    {
      type: 'email' as const,
      message: '请输入正确的邮箱格式',
      trigger: 'blur',
    },
  ],
}
// 重置编辑表单
const resetEditForm = () => {
  if (editFormRef.value) {
    editFormRef.value.resetFields() // 重置表单字段
  }
}
// 处理编辑事件
const handleEditUser = async (row: User) => {
  const response = await getUserByid(row.id)
  try {
    Object.assign(editForm.value, response.data.data) // 填充表单数据
    // 处理日期格式
    if (editForm.value.birthDate) {
      const date = new Date(editForm.value.birthDate)
      editForm.value.birthDate = `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
    }
    dialogVisible.value = true // 显示编辑对话框
  } catch (error) {
    console.error('编辑用户失败：', error)
  }
}
const submitEditForm = async () => {
  try {
    await editFormRef.value.validate()
    // 处理日期格式
    const userData = { ...editForm.value }
    if (userData.birthDate && typeof userData.birthDate !== 'string') {
      // 如果是 Date 对象，则转换为 YYYY-MM-DD 格式的字符串
      const date = new Date(userData.birthDate)
      userData.birthDate = `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
    }
    console.log('提交更新请求：', userData)
    const response = await updateUser(userData)
    if (response.data.code === 200) {
      ElMessage.success('用户信息更新成功')
      dialogVisible.value = false // 关闭对话框
      // 刷新当前页数据，保持数据顺序
      fetchUserList(tableConfig.value.currentPage, tableConfig.value.pageSize)
    } else {
      ElMessage.error('更新失败：' + response.data.message)
    }
  } catch (error) {
    console.error('更新用户失败：', error)
    ElMessage.error('更新用户失败：' + (error as Error).message)
  }
}
// 页面挂载时获取用户数据
onMounted(() => {
  fetchUserList()
})
</script>

<template>
  <table-component
    :columns="tableConfig.columns"
    :title="tableConfig.title"
    :data="tableConfig.data"
    :total="tableConfig.total"
    :searchKeys="[
      'name',
      'sex',
      'birthDate',
      'department',
      'telephone',
      'email',
    ]"
    @edit="handleEditUser"
    @delete="handleDeleteUser"
    @update-page="handlePageChange"
  ></table-component>
  <div class="edit-dialog">
    <!-- 编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" title="编辑用户信息" width="500px">
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="rules"
        label-width="80px"
      >
        <!-- 用户名输入框 -->
        <el-form-item label="用户名" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入用户名" />
        </el-form-item>
        <!-- 权限选择框 -->
        <el-form-item label="权限" prop="role">
          <el-radio-group v-model="editForm.role">
            <el-radio label="ADMIN">管理员</el-radio>
            <el-radio label="USER">普通用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- 性别选择框 -->
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="editForm.sex">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 出生日期选择器 -->
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="editForm.birthDate"
            type="date"
            placeholder="选择出生日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <!-- 部门输入框 -->
        <el-form-item label="部门" prop="department">
          <el-input v-model="editForm.department" placeholder="请输入部门" />
        </el-form-item>

        <!-- 电话输入框 -->
        <el-form-item label="电话" prop="telephone">
          <el-input v-model="editForm.telephone" placeholder="请输入电话号码" />
        </el-form-item>

        <!-- 邮箱输入框 -->
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="editForm.email"
            type="email"
            placeholder="请输入邮箱"
          />
        </el-form-item>
      </el-form>

      <!-- 对话框底部按钮 -->
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetEditForm">重置</el-button>
          <el-button type="primary" @click="submitEditForm">提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped></style>
