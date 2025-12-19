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
// 分页查询参数类型
export interface PageQueryParams {
  pageNum: number
  pageSize: number
}
// 用户信息类型
export interface User {
  id: number
  name: string
  sex: string
  birthDate: string
  department: string
  telephone: string
  email: string
  role: string
  createTime: string
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
// 查询用户接口
export const getUserByid = (id: number) => {
  return request.get(`/api/users/${id}`)
}
// 分页查询接口
export const getUserPagingQuery = (params: PageQueryParams) => {
  return request.get('/api/users/page', { params })
}
// 删除用户接口
export const deleteUser = (id: number) => {
  return request.delete(`/api/users/del/${id}`)
}
// 修改用户接口
export const updateUser = (data: User) => {
  return request.put(`/api/users/update`, data)
}
