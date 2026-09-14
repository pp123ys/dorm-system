<template>
  <el-card class="page-card">
    <template #header>
      <div class="card-header">
        <span>学生管理</span>
        <div class="toolbar" style="margin: 0">
          <el-input
            v-model="keyword"
            placeholder="按学号或姓名筛选"
            clearable
            style="width: 220px"
          />
          <el-button type="primary" @click="openAdd">新增学生</el-button>
        </div>
      </div>
    </template>

    <el-table :data="filtered" v-loading="loading" stripe empty-text="暂无学生数据">
      <el-table-column prop="no" label="学号" width="120" />
      <el-table-column prop="name" label="姓名" min-width="110" />
      <el-table-column prop="sex" label="性别" width="80">
        <template #default="{ row }">
          <el-tag :type="row.sex === '男' ? 'primary' : 'danger'" effect="plain">{{ row.sex }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="80" />
      <el-table-column prop="phone" label="电话" min-width="140" />
      <el-table-column label="住宿位置" min-width="170">
        <template #default="{ row }">
          <el-tag v-if="row.checkedIn" type="success" effect="plain">{{ row.location }}</el-tag>
          <span v-else style="color: #909399">未入住</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑学生' : '新增学生'" width="440px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="学号" prop="no">
          <el-input v-model.number="form.no" :disabled="!!editing" placeholder="学号唯一，不可修改" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="10" :max="100" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="可空" maxlength="20" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listStudents, addStudent, updateStudent, deleteStudent } from '../api/student'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const keyword = ref('')
const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref(null)

const form = reactive({ no: '', name: '', sex: '男', age: 18, phone: '' })

const rules = {
  no: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  sex: [{ required: true, message: '请选择性别', trigger: 'change' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }]
}

const filtered = computed(() => {
  const k = keyword.value.trim()
  if (!k) return rows.value
  return rows.value.filter(
    (r) => String(r.no).includes(k) || (r.name && r.name.includes(k))
  )
})

const load = async () => {
  loading.value = true
  try {
    const res = await listStudents()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  editing.value = null
  Object.assign(form, { no: '', name: '', sex: '男', age: 18, phone: '' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  editing.value = row
  Object.assign(form, {
    no: row.no,
    name: row.name,
    sex: row.sex,
    age: row.age,
    phone: row.phone || ''
  })
  dialogVisible.value = true
}

const save = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editing.value) {
      await updateStudent(editing.value.no, {
        name: form.name,
        sex: form.sex,
        age: form.age,
        phone: form.phone
      })
      ElMessage.success('修改成功')
    } else {
      await addStudent({
        no: form.no,
        name: form.name,
        sex: form.sex,
        age: form.age,
        phone: form.phone
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    // 例如"该学号已存在"，由拦截器提示
  } finally {
    saving.value = false
  }
}

const doDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除学生「${row.name}」吗？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteStudent(row.no)
    ElMessage.success('删除成功')
    await load()
  } catch (e) {
    // 例如"该学生正在1号楼101房1床, 请先办理退住"
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
