<script lang="ts" setup>
import { onMounted, ref, onUnmounted, computed } from 'vue'
import type { StyleSpecification } from 'mapbox-gl'
import mapboxgl from 'mapbox-gl'
import 'mapbox-gl/dist/mapbox-gl.css'
import { GetFeaturesAPI } from '@/api/geoserver' //geoserver要素API
// 导入环境变量（Vite项目）
const tiandituToken = import.meta.env.VITE_TIANDITU_TOKEN
const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN
mapboxgl.accessToken = mapboxToken //设置Mapbox访问令牌
const mapContainer = ref<HTMLDivElement | null>(null) //地图容器引用
let map: mapboxgl.Map | null = null // Mapbox地图实例引用
const currentLayerType = ref<TiandituMapType>('vector')
// GeoJSON数据
const FireHydrantGeojson = ref<GeoJSON.FeatureCollection>({
  type: 'FeatureCollection',
  features: [],
})
// 鼠标位置信息
const mousePosition = ref({
  lng: 0,
  lat: 0,
})
// 定义一个mapbox空白样式
const blankStyle = {
  version: 8,
  name: 'BlankMap',
  sources: {},
  layers: [
    {
      id: 'background',
      type: 'background',
      paint: {
        // 'background-color': '#08294A' /* 背景颜色 */
        'background-color': 'rgba(255, 255, 255, 0)' /* 背景颜色-透明 */,
      },
    },
  ],
}
// 天地图图层类型定义
type TiandituLayerType = 'vec' | 'cva' | 'img' | 'cia'
type TiandituMapType = 'vector' | 'image'
const subdomains = ['0', '1', '2', '3', '4', '5', '6', '7'] // 天地图子域名列表

// 生成单个图层的URL集合
const generateLayerUrls = (layerType: TiandituLayerType, token: string) => {
  // 天地图WMTS服务基础URL模板
  const urlTemplate =
    `
    https://t{s}.tianditu.gov.cn/${layerType}_w/wmts?service=WMTS&` +
    `request=GetTile&` +
    `version=1.0.0&` +
    `LAYER=${layerType}&` +
    `style=default&tilematrixSet=w&TileMatrix={z}&` +
    `TileRow={y}&TileCol={x}&format=tiles&` +
    `tk=${token}`
  return subdomains.map((sub) => urlTemplate.replace('{s}', sub))
}

// 天地图图层配置生成器
const tiandituLayers = {
  // 矢量图层组（底图+注记）
  vector: {
    base: {
      name: 'tdt-vector-base',
      url: generateLayerUrls('vec', tiandituToken),
      maxZoom: 17,
    },
    label: {
      name: 'tdt-vector-label',
      url: generateLayerUrls('cva', tiandituToken),
      maxZoom: 17,
    },
  },
  // 影像图层组（底图+注记）
  image: {
    base: {
      name: 'tdt-image-base',
      url: generateLayerUrls('img', tiandituToken),
      maxZoom: 17,
    },
    label: {
      name: 'tdt-image-label',
      url: generateLayerUrls('cia', tiandituToken),
      maxZoom: 17,
    },
  },
}
console.log(tiandituLayers)

// ----------------添加天地图数据源和图层-----------------------
// 添加天地图数据源和图层
const addTiandituSourceAndLayer = (type: TiandituMapType) => {
  if (!map) return
  try {
    console.log(`切换到天地图${type}图层`)
    currentLayerType.value = type
    const layers = tiandituLayers[type]

    // 移除已存在的同名图层和数据源
    if (map.getLayer(layers.base.name)) {
      map.removeLayer(layers.base.name)
      map.removeSource(layers.base.name)
    }
    if (map.getLayer(layers.label.name)) {
      map.removeLayer(layers.label.name)
      map.removeSource(layers.label.name)
    }

    // 添加底图数据源和图层
    map.addSource(layers.base.name, {
      type: 'raster',
      tiles: layers.base.url,
      tileSize: 256,
      maxzoom: layers.base.maxZoom,
    })
    map.addLayer({
      id: layers.base.name,
      type: 'raster',
      source: layers.base.name,
      layout: {
        visibility: 'visible',
      },
    })

    // 添加注记数据源和图层
    map.addSource(layers.label.name, {
      type: 'raster',
      tiles: layers.label.url,
      tileSize: 256,
      maxzoom: layers.label.maxZoom,
    })
    map.addLayer({
      id: layers.label.name,
      type: 'raster',
      source: layers.label.name,
      layout: {
        visibility: 'visible',
      },
    })
  } catch (error) {
    console.error('切换天地图图层时出错:', error)
  }
}

// 切换图层函数
const toggleLayer = () => {
  const newType = currentLayerType.value === 'vector' ? 'image' : 'vector'
  addTiandituSourceAndLayer(newType)
  addGeoJSONLayer('sdjzdx_FireHydranty_Point')
}
// -----------------添加 Marker 标记 ---------------------

const markers: mapboxgl.Marker[] = [] // 存储所有标记的数组
const creatMaker = (feature: GeoJSON.Feature) => {
  let corrdinates: [number, number] = [0, 0]
  // 如果是点要素
  if (feature.geometry?.type === 'Point') {
    corrdinates = feature.geometry.coordinates as [number, number]
  }
  // 创建自定义HTML元素作为标记
  const markerHtml = document.createElement('div')
  markerHtml.className = 'custom-marker' // 自定义样式类
  // 根据currentStatus属性设置不同颜色
  // normal、Abnormal、Error
  markerHtml.style.backgroundColor =
    feature.properties?.currentStatus === 'repairing'
      ? '#FFC107'
      : feature.properties?.currentStatus === 'error'
        ? '#F44336'
        : '#4CAF50'
  markerHtml.textContent = `ID:${feature.properties?.Name} ` // 显示要素编号
  const marker = new mapboxgl.Marker({
    element: markerHtml, // 指定自定义HTML元素作为标记
    anchor: 'bottom', // 设置标记的锚点位置
  })
    .setLngLat(corrdinates)
    .addTo(map as mapboxgl.Map)
  // 通过HTMLElement的dataset属性添加自定义属性
  marker.getElement().dataset.featureId = String(feature.properties?.Name)
  return marker
}
// -----------------添加 GeoJSON 层 ---------------------
/**
 * 加载GeoJSON数据并添加图层
 * @param layerName - 要加载的GeoJSON数据源名称
 */
const addGeoJSONLayer = async (layerName: string) => {
  try {
    if (!map) return
    // 如果存在旧数据源，则移除
    if (map.getSource(layerName)) {
      map.removeLayer(layerName + 'Layer')
      map.removeSource(layerName)
    }
    // 获取GeoJSON数据
    const Geojson = await GetFeaturesAPI(layerName)
    FireHydrantGeojson.value = Geojson
    console.log(Geojson)
    // 添加GeoJSON数据源和图层
    map.addSource(layerName, {
      type: 'geojson',
      data: Geojson,
    })
    map.addLayer({
      id: layerName + 'Layer',
      type: 'circle',
      source: layerName,
      paint: {
        'circle-radius': 8,
        // 根据currentstatus属性设置不同颜色
        'circle-color': [
          'match',
          ['get', 'currentStatus'], // 获取currentstatus属性值
          'normal',
          '#4CAF50', // 正常状态 - 绿色
          'repairing',
          '#FFC107', // 异常状态 - 黄色
          'error',
          '#F44336', // 错误状态 - 红色
          '#9E9E9E', // 默认颜色 - 灰色（如果属性值不在上述列表中）
        ],
        'circle-stroke-width': 2,
        'circle-stroke-color': '#fff',
      },
    })
    // 清空marker标记
    if (markers.length > 0) {
      markers.forEach((marker) => marker.remove())
      markers.length = 0 // 清空数组
    }
    // 遍历添加marker标记
    Geojson.features.forEach((feature: GeoJSON.Feature) => {
      const marker = creatMaker(feature)
      markers.push(marker)
    })
  } catch (error) {
    console.error('加载GeoJSON数据失败：', error)
  }
}
// -----------------添加要素弹窗- ---------------------
const selectedFeature = ref<GeoJSON.Feature | null>(null)
const showFeaturePopup = ref(false) // 显示要素弹窗
const popupFormaData = ref() // 格式化弹窗信息
// 格式化压力显示
const formatPressure = (pressure: number | string) => {
  if (pressure === null || pressure === undefined || pressure === '') {
    return '维修中-无法获取'
  }
  return pressure
}

// 格式化状态显示
const formatStatus = (status: string) => {
  switch (status) {
    case 'normal':
      return '正常'
    case 'repairing':
      return '维修中'
    case 'error':
      return '故障'
    default:
      return status || '未知状态'
  }
}
// 获取点击的要素
const getClickFeature = (e: mapboxgl.MapMouseEvent) => {
  if (!e.features) return
  const feature = e.features[0] // 获取点击的要素
  if (!feature) return
  selectedFeature.value = feature // 保存点击的要素
  popupFormaData.value = {
    name: feature.properties?.Name,
    currentPressure: formatPressure(feature.properties?.currentPressure),
    currentStatus: formatStatus(feature.properties?.currentStatus),
    installationDate: feature.properties?.installationDate,
    managementUnit: feature.properties?.managementUnit,
  }

  map?.flyTo({
    center: e.lngLat,
    zoom: 17,
  })
}
// 点击要素 - 显示弹窗信息
const handleFeatureClickInfo = (e: mapboxgl.MapMouseEvent) => {
  getClickFeature(e)
  showFeaturePopup.value = true
}
//  ------------------GeoJSON数据编辑功能---------------------
// 状态变量
const isEditingMode = ref(false) // 编辑模式
const editingMode = ref('') // 编辑模式名称

// const EditingGeoJsonData = ref() // 正在编辑的GeoJSON数据
// const EditingFeature = ref() // 正在编辑的要素

// 计算属性，状态文本信息
const statusText = computed(() => {
  if (!isEditingMode.value) {
    return '未启用编辑'
  }
  switch (editingMode.value) {
    case 'addFeature':
      return '编辑模式: 添加要素'
    case 'updateFeature':
      return '编辑模式: 更新要素'
    case 'deleteFeature':
      return '编辑模式: 删除要素'
    default:
      return '编辑模式: 选择操作'
  }
})
// 辅助函数
const getModeText = (mode: string) => {
  switch (mode) {
    case 'addFeature':
      return '添加要素'
    case 'updateFeature':
      return '更新要素'
    case 'deleteFeature':
      return '删除要素'
    default:
      return ''
  }
}
// 更新鼠标样式
const updateMapCursor = () => {
  if (!map) return
  // 非编辑模式，使用地图默认样式（张开小手）
  if (!isEditingMode.value) {
    map.getCanvas().style.cursor = '' // 默认样式
    return
  }
  // 编辑模式，根据模式名称更新鼠标样式
  switch (editingMode.value) {
    case 'addFeature':
      map.getCanvas().style.cursor = 'crosshair' // 添加十字准星
      break
    case 'updateFeature':
    case 'deleteFeature':
      map.getCanvas().style.cursor = 'default' // 更新和删除，箭头样式
      break
    default:
      map.getCanvas().style.cursor = 'default' // 编辑模式但未选择具体功能，箭头
      break
  }
}
// 切换是否开启编辑模式
const toggleEditMode = async () => {
  if (!map) return
  if (isEditingMode.value) {
    // 如果当前有活动编辑模式，提示确认退出
    if (editingMode.value) {
      try {
        await ElMessageBox.confirm(
          `当前正在${getModeText(editingMode.value)}，退出编辑模式将丢失未保存的更改，是否确认退出？`,
          '确认退出',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          },
        )
        isEditingMode.value = false
        editingMode.value = ''
        ElMessage.success('已退出编辑模式')
      } catch {
        return
      }
    } else {
      isEditingMode.value = false
      ElMessage.success('已退出编辑模式')
    }
  } else {
    isEditingMode.value = true
    ElMessage.success('已进入编辑模式')
  }
  updateMapCursor()
}
// 切换编辑模式
const switchEditingMode = async (newMode: string) => {
  // 如果切换的是当前模式，则退出该模式
  if (editingMode.value === newMode) {
    editingMode.value = ''
    ElMessage.info(`已退出${getModeText(newMode)}模式`)
    return
  }
  // 如果当前有其他活动模式，提示确认切换
  if (editingMode.value && editingMode.value !== newMode) {
    try {
      await ElMessageBox.confirm(
        `当前正在${getModeText(editingMode.value)}，切换模式将丢失未保存的更改，是否确认切换？`,
        '确认切换',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        },
      )
      editingMode.value = newMode
      ElMessage.success(`已切换到${getModeText(newMode)}模式`)
    } catch {
      // 用户取消操作
      return
    }
  } else {
    editingMode.value = newMode
    ElMessage.success(`已进入${getModeText(newMode)}模式`)
  }
  updateMapCursor()
}

const saveChanges = () => {
  ElMessage.success('更改已保存')
}

//------------------定义初始化地图的函数---------------------
const initMap = () => {
  map = new mapboxgl.Map({
    container: mapContainer.value!, // 指定地图容器的ID
    style: blankStyle as StyleSpecification, // 指定地图样式
    center: [117.1772, 36.6826], // 地图中心点
    zoom: 16.5, // 地图缩放级别
  })
  map.on('load', () => {
    // 添加天地图数据源和图层
    addTiandituSourceAndLayer('vector')
    // 加载GeoJSON数据并添加图层
    addGeoJSONLayer('sdjzdx_FireHydranty_Point')
  })
  // 监听鼠标移动事件，获取鼠标位置
  map.on('mousemove', (e) => {
    mousePosition.value = {
      lng: parseFloat(e.lngLat.lng.toFixed(4)),
      lat: parseFloat(e.lngLat.lat.toFixed(4)),
    }
  })
  // 鼠标移动进入要素图层修改样式
  map.on('mouseenter', 'sdjzdx_FireHydranty_PointLayer', () => {
    if (!map) return
    // 非编辑模式下，要素上显示pointer
    if (!isEditingMode.value) {
      map.getCanvas().style.cursor = 'pointer'
      return
    }
    // 编辑模式下，更新和删除模式需要在要素上显示pointer
    if (
      editingMode.value === 'updateFeature' ||
      editingMode.value === 'deleteFeature'
    ) {
      map.getCanvas().style.cursor = 'pointer'
    }
    // 编辑模式下，添加模式保持十字样式不变（不处理）
  })
  // 鼠标移出图层恢复样式
  map.on('mouseleave', 'sdjzdx_FireHydranty_PointLayer', () => {
    if (!map) return
    // 移出要素时，恢复为当前编辑模式设置的样式
    updateMapCursor()
  })
  // 鼠标点击要素
  map.on('click', 'sdjzdx_FireHydranty_PointLayer', (e) => {
    if (isEditingMode.value) {
      // 如果当前是编辑模式，则处理要素点击逻辑
      switch (editingMode.value) {
        case 'updateFeature':
          console.log('更新要素逻辑')
          // 更新要素逻辑
          break
        case 'deleteFeature':
          console.log('删除要素逻辑')
          // 删除要素逻辑
          break
        case 'addFeature':
          ElMessage.error('此位置已有消防栓要素！')
          // 添加要素逻辑
          break
        default:
          // 其他模式逻辑
          break
      }
    } else {
      // 如果当前不是编辑模式，则处理要素点击逻辑
      console.log('要素点击事件')
      handleFeatureClickInfo(e)
    }
  })
  // 地图点击事件
  map.on('click', (e) => {
    if (!map) return
    // 检查点击位置是否有sdjzdx_FireHydranty_PointLayer 图层要素
    const features = map.queryRenderedFeatures(e.point, {
      layers: ['sdjzdx_FireHydranty_PointLayer'],
    })
    // 如果有要素，不处理地图点击（要素点击事件已处理）
    if (features.length > 0) {
      return
    }
    // 添加模式下在空白区域点击
    if (editingMode.value === 'addFeature') {
      console.log('添加要素逻辑')
      // 执行添加要素的具体逻辑
      return
    }
    // 其他情况下隐藏要素信息弹窗
    showFeaturePopup.value = false
  })
}

// 卸载地图函数
const unloadMap = () => {
  if (map) {
    map.remove() // 移除地图实例
  }
}
onMounted(() => {
  initMap()
})
onUnmounted(() => {
  unloadMap()
})
</script>
<template>
  <div>
    <div ref="mapContainer" class="map-container"></div>

    <!-- 编辑工具容器 -->
    <div class="edit-controls">
      <!-- 状态显示和编辑开关区域 - 始终显示 -->
      <div class="control-header">
        <el-button
          :type="isEditingMode ? 'danger' : 'primary'"
          @click="toggleEditMode"
        >
          {{ isEditingMode ? '结束编辑' : '开始编辑' }}
        </el-button>
        <span class="status-text">{{ statusText }}</span>
      </div>
      <!-- 编辑工具区域 -->
      <div v-if="isEditingMode" class="edit-tools">
        <el-button type="success" @click="saveChanges"> 保存更改 </el-button>
        <el-button
          :type="editingMode === 'addFeature' ? 'danger' : 'primary'"
          @click="switchEditingMode('addFeature')"
        >
          {{ editingMode === 'addFeature' ? '结束添加' : '添加要素' }}
        </el-button>

        <el-button
          :type="editingMode === 'updateFeature' ? 'danger' : 'primary'"
          @click="switchEditingMode('updateFeature')"
        >
          {{ editingMode === 'updateFeature' ? '结束更新' : '更新要素' }}
        </el-button>

        <el-button
          :type="editingMode === 'deleteFeature' ? 'danger' : 'primary'"
          @click="switchEditingMode('deleteFeature')"
        >
          {{ editingMode === 'deleteFeature' ? '结束删除' : '删除要素' }}
        </el-button>
      </div>
    </div>
    <!-- 工具箱区域  图层切换 编辑工具-->
    <div class="layercontroller">
      <el-button type="primary" @click="toggleLayer">
        切换{{ currentLayerType === 'vector' ? '影像' : '矢量' }}
      </el-button>
    </div>

    <!-- 简化信息 -->
    <div class="infocard">
      <span>鼠标位置: {{ mousePosition.lng }}, {{ mousePosition.lat }}</span>
    </div>
  </div>
  <!-- 弹窗 -->
  <div v-if="showFeaturePopup" class="popup">
    <div class="popup-title">
      <span class="popup-title-text"
        >当前选择要素：{{ popupFormaData.name }}</span
      >
    </div>
    <span class="popup-item">当前状态：{{ popupFormaData.currentStatus }}</span>
    <span class="popup-item"
      >当前压力：{{ popupFormaData.currentPressure }}</span
    >
    <span class="popup-item"
      >安装日期：{{ popupFormaData.installationDate }}</span
    >
    <span class="popup-item"
      >管理单位：{{ popupFormaData.managementUnit }}</span
    >
  </div>
</template>
<style scoped lang="scss">
.map-container {
  height: calc(100vh - 60px);
  width: calc(100vw - 200px);
}
.infocard {
  position: absolute;
  bottom: 5px;
  right: 10px;
  font-size: 14px;
  color: rgb(63, 59, 59);
}
/* 弹窗样式 */
.popup {
  position: absolute;
  top: 70px;
  right: 10px;
  width: 350px;
  height: 250px;
  padding: 20px;
  border-radius: 10px;
  background-color: #ffffffd5;
  border: 1px solid #4c9ff4;
  .popup-title {
    padding-bottom: 15px;
    margin-bottom: 20px;
    border-bottom: 1px solid #ccc;
  }
  .popup-item {
    display: block;
    margin-bottom: 10px;
  }
}
// 图层切换样式
.layercontroller {
  position: absolute;
  top: 70px;
  right: 10px;
}
// 编辑功能样式
.edit-controls {
  position: absolute;
  top: 70px;
  left: 220px;
  width: auto;
  z-index: 1000;

  // 状态显示和编辑开关区域
  .control-header {
    display: flex;
    margin-bottom: 10px;
    line-height: 30px;
    gap: 10px;
    .status-text {
      font-size: 14px;
      color: #606266;
      white-space: nowrap;
    }
  }

  // 编辑工具区域
  .edit-tools {
    background-color: #ffffff98;
    border-radius: 5px;
    display: flex;
    gap: 8px;
  }
}
</style>
<!-- 自定义标记样式 -->
<style>
.custom-marker {
  position: absolute;
  color: #ffffff;
  background-color: #1bd961;
  border: 2px solid #fff;
  border-radius: 5px;
  padding: 2px 5px;
  font-size: 12px;
  top: -15px;
}

.custom-marker::after {
  content: '';
}
</style>
