<script setup lang="ts">
const MenuList = [
  {
    title: '地理数据管理',
    subNav: [
      {
        title: '消防栓点数据',
        path: '/app/console/hydrantlist',
      },
      {
        title: '建筑物面数据',
        path: '/app/console/testtlist',
      },
    ],
  },
  {
    title: '数据表管理',
    subNav: [
      {
        title: '用户数据表',
        path: '/app/console/userlist',
      },
    ],
  },
]
</script>

<template>
  <div class="layout">
    <!-- 左侧菜单 -->
    <div class="subnav">
      <!-- 通过props传递数据给子组件 -->
      <nav-component :menuList="MenuList"></nav-component>
    </div>
    <!-- 右侧内容 -->
    <router-view />
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100%;
  .subnav {
    width: 200px;
    background-color: #f5f5f5;
  }
}
</style>
