<template>
  <div class="study-stats">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>学习成果统计</span>
          <div>
            <el-button type="primary" @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <!-- 教师视图 -->
      <div v-if="userType === 'teacher'">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-card shadow="hover">
              <template #header>
                <span>学生学习情况概览</span>
              </template>
              <div v-if="studentsStats.length > 0">
                <el-table :data="studentsStats" style="width: 100%;" stripe>
                  <el-table-column prop="name" label="学生姓名" />
                  <el-table-column prop="grade" label="年级" />
                  <el-table-column prop="totalHours" label="总学习时长(小时)" />
                  <el-table-column prop="weeklyHours" label="本周学习时长(小时)" />
                  <el-table-column prop="monthlyHours" label="本月学习时长(小时)" />
                </el-table>
              </div>
              <div v-else>加载中...</div>
            </el-card>
          </el-col>
        </el-row>

        <el-divider />

        <el-card shadow="hover">
          <template #header>
            <span>学生学习时长对比（总时长）</span>
          </template>
          <div v-if="chartData.length === 0" style="text-align:center;color:#c0c4cc;padding:40px;">暂无学生数据</div>
          <div v-else class="bar-chart">
            <div v-for="item in chartData" :key="item.name" class="bar-row">
              <span class="bar-label">{{ item.name }}</span>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: item.pct + '%' }"></div>
              </div>
              <span class="bar-value">{{ item.totalHours }}h</span>
            </div>
            <div class="bar-legend">
              <span>本周：</span>
              <span v-for="item in chartData" :key="'w'+item.name" class="legend-item">
                {{ item.name }} {{ item.weeklyHours }}h
              </span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 学生视图 -->
      <div v-else-if="userType === 'student'">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="总学习时长（含本次在线）" :value="totalHoursWithSession" :precision="1" suffix="小时">
              <template #prefix>
                <el-icon><Clock /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="本周学习时长" :value="weeklyHours" :precision="1" suffix="小时" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="本月学习时长" :value="monthlyHours" :precision="1" suffix="小时" />
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="8">
            <el-statistic title="当前在线时长" :value="currentSessionTime" suffix="分钟" />
          </el-col>
        </el-row>

        <el-divider />

        <el-tabs v-model="activeTab">
          <el-tab-pane label="周报" name="weekly">
            <el-card shadow="hover">
              <div v-if="weeklyReport">
                <h4>周报 ({{ weeklyReport.startDate }} ~ {{ weeklyReport.endDate }})</h4>
                <p>本周学习总时长: <strong>{{ weeklyReport.totalHours }} 小时</strong></p>
                <el-table :data="weeklyTableData" style="width: 100%; margin-top: 20px;" stripe>
                  <el-table-column prop="date" label="日期" width="150" />
                  <el-table-column prop="duration" label="学习时长(分钟)" />
                </el-table>
                <el-button type="success" style="margin-top: 20px;" @click="exportWeeklyReport">导出周报</el-button>
              </div>
              <div v-else>加载中...</div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="月报" name="monthly">
            <el-card shadow="hover">
              <div v-if="monthlyReport">
                <h4>月报 ({{ monthlyReport.startDate }} ~ {{ monthlyReport.endDate }})</h4>
                <p>本月学习总时长: <strong>{{ monthlyReport.totalHours }} 小时</strong></p>
                <el-table :data="monthlyTableData" style="width: 100%; margin-top: 20px;" stripe>
                  <el-table-column prop="week" label="周次" width="150" />
                  <el-table-column prop="duration" label="学习时长(分钟)" />
                </el-table>
                <el-button type="success" style="margin-top: 20px;" @click="exportMonthlyReport">导出月报</el-button>
              </div>
              <div v-else>加载中...</div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { Clock } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getTotalDuration, getWeeklyReport, getMonthlyReport, getAllStudentsStats } from '@/api';
import { sessionMinutes } from '@/utils/sessionTimer';

const userInfo = ref(JSON.parse(localStorage.getItem('userInfo')));
const userType = ref(localStorage.getItem('userType'));
const userId = ref(userInfo.value?.id || 1);
const activeTab = ref('weekly');
const totalHours = ref(0);
const weeklyHours = ref(0);
const monthlyHours = ref(0);
const weeklyReport = ref(null);
const monthlyReport = ref(null);
const studentsStats = ref([]);

// 当前在线时长：直接引用 App.vue 驱动的全局共享 ref，保证完全同步
const currentSessionTime = computed(() => sessionMinutes.value);

// 总时长 = DB 记录时长 + 本次在线时长（小时）
const totalHoursWithSession = computed(() =>
  Math.round((totalHours.value + sessionMinutes.value / 60) * 10) / 10
);

const weeklyTableData = computed(() => {
  if (!weeklyReport.value || !weeklyReport.value.dailyStudy) return [];
  return Object.entries(weeklyReport.value.dailyStudy).map(([date, duration]) => ({
    date,
    duration
  }));
});

const monthlyTableData = computed(() => {
  if (!monthlyReport.value || !monthlyReport.value.weeklyStudy) return [];
  return Object.entries(monthlyReport.value.weeklyStudy).map(([week, duration]) => ({
    week: week === 'week1' ? '第一周' : week === 'week2' ? '第二周' : week === 'week3' ? '第三周' : '第四周',
    duration
  }));
});

const loadData = async () => {
  try {
    if (userType.value === 'teacher') {
      // 教师加载所有学生的学习数据
      const res = await getAllStudentsStats();
      studentsStats.value = res.data.studentsStats || [];
    } else if (userType.value === 'student') {
      // 学生加载自己的学习数据
      const totalRes = await getTotalDuration(userId.value);
      totalHours.value = totalRes.data.totalHours || 0;

      const weeklyRes = await getWeeklyReport(userId.value);
      weeklyReport.value = weeklyRes.data;
      weeklyHours.value = weeklyRes.data.totalHours || 0;

      const monthlyRes = await getMonthlyReport(userId.value);
      monthlyReport.value = monthlyRes.data;
      monthlyHours.value = monthlyRes.data.totalHours || 0;
    }
  } catch (error) {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败');
  }
};

// 图表数据：从 studentsStats 派生，用于 CSS 柱状图
const chartData = computed(() => {
  const data = studentsStats.value;
  if (!data.length) return [];
  const max = Math.max(...data.map(s => Number(s.totalHours) || 0), 1);
  return data.map(s => ({
    name: s.name || s.username || '未知',
    totalHours: Number(s.totalHours) || 0,
    weeklyHours: Number(s.weeklyHours) || 0,
    pct: Math.round(((Number(s.totalHours) || 0) / max) * 100),
  }));
});

const exportWeeklyReport = () => {
  if (!weeklyReport.value) return;
  const content = `学习周报\n${weeklyReport.value.startDate} ~ ${weeklyReport.value.endDate}\n总学习时长: ${weeklyReport.value.totalHours}小时\n\n每日学习情况:\n`;
  const dailyContent = weeklyTableData.value.map(item => `${item.date}: ${item.duration}分钟`).join('\n');
  downloadFile('周报.txt', content + dailyContent);
};

const exportMonthlyReport = () => {
  if (!monthlyReport.value) return;
  const content = `学习月报\n${monthlyReport.value.startDate} ~ ${monthlyReport.value.endDate}\n总学习时长: ${monthlyReport.value.totalHours}小时\n\n每周学习情况:\n`;
  const weeklyContent = monthlyTableData.value.map(item => `${item.week}: ${item.duration}分钟`).join('\n');
  downloadFile('月报.txt', content + weeklyContent);
};

const downloadFile = (filename, content) => {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.study-stats {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-row {
  margin-bottom: 20px;
}

/* ── 柱状图 ── */
.bar-chart { padding: 12px 0; }
.bar-row {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
  gap: 10px;
}
.bar-label { width: 72px; font-size: 13px; color: #606266; text-align: right; flex-shrink: 0; }
.bar-track {
  flex: 1;
  height: 22px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 4px;
  transition: width 0.6s ease;
  min-width: 4px;
}
.bar-value { width: 40px; font-size: 13px; color: #303133; font-weight: 600; flex-shrink: 0; }
.bar-legend { margin-top: 16px; font-size: 12px; color: #909399; display: flex; flex-wrap: wrap; gap: 12px; }
.legend-item { background: #f5f7fa; padding: 2px 8px; border-radius: 10px; }
</style>
