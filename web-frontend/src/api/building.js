import request from './request'

export const listBuildings = () => request.get('/buildings')
export const getBuilding = (id) => request.get(`/buildings/${id}`)
export const addBuilding = (data) => request.post('/buildings', data)
export const updateBuilding = (id, data) => request.put(`/buildings/${id}`, data)
export const deleteBuilding = (id) => request.delete(`/buildings/${id}`)
