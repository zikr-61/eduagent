<template>
  <div class="study-stats">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>学习成果统计</span>
          <el-button type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8">
          <el-statistic title="总学习时长" :value="totalHours" suffix="小时">
            <template #prefix>
              <el-icon><Clock /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="8">
          <el-statistic title="本周学习时长" :value="weeklyHours" suffix="小时" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="本月学习时长" :value="monthlyHours" suffix="小时" />
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

        <el-tab-pane label="添加学习记录" name="add">
          <el-card shadow="hover">
            <el-form :model="studyForm" label-width="120px">
              <el-form-item label="学习时长(分钟)">
                <el-input-number v-model="studyForm.durationMinutes" :min="1" :max="720" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleAddRecord">添加记录</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { Clock } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getTotalDuration, getWeeklyReport, getMonthlyReport, addStudyRecord as addRecordApi } from '@/api';

const userId = ref(1);
const activeTab = ref('weekly');
const totalHours = ref(0);
const weeklyHours = ref(0);
const monthlyHours = ref(0);
const weeklyReport = ref(null);
const monthlyReport = ref(null);

const studyForm = ref({
  durationMinutes: 30
});

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
    const totalRes = await getTotalDuration(userId.value);
    totalHours.value = totalRes.data.totalHours || 0;

    const weeklyRes = await getWeeklyReport(userId.value);
    weeklyReport.value = weeklyRes.data;
    weeklyHours.value = weeklyRes.data.totalHours || 0;

    const monthlyRes = await getMonthlyReport(userId.value);
    monthlyReport.value = monthlyRes.data;
    monthlyHours.value = monthlyRes.data.totalHours || 0;
  } catch (error) {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败');
  }
};

const handleAddRecord = async () => {
  try {
    await addRecordApi(userId.value, studyForm.value.durationMinutes);
    ElMessage.success('添加成功');
    loadData();
    studyForm.value.durationMinutes = 30;
  } catch (error) {
    console.error('添加失败:', error);
    ElMessage.error('添加失败');
  }
};

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
</style>
