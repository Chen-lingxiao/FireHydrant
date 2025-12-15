<script lang="ts" setup>
import { onMounted, ref, onUnmounted, computed } from 'vue'
import type { StyleSpecification } from 'mapbox-gl'
import mapboxgl from 'mapbox-gl'
import 'mapbox-gl/dist/mapbox-gl.css'
import { GetFeaturesAPI, EditPointFeaturesAPI } from '@/api/geoserver' //geoserver要素API
// 导入环境变量（Vite项目）
const tiandituToken = import.meta.env.VITE_TIANDITU_TOKEN
mapboxgl.accessToken = import.meta.env.VITE_MAPBOX_TOKEN

const mapContainer = ref<HTMLDivElement | null>(null) //地图容器引用
let map: mapboxgl.Map | null = null // Mapbox地图实例引用
const currentLayerType = ref<TiandituMapType>('vector')
// GeoJSON数据
const fireHydrantGeojson = ref<GeoJSON.FeatureCollection>({
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
    fireHydrantGeojson.value = Geojson
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
  map?.flyTo({
    center: e.lngLat,
    zoom: 17,
  })
  return feature
}
// 点击要素 - 显示弹窗信息
const handleFeatureClickInfo = (e: mapboxgl.MapMouseEvent) => {
  const feature = getClickFeature(e)
  if (!feature) return
  selectedFeature.value = feature // 保存点击的要素
  popupFormaData.value = {
    name: feature.properties?.Name,
    currentPressure: formatPressure(feature.properties?.currentPressure),
    currentStatus: formatStatus(feature.properties?.currentStatus),
    installationDate: feature.properties?.installationDate,
    managementUnit: feature.properties?.managementUnit,
  }
  showFeaturePopup.value = true
}
//  ------------------GeoJSON数据编辑功能---------------------
// 状态变量
const isEditingMode = ref(false) // 编辑模式
const editingMode = ref('') // 编辑模式名称

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
        // updateMapCursor() // 更新鼠标样式
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
  updateMapCursor() // 更新鼠标样式
}
// 切换编辑模式
const switchEditingMode = async (newMode: string) => {
  // 如果切换的是当前模式，则退出该模式
  if (editingMode.value === newMode) {
    editingMode.value = ''
    ElMessage.info(`已退出${getModeText(newMode)}模式`)
    updateMapCursor() // 更新鼠标样式
    removeTempPoint() // 移除临时点
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
      removeTempPoint() // 移除临时点
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
const editingGeoJsonData = ref<GeoJSON.FeatureCollection>({
  type: 'FeatureCollection',
  features: [],
}) // 正在编辑的GeoJSON数据
const temporaryCoordinates = ref<[number, number]>([0, 0]) // 临时坐标存储
const showFeatureForm = ref(false) // 显示要素信息表单
//------------------添加要素方法-----------------------
/**
 * 删除临时点要素的方法
 */
const removeTempPoint = () => {
  if (!map || !map.isStyleLoaded()) return // 地图未加载则直接返回

  // 1. 移除临时点图层（如果存在）
  if (map.getLayer('temporary-point-layer')) {
    map.removeLayer('temporary-point-layer')
  }

  // 2. 移除临时点数据源（如果存在）
  if (map.getSource('temporary-point')) {
    map.removeSource('temporary-point')
  }
}
/**
 * 生成临时点要素的方法
 * @param {mapboxgl.LngLat} lngLat - 点的经纬度坐标
 */
const createTempPoint = (lngLat: [number, number]) => {
  if (!map) return
  // 移出临时点
  removeTempPoint()
  // 创建临时点
  map.addSource('temporary-point', {
    type: 'geojson',
    data: {
      type: 'Feature',
      geometry: {
        type: 'Point',
        coordinates: lngLat,
      },
      properties: {},
    },
  })
  map.addLayer({
    id: 'temporary-point-layer',
    type: 'circle',
    source: 'temporary-point',
    paint: {
      'circle-radius': 8,
      'circle-color': '#49A2FF', // 点颜色
      'circle-stroke-width': 2, // 描边宽度
      'circle-stroke-color': '#FFFFFF', // 描边颜色（白色）
    },
  })
}
const featureFormRef = ref() // 要素信息表单引用
// 要素信息表单数据
interface FeatureFormData {
  name: string // 消防栓编号（使用小写）
  currentStatus: string // 设备状态
  currentPressure: number // 当前压力（改为数字类型）
  managementUnit: string // 管理单位
  installationDate: string // 安装日期
}
// 要素信息表单
const featureFormData = ref<FeatureFormData>({
  name: '', // 消防栓编号
  currentStatus: '', // 设备状态
  currentPressure: 0, // 当前压力
  managementUnit: '', // 管理单位
  installationDate: '', // 安装日期
})
// 要素信息表单验证规则
const featureFormDataRules = ref({
  name: [{ required: true, message: '请输入消防栓编号', trigger: 'blur' }],
  currentStatus: [
    { required: true, message: '请选择设备状态', trigger: 'change' },
  ],
  currentPressure: [
    {
      required: true,
      type: 'number',
      message: '请输入压力值',
      trigger: 'blur',
    },
  ],
  managementUnit: [
    { required: true, message: '请输入管理单位', trigger: 'blur' },
  ],
  installationDate: [
    { required: true, message: '请选择安装日期', trigger: 'change' },
  ],
})
// 表单信息提交处理
const handleFeatureFormSubmit = async (operation: string) => {
  if (!featureFormRef.value) return
  try {
    await featureFormRef.value.validate() // 验证表单
  } catch (error) {
    console.log('表单验证失败:', error)
    return
  }
  // 根据操作类型处理
  if (operation === 'addFeature') {
    // 创建要素表单提交逻辑
    handleCreateFeatureSubmit()
  } else if (operation === 'updateFeature') {
    // 更新要素表单提交逻辑
    handleUpdateFeatureSubmit()
  }
}
// 表单提交（创建要素）
const handleCreateFeatureSubmit = () => {
  // 创建临时要素.时间戳生成临时 ID
  const templateFeature: GeoJSON.Feature = {
    type: 'Feature',
    id: `temp_${Date.now()}`, // 临时 ID：避免与正式数据 ID 冲突
    geometry: {
      type: 'Point',
      coordinates: temporaryCoordinates.value,
    },
    properties: {
      Name: featureFormData.value.name,
      currentStatus: featureFormData.value.currentStatus,
      currentPressure: featureFormData.value.currentPressure,
      managementUnit: featureFormData.value.managementUnit,
      installationDate: featureFormData.value.installationDate,
    },
  }
  console.log('创建临时要素:', templateFeature)
  // 将临时要素添加到 临时GeoJSON 数据中
  editingGeoJsonData.value.features.push(templateFeature)
  // 合并原始数据和临时数据，在地图上更新显示
  const source = map?.getSource('sdjzdx_FireHydranty_Point')
  if (source) {
    // 合并数据
    const mergedData: GeoJSON.FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        ...fireHydrantGeojson.value.features,
        ...editingGeoJsonData.value.features,
      ],
    }
    // 更新数据源
    ;(source as mapboxgl.GeoJSONSource).setData(mergedData)
  }
  // 添加marker标记
  const newMarker = creatMaker(templateFeature)
  markers.push(newMarker)
  showFeatureForm.value = false // 关闭表单弹窗
  removeTempPoint() // 移除临时点
  ElMessage.success('要素添加成功！')
}
// 更新要素表单提交逻辑
const handleUpdateFeatureSubmit = () => {
  // 创建临时要素,属性为表单数据
  const templateFeature: GeoJSON.Feature = {
    type: 'Feature',
    id: selectedFeature.value?.id, // 使用选中要素的 ID
    geometry: selectedFeature.value?.geometry || {
      type: 'Point',
      coordinates: [0, 0],
    },
    properties: {
      // 表单数据映射到要素属性
      Name: featureFormData.value.name,
      currentStatus: featureFormData.value.currentStatus,
      currentPressure: featureFormData.value.currentPressure,
      managementUnit: featureFormData.value.managementUnit,
      installationDate: featureFormData.value.installationDate,
    },
  }
  console.log('创建临时要素:', templateFeature)
  // 将临时要素添加到 临时GeoJSON 数据中
  editingGeoJsonData.value.features.push(templateFeature)
  // 合并原始数据和临时数据，在地图上更新显示
  const source = map?.getSource('sdjzdx_FireHydranty_Point')
  if (source) {
    // 合并数据
    const mergedData: GeoJSON.FeatureCollection = {
      type: 'FeatureCollection',
      features: [
        ...fireHydrantGeojson.value.features,
        ...editingGeoJsonData.value.features,
      ],
    }
    // 更新数据源
    ;(source as mapboxgl.GeoJSONSource).setData(mergedData)
  }
  // 查找并更新现有的marker，而不是创建新的
  const featureId = templateFeature.properties?.Name
  if (featureId) {
    // 查找现有的marker并移除
    const markerIndex = markers.findIndex(
      (marker) => marker.getElement().dataset.featureId === featureId,
    )

    if (markerIndex !== -1) {
      markers[markerIndex]?.remove()
      markers.splice(markerIndex, 1)
    }

    // 创建新的marker
    const newMarker = creatMaker(templateFeature)
    markers.push(newMarker)
  }

  showFeatureForm.value = false // 关闭表单弹窗
  ElMessage.success('要素更新成功！')
}
// 创建要素方法
const handleCreateFeature = (e: mapboxgl.MapMouseEvent) => {
  if (!map) return
  // 获取点击位置坐标信息
  temporaryCoordinates.value = [e.lngLat.lng, e.lngLat.lat]
  // 重置表单数据
  featureFormData.value = {
    name: '',
    currentStatus: '',
    currentPressure: 0,
    managementUnit: '',
    installationDate: '',
  }
  showFeatureForm.value = true // 显示要素信息表单
}
// 更新要素方法
const handleUpdateFeature = (e: mapboxgl.MapMouseEvent) => {
  if (!map) return
  // 获取点击要素
  const feature = getClickFeature(e)
  if (!feature) return
  selectedFeature.value = feature // 保存点击的要素
  featureFormData.value = {
    name: feature?.properties?.Name,
    currentStatus: feature?.properties?.currentStatus,
    currentPressure: feature?.properties?.currentPressure || 0,
    managementUnit: feature?.properties?.managementUnit,
    installationDate: feature?.properties?.installationDate,
  }
  showFeatureForm.value = true // 显示要素信息表单
}

// // 删除要素方法
// const handleDeleteFeature = (e: mapboxgl.MapMouseEvent) => {}

const handleSaveFeature = async (operation: string) => {
  console.log('准备保存，操作类型:', operation)
  console.log('待保存的数据:', editingGeoJsonData.value.features)

  if (!editingGeoJsonData.value.features.length) {
    ElMessage.warning('没有需要保存的更改')
    console.log('没有需要保存的更改')
    return
  }

  // 调用API保存编辑的GeoJSON数据
  try {
    ElMessage.info('正在保存更改...')
    switch (operation) {
      case 'addFeature':
        await EditPointFeaturesAPI(
          editingGeoJsonData.value.features,
          'addFeature',
          'sdjzdx_FireHydranty_Point',
        )
        break
      case 'updateFeature':
        console.log('更新要素开始')
        await EditPointFeaturesAPI(
          editingGeoJsonData.value.features,
          'update',
          'sdjzdx_FireHydranty_Point',
        )
        console.log('更新要素成功')
        break
      case 'deleteFeature':
        await EditPointFeaturesAPI(
          editingGeoJsonData.value.features,
          'delete',
          'sdjzdx_FireHydranty_Point',
        )
        break
      default:
        ElMessage.warning('无效的操作类型')
        console.log('无效的操作类型:', operation)
        return
    }
    ElMessage.success('数据保存成功')
    // 重置数据
    editingGeoJsonData.value = {
      type: 'FeatureCollection',
      features: [],
    }
    // 刷新数据源
    addGeoJSONLayer('sdjzdx_FireHydranty_Point')
  } catch (error) {
    ElMessage.error('数据保存失败')
    console.log('数据保存失败:', error)
  }
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
          handleUpdateFeature(e)
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
      handleCreateFeature(e)
      // 飞行到点击位置
      map.flyTo({
        center: e.lngLat,
        zoom: 17,
      })
      // 生成一个临时要素点
      createTempPoint([e.lngLat.lng, e.lngLat.lat])
      return
    }
    // 其他情况下隐藏要素信息弹窗
    removeTempPoint() // 移除临时点
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
        <el-button type="success" @click="handleSaveFeature(editingMode)">
          保存更改
        </el-button>
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
  <!-- 添加要素弹窗表单 -->
  <div v-if="showFeatureForm" class="custom-dialog">
    <!-- 表单：绑定表单实例、数据、验证规则 -->
    <el-form
      ref="featureFormRef"
      :model="featureFormData"
      :rules="featureFormDataRules"
      label-width="120px"
    >
      <div class="dialog-header">
        <h3>要素信息表单</h3>
      </div>

      <!-- 消防栓编号：文本 -->
      <el-form-item label="消防栓编号" prop="Name">
        <el-input
          v-model="featureFormData.name"
          placeholder="请输入消防栓编号"
        ></el-input>
      </el-form-item>
      <!-- 1. 设备状态：下拉选择（正常/维修/错误） -->
      <el-form-item label="设备状态" prop="currentStatus">
        <el-select v-model="featureFormData.currentStatus">
          <el-option label="正常" value="normal"></el-option>
          <el-option label="维修" value="repairing"></el-option>
          <el-option label="错误" value="error"></el-option>
        </el-select>
      </el-form-item>

      <!-- 2. 当前压力：数字输入（步长 0.01，验证必填+数字类型） -->
      <el-form-item label="当前压力" prop="currentPressure">
        <el-input
          v-model.number="featureFormData.currentPressure"
          type="number"
          step="0.01"
          placeholder="请输入压力值"
        ></el-input>
      </el-form-item>

      <!-- 3. 管理单位：文本输入（验证必填） -->
      <el-form-item label="管理单位" prop="managementUnit">
        <el-input
          v-model="featureFormData.managementUnit"
          placeholder="请输入管理单位"
        ></el-input>
      </el-form-item>

      <!-- 4. 安装日期：日期选择器（格式 YYYY-MM-DD，验证必填） -->
      <el-form-item label="安装日期" prop="installationDate">
        <el-date-picker
          v-model="featureFormData.installationDate"
          type="date"
          placeholder="选择安装日期"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        ></el-date-picker>
      </el-form-item>
    </el-form>

    <!-- 弹窗底部按钮：取消/确认添加 -->
    <div class="dialog-footer">
      <el-button @click="((showFeatureForm = false), removeTempPoint())"
        >取消</el-button
      >
      <el-button
        type="primary"
        @click="(handleFeatureFormSubmit(editingMode), removeTempPoint())"
        >确认</el-button
      >
    </div>
  </div>
  <!-- 更新要素弹窗表单 -->
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
  right: 20px;
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
.custom-dialog {
  position: absolute;
  top: 110px;
  right: 20px;
  background-color: #ffffff;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid #ebeef5;
  .dialog-header {
    font-size: 16px;
    color: #333;
    margin-bottom: 20px;
  }
  .el-form-item {
    margin-bottom: 22px;
  }

  // 仅新增按钮容器基础样式
  .dialog-footer {
    display: flex;
    justify-content: flex-end; // 按钮右对齐
    gap: 10px; // 按钮间基础间距
    margin-top: 20px; // 与表单拉开基础距离
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
