<script setup lang="ts"></script>

<template>
  <div class="main-layout">
    <!-- 头部 -->
    <app-header></app-header>
    <!-- 主体部分 -->
    <div class="container">
      <!-- 路由占位符 决定放地图 /用户界面 -->
      <router-view></router-view>
    </div>
    <!-- 底部 -->
    <app-footer></app-footer>
  </div>
</template>

<style scoped>
.main-layout {
  height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  /* 垂直方向 */
}

.container {
  flex: 1;
  /* 填充剩余空间 */
  overflow: hidden;
}
</style>
