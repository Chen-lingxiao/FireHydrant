<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as Cesium from 'cesium'
import * as echarts from 'echarts'
import { GetFeaturesAPI } from '@/api/geoserver.ts'

// 定义GeoJSON要素类型
interface GeoJsonFeature {
  properties: {
    FID: string
    Name: string
    currentStatus: string
    currentPressure: number
    managementUnit: string
  }
}

// 定义消防栓数据类型
interface FireHydrantData {
  id: string
  name: string
  status: string
  pressure: number
  managementUnit: string
}

// 导入环境变量（Vite项目）
// const tiandituToken = import.meta.env.VITE_TIANDITU_TOKEN //天地图token
const cesiumToken = import.meta.env.VITE_CESIUM_TOKEN // cesium token
const mapboxToken = import.meta.env.VITE_MAPBOX_TOKEN // Mapbox token
Cesium.Ion.defaultAccessToken = cesiumToken // 设置Cesium Ion的访问令牌
const mapboxStyleId = 'mapbox/dark-v11' // Mapbox样式ID
const viewer = ref<Cesium.Viewer | null>(null)
// const FireHydrantGeojson = ref<Cesium.GeoJsonDataSource | null>(null)
const mapboxImageryProvider = new Cesium.UrlTemplateImageryProvider({
  url: `https://api.mapbox.com/styles/v1/${mapboxStyleId}/tiles/256/{z}/{x}/{y}?access_token=${mapboxToken}`,
  credit: '© Mapbox © OpenStreetMap contributors', // 版权信息
  tilingScheme: new Cesium.WebMercatorTilingScheme(), // Web墨卡托投影（Mapbox默认）
  maximumLevel: 18, // 最大缩放级别（Mapbox最大支持22，根据需求调整）
})
// 加载geojson数据
const getGeojson = async (layerName: string) => {
  try {
    const response = await GetFeaturesAPI(layerName)
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
// 创建一个映射表，用于存储所有实体
const entityMap = new Map<string, Cesium.Entity>()
const showPopup = ref(false) // 控制弹窗显示
const selectedHydrant = ref<FireHydrantData | null>(null) // 选中的消防栓数据
const selectedHighlightEntity = ref<Cesium.Entity | null>(null) // 新增：跟踪高亮临时实体
// 加载消防栓点数据
const loadFireHydrantData = async () => {
  try {
    // 从GeoServer获取数据
    const geojsonData = await getGeojson('sdjzdx_FireHydranty_Point')
    // console.log('消防栓GeoJSON数据：', FireHydrantGeojson.value)
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
        if (name) {
          // 添加到entityMap 存储到映射表
          entityMap.set(name, entity)
        }
        if (!position) return
        // 添加点击事件
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
          const createPulseCircle = (
            delay: number,
            maxRadius: number,
            loop: number,
          ) => {
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
                    const progress = (elapsed % loop) / loop // 循环播放，确保在loop秒内完成一次完整的动画

                    // 使用更平滑的透明度过渡，避免突然消失
                    const alpha = 0.2 * Math.sin(progress * Math.PI) // 正弦波实现平滑过渡
                    return pulseColor.withAlpha(alpha)
                  }, false),
                ),
                height: 1,
                heightReference: Cesium.HeightReference.CLAMP_TO_GROUND,
                outline: false,
              },
            })
          }

          // 更均匀的脉冲环（8个），调整延迟步进为0.75以优化动画流畅度
          dataSource.entities.add(createPulseCircle(0, 6, 6))
          dataSource.entities.add(createPulseCircle(0.75, 9, 6))
          dataSource.entities.add(createPulseCircle(1.5, 12, 6))
          dataSource.entities.add(createPulseCircle(2.25, 15, 6))
          dataSource.entities.add(createPulseCircle(3.0, 18, 6))
          dataSource.entities.add(createPulseCircle(3.75, 21, 6))
          dataSource.entities.add(createPulseCircle(4.5, 24, 6))
          dataSource.entities.add(createPulseCircle(5.25, 27, 6))
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

// echarts 表格数据
const statusPieChart = ref<echarts.ECharts | null>(null) // 设备状态数量饼图
const pressureBarChart = ref<echarts.ECharts | null>(null) // 压力分布柱状图
const avgPressureLineChart = ref<echarts.ECharts | null>(null) // 平均压力变化折线图
// 压力区间配置（贴合消防栓实际压力标准）
const PRESSURE_RANGES = [
  { label: '<0.15', min: -Infinity, max: 0.15, color: '#ff4d4f' },
  { label: '0.15-0.3', min: 0.15, max: 0.3, color: '#52c41a' },
  { label: '0.3-0.6', min: 0.3, max: 0.6, color: '#faad14' },
  { label: '>0.6', min: 0.6, max: Infinity, color: '#e53935' },
]
// 正常消防栓列表
const normalFireHydrants = ref<FireHydrantData[]>([])
const errorFireHydrants = ref<FireHydrantData[]>([])
const repairingFireHydrants = ref<FireHydrantData[]>([])
// 获取设备状态数量饼图数据
let statusPieData: { value: number; name: string }[] = []
// 压力分布柱状图数据
let pressureBarData: {
  value: number
  name: string
  itemStyle: { color: string }
}[] = []
// 平均压力变化折线图数据
const avgPressureLineData: {
  value: number
  name: string
}[] = []
let pressureLineChartInterval: number | null = null // 折线图定时器ID

// 加载消防栓点数据
const loadEcharts = async () => {
  try {
    // 从GeoServer获取数据
    const geojsonData = await getGeojson('sdjzdx_FireHydranty_Point')
    console.log('加载echarts消防栓点数据：', geojsonData)
    if (geojsonData) {
      const data = geojsonData.features.map((feature: GeoJsonFeature) => {
        return {
          id: feature.properties.FID, // ID
          name: feature.properties.Name, // 名称
          status: feature.properties.currentStatus, // 设备状态
          pressure: feature.properties.currentPressure, // 压力
          managementUnit: feature.properties.managementUnit, // 所属管理单元
        }
      })
      // console.log('处理后的消防栓数据：', data)
      // -------------------------- 按照状态过滤筛选数据 --------------------------
      normalFireHydrants.value = data.filter(
        (item: { status: string }) => item.status === 'normal',
      )
      errorFireHydrants.value = data.filter(
        (item: { status: string }) => item.status === 'error',
      )
      repairingFireHydrants.value = data.filter(
        (item: { status: string }) => item.status === 'repairing',
      )

      // -------------------------- 统计设备状态数量（饼图） --------------------------
      statusPieData = [
        { value: normalFireHydrants.value.length, name: '正常' },
        { value: errorFireHydrants.value.length, name: '异常' },
        { value: repairingFireHydrants.value.length, name: '维修' },
      ]
      // -------------------------- 统计压力区间数量（柱状图） --------------------------
      pressureBarData = PRESSURE_RANGES.map((range) => {
        const filteredData = data.filter(
          (item: { pressure: number }) =>
            item.pressure >= range.min && item.pressure <= range.max,
        )
        return {
          value: filteredData.length,
          name: range.label,
          itemStyle: {
            color: range.color,
          },
        }
      })
      // -------------------------- 模拟数据压力变化（折线图） --------------------------
      // 模拟数据
      // 获得当前数据压力平均值,保留2位小数
      const avgPressure = (
        data.reduce(
          (acc: number, item: { pressure: number }) => acc + item.pressure,
          0,
        ) / data.length
      ).toFixed(2)
      // 定时器，假设每3小时更新一次，新数据为模拟数据
      avgPressureLineData.push({
        value: Number(avgPressure),
        name: new Date().toLocaleTimeString(),
      })

      // 如果已有定时器，先清除
      if (pressureLineChartInterval) {
        clearInterval(pressureLineChartInterval)
      }

      // 设置新的定时器，每3小时更新一次数据 (为了演示效果，这里设置为10秒)
      pressureLineChartInterval = window.setInterval(() => {
        // 生成 0.2-0.8 之间的随机数，保留2位小数（适配压力数据格式）
        const randomNum = Number((0.2 + Math.random() * (0.8 - 0.2)).toFixed(2))
        // 添加新数据到数组尾部
        avgPressureLineData.push({
          value: randomNum,
          name: new Date().toLocaleTimeString(),
        })
        // 关键：若数组长度超过8，删除最前面（最早）的元素
        if (avgPressureLineData.length > 8) {
          avgPressureLineData.shift() // 删除数组第一个元素
        }

        // 更新图表
        if (avgPressureLineChart.value) {
          avgPressureLineChart.value.setOption({
            xAxis: {
              data: avgPressureLineData.map((item) => item.name),
            },
            series: [
              {
                data: avgPressureLineData.map((item) => item.value),
              },
            ],
          })
        }
        console.log('更新压力分布数据：', avgPressureLineData)
      }, 10000) // 10秒更新一次（模拟3小时）
    }
  } catch (error) {
    console.error('加载消防栓点数据失败：', error)
  }
}
//-------------------------- 初始化设备状态数量饼图 --------------------------//
const initStatusPieChart = (
  statusPieData: { value: number; name: string }[],
) => {
  // 确保DOM元素存在
  const chartDom = document.getElementById('status-pie-chart')
  if (!chartDom) return

  statusPieChart.value = echarts.init(chartDom)

  // 处理空数据情况
  const processedData =
    statusPieData && statusPieData.length > 0
      ? statusPieData.map((item) => ({
          value: item.value,
          name: item.name,
        }))
      : [{ value: 0, name: '暂无数据' }]

  const option = {
    title: {
      text: '设备状态数量',
      left: 'center',
      textStyle: {
        fontSize: 14,
        color: '#ffffff', // 提亮标题文字颜色
      },
    },
    // 提示框
    tooltip: {
      trigger: 'item',
      textStyle: {
        color: '#000000',
      },
    },
    // 图例
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: {
        fontSize: 12,
        color: '#ffffff', // 提亮图例文字颜色
      },
    },
    // series
    series: [
      {
        name: '设备状态',
        type: 'pie',
        radius: ['30%', '55%'], // 设置饼图的半径
        data: processedData,
        color: ['#52c41a', '#ff0000', '#ffa500'], // 绿色、红色、橙色
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
        label: {
          color: '#ffffff', // 提亮标签文字颜色
        },
      },
    ],
  }
  statusPieChart.value?.setOption(option)
}
//-------------------------- 初始化状态分布柱状图 --------------------------//
const initPressureBarChart = (
  pressureBarData: {
    value: number
    name: string
    itemStyle: { color: string }
  }[],
) => {
  // 确保DOM元素存在
  const chartDom = document.getElementById('pressure-bar-chart')
  if (!chartDom) return

  pressureBarChart.value = echarts.init(chartDom)

  // 处理空数据情况
  const processedData =
    pressureBarData && pressureBarData.length > 0
      ? pressureBarData
      : [{ value: 0, name: '暂无数据', itemStyle: { color: '#999999' } }]

  const option = {
    title: {
      text: '压力分布柱状图',
      left: 'center',
      textStyle: {
        fontSize: 14,
        color: '#ffffff', // 提亮标题文字颜色
      },
    },
    tooltip: {
      trigger: 'item',
      textStyle: {
        color: '#000000',
      },
    },
    xAxis: {
      type: 'category',
      data: processedData.map((item) => item.name),
      axisLabel: {
        fontSize: 10,
        color: '#ffffff', // 提亮X轴标签文字颜色
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 10,
        color: '#ffffff', // 提亮Y轴标签文字颜色
      },
    },
    series: [
      {
        data: processedData.map((item) => ({
          value: item.value,
          itemStyle: item.itemStyle,
        })),
        type: 'bar',
        label: {
          show: true,
          position: 'top',
          color: '#ffffff', // 提亮柱状图标签文字颜色
        },
      },
    ],
  }
  pressureBarChart.value?.setOption(option)
}
// -------------------------- 初始化平均压力变化折线图 --------------------------
const initPressureLineChart = (
  avgPressureLineData: { value: number; name: string }[],
) => {
  // 确保DOM元素存在
  const chartDom = document.getElementById('avg-pressure-line-chart')
  if (!chartDom) return
  avgPressureLineChart.value = echarts.init(chartDom) // 初始化折线图
  const option = {
    title: {
      text: '平均压力变化折线图',
      // 副标题
      subtext: '(模拟数据)',
      left: 'center',
      textStyle: {
        fontSize: 14,
        color: '#ffffff',
      },
    },
    tooltip: {
      trigger: 'item',
      textStyle: {
        // 黑色文字
        color: '#000000',
      },
    },
    xAxis: {
      type: 'category',
      data: avgPressureLineData.map((item) => item.name),
      axisLabel: {
        color: '#ffffff', // 提亮X轴标签文字颜色
      },
    },
    yAxis: {
      type: 'value',
      name: '压力值 (MPa)',
      nameTextStyle: {
        color: '#ffffff', // 提亮Y轴名称文字颜色
      },
      axisLabel: {
        color: '#ffffff', // 提亮Y轴标签文字颜色
      },
    },
    series: [
      {
        data: avgPressureLineData.map((item) => item.value),
        type: 'line',
        smooth: false, // 直线
        symbolSize: 8, // 标记点大小
        label: {
          show: true,
          position: 'top',
          formatter: '{c} MPa',
          color: '#ffffff', // 提亮数据标签文字颜色
        },
        lineStyle: {
          color: '#ffffff', // 提亮点标记颜色
        },
        itemStyle: {
          color: '#ffffff', // 提亮点标记颜色
        },
      },
    ],
  }
  avgPressureLineChart.value?.setOption(option)
}
/**
 *  高亮效果
 * @param entity   Cesium.Entity
 */
const addHightlight = (entity: Cesium.Entity) => {
  // 创建高亮临时实体（复制位置，设置高亮样式）
  const position = entity.position?.getValue(Cesium.JulianDate.now())
  if (position) {
    const highlightEntity = new Cesium.Entity({
      position: position,
      point: new Cesium.PointGraphics({
        color: Cesium.Color.fromCssColorString('#339af0'), // 高亮颜色
        pixelSize: 32, // 增大点大小
        outlineColor: Cesium.Color.WHITE, // 白色轮廓
        outlineWidth: 1, // 加粗轮廓
        heightReference: Cesium.HeightReference.CLAMP_TO_GROUND,
        disableDepthTestDistance: Number.POSITIVE_INFINITY,
      }),
    })
    viewer.value?.entities.add(highlightEntity)
    selectedHighlightEntity.value = highlightEntity
  }
}
const removeHightlight = () => {
  // 先移除之前的高亮临时实体（如果存在）
  if (selectedHighlightEntity.value) {
    viewer.value?.entities.remove(selectedHighlightEntity.value)
    selectedHighlightEntity.value = null
  }
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
    infoBox: false, // 信息框（点击实体看详情）
    vrButton: false, // 禁用VR按钮
    selectionIndicator: false, // 禁用选中指示器（绿色框）
  })
  // 关闭抗锯齿
  viewer.value.scene.postProcessStages.fxaa.enabled = false
  // 添加Mapbox底图
  viewer.value.imageryLayers.addImageryProvider(mapboxImageryProvider)

  // 添加自定义点击事件处理
  const handler = new Cesium.ScreenSpaceEventHandler(viewer.value.scene.canvas)

  handler.setInputAction(
    (event: Cesium.ScreenSpaceEventHandler.PositionedEvent) => {
      // const pickedObject = viewer.value?.scene.pick(event.position)
      // 使用 drillPick 来获取所有重叠的对象，按深度排序
      const pickedObjects = viewer.value?.scene.drillPick(event.position) || []

      let foundHydrant = false // 标记是否找到消防栓
      for (const picked of pickedObjects) {
        const entity = picked.id as Cesium.Entity | undefined
        if (entity && entity.properties && entity.properties.Name) {
          // 检查是否是消防栓实体（有 Name 属性）
          const properties = entity.properties
          // 先移除之前的高亮临时实体（如果存在）
          removeHightlight()
          // 创建高亮临时实体（复制位置，设置高亮样式）
          addHightlight(entity)
          // 更新选中的消防栓数据
          selectedHydrant.value = {
            id: properties?.FID?.getValue(),
            name: properties?.Name?.getValue(),
            status: properties?.currentStatus?.getValue(),
            pressure: properties?.currentPressure?.getValue(),
            managementUnit: properties?.managementUnit?.getValue(),
          }
          const position = entity.position?.getValue(Cesium.JulianDate.now())
          // 显示弹窗
          showPopup.value = true

          // 飞到正上方俯瞰
          if (position) {
            const cartographic = Cesium.Cartographic.fromCartesian(position)
            const elevatedPosition = Cesium.Cartesian3.fromRadians(
              cartographic.longitude,
              cartographic.latitude,
              cartographic.height + 500, // 300米高度俯瞰
            )

            viewer.value?.camera.flyTo({
              destination: elevatedPosition,
              duration: 1.5,
              orientation: {
                heading: Cesium.Math.toRadians(0.0),
                pitch: Cesium.Math.toRadians(-90.0), // 正上方俯瞰
                roll: 0.0,
              },
            })
          }

          foundHydrant = true
          break // 找到第一个消防栓后停止循环
        }
      }

      if (!foundHydrant) {
        // 如果没有找到消防栓，关闭弹窗
        showPopup.value = false
        if (selectedHighlightEntity.value) {
          viewer.value?.entities.remove(selectedHighlightEntity.value)
          selectedHighlightEntity.value = null
        }
      }
    },
    Cesium.ScreenSpaceEventType.LEFT_CLICK,
  )
  flyToInitialView() // 飞到初始视角
}

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

  // 清理折线图定时器
  if (pressureLineChartInterval) {
    clearInterval(pressureLineChartInterval)
    pressureLineChartInterval = null
  }
}

// 添加 handleClick 方法，用于处理表格中"查看"按钮的点击事件
const handleClick = (row: FireHydrantData) => {
  // 打印对应行信息
  console.log(row)
  // 根据name查找实体
  const entity = entityMap.get(row.name)
  if (entity && viewer.value) {
    const properties = entity.properties
    // 先移除之前的高亮临时实体（如果存在）
    removeHightlight()
    // 创建高亮临时实体（复制位置，设置高亮样式）
    addHightlight(entity)
    // 更新选中的消防栓数据
    selectedHydrant.value = {
      id: properties?.FID?.getValue(),
      name: properties?.Name?.getValue(),
      status: properties?.currentStatus?.getValue(),
      pressure: properties?.currentPressure?.getValue(),
      managementUnit: properties?.managementUnit?.getValue(),
    }
    // 显示弹窗
    showPopup.value = true
    const position = entity.position?.getValue(Cesium.JulianDate.now())
    if (position) {
      // 将贴地点位转换为Cartographic格式添加高度
      const cartographic = Cesium.Cartographic.fromCartesian(position)
      const elevatedPosition = Cesium.Cartesian3.fromRadians(
        cartographic.longitude,
        cartographic.latitude,
        cartographic.height + 500, // 添加500米高度
      )
      // 飞行到该位置
      viewer.value.camera.flyTo({
        destination: elevatedPosition,
        duration: 1.5, // 飞行时间（秒）
        // 摄像机角度
        orientation: {
          heading: Cesium.Math.toRadians(0.0), // 朝向
          pitch: Cesium.Math.toRadians(-90.0), // 俯仰角
          roll: 0.0,
        },
      })
    }
  } else {
    console.warn(`未找到name为 ${row.name} 的实体`)
  }
}
const dateRef = ref<HTMLElement | null>(null)
const dayRef = ref<HTMLElement | null>(null)
const timeRef = ref<HTMLElement | null>(null)
const timer = null

const updateClock = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const days = [
    '星期日',
    '星期一',
    '星期二',
    '星期三',
    '星期四',
    '星期五',
    '星期六',
  ]
  const weekday = days[now.getDay()]
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')

  // 使用ref更新所有元素
  if (dateRef.value) {
    dateRef.value.innerText = `${year}年${month}月${day}日`
  }
  if (dayRef.value) {
    dayRef.value.innerText = `${weekday}`
  }
  if (timeRef.value) {
    timeRef.value.innerText = `${hours}:${minutes}:${seconds}`
  }
}

// 初始化echarts
const initEcharts = () => {
  initStatusPieChart(statusPieData) // 初始化状态饼图
  initPressureBarChart(pressureBarData) // 初始化压力分布柱状图
  initPressureLineChart(avgPressureLineData) // 初始化平均压力变化折线图
}
onMounted(async () => {
  initCesium() // 初始化Cesium
  loadLayers() // 加载GeoJSON图层
  await loadEcharts() // 加载echarts图表
  initEcharts() // 初始化echarts图表
  updateClock()
  setInterval(updateClock, 1000) // 每秒更新一次时间
  console.log(normalFireHydrants.value, errorFireHydrants.value)
})
onBeforeUnmount(() => {
  destroyCesium()
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <div class="cesium-container">
    <div class="cesium-viewer" id="cesiumContainer"></div>
    <!-- 消防栓信息弹窗 -->
    <div v-if="showPopup && selectedHydrant" class="hydrant-popup">
      <div class="popup-content">
        <h3>消防栓信息</h3>
        <div class="info-row">
          <span class="label">编号:</span>
          <span class="value">{{ selectedHydrant.name }}</span>
        </div>
        <div class="info-row">
          <span class="label">状态:</span>
          <span class="value">{{ selectedHydrant.status }}</span>
        </div>
        <div class="info-row">
          <span class="label">压力:</span>
          <span class="value">{{ selectedHydrant.pressure }} MPa</span>
        </div>
        <div class="info-row">
          <span class="label">管理单位:</span>
          <span class="value">{{ selectedHydrant.managementUnit }}</span>
        </div>
        <button class="close-btn" @click="showPopup = false">x</button>
      </div>
    </div>
  </div>
  <div class="leaft-info">
    <div class="echarts">
      <div id="status-pie-chart" style="width: 100%; height: 100%"></div>
    </div>
    <div class="echarts">
      <div id="avg-pressure-line-chart" style="width: 100%; height: 100%"></div>
    </div>
    <div class="echarts">
      <div id="pressure-bar-chart" style="width: 100%; height: 100%"></div>
    </div>
  </div>
  <div class="right-info">
    <div class="info-card">
      <h3>系统信息</h3>
      <div class="info-grid">
        <div class="info-item">
          <p class="info-label">正常消防栓数量</p>
          <p class="info-value">{{ normalFireHydrants.length }}</p>
        </div>
        <div class="info-item">
          <p class="info-label">压力异常消防栓数量</p>
          <p class="info-value error">{{ errorFireHydrants.length }}</p>
        </div>
        <div class="info-item">
          <p class="info-label">维修中消防栓数量</p>
          <p class="info-value warning">
            {{ repairingFireHydrants.length }}
          </p>
        </div>
        <div id="clock" class="info-time">
          <div
            id="date"
            ref="dateRef"
            style="font-size: 20px; margin-bottom: 5px"
          >
            2023年10月15日
          </div>
          <div
            id="day"
            ref="dayRef"
            style="font-size: 16px; margin-bottom: 10px"
          >
            星期日
          </div>
          <div
            id="time"
            ref="timeRef"
            style="font-size: 32px; font-weight: bold; letter-spacing: 1px"
          >
            12:00:00
          </div>
        </div>
      </div>
    </div>
    <div class="table-card">
      <h3>故障消防栓列表</h3>
      <el-table :data="errorFireHydrants" style="width: 100%" height="230px">
        <el-table-column prop="name" label="编号" min-width="100" />
        <el-table-column
          prop="managementUnit"
          label="管理单位"
          min-width="100"
        />
        <el-table-column prop="pressure" label="MPa" min-width="50" />
        <el-table-column fixed="right" label="操作" min-width="50">
          <template #default="scope">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleClick(scope.row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="table-card">
      <h3>维修消防栓信息</h3>
      <el-table
        :data="repairingFireHydrants"
        style="width: 100%"
        height="230px"
      >
        <el-table-column prop="name" label="编号" min-width="100" />
        <el-table-column
          prop="managementUnit"
          label="管理单位"
          min-width="100"
        />
        <el-table-column prop="pressure" label="MPa" min-width="50" />
        <el-table-column fixed="right" label="操作" min-width="50">
          <template #default="scope">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleClick(scope.row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped lang="scss">
.cesium-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 60px);
  .cesium-viewer {
    width: 100%;
    height: 100%;
  }
}
.leaft-info {
  width: 400px;
  height: calc(100vh - 60px);
  position: absolute;
  top: 60px;
  left: 0px;
  z-index: 100;
  backdrop-filter: blur(10px);
  background: linear-gradient(to right, #000c1d, #2740584d);
  border: 1px solid var(--card-border);
  display: flex;
  flex-direction: column;
  padding: 10px;
  box-sizing: border-box;
  .echarts {
    flex: 1;
    margin-bottom: 10px;
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 8px;
    padding: 0;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
  }
}
.right-info {
  width: 400px;
  height: calc(100vh - 60px);
  position: absolute;
  top: 60px;
  right: 0px;
  z-index: 100;
  backdrop-filter: blur(10px);
  background: linear-gradient(to left, #000c1d, #2740584d);
  border: 1px solid var(--card-border);
  padding: 10px;
  box-sizing: border-box;
  color: white;
  display: flex;
  flex-direction: column;
  gap: 10px;
  .info-card {
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 8px;
    padding: 15px;
    box-sizing: border-box;
    flex: 1;
    position: relative;
    .info-grid {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      .info-item {
        padding-top: 20px;
        display: flex;
        flex-direction: column;
        gap: 5px;
      }
      .info-time {
        width: 200px;
        position: absolute;
        right: 20px;
        background: #22222200;
        color: rgb(0, 116, 248);
        border-radius: 8px;
        font-family: Arial, sans-serif;
        text-align: center;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
      }
    }
  }
  .table-card {
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 8px;
    padding: 15px;
    box-sizing: border-box;
    flex: 1;
    h3 {
      margin: 0 0 15px 0;
      color: white;
      font-size: 16px;
      font-weight: bold;
    }
  }

  .info-label {
    margin: 0;
    color: #aaa;
    font-size: 14px;
  }
  .info-value {
    margin: 0;
    color: #fff;
    font-size: 20px;
    font-weight: bold;
  }
  .info-value.error {
    color: #ff4d4f;
  }
  .info-value.warning {
    color: #faad14;
  }
  // element-ui el-table样式
  :deep(.el-table) {
    background-color: transparent;
    color: white;
  }
  :deep(.el-table__body-wrapper) {
    background-color: rgba(0, 0, 0, 0.2);
  }
  :deep(.el-table th) {
    background-color: rgba(0, 0, 0, 0.3);
    color: white;
  }
  :deep(.el-table tr) {
    background-color: transparent;
    color: white;
  }
  :deep(.el-table .el-table__row:hover) {
    background-color: rgba(255, 255, 255, 0.1);
    color: #000c1d;
  }
}
.hydrant-popup {
  position: absolute;
  top: 20%;
  right: 400px;
  transform: translate(-50%, -50%);
  background: #0b192748;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  padding: 20px;
  min-width: 250px;
  z-index: 1000;
  .popup-content h3 {
    margin: 0 0 15px 0;
    color: #fff;
    text-align: center;
  }
  .info-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f0f0f0;
    .label {
      font-weight: bold;
      color: #fff;
    }
    .value {
      color: #fff;
    }
  }
  .close-btn {
    position: absolute;
    top: 0;
    right: 0;
    width: 20px;
    height: 20px;
    background: #0b1927;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    text-align: center;
    line-height: 20px;
  }

  .close-btn:hover {
    background: #40a9ff;
  }
}
</style>
