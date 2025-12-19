<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserByid } from '@/api/user'
import { useUserStore } from '@/stores'

interface UserData {
  id: number
  name: string
  sex: string
  birthDate: string
  department: string
  role: string
  email: string
  telephone: string
  createTime: string
}

const userStore = useUserStore()
const userInfo = ref<UserData | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

// 时间格式化函数
const formatDate = (dateString: string, includeTime: boolean = true) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  if (isNaN(date.getTime())) return dateString // 如果日期无效，返回原字符串

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  if (!includeTime) return `${year}-${month}-${day}`

  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 获取用户首字母（安全处理）
const getAvatarText = (name: string | undefined | null) => {
  return name ? name.charAt(0).toUpperCase() : '?'
}

const getUserInfo = async () => {
  try {
    loading.value = true
    error.value = null

    // 检查用户信息是否存在
    if (userStore.userInfo && userStore.userInfo.id) {
      const response = await getUserByid(userStore.userInfo.id)
      console.log('用户信息：', response.data)

      // 验证返回的数据结构
      if (response.data && response.data.data && response.data.data.name) {
        userInfo.value = response.data.data
      } else {
        throw new Error('返回的用户数据格式不正确或缺少必要字段')
      }
    } else {
      error.value = '用户未登录或用户ID不存在'
    }
  } catch (err) {
    console.error('获取用户信息失败：', err)
    error.value = '获取用户信息失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  getUserInfo()
})
</script>

<template>
  <div class="user-info-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>正在加载用户信息...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">⚠️</div>
      <h3>加载失败</h3>
      <p>{{ error }}</p>
      <button @click="getUserInfo" class="retry-btn">重新加载</button>
    </div>

    <!-- 成功状态 -->
    <div v-else-if="userInfo" class="user-info-card">
      <!-- 头部 -->
      <div class="card-header">
        <div class="user-avatar">
          <span class="avatar-text">{{ getAvatarText(userInfo.name) }}</span>
        </div>
        <div class="header-text">
          <h2>{{ userInfo.name }}</h2>
          <div class="user-role">
            <span class="role-badge">{{ userInfo.role }}</span>
          </div>
        </div>
        <div class="status-indicator">
          <span class="pulse-dot"></span>
          <span>活跃</span>
        </div>
      </div>

      <!-- 信息网格 -->
      <div class="info-grid">
        <div class="info-section">
          <div class="section-title">
            <span class="icon">👤</span>
            <span>基本信息</span>
          </div>
          <div class="section-content">
            <div class="info-row">
              <span class="label">姓名</span>
              <span class="value">{{ userInfo.name || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">性别</span>
              <span class="value">{{ userInfo.sex || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">出生日期</span>
              <span class="value">{{
                formatDate(userInfo.birthDate, false)
              }}</span>
            </div>
          </div>
        </div>

        <div class="info-section">
          <div class="section-title">
            <span class="icon">🏢</span>
            <span>工作信息</span>
          </div>
          <div class="section-content">
            <div class="info-row">
              <span class="label">部门</span>
              <span class="value">{{ userInfo.department || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">角色</span>
              <span class="value">
                <span class="inline-badge">{{ userInfo.role || '-' }}</span>
              </span>
            </div>
          </div>
        </div>

        <div class="info-section">
          <div class="section-title">
            <span class="icon">📧</span>
            <span>联系方式</span>
          </div>
          <div class="section-content">
            <div class="info-row">
              <span class="label">电子邮箱</span>
              <span class="value">{{ userInfo.email || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="label">手机号码</span>
              <span class="value">{{ userInfo.telephone || '-' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部元数据 -->
      <div class="card-footer">
        <div class="meta-item">
          <span class="meta-label">用户ID</span>
          <span class="meta-value">#{{ userInfo.id }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">创建时间</span>
          <span class="meta-value">{{ formatDate(userInfo.createTime) }}</span>
        </div>
        <div class="meta-item">
          <span class="meta-label">数据状态</span>
          <span class="meta-value success">已验证</span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">🔍</div>
      <p>暂无用户信息</p>
    </div>
  </div>
</template>

<style scoped>
/* 主容器 */
.user-info-container {
  min-height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e6e9ff 100%);
}

/* 卡片主体 */
.user-info-card {
  width: 100%;
  max-width: 900px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  animation: slideIn 0.4s ease-out;
}

/* 头部样式 */
.card-header {
  background: linear-gradient(135deg, #4a6bff 0%, #6a82ff 100%);
  padding: 32px 40px;
  display: flex;
  align-items: center;
  gap: 24px;
  color: white;
  position: relative;
}

.user-avatar {
  width: 72px;
  height: 72px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

.header-text h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.user-role {
  margin-top: 6px;
}

.role-badge {
  background: rgba(255, 255, 255, 0.25);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  backdrop-filter: blur(5px);
}

.status-indicator {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.15);
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

/* 信息网格 */
.info-grid {
  padding: 32px 40px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 32px;
}

.info-section {
  background: #f8faff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8ecff;
  transition: all 0.3s ease;
}

.info-section:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(74, 107, 255, 0.12);
  border-color: #4a6bff;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 700;
  color: #4a6bff;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e0e7ff;
}

.icon {
  font-size: 16px;
}

.section-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #e8ecff;
}

.info-row:last-child {
  border-bottom: none;
}

.label {
  font-size: 13px;
  color: #7a8a9e;
  font-weight: 500;
}

.value {
  font-size: 15px;
  color: #2c3e50;
  font-weight: 600;
  text-align: right;
}

.inline-badge {
  background: #fff4e6;
  color: #f59e0b;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
}

/* 底部元数据 */
.card-footer {
  background: #f8faff;
  padding: 20px 40px;
  border-top: 1px solid #e8ecff;
  display: flex;
  justify-content: space-around;
  gap: 24px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  text-align: center;
  min-width: 140px;
}

.meta-label {
  font-size: 12px;
  color: #7a8a9e;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
}

.meta-value {
  font-size: 14px;
  color: #2c3e50;
  font-weight: 700;
}

.meta-value.success {
  color: #10b981;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  max-width: 400px;
  width: 100%;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #e0e7ff;
  border-top: 4px solid #4a6bff;
  border-radius: 50%;
  margin: 0 auto 20px;
  animation: spin 1s linear infinite;
}

.loading-state p {
  color: #5a6a85;
  font-size: 16px;
  font-weight: 500;
}

/* 错误状态 */
.error-state {
  text-align: center;
  padding: 60px 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  max-width: 450px;
  width: 100%;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-state h3 {
  color: #ef4444;
  margin-bottom: 12px;
  font-size: 20px;
}

.error-state p {
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.5;
}

.retry-btn {
  background: #4a6bff;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-btn:hover {
  background: #3a5bef;
  transform: translateY(-1px);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  max-width: 400px;
  width: 100%;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  color: #64748b;
  font-size: 16px;
  font-weight: 500;
}

/* 动画 */
@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 8px rgba(16, 185, 129, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .status-indicator {
    margin-left: 0;
    width: fit-content;
  }

  .info-grid {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .card-footer {
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .meta-item {
    width: 100%;
    border-bottom: 1px dashed #e8ecff;
    padding-bottom: 12px;
  }

  .meta-item:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

@media (max-width: 480px) {
  .user-info-container {
    padding: 12px;
  }

  .card-header {
    padding: 24px 20px;
  }

  .user-avatar {
    width: 60px;
    height: 60px;
    font-size: 24px;
  }

  .header-text h2 {
    font-size: 22px;
  }
}
</style>
