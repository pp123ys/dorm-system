<template>
  <el-card class="page-card">
    <template #header>
      <div class="card-header">
        <span>入住退住办理</span>
      </div>
    </template>

    <el-tabs v-model="tab">
      <!-- ============ 办理入住 ============ -->
      <el-tab-pane label="办理入住" name="checkin">
        <el-steps :active="step" simple style="margin-bottom: 18px">
          <el-step title="输入学号" />
          <el-step title="选择楼栋" />
          <el-step title="选择床位" />
          <el-step title="确认办理" />
        </el-steps>

        <el-form label-width="110px" style="max-width: 560px">
          <el-form-item label="学号">
            <el-input v-model.number="ciForm.studentNo" placeholder="请输入学号" style="width: 220px" />
            <el-button type="primary" style="margin-left: 12px" :loading="querying" @click="queryStudent">
              查询学生
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="student"
          :closable="false"
          type="success"
          style="max-width: 560px; margin-bottom: 16px"
          :title="`${student.no} ${student.name} ${student.sex}　当前：${student.location}`"
        />

        <div v-if="student && !student.checkedIn">
          <el-form label-width="110px" style="max-width: 560px">
            <el-form-item label="选择楼栋">
              <el-select v-model="ciForm.buildingId" placeholder="请选择有空床的楼栋" style="width: 300px" @change="loadFreeBeds">
                <el-option
                  v-for="b in freeBuildings"
                  :key="b.id"
                  :label="`${b.name}（${b.sex}）空床 ${b.freeCount}`"
                  :value="b.id"
                />
              </el-select>
            </el-form-item>
          </el-form>

          <el-table
            :data="freeBeds"
            v-loading="bedLoading"
            stripe
            style="max-width: 720px"
            empty-text="请选择楼栋"
            :row-class-name="bedRowClass"
          >
            <el-table-column width="90">
              <template #default="{ row }">
                <el-button
                  link
                  :type="ciForm.bedId === row.id ? 'success' : 'primary'"
                  @click="selectBed(row)"
                >
                  {{ ciForm.bedId === row.id ? '已选' : '选择' }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="床位ID" width="90" />
            <el-table-column prop="buildingName" label="楼栋" min-width="120" />
            <el-table-column prop="roomNo" label="房间号" width="110" />
            <el-table-column prop="bedNo" label="床位号" width="90" />
          </el-table>

          <el-button
            type="primary"
            style="margin-top: 16px"
            :disabled="!ciForm.bedId"
            :loading="submitting"
            @click="doCheckIn"
          >
            确认办理入住
          </el-button>
        </div>
      </el-tab-pane>

      <!-- ============ 办理退住 ============ -->
      <el-tab-pane label="办理退住" name="checkout">
        <el-form label-width="110px" style="max-width: 560px">
          <el-form-item label="学号">
            <el-input v-model.number="coForm.studentNo" placeholder="请输入学号" style="width: 220px" />
            <el-button type="primary" style="margin-left: 12px" :loading="querying" @click="queryStay">
              查询住宿信息
            </el-button>
          </el-form-item>
        </el-form>

        <el-descriptions v-if="stayStudent" :column="1" border style="max-width: 560px; margin-bottom: 16px">
          <el-descriptions-item label="学号">{{ stayStudent.no }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ stayStudent.name }}</el-descriptions-item>
          <el-descriptions-item label="当前住宿位置">
            <el-tag :type="stayStudent.checkedIn ? 'success' : 'info'" effect="plain">
              {{ stayStudent.location }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-button
          v-if="stayStudent && stayStudent.checkedIn"
          type="danger"
          :loading="submitting"
          @click="doCheckOut"
        >
          确认办理退住
        </el-button>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  freeBuildings as apiFreeBuildings,
  freeBeds as apiFreeBeds,
  checkInTarget,
  checkIn,
  checkOut,
  stayInfo
} from '../api/stay'

const tab = ref('checkin')
const querying = ref(false)
const submitting = ref(false)
const bedLoading = ref(false)

const student = ref(null)
const stayStudent = ref(null)
const freeBuildings = ref([])
const freeBeds = ref([])

const ciForm = reactive({ studentNo: '', buildingId: null, bedId: null })
const coForm = reactive({ studentNo: '' })

const step = ref(0)

const loadFreeBuildings = async () => {
  const res = await apiFreeBuildings()
  freeBuildings.value = res.data || []
}

const queryStudent = async () => {
  if (!ciForm.studentNo) {
    ElMessage.warning('请输入学号')
    return
  }
  querying.value = true
  try {
    const res = await checkInTarget(ciForm.studentNo)
    student.value = res.data
    step.value = 1
    ciForm.buildingId = null
    ciForm.bedId = null
    freeBeds.value = []
    await loadFreeBuildings()
  } catch (e) {
    // 学生不存在 / 已入住，由拦截器提示
    student.value = null
    step.value = 0
  } finally {
    querying.value = false
  }
}

const loadFreeBeds = async () => {
  if (!ciForm.buildingId) return
  bedLoading.value = true
  try {
    const res = await apiFreeBeds(ciForm.buildingId)
    freeBeds.value = res.data || []
    ciForm.bedId = null
    step.value = 2
  } finally {
    bedLoading.value = false
  }
}

// 选中床位（同时推进步骤条）
const selectBed = (row) => {
  ciForm.bedId = row.id
  step.value = 3
}

// 选中行高亮，配合"选择"按钮给出反馈
const bedRowClass = ({ row }) => (ciForm.bedId === row.id ? 'bed-selected' : '')

const doCheckIn = async () => {
  submitting.value = true
  try {
    await checkIn(ciForm.studentNo, ciForm.bedId)
    ElMessage.success('办理入住成功')
    const res = await stayInfo(ciForm.studentNo)
    student.value = res.data
    step.value = 4
    freeBeds.value = []
    ciForm.buildingId = null
    ciForm.bedId = null
    await loadFreeBuildings()
  } catch (e) {
    // 床位被占/性别不符等，由拦截器提示
  } finally {
    submitting.value = false
  }
}

const queryStay = async () => {
  if (!coForm.studentNo) {
    ElMessage.warning('请输入学号')
    return
  }
  querying.value = true
  try {
    const res = await stayInfo(coForm.studentNo)
    stayStudent.value = res.data
  } catch (e) {
    stayStudent.value = null
  } finally {
    querying.value = false
  }
}

const doCheckOut = async () => {
  try {
    await ElMessageBox.confirm(
      `确定为学生「${stayStudent.value.name}」办理退住吗？`,
      '退住确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    await checkOut(coForm.studentNo)
    ElMessage.success('办理退住成功')
    await queryStay()
  } catch (e) {
    // 业务提示已弹出
  } finally {
    submitting.value = false
  }
}

onMounted(loadFreeBuildings)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

:deep(.bed-selected) {
  background: #f0f9eb !important;
}
</style>
