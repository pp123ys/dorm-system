import request from './request'

export const listStudents = () => request.get('/students')
export const addStudent = (data) => request.post('/students', data)
export const updateStudent = (no, data) => request.put(`/students/${no}`, data)
export const deleteStudent = (no) => request.delete(`/students/${no}`)
