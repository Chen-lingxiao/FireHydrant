<script setup lang="ts">
import { ref, computed } from 'vue'
// 当前页码和每页显示数量
const currentPage = ref(1)
const pageSize = ref(10)

// 定义表格列配置接口
interface TableColumn {
  label: string // 列标题
  prop: string // 对应数据字段
  width?: string | number // 列宽度（可选）
}

// props 定义
interface TableData {
  [key: string]: string | number | boolean | null | undefined
}

const props = defineProps<{
  title: string // 表格标题
  columns: TableColumn[] // 列配置数组
  data: TableData[] // 表格数据数组
  total: number // 数据总数
  searchKeys: string[] // 搜索关键字列表
}>()

// 定义表格数据过滤逻辑
const search = ref('')
// 过滤表格数据
const filterTableData = computed(() => {
  // 如果没有搜索关键字，返回所有数据
  if (!search.value) {
    return props.data
  }
  // 根据搜索关键字过滤数据
  return props.data.filter((item) =>
    props.searchKeys.some((key) => {
      const value = item[key]
      // 检查字段值是否包含搜索关键字（不区分大小写）
      return (
        value !== null &&
        value !== undefined &&
        value.toString().toLowerCase().includes(search.value.toLowerCase())
      )
    }),
  )
})
const emit = defineEmits<{
  (e: 'update-page', page: number, pageSize: number): void
  (e: 'edit', row: TableData): void
  (e: 'delete', row: TableData): void
}>()
// 处理当前页变化
const handleCurrentChange = (val: number) => {
  currentPage.value = val // 更新当前页码
  emit('update-page', val, pageSize.value)
}
// 每页条数变化
const handleSizeChange = (val: number) => {
  currentPage.value = 1 // 重置到第一页
  emit('update-page', currentPage.value, val)
}
// 编辑按钮点击事件
const handleEdit = (row: TableData) => {
  console.log(row)
  emit('edit', row)
}
// 删除按钮点击事件
const handleDelete = (row: TableData) => {
  console.log(row)
  emit('delete', row)
}
</script>

<template>
  <div class="table-data">
    <!-- 标题 -->
    <div class="table-title">{{ props.title }}</div>
    <el-table :data="filterTableData" class="table-content" width="100%">
      <el-table-column
        v-for="(col, index) in columns"
        :key="index"
        :label="col.label"
        :prop="col.prop"
        :width="col.width"
      />
      <el-table-column align="center" label="操作" width="200" fixed="right">
        <template #header>
          <el-input v-model="search" placeholder="输入进行搜索" />
        </template>
        <template #default="scope">
          <el-button @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDelete(scope.row)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页组件 -->
    <div class="page">
      <div class="demo-pagination-block">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10]"
          :pager-count="5"
          layout="total, sizes, prev, pager, next, jumper"
          :total="props.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.table-data {
  width: calc(100% - 200px);
  min-height: 500px;
  padding: 20px;
  position: relative;

  .table-title {
    font-size: 32px;
    color: #409eff;
    margin-bottom: 20px;
  }

  .table-content {
    width: 100%;
    overflow-y: auto;
    border: 1px solid #ebeef5;
  }

  .page {
    padding-top: 30px;
    position: absolute;
    right: 30px;
    background-color: white;
    padding-bottom: 10px;
  }
}
</style>
