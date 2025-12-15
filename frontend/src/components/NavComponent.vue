<script setup lang="ts">
// import { defineProps } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
// 菜单列表
interface MenuItem {
  title: string
  subNav: subNavNavItem[]
}
// 子菜单列表
interface subNavNavItem {
  title: string
  path: string
}
// 父组件传入的菜单列表
const props = defineProps<{
  menuList: MenuItem[]
}>()
</script>
<template>
  <el-menu
    :default-active="route.path"
    router
    background-color="#545c64"
    text-color="#fff"
    active-text-color="#74c0fc"
  >
    <el-sub-menu
      v-for="(item, index) in props.menuList"
      :key="index"
      :index="index.toString()"
    >
      <template #title>
        {{ item.title }}
      </template>
      <el-menu-item
        :index="subitem.path"
        v-for="(subitem, index) in item.subNav"
        :key="index"
      >
        {{ subitem.title }}
      </el-menu-item>
    </el-sub-menu>
  </el-menu>
</template>
