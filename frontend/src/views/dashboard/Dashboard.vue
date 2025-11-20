<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as Cesium from 'cesium'
// 导入环境变量（Vite项目）
const tiandituToken = import.meta.env.VITE_TIANDITU_TOKEN //天地图token
const cesiumToken = import.meta.env.VITE_CESIUM_TOKEN // Mapbox token
Cesium.Ion.defaultAccessToken = cesiumToken // 设置Cesium Ion的访问令牌

const viewer = ref<Cesium.Viewer | null>(null)

const layerProviders = {
  vector: {
    base: new Cesium.WebMapTileServiceImageryProvider({
      url: `https://t{s}.tianditu.gov.cn/vec_w/wmts?tk=${tiandituToken}`,
      layer: 'vec',
      style: 'default',
      format: 'tiles',
      tileMatrixSetID: 'w',
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
      maximumLevel: 18,
    }),
    annotation: new Cesium.WebMapTileServiceImageryProvider({
      url: `https://t{s}.tianditu.gov.cn/cva_w/wmts?tk=${tiandituToken}`,
      layer: 'cva',
      style: 'default',
      format: 'tiles',
      tileMatrixSetID: 'w',
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
      maximumLevel: 18,
    }),
  },
  imagery: {
    base: new Cesium.WebMapTileServiceImageryProvider({
      url: `https://t{s}.tianditu.gov.cn/img_w/wmts?tk=${tiandituToken}`,
      layer: 'img',
      style: 'default',
      format: 'tiles',
      tileMatrixSetID: 'w',
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
      maximumLevel: 18,
    }),
    annotation: new Cesium.WebMapTileServiceImageryProvider({
      url: `https://t{s}.tianditu.gov.cn/cia_w/wmts?tk=${tiandituToken}`,
      layer: 'cia',
      style: 'default',
      format: 'tiles',
      tileMatrixSetID: 'w',
      subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
      maximumLevel: 18,
    }),
  },
}
// 图层引用
const layerRefs = {
  vector: {
    base: null as Cesium.ImageryLayer | null,
    annotation: null as Cesium.ImageryLayer | null,
  },
  imagery: {
    base: null as Cesium.ImageryLayer | null,
    annotation: null as Cesium.ImageryLayer | null,
  },
}

const activeLayerType = ref<'vector' | 'imagery'>('vector')
const initCesium = () => {
  // 创建Cesium viewer
  viewer.value = new Cesium.Viewer('cesiumContainer', {
    animation: false, // 禁用动画控件
    timeline: false, // 禁用时间轴
    baseLayerPicker: false, // 禁用图层选择器
    navigationHelpButton: false, // 禁用导航帮助
    sceneModePicker: false, // 禁用场景模式切换
    infoBox: false, // 保留信息框（点击实体看详情）
    vrButton: false, // 禁用VR按钮
  })

  layerRefs.vector.base =
    viewer.value.imageryLayers.addImageryProvider(
      layerProviders.vector.base,
    )
  layerRefs.vector.annotation =
    viewer.value.imageryLayers.addImageryProvider(
      layerProviders.vector.annotation,
    )
  layerRefs.imagery.base =
    viewer.value.imageryLayers.addImageryProvider(
      layerProviders.imagery.base,
    )
  layerRefs.imagery.annotation =
    viewer.value.imageryLayers.addImageryProvider(
      layerProviders.imagery.annotation,
    )
  switchBaseLayer('vector')
  flyToInitialView()
}
/**
 * 切换到指定类型的底图
 * @param type 图层类型 - 'vector' 或 'imagery'
 */
const switchBaseLayer = (type: 'vector' | 'imagery'): void => {
  if (!viewer.value) return

  // 隐藏所有图层
  Object.values(layerRefs).forEach((layerGroup) => {
    if (layerGroup.base) layerGroup.base.show = false
    if (layerGroup.annotation) layerGroup.annotation.show = false
  })

  // 显示选中的图层
  const targetLayer = layerRefs[type]
  if (targetLayer.base) targetLayer.base.show = true
  if (targetLayer.annotation) targetLayer.annotation.show = true

  activeLayerType.value = type
}

/**
 * 飞到初始视角
 */
const flyToInitialView = (): void => {
  if (!viewer.value) return

  viewer.value.camera.flyTo({
    destination: Cesium.Cartesian3.fromDegrees(
      117.0,
      36.0,
      1000000.0,
    ),
    orientation: {
      heading: Cesium.Math.toRadians(0.0),
      pitch: Cesium.Math.toRadians(-90.0),
      roll: 0.0,
    },
  })
}

/**
 * 清理Cesium资源
 */
const destroyCesium = (): void => {
  if (viewer.value) {
    viewer.value.destroy()
    viewer.value = null
  }
}
onMounted(() => {
  initCesium() // 初始化Cesium
})
onBeforeUnmount(() => {
  destroyCesium()
})
</script>

<template>
  <div class="cesium-container">
    <div class="cesium-viewer" id="cesiumContainer"></div>
    <div class="layer-controls">
      <button
        @click="switchBaseLayer('vector')"
        :class="{ active: activeLayerType === 'vector' }"
        class="control-btn"
      >
        矢量地图
      </button>
      <button
        @click="switchBaseLayer('imagery')"
        :class="{ active: activeLayerType === 'imagery' }"
        class="control-btn"
      >
        影像地图
      </button>
    </div>
  </div>
</template>

<style scoped>
.cesium-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
}

.cesium-viewer {
  width: 100%;
  height: 100%;
}

.layer-controls {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.control-btn {
  padding: 8px 16px;
  background: rgba(42, 42, 42, 0.9);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.control-btn:hover {
  background: rgba(60, 60, 60, 0.9);
}

.control-btn.active {
  background: rgba(0, 96, 255, 0.9);
}
</style>
