import request from './request'

export const freeBuildings = () => request.get('/stays/free-buildings')
export const freeBeds = (buildingId) =>
  request.get('/stays/free-beds', { params: buildingId ? { buildingId } : {} })
export const stayInfo = (studentNo) => request.get(`/stays/${studentNo}`)
export const checkInTarget = (studentNo) => request.get(`/stays/${studentNo}/check-in-target`)
export const checkIn = (studentNo, bedId) => request.post('/stays/check-in', { studentNo, bedId })
export const checkOut = (studentNo) => request.post('/stays/check-out', { studentNo })
