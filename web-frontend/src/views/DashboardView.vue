<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <el-card class="stat-card">
          <div class="stat-label">{{ c.label }}</div>
          <div class="stat-value" :style="{ color: c.color }">{{ c.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="page-card" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>楼栋占用概览</span>
          <el-button size="small" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table :data="rows" v-loading="loading" stripe empty-text="暂无楼栋数据">
        <el-table-column prop="name" label="楼栋名称" min-width="120" />
        <el-table-column prop="sex" label="类型" width="80" />
        <el-table-column prop="floors" label="楼层" width="80" />
        <el-table-column prop="roomCount" label="房间数" width="90" />
        <el-table-column prop="bedCount" label="总床位" width="90" />
        <el-table-column prop="occupiedCount" label="已住" width="80" />
        <el-table-column prop="freeCount" label="空床" width="80" />
        <el-table-column label="占用率" width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="row.bedCount ? Math.round((row.occupiedCount * 100) / row.bedCount) : 0"
              :stroke-width="14"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { overview } from '../api/stat'

const loading = ref(false)
const rows = ref([])

const sum = (key) => rows.value.reduce((acc, r) => acc + (r[key] || 0), 0)

const cards = computed(() => [
  { label: '楼栋数', value: rows.value.length, color: '#1f5f8b' },
  { label: '总床位', value: sum('bedCount'), color: '#2e8b57' },
  { label: '已住人数', value: sum('occupiedCount'), color: '#e6a23c' },
  { label: '空床位数', value: sum('freeCount'), color: '#909399' }
])

const load = async () => {
  loading.value = true
  try {
    const res = await overview()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stat-card {
  text-align: center;
}

.stat-label {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
