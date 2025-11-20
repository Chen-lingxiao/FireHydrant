import request from '@/utils/request'
// 登录参数类型
export interface LoginParams {
  name: string
  password: string
}
// 注册参数类型
export interface RegisterParams {
  name: string
  password: string
  // email: string
}

// 登录接口
export const login = (data: LoginParams) => {
  return request.post('/api/users/login', data)
}
// 注册接口
export const register = (data: RegisterParams) => {
  return request.post('/api/users/register', data)
}
// 退出登录接口
export const logout = () => {
  return request.post('/api/users/logout')
}
