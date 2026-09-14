import request from './request'

export const overview = () => request.get('/stats/overview')
export const roomRoster = (roomId) => request.get(`/stats/rooms/${roomId}/roster`)
export const freeBeds = (buildingId) =>
  request.get('/stats/free-beds', { params: buildingId ? { buildingId } : {} })
export const studentStay = (studentNo) => request.get(`/stats/students/${studentNo}`)
export const checkinHistory = (studentNo) =>
  request.get('/stats/checkins', { params: studentNo ? { studentNo } : {} })
