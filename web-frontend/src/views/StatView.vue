<template>
  <el-card class="page-card">
    <template #header>
      <span>查询统计</span>
    </template>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 楼栋占用概览 -->
      <el-tab-pane label="楼栋占用概览" name="overview">
        <el-table :data="overviewRows" v-loading="loading" stripe empty-text="暂无数据">
          <el-table-column prop="name" label="楼栋名称" min-width="130" />
          <el-table-column prop="sex" label="类型" width="80" />
          <el-table-column prop="roomCount" label="房间数" width="90" />
          <el-table-column prop="bedCount" label="总床位" width="90" />
          <el-table-column prop="occupiedCount" label="已住" width="80" />
          <el-table-column prop="freeCount" label="空床" width="80" />
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <!-- 房间住宿名单 -->
      <el-tab-pane label="房间住宿名单" name="roster">
        <div class="toolbar">
          <el-input v-model.number="rosterRoomId" placeholder="请输入房间ID" style="width: 200px" />
          <el-button type="primary" :loading="loading" @click="loadRoster">查询</el-button>
        </div>
        <el-table :data="rosterRows" v-loading="loading" stripe empty-text="该房间暂无在住学生">
          <el-table-column prop="no" label="学号" width="120" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="sex" label="性别" width="80" />
          <el-table-column prop="age" label="年龄" width="80" />
          <el-table-column prop="phone" label="电话" min-width="150" />
        </el-table>
      </el-tab-pane>

      <!-- 空床位清单 -->
      <el-tab-pane label="空床位清单" name="free">
        <div class="toolbar">
          <el-select v-model="freeBuildingId" placeholder="全部楼栋" clearable style="width: 200px" @change="loadFreeBeds">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
          <el-button @click="loadFreeBeds">刷新</el-button>
          <span class="count">共 {{ freeRows.length }} 个空床位</span>
        </div>
        <el-table :data="freeRows" v-loading="loading" stripe empty-text="暂无空床位">
          <el-table-column prop="id" label="床位ID" width="90" />
          <el-table-column prop="buildingName" label="楼栋" min-width="120" />
          <el-table-column prop="roomNo" label="房间号" width="110" />
          <el-table-column prop="bedNo" label="床位号" width="90" />
        </el-table>
      </el-tab-pane>

      <!-- 学生住宿信息 -->
      <el-tab-pane label="学生住宿信息" name="student">
        <div class="toolbar">
          <el-input v-model.number="stayNo" placeholder="请输入学号" style="width: 200px" />
          <el-button type="primary" :loading="loading" @click="loadStudentStay">查询</el-button>
        </div>
        <el-descriptions v-if="stayDetail" :column="1" border style="max-width: 520px">
          <el-descriptions-item label="学号">{{ stayDetail.no }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ stayDetail.name }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ stayDetail.sex }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ stayDetail.phone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="住宿位置">
            <el-tag :type="stayDetail.checkedIn ? 'success' : 'info'" effect="plain">
              {{ stayDetail.location }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- 入住退住流水 -->
      <el-tab-pane label="入住退住流水" name="history">
        <div class="toolbar">
          <el-input v-model.number="historyNo" placeholder="学号（留空=全部学生）" style="width: 240px" clearable />
          <el-button type="primary" :loading="loading" @click="loadHistory">查询</el-button>
          <span class="count">共 {{ historyRows.length }} 条</span>
        </div>
        <el-table :data="historyRows" v-loading="loading" stripe empty-text="暂无流水记录">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column prop="studentNo" label="学号" width="110" />
          <el-table-column prop="studentName" label="姓名" width="110" />
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-tag :type="row.action === '入住' ? 'success' : 'warning'" effect="plain">
                {{ row.action }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="location" label="位置" min-width="160" />
          <el-table-column prop="operator" label="操作人" width="110" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  overview,
  roomRoster,
  freeBeds,
  studentStay,
  checkinHistory
} from '../api/stat'
import { listBuildings } from '../api/building'

const tab = ref('overview')
const loading = ref(false)

const overviewRows = ref([])
const rosterRows = ref([])
const freeRows = ref([])
const historyRows = ref([])
const stayDetail = ref(null)
const buildings = ref([])

const rosterRoomId = ref('')
const freeBuildingId = ref(null)
const stayNo = ref('')
const historyNo = ref('')

const loadOverview = async () => {
  loading.value = true
  try {
    const res = await overview()
    overviewRows.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadRoster = async () => {
  if (!rosterRoomId.value) {
    ElMessage.warning('请输入房间ID')
    return
  }
  loading.value = true
  try {
    const res = await roomRoster(rosterRoomId.value)
    rosterRows.value = res.data || []
  } catch (e) {
    rosterRows.value = []
  } finally {
    loading.value = false
  }
}

const loadFreeBeds = async () => {
  loading.value = true
  try {
    const res = await freeBeds(freeBuildingId.value)
    freeRows.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadStudentStay = async () => {
  if (!stayNo.value) {
    ElMessage.warning('请输入学号')
    return
  }
  loading.value = true
  try {
    const res = await studentStay(stayNo.value)
    stayDetail.value = res.data
  } catch (e) {
    stayDetail.value = null
  } finally {
    loading.value = false
  }
}

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await checkinHistory(historyNo.value)
    historyRows.value = res.data || []
  } finally {
    loading.value = false
  }
}

const onTabChange = (name) => {
  if (name === 'overview') loadOverview()
  else if (name === 'free') loadFreeBeds()
  else if (name === 'history') loadHistory()
}

onMounted(async () => {
  await loadOverview()
  const res = await listBuildings()
  buildings.value = res.data || []
})
</script>

<style scoped>
.count {
  color: #909399;
  font-size: 13px;
}
</style>
