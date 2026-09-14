<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo">寓安 · 宿舍管理</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon><span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/buildings">
          <el-icon><OfficeBuilding /></el-icon><span>楼栋管理</span>
        </el-menu-item>
        <el-menu-item index="/rooms">
          <el-icon><Grid /></el-icon><span>房间床位管理</span>
        </el-menu-item>
        <el-menu-item index="/students">
          <el-icon><User /></el-icon><span>学生管理</span>
        </el-menu-item>
        <el-menu-item index="/stays">
          <el-icon><Key /></el-icon><span>入住退住办理</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><TrendCharts /></el-icon><span>查询统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="title">{{ currentTitle }}</div>
        <div class="spacer"></div>
        <span class="user">当前用户：{{ loginName }}</span>
        <el-button link type="primary" @click="doLogout">退出登录</el-button>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataLine, OfficeBuilding, Grid, User, Key, TrendCharts } from '@element-plus/icons-vue'
import { logout } from '../api/auth'
import { TOKEN_KEY, USER_KEY } from '../api/request'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')
const loginName = computed(() => localStorage.getItem(USER_KEY) || '')

const doLogout = async () => {
  try {
    await logout()
  } catch (e) {
    // 即使后端调用失败也要清掉本地登录态
  }
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #1f2d3d;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  letter-spacing: 1px;
  background: #17232f;
}

.menu {
  border-right: none;
  background: #1f2d3d;
}

.menu :deep(.el-menu-item) {
  color: #c0c4cc;
}

.menu :deep(.el-menu-item:hover) {
  background: #263445;
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: #2e8b57;
  color: #fff;
}

.header {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header .title {
  font-size: 16px;
  font-weight: 600;
}

.header .spacer {
  flex: 1;
}

.header .user {
  margin-right: 16px;
  color: #606266;
  font-size: 14px;
}

.main {
  background: #f5f7fa;
  padding: 18px;
}
</style>
