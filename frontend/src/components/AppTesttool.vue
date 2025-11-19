<script setup lang="ts">
import { GetFeatures } from '@/api/GeoJSON'
/**
 * 获取geojson数据
 * @param {string} layerName - 图层名称
 * @returns {Promise<void>} - 无返回值
 */
const getGeojson = async (layerName: string) => {
  try {
    const response = await GetFeatures(layerName)
    console.log('GeoJSON数据：', response)
  } catch (error) {
    console.error('获取GeoJSON数据失败：', error)
  }
}
</script>
<template>
  <div class="testTool">
    <el-button
      type="primary"
      @click="getGeojson('sdjzdx_FireHydranty_Point')"
      >获取geojson(点数据)</el-button
    >
    <el-button type="primary" @click="getGeojson('sdjzdx_Road_Line')"
      >获取geojson(线数据)</el-button
    >
    <el-button
      type="primary"
      @click="getGeojson('sdjzdx_Buildings_Poly')"
      >获取geojson(面数据)</el-button
    >
  </div>
</template>
<style scoped>
/* 浮动在底部 */
.testTool {
  position: absolute;
  bottom: 10px;
  right: 10px;
}
</style>
