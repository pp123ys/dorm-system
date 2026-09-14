<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="brand">
        <div class="logo">寓安</div>
        <div class="title">学生宿舍管理系统</div>
        <div class="subtitle">请使用管理员账号登录</div>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名" prop="loginName">
          <el-input v-model="form.loginName" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <div class="hint">默认账号：admin / 123456</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { TOKEN_KEY, USER_KEY } from '../api/request'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  loginName: 'admin',
  password: '123456'
})

const rules = {
  loginName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({ loginName: form.loginName, password: form.password })
    localStorage.setItem(TOKEN_KEY, res.data.token)
    localStorage.setItem(USER_KEY, res.data.loginName)
    ElMessage.success('登录成功')
    const target = route.query.redirect || '/dashboard'
    router.push(target)
  } catch (e) {
    // 错误提示已由 axios 响应拦截统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f5f8b 0%, #2e8b57 100%);
}

.login-card {
  width: 380px;
  padding: 8px 12px 4px;
}

.brand {
  text-align: center;
  margin-bottom: 18px;
}

.logo {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 6px;
  color: #1f5f8b;
}

.title {
  margin-top: 6px;
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.submit-btn {
  width: 100%;
}

.hint {
  margin-top: 14px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}
</style>
