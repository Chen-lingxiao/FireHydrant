# WFS-T 服务配置规范

## 1. WFS-T 插入点要素 (Insert Point Feature)

```xml
<wfs:Transaction service="{SERVICE_TYPE}" version="{WFS_VERSION}"
  xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:gml="http://www.opengis.net/gml"
  xmlns:workspace="{WORKSPACE_NAMESPACE}">
  <wfs:Insert>
    <{LAYER_NAME}>
      <{GEOMETRY_FIELD}>
        <gml:Point srsName="{COORDINATE_SYSTEM}">
          <gml:coordinates>{LONGITUDE},{LATITUDE}</gml:coordinates>
        </gml:Point>
      </{GEOMETRY_FIELD}>
      <!-- 属性字段 -->
      <{PROPERTY_NAME_1}>{PROPERTY_VALUE_1}</{PROPERTY_NAME_1}>
      <{PROPERTY_NAME_2}>{PROPERTY_VALUE_2}</{PROPERTY_NAME_2}>
    </{LAYER_NAME}>
  </wfs:Insert>
</wfs:Transaction>
```

参数说明：
- `{SERVICE_TYPE}`: 服务类型，固定为"WFS"
- `{WFS_VERSION}`: WFS 版本号，常用"1.0.0"
- `xmlns:wfs`: WFS 命名空间
- `xmlns:gml`: GML 命名空间
- `{WORKSPACE_NAMESPACE}`: 工作区命名空间URL
- `{LAYER_NAME}`: 图层名称（使用本地名称，不要加命名空间前缀）
- `{GEOMETRY_FIELD}`: 几何字段名称
- `{COORDINATE_SYSTEM}`: 坐标系，如"EPSG:4326"表示 WGS84
- `{LONGITUDE}`: 经度值
- `{LATITUDE}`: 纬度值
- `{PROPERTY_NAME_N}`: 属性字段名称
- `{PROPERTY_VALUE_N}`: 属性字段值

## 2. WFS-T 插入线要素 (Insert Line Feature)

```xml
<wfs:Transaction service="{SERVICE_TYPE}" version="{WFS_VERSION}"
  xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:gml="http://www.opengis.net/gml"
  xmlns:workspace="{WORKSPACE_NAMESPACE}">
  <wfs:Insert>
    <{LAYER_NAME}>
      <{GEOMETRY_FIELD}>
        <gml:LineString srsName="{COORDINATE_SYSTEM}">
          <gml:coordinates>{COORDINATES_LIST}</gml:coordinates>
        </gml:LineString>
      </{GEOMETRY_FIELD}>
      <!-- 属性字段 -->
      <{PROPERTY_NAME_1}>{PROPERTY_VALUE_1}</{PROPERTY_NAME_1}>
      <{PROPERTY_NAME_2}>{PROPERTY_VALUE_2}</{PROPERTY_NAME_2}>
    </{LAYER_NAME}>
  </wfs:Insert>
</wfs:Transaction>
```

参数说明：
- `{COORDINATES_LIST}`: 坐标列表，格式为"经度1,纬度1 经度2,纬度2 ..."，坐标之间用空格分隔

## 3. WFS-T 插入面要素 (Insert Polygon Feature)

```xml
<wfs:Transaction service="{SERVICE_TYPE}" version="{WFS_VERSION}"
  xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:gml="http://www.opengis.net/gml"
  xmlns:workspace="{WORKSPACE_NAMESPACE}">
  <wfs:Insert>
    <{LAYER_NAME}>
      <{GEOMETRY_FIELD}>
        <gml:Polygon srsName="{COORDINATE_SYSTEM}">
          <gml:outerBoundaryIs>
            <gml:LinearRing>
              <gml:coordinates>{COORDINATES_LIST}</gml:coordinates>
            </gml:LinearRing>
          </gml:outerBoundaryIs>
        </gml:Polygon>
      </{GEOMETRY_FIELD}>
      <!-- 属性字段 -->
      <{PROPERTY_NAME_1}>{PROPERTY_VALUE_1}</{PROPERTY_NAME_1}>
      <{PROPERTY_NAME_2}>{PROPERTY_VALUE_2}</{PROPERTY_NAME_2}>
    </{LAYER_NAME}>
  </wfs:Insert>
</wfs:Transaction>
```

参数说明：
- `{COORDINATES_LIST}`: 坐标列表，格式为"经度1,纬度1 经度2,纬度2 ... 经度1,纬度1"，第一个和最后一个坐标必须相同以闭合多边形

## 4. WFS-T 插入多点要素 (Insert MultiPoint Feature)

```xml
<wfs:Transaction service="{SERVICE_TYPE}" version="{WFS_VERSION}"
  xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:gml="http://www.opengis.net/gml"
  xmlns:workspace="{WORKSPACE_NAMESPACE}">
  <wfs:Insert>
    <{LAYER_NAME}>
      <{GEOMETRY_FIELD}>
        <gml:MultiPoint srsName="{COORDINATE_SYSTEM}">
          <gml:pointMember>
            <gml:Point>
              <gml:coordinates>{LONGITUDE_1},{LATITUDE_1}</gml:coordinates>
            </gml:Point>
          </gml:pointMember>
          <gml:pointMember>
            <gml:Point>
              <gml:coordinates>{LONGITUDE_2},{LATITUDE_2}</gml:coordinates>
            </gml:Point>
          </gml:pointMember>
        </gml:MultiPoint>
      </{GEOMETRY_FIELD}>
      <!-- 属性字段 -->
      <{PROPERTY_NAME_1}>{PROPERTY_VALUE_1}</{PROPERTY_NAME_1}>
      <{PROPERTY_NAME_2}>{PROPERTY_VALUE_2}</{PROPERTY_NAME_2}>
    </{LAYER_NAME}>
  </wfs:Insert>
</wfs:Transaction>
```

参数说明：
- 可以根据需要添加更多的`<gml:pointMember>`元素来表示更多的点

## 5. WFS-T 更新点要素 (Update Point Feature)

```xml
<wfs:Transaction service="{SERVICE_TYPE}" version="{WFS_VERSION}"
  xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:ogc="http://www.opengis.net/ogc">
  <wfs:Update typeName="{WORKSPACE}:{LAYER_NAME}">
    <wfs:Property>
      <wfs:Name>{PROPERTY_NAME_1}</wfs:Name>
      <wfs:Value>{NEW_VALUE_1}</wfs:Value>
    </wfs:Property>
    <wfs:Property>
      <wfs:Name>{PROPERTY_NAME_2}</wfs:Name>
      <wfs:Value>{NEW_VALUE_2}</wfs:Value>
    </wfs:Property>
    <!-- 定位要更新的要素 -->
    <ogc:Filter>
      <ogc:FeatureId fid="{FEATURE_ID}"/>
    </ogc:Filter>
  </wfs:Update>
</wfs:Transaction>
```

参数说明：
- `{SERVICE_TYPE}`: 服务类型，固定为"WFS"
- `{WFS_VERSION}`: WFS 版本号，常用"1.0.0"
- `{WORKSPACE}`: 工作区名称
- `{LAYER_NAME}`: 图层名称
- `{PROPERTY_NAME_N}`: 要更新的属性名称
- `{NEW_VALUE_N}`: 新的属性值
- `{FEATURE_ID}`: 要素唯一标识符
- `xmlns:ogc`: OGC 命名空间，用于过滤器

## 6. WFS-T 删除点要素 (Delete Point Feature)

```xml
<wfs:Transaction service="{SERVICE_TYPE}" version="{WFS_VERSION}"
  xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:ogc="http://www.opengis.net/ogc">
  <wfs:Delete typeName="{WORKSPACE}:{LAYER_NAME}">
    <ogc:Filter>
      <ogc:FeatureId fid="{FEATURE_ID}"/>
    </ogc:Filter>
  </wfs:Delete>
</wfs:Transaction>
```

参数说明：
- `{SERVICE_TYPE}`: 服务类型，固定为"WFS"
- `{WFS_VERSION}`: WFS 版本号，常用"1.0.0"
- `{WORKSPACE}`: 工作区名称
- `{LAYER_NAME}`: 图层名称
- `{FEATURE_ID}`: 要删除的要素唯一标识符

## 注意事项

1. 坐标顺序：使用 EPSG:4326 坐标系时，经纬度顺序应为"{LONGITUDE},{LATITUDE}"
2. 命名空间：在 Transaction 根元素声明命名空间，但在 Insert 操作中使用本地元素名称（不带前缀）
3. 图层名称：确保图层名称与 GeoServer 中发布的图层名称完全一致
4. 属性字段：确保属性字段名称与图层定义中的字段名称一致
5. 面要素：面要素的第一个和最后一个坐标必须相同以闭合多边形