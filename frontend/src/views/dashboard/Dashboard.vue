<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as Cesium from 'cesium'
import { GetFeatures } from '@/api/geoserver.ts'
// 导入环境变量（Vite项目）
// const tiandituToken = import.meta.env.VITE_TIANDITU_TOKEN //天地图token
const cesiumToken = import.meta.env.VITE_CESIUM_TOKEN // cesium token
const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN // Mapbox token
Cesium.Ion.defaultAccessToken = cesiumToken // 设置Cesium Ion的访问令牌
const mapboxStyleId = 'mapbox/dark-v11' // Mapbox样式ID
const viewer = ref<Cesium.Viewer | null>(null)
const mapboxImageryProvider = new Cesium.UrlTemplateImageryProvider({
  url: `https://api.mapbox.com/styles/v1/${mapboxStyleId}/tiles/256/{z}/{x}/{y}?access_token=${mapboxToken}`,
  credit: '© Mapbox © OpenStreetMap contributors', // 版权信息
  tilingScheme: new Cesium.WebMercatorTilingScheme(), // Web墨卡托投影（Mapbox默认）
  maximumLevel: 18, // 最大缩放级别（Mapbox最大支持22，根据需求调整）
  // flipXY: false, // Mapbox瓦片坐标无需翻转（Cesium默认适配）
})
// const layerProviders = {
//   vector: {
//     base: new Cesium.WebMapTileServiceImageryProvider({
//       url: `https://t{s}.tianditu.gov.cn/vec_w/wmts?tk=${tiandituToken}`,
//       layer: 'vec',
//       style: 'default',
//       format: 'tiles',
//       tileMatrixSetID: 'w',
//       subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
//       maximumLevel: 18,
//     }),
//     annotation: new Cesium.WebMapTileServiceImageryProvider({
//       url: `https://t{s}.tianditu.gov.cn/cva_w/wmts?tk=${tiandituToken}`,
//       layer: 'cva',
//       style: 'default',
//       format: 'tiles',
//       tileMatrixSetID: 'w',
//       subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
//       maximumLevel: 18,
//     }),
//   },
//   imagery: {
//     base: new Cesium.WebMapTileServiceImageryProvider({
//       url: `https://t{s}.tianditu.gov.cn/img_w/wmts?tk=${tiandituToken}`,
//       layer: 'img',
//       style: 'default',
//       format: 'tiles',
//       tileMatrixSetID: 'w',
//       subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
//       maximumLevel: 18,
//     }),
//     annotation: new Cesium.WebMapTileServiceImageryProvider({
//       url: `https://t{s}.tianditu.gov.cn/cia_w/wmts?tk=${tiandituToken}`,
//       layer: 'cia',
//       style: 'default',
//       format: 'tiles',
//       tileMatrixSetID: 'w',
//       subdomains: ['0', '1', '2', '3', '4', '5', '6', '7'],
//       maximumLevel: 18,
//     }),
//   },
// }
// // 图层引用
// const layerRefs = {
//   vector: {
//     base: null as Cesium.ImageryLayer | null,
//     annotation: null as Cesium.ImageryLayer | null,
//   },
//   imagery: {
//     base: null as Cesium.ImageryLayer | null,
//     annotation: null as Cesium.ImageryLayer | null,
//   },
// }

// const activeLayerType = ref<'vector' | 'imagery'>('vector')
// const SCI_FI_COLORS = {
//   PRIMARY_BLUE: Cesium.Color.fromCssColorString('#00D4FF'),
//   SECONDARY_BLUE: Cesium.Color.fromCssColorString('#0099FF'),
//   ACCENT_BLUE: Cesium.Color.fromCssColorString('#0066FF'),
//   GLOW_BLUE: Cesium.Color.fromCssColorString('#00FFFF'),
//   NORMAL_GREEN: Cesium.Color.fromCssColorString('#00FF88'),
//   ERROR_RED: Cesium.Color.fromCssColorString('#FF3366'),
//   BUILDING_BASE: Cesium.Color.fromCssColorString('#1E3A5F'),
//   BUILDING_TOP: Cesium.Color.fromCssColorString('#00D4FF'),
// }
// 加载geojson数据
const getGeojson = async (layerName: string) => {
  try {
    const response = await GetFeatures(layerName)
    // console.log('GeoJSON数据：', response)
    return response
  } catch (error) {
    console.error('获取GeoJSON数据失败：', error)
  }
}
// 加载道路线数据
const loadRoadData = async () => {
  try {
    const geojsonData = await getGeojson('sdjzdx_Road_Line')

    if (geojsonData) {
      const dataSource = await Cesium.GeoJsonDataSource.load(geojsonData)

      dataSource.entities.values.forEach((entity) => {
        if (entity.polyline) {
          // 使用PolylineGlowMaterialProperty实现道路流光效果
          entity.polyline.material = new Cesium.PolylineGlowMaterialProperty({
            color: Cesium.Color.BLUE, // 主颜色改为蓝色，对比度更强
            glowPower: 0.2, // 调整光晕强度 (推荐值: 0.1-0.25)
            taperPower: 0.8, // 调整边缘渐变 (推荐值: 0.5-1.0)
          })

          entity.polyline.width = new Cesium.ConstantProperty(8) // 增加线宽使效果更明显
        }
      })

      viewer.value?.dataSources.add(dataSource)
    }
  } catch (error) {
    console.error('显示道路线数据失败：', error)
  }
}
// 加载消防栓点数据
const loadFireHydrantData = async () => {
  try {
    // 从GeoServer获取数据
    const geojsonData = await getGeojson('sdjzdx_FireHydranty_Point')
    if (geojsonData) {
      // 使用Cesium的GeoJsonDataSource加载数据
      const dataSource = await Cesium.GeoJsonDataSource.load(geojsonData)

      // 遍历所有实体并设置为蓝色小圆点样式
      dataSource.entities.values.forEach((entity) => {
        // 移除默认的广告牌和标签
        entity.billboard = undefined
        entity.label = undefined

        // 创建点图形
        entity.point = new Cesium.PointGraphics({
          color: Cesium.Color.SKYBLUE,
          pixelSize: 8,
          // outlineColor: Cesium.Color.WHITE,
          // outlineWidth: 2,
        })
      })

      // 将数据源添加到管理器
      viewer.value?.dataSources.add(dataSource)
    }
  } catch (error) {
    console.error('显示消防栓点数据失败：', error)
  }
}
// 加载范围面数据
const loadCampusBoundaryData = async () => {
  try {
    // 从GeoServer获取数据
    const geojsonData = await getGeojson('sdjzdx_Boundary_Poly')

    if (geojsonData) {
      // 使用Cesium的GeoJsonDataSource加载数据
      await Cesium.GeoJsonDataSource.load(geojsonData, {
        stroke: Cesium.Color.SKYBLUE, // 线/面轮廓颜色
        fill: Cesium.Color.BLACK.withAlpha(0), // 面填充颜色及透明度
        strokeWidth: 10, // 线/面轮廓宽度
      }).then((dataSource) => {
        // 将数据源添加到管理器
        viewer.value?.dataSources.add(dataSource)
      })
    }
  } catch (error) {
    console.error('显示校园范围数据失败：', error)
  }
}
// 加载3dtiles图层
const load3DTilesLayer = async () => {
  try {
    // 创建3D Tiles图层
    const tileset = await Cesium.Cesium3DTileset.fromUrl(
      '/output_tiles/tileset.json', // 文件路径
      {
        maximumScreenSpaceError: 4, // 默认为16，数值越小，质量越高
        show: true,
      },
    )
    // 添加3D Tiles图层到场景
    viewer.value?.scene.primitives.add(tileset)
  } catch (error) {
    console.error('加载3D Tiles图层失败：', error)
  }
}
// 加载并显示所有图层
const loadGeoJSONLayers = async () => {
  loadCampusBoundaryData() // 加载校园范围数据
  loadFireHydrantData() // 加载消防栓点数据
  loadRoadData() // 加载道路线数据
  load3DTilesLayer() // 加载3D Tiles图层
}

/**
 * 初始化Cesium Viewer
 */
const initCesium = async () => {
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
  // 添加Mapbox底图
  viewer.value.imageryLayers.addImageryProvider(mapboxImageryProvider)
  // layerRefs.vector.base =
  //   viewer.value.imageryLayers.addImageryProvider(
  //     layerProviders.vector.base,
  //   )
  // layerRefs.vector.annotation =
  //   viewer.value.imageryLayers.addImageryProvider(
  //     layerProviders.vector.annotation,
  //   )
  // layerRefs.imagery.base =
  //   viewer.value.imageryLayers.addImageryProvider(
  //     layerProviders.imagery.base,
  //   )
  // layerRefs.imagery.annotation =
  //   viewer.value.imageryLayers.addImageryProvider(
  //     layerProviders.imagery.annotation,
  //   )
  // switchBaseLayer('vector')
  flyToInitialView()
}
/**
 * 切换到指定类型的底图
 * @param type 图层类型 - 'vector' 或 'imagery'
 */
// const switchBaseLayer = (type: 'vector' | 'imagery'): void => {
//   if (!viewer.value) return

//   // 隐藏所有图层
//   Object.values(layerRefs).forEach((layerGroup) => {
//     if (layerGroup.base) layerGroup.base.show = false
//     if (layerGroup.annotation) layerGroup.annotation.show = false
//   })

//   // 显示选中的图层
//   const targetLayer = layerRefs[type]
//   if (targetLayer.base) targetLayer.base.show = true
//   if (targetLayer.annotation) targetLayer.annotation.show = true

//   activeLayerType.value = type
// }

/**
 * 飞到初始视角
 */
const flyToInitialView = (): void => {
  if (!viewer.value) return

  viewer.value.camera.flyTo({
    destination: Cesium.Cartesian3.fromDegrees(117.177, 36.673, 650.0),
    orientation: {
      heading: Cesium.Math.toRadians(10.0), // 朝向
      pitch: Cesium.Math.toRadians(-30.0), // 俯仰角
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
  loadGeoJSONLayers() // 加载GeoJSON图层
})
onBeforeUnmount(() => {
  destroyCesium()
})
</script>

<template>
  <div class="cesium-container">
    <div class="cesium-viewer" id="cesiumContainer"></div>
    <!--
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
    </div> -->
  </div>
  <!-- <app-test></app-test> -->
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
