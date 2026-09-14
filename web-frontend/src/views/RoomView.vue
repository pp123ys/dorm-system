<template>
  <el-card class="page-card">
    <template #header>
      <div class="card-header">
        <span>房间与床位管理</span>
        <div>
          <el-select v-model="buildingId" placeholder="请选择楼栋" style="width: 180px" @change="loadRooms">
            <el-option v-for="b in buildings" :key="b.id" :label="`${b.name}（${b.sex}）`" :value="b.id" />
          </el-select>
          <el-button type="primary" style="margin-left: 12px" :disabled="!buildingId" @click="openAddRoom">
            新增房间
          </el-button>
        </div>
      </div>
    </template>

    <el-table :data="rooms" v-loading="loading" stripe empty-text="请选择楼栋查看房间">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roomNo" label="房间号" width="110" />
      <el-table-column prop="capacity" label="床位数" width="90" />
      <el-table-column prop="occupiedCount" label="已住" width="80" />
      <el-table-column prop="freeCount" label="空床" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '正常' ? 'success' : 'info'" effect="plain">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openBeds(row)">床位明细</el-button>
          <el-button link type="primary" @click="openCapacity(row)">改容量</el-button>
          <el-button link type="warning" @click="toggleRoomStatus(row)">
            {{ row.status === '正常' ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="doDeleteRoom(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增房间 -->
    <el-dialog v-model="addVisible" title="新增房间" width="420px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="90px">
        <el-form-item label="房间号" prop="roomNo">
          <el-input v-model="addForm.roomNo" placeholder="如 301" />
        </el-form-item>
        <el-form-item label="床位数" prop="capacity">
          <el-input-number v-model="addForm.capacity" :min="1" :max="20" />
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        :closable="false"
        title="保存后将自动生成 1~N 号床位，房间与床位在同一事务内写入"
      />
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoom">保存</el-button>
      </template>
    </el-dialog>

    <!-- 改容量 -->
    <el-dialog v-model="capVisible" title="修改床位容量" width="420px">
      <el-form label-width="90px">
        <el-form-item label="当前容量">
          <span>{{ currentRoom && currentRoom.capacity }}</span>
        </el-form-item>
        <el-form-item label="新容量">
          <el-input-number v-model="newCapacity" :min="1" :max="20" />
        </el-form-item>
      </el-form>
      <el-alert
        type="warning"
        :closable="false"
        title="扩容会自动补床位；缩容只删除空床位，若有在住学生会被拒绝"
      />
      <template #footer>
        <el-button @click="capVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCapacity">保存</el-button>
      </template>
    </el-dialog>

    <!-- 床位明细 -->
    <el-drawer v-model="bedVisible" :title="`床位明细 - ${currentRoom ? currentRoom.roomNo : ''} 房间`" size="520px">
      <el-table :data="beds" v-loading="bedLoading" stripe empty-text="该房间暂无床位">
        <el-table-column prop="bedNo" label="床位号" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '正常' ? 'success' : 'info'" effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入住情况" min-width="160">
          <template #default="{ row }">
            <span v-if="row.occupied">{{ row.studentNo }} {{ row.studentName }}</span>
            <el-tag v-else type="success" effect="plain" size="small">空闲</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button link type="warning" :disabled="row.occupied" @click="toggleBedStatus(row)">
              {{ row.status === '正常' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listBuildings } from '../api/building'
import {
  listRooms,
  listBeds,
  addRoom,
  updateRoomCapacity,
  updateRoomStatus,
  updateBedStatus,
  deleteRoom
} from '../api/room'

const buildings = ref([])
const buildingId = ref(null)
const rooms = ref([])
const beds = ref([])
const loading = ref(false)
const saving = ref(false)
const bedLoading = ref(false)

const addVisible = ref(false)
const capVisible = ref(false)
const bedVisible = ref(false)
const currentRoom = ref(null)
const newCapacity = ref(4)
const addFormRef = ref(null)

const addForm = reactive({ roomNo: '', capacity: 4 })
const addRules = {
  roomNo: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入床位数', trigger: 'blur' }]
}

const loadBuildings = async () => {
  const res = await listBuildings()
  buildings.value = res.data || []
  if (buildings.value.length && !buildingId.value) {
    buildingId.value = buildings.value[0].id
  }
}

const loadRooms = async () => {
  if (!buildingId.value) return
  loading.value = true
  try {
    const res = await listRooms(buildingId.value)
    rooms.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openAddRoom = () => {
  Object.assign(addForm, { roomNo: '', capacity: 4 })
  addVisible.value = true
}

const saveRoom = async () => {
  const valid = await addFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await addRoom({ buildingId: buildingId.value, roomNo: addForm.roomNo, capacity: addForm.capacity })
    ElMessage.success('新增成功，床位已自动生成')
    addVisible.value = false
    await loadRooms()
  } catch (e) {
    // 业务提示已弹出
  } finally {
    saving.value = false
  }
}

const openCapacity = (row) => {
  currentRoom.value = row
  newCapacity.value = row.capacity
  capVisible.value = true
}

const saveCapacity = async () => {
  saving.value = true
  try {
    await updateRoomCapacity(currentRoom.value.id, newCapacity.value)
    ElMessage.success('修改成功')
    capVisible.value = false
    await loadRooms()
  } catch (e) {
    // 例如"该房间还有在住学生，不能缩容到..."
  } finally {
    saving.value = false
  }
}

const toggleRoomStatus = async (row) => {
  const next = row.status === '正常' ? '停用' : '正常'
  try {
    await updateRoomStatus(row.id, next)
    ElMessage.success('操作成功')
    await loadRooms()
  } catch (e) {
    // 业务提示已弹出
  }
}

const openBeds = async (row) => {
  currentRoom.value = row
  bedVisible.value = true
  bedLoading.value = true
  try {
    const res = await listBeds(row.id)
    beds.value = res.data || []
  } finally {
    bedLoading.value = false
  }
}

const toggleBedStatus = async (row) => {
  const next = row.status === '正常' ? '停用' : '正常'
  try {
    await updateBedStatus(row.id, next)
    ElMessage.success('操作成功')
    const res = await listBeds(currentRoom.value.id)
    beds.value = res.data || []
  } catch (e) {
    // 业务提示已弹出
  }
}

const doDeleteRoom = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除房间「${row.roomNo}」吗？其床位会一并删除。`,
      '删除确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteRoom(row.id)
    ElMessage.success('删除成功')
    await loadRooms()
  } catch (e) {
    // 例如"该房间还有在住学生"
  }
}

onMounted(async () => {
  await loadBuildings()
  await loadRooms()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
