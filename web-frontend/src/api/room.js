import request from './request'

export const listRooms = (buildingId) => request.get(`/buildings/${buildingId}/rooms`)
export const listBeds = (roomId) => request.get(`/rooms/${roomId}/beds`)
export const addRoom = (data) => request.post('/rooms', data)
export const updateRoomCapacity = (roomId, capacity) =>
  request.put(`/rooms/${roomId}/capacity`, { capacity })
export const updateRoomStatus = (roomId, status) =>
  request.put(`/rooms/${roomId}/status`, { status })
export const updateBedStatus = (bedId, status) =>
  request.put(`/beds/${bedId}/status`, { status })
export const deleteRoom = (roomId) => request.delete(`/rooms/${roomId}`)
