import axios from 'axios' // 引入axios库
import type { Feature } from 'geojson'

// 图层固定信息（统一维护，避免硬编码）
const LAYER_INFO = {
  baseURL: 'http://localhost:8085/geoserver', // GeoServer基础URL
  workspace: 'sdjzdx', // 工作区名称
  namespace: 'http://localhost:8085/geoserver/sdjzdx', // 工作区命名空间（需与GeoServer一致）
  geomField: 'geom', // 几何字段名
}
// 从GeoServer加载要素数据
/**
 * @param none
 * @returns GeoJSON.FeatureCollection
 */
export const GetFeatures = async (layerName: string) => {
  const url = `${LAYER_INFO.baseURL}/wfs`
  // WFS GetFeature请求参数
  const params = {
    service: 'WFS',
    version: '1.1.0',
    request: 'GetFeature',
    typeName: `${LAYER_INFO.workspace}:${layerName}`, // 图层名称，格式为 workspace:layerName
    outputFormat: 'application/json',
    srsName: 'urn:ogc:def:crs:EPSG::4326',
  }
  try {
    const response = await axios.get(url, { params })
    // 遍历循环，分割id fire_hydrants.1 --> 1
    response.data.features.forEach((feature: Feature) => {
      if (feature.id) {
        const idParts = String(feature.id).split('.')
        feature.id = idParts.length > 1 ? idParts[1] : feature.id
      }
    })
    return response.data
  } catch (error) {
    console.error('加载要素失败:', error)
    throw error
  }
}
