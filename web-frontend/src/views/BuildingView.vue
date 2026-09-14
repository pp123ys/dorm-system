<template>
  <el-card class="page-card">
    <template #header>
      <div class="card-header">
        <span>楼栋管理</span>
        <el-button type="primary" @click="openAdd">新增楼栋</el-button>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" stripe empty-text="暂无楼栋数据">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="楼栋名称" min-width="140" />
      <el-table-column prop="sex" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.sex === '男' ? 'primary' : 'danger'" effect="plain">{{ row.sex }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="floors" label="楼层数" width="90" />
      <el-table-column prop="roomCount" label="房间数" width="90" />
      <el-table-column prop="bedCount" label="床位数" width="90" />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑楼栋' : '新增楼栋'" width="420px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="form.name" placeholder="如 3号楼" />
        </el-form-item>
        <el-form-item label="类型" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="楼层数" prop="floors">
          <el-input-number v-model="form.floors" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listBuildings, addBuilding, updateBuilding, deleteBuilding } from '../api/building'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref(null)

const form = reactive({ name: '', sex: '男', floors: 6, remark: '' })

const rules = {
  name: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }],
  sex: [{ required: true, message: '请选择类型', trigger: 'change' }],
  floors: [{ required: true, message: '请输入楼层数', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await listBuildings()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  editing.value = null
  Object.assign(form, { name: '', sex: '男', floors: 6, remark: '' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  editing.value = row
  Object.assign(form, { name: row.name, sex: row.sex, floors: row.floors, remark: row.remark || '' })
  dialogVisible.value = true
}

const save = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await updateBuilding(editing.value.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await addBuilding({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    // 后端业务提示已由拦截器弹出
  } finally {
    saving.value = false
  }
}

const doDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除楼栋「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteBuilding(row.id)
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    // 例如"该楼栋下还有N个房间"，由拦截器提示
  }
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
