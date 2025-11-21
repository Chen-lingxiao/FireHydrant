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
          entity.polyline.material = new Cesium.ColorMaterialProperty(
            Cesium.Color.fromCssColorString('#4fc3f7').withAlpha(0.9),
          ) // 设置蓝色
          entity.polyline.width = new Cesium.ConstantProperty(1) // 增加线宽使效果更明显
          // 贴地
          entity.polyline.clampToGround = new Cesium.ConstantProperty(true)
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
        const currentStatus = entity.properties?.currentStatus.getValue()
        const name = entity.properties?.Name.getValue()
        const position = entity.position?.getValue(Cesium.JulianDate.now())

        if (!position) return

        // 确定颜色
        let pointColor = Cesium.Color.SKYBLUE
        let pulseColor = Cesium.Color.BLUE

        if (currentStatus === 'normal') {
          pointColor = Cesium.Color.LIMEGREEN
          pulseColor = Cesium.Color.GREEN
        } else if (currentStatus === 'error') {
          pointColor = Cesium.Color.RED
          pulseColor = Cesium.Color.RED
        } else if (currentStatus === 'repairing') {
          pointColor = Cesium.Color.ORANGE
          pulseColor = Cesium.Color.ORANGE
        }
        // 创建点图形
        entity.point = new Cesium.PointGraphics({
          color: pointColor,
          pixelSize: 8,
          outlineColor: pointColor,
          outlineWidth: 1,
          heightReference: Cesium.HeightReference.CLAMP_TO_GROUND, // 贴地
          disableDepthTestDistance: Number.POSITIVE_INFINITY, // 关键：禁用深度测试
        })
        // 只给错误状态的消防栓添加动态扩散效果
        if (currentStatus === 'error') {
          // 创建动态扩散效果 - 使用圆形
          const createPulseCircle = (delay: number, maxRadius: number) => {
            const startTime = new Date().getTime()

            return new Cesium.Entity({
              position: position,
              ellipse: {
                semiMinorAxis: maxRadius, // 固定半径
                semiMajorAxis: maxRadius, // 固定半径，确保是圆形
                material: new Cesium.ColorMaterialProperty(
                  new Cesium.CallbackProperty(() => {
                    const elapsed =
                      (new Date().getTime() - startTime - delay * 1000) / 1000
                    const progress = (elapsed % 6) / 6 // 3秒循环

                    // 使用更平滑的透明度过渡，避免突然消失
                    const alpha = 0.6 * Math.sin(progress * Math.PI) // 正弦波实现平滑过渡

                    // const alpha = 0.6 * (1 - progress / 0.8)
                    return pulseColor.withAlpha(alpha)
                  }, false),
                ),
                height: 1,
                heightReference: Cesium.HeightReference.CLAMP_TO_GROUND,
                outline: false,
              },
            })
          }

          // 更密集的脉冲环（8个）
          dataSource.entities.add(createPulseCircle(0, 6))
          dataSource.entities.add(createPulseCircle(0.3, 9))
          dataSource.entities.add(createPulseCircle(0.6, 12))
          dataSource.entities.add(createPulseCircle(0.9, 15))
          dataSource.entities.add(createPulseCircle(1.2, 18))
          dataSource.entities.add(createPulseCircle(1.5, 21))
          dataSource.entities.add(createPulseCircle(1.8, 24))
          dataSource.entities.add(createPulseCircle(2.1, 27))
        }

        // 给错误和维修状态的点加标签
        if (currentStatus === 'error' || currentStatus === 'repairing') {
          entity.label = new Cesium.LabelGraphics({
            showBackground: true, // 启用标签背景
            backgroundColor: pointColor.withAlpha(0.8), // 标签背景颜色
            scale: 0.5, // 缩小字体大小
            font: 'bold 32px sans-serif',
            text: name,
            fillColor: Cesium.Color.WHITE, // 标签字体颜色
            outlineColor: Cesium.Color.BLACK, // 标签边框颜色
            outlineWidth: 1, // 边框宽度
            style: Cesium.LabelStyle.FILL_AND_OUTLINE, // 同时具有填充和轮廓样式
            verticalOrigin: Cesium.VerticalOrigin.BOTTOM, // 标签垂直对齐方式
            pixelOffset: new Cesium.Cartesian2(0, -20), // 标签位置稍微偏移
            heightReference: Cesium.HeightReference.CLAMP_TO_GROUND, // 贴地
            disableDepthTestDistance: Number.POSITIVE_INFINITY, // 关键：禁用深度测试
          })
        }
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
      const dataSource = await Cesium.GeoJsonDataSource.load(geojsonData)
      dataSource.entities.values.forEach((entity) => {
        if (entity.polygon) {
          entity.polygon.material = new Cesium.ColorMaterialProperty(
            Cesium.Color.fromCssColorString('#1e88e5').withAlpha(0.25), // 半透明蓝色
          )
          entity.polygon.outline = new Cesium.ConstantProperty(true)
          entity.polygon.outlineColor = new Cesium.ConstantProperty(
            Cesium.Color.fromCssColorString('#64b5f6').withAlpha(0.8), // 亮蓝色
          )
          entity.polygon.outlineWidth = new Cesium.ConstantProperty(2) // 轮廓线宽
        }
      })
      viewer.value?.dataSources.add(dataSource)
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
    // 纯渐变色+动态光圈

    const customShader = new Cesium.CustomShader({
      fragmentShaderText: `
            void fragmentMain(FragmentInput fsInput, inout czm_modelMaterial material) {
                // 获取模型坐标位置 - 使用Z轴作为垂直方向
                vec3 positionMC = fsInput.attributes.positionMC;

                // ==================== 基础渐变色部分 ====================
                // 创建从底部到顶部的蓝绿色渐变
                // 底部: vec3(0.0, 1.0, 1.0) - 青色
                // 顶部: vec3(0.0, 0.5, 0.8) - 深蓝色
                float heightFactor = positionMC.z * 0.02; // 渐变系数
                material.diffuse = vec3(0.0, 1.0 - heightFactor, 1.0 - heightFactor * 0.6);

                // ==================== 动态光圈参数详解 ====================
                float baseHeight = 0.0;        // 基础高度：从地面开始 (0米)
                float buildingHeight = 25.0;   // 建筑总高度:25米
                float glowWidth = 1.0;         // 光圈宽度:3米 (控制光环粗细)
                float moveSpeed = 300.0;       // 移动速度：数值越大移动越慢

                // ==================== 光圈计算逻辑 ====================
                // 1. 计算当前点相对于建筑底部的高度
                float currentHeight = positionMC.z - baseHeight;

                // 2. 计算光圈当前位置 (0.0 ~ 1.0 循环)
                // czm_frameNumber: Cesium帧计数器,每帧自动+1
                // fract(): 取小数部分,实现0-1循环
                float glowPosition = fract(czm_frameNumber / moveSpeed);

                // 3. 将当前高度归一化到0-1范围
                float normalizedHeight = clamp(currentHeight / buildingHeight, 0.0, 1.0);

                // 4. 计算当前点与光圈位置的距离
                float distanceToGlow = abs(normalizedHeight - glowPosition);

                // 5. 计算光圈强度 (距离越近强度越高)
                float glowIntensity = 1.0 - smoothstep(0.0, glowWidth / buildingHeight, distanceToGlow);

                // ==================== 应用光圈效果 ====================
                // 创建白色光圈颜色
                vec3 glowColor = vec3(0.0, 0.8, 1.0);

                // 混合原始颜色和光圈颜色
                material.diffuse = mix(material.diffuse, glowColor, glowIntensity * 0.9);

                // 增加整体亮度
                material.diffuse += material.diffuse * glowIntensity * 1.0;
            }`,
    })
    tileset.customShader = customShader
    // 添加3D Tiles图层到场景
    viewer.value?.scene.primitives.add(tileset)
  } catch (error) {
    console.error('加载3D Tiles图层失败：', error)
  }
}

// 加载并显示所有图层
const loadLayers = async () => {
  await loadCampusBoundaryData() // 加载校园范围数据
  await loadRoadData() // 加载道路线数据
  await load3DTilesLayer() // 加载3D Tiles图层
  await loadFireHydrantData() // 加载消防栓点数据（最后加载确保在最上层）
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
  // 关闭抗锯齿
  viewer.value.scene.postProcessStages.fxaa.enabled = false
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
  flyToInitialView() // 飞到初始视角
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
    destination: Cesium.Cartesian3.fromDegrees(117.1745, 36.6735, 600.0),
    orientation: {
      heading: Cesium.Math.toRadians(20.0), // 朝向
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
  loadLayers() // 加载GeoJSON图层
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
