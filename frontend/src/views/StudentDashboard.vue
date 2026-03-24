<template>
  <div class="student-dashboard">
    <el-card class="dashboard-card">
      <template #header>
        <div class="card-header">
          <span>学习中心</span>
          <el-button type="danger" text @click="logout">退出登录</el-button>
        </div>
      </template>

      <div class="user-info">
        <h3>欢迎，{{ userInfo?.name }}</h3>
        <p>年级：{{ userInfo?.grade }}</p>
      </div>

      <el-divider />

      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>待完成作业</span>
            </template>
            <div v-if="pendingHomework.length > 0">
              <el-table :data="pendingHomework" stripe>
                <el-table-column label="作业标题">
                  <template #default="{ row }">{{ row.homework?.title || '-' }}</template>
                </el-table-column>
                <el-table-column label="截止日期">
                  <template #default="{ row }">{{ row.homework?.dueDate || '-' }}</template>
                </el-table-column>
                <el-table-column label="操作">
                  <template #default="scope">
                    <el-button type="primary" size="small" @click="viewHomework(scope.row)">
                      查看
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else class="empty-state">
              暂无待完成作业
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>学习统计</span>
            </template>
            <div class="stats">
              <el-statistic title="总学习时长" :value="totalStudyTime" suffix="分钟" />
              <el-statistic title="本周学习" :value="weekStudyTime" suffix="分钟" />
              <el-statistic title="完成作业" :value="completedHomeworkCount" suffix="份" />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <el-card shadow="hover">
            <template #header>
              <span>学习任务</span>
            </template>
            <div class="recommended-tasks">
              <el-button type="success" size="large" @click="generateKnowledgeSummary">
                生成知识点总结
              </el-button>
              <el-button type="info" size="large" @click="practiceExercises">
                练习习题
              </el-button>
              <el-button type="warning" size="large" @click="reviewErrorQuestions">
                复习错题
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getPendingStudentHomework, getTotalDuration, getWeeklyReport, getStudentHomeworkList } from '@/api';

export default {
  name: 'StudentDashboard',
  setup() {
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo')));
    const pendingHomework = ref([]);
    const totalStudyTime = ref(0);
    const weekStudyTime = ref(0);
    const completedHomeworkCount = ref(0);

    const loadData = async () => {
      try {
        const userId = userInfo.value?.id;
        if (!userId) {
          ElMessage.warning('用户信息不存在');
          return;
        }

        // 获取待完成作业
        const pendingRes = await getPendingStudentHomework(userId);
        pendingHomework.value = pendingRes.data.data || [];

        // 获取总学习时长
        const totalRes = await getTotalDuration(userId);
        totalStudyTime.value = totalRes.data.totalMinutes || 0;

        // 获取本周学习时长
        const weeklyRes = await getWeeklyReport(userId);
        weekStudyTime.value = weeklyRes.data.totalMinutes || 0;

        // 获取已完成作业数量
        const allRes = await getStudentHomeworkList(userId);
        const allHomework = allRes.data.data || [];
        completedHomeworkCount.value = allHomework.filter(h => h.status === 'completed').length;
      } catch (error) {
        console.error('加载数据失败:', error);
        ElMessage.error('加载数据失败');
        // 加载失败时使用模拟数据
        pendingHomework.value = [
          { id: 1, title: '数学第三章习题', dueDate: '2024-01-20' },
          { id: 2, title: '英语单词默写', dueDate: '2024-01-18' }
        ];
        totalStudyTime.value = 1250;
        weekStudyTime.value = 120;
        completedHomeworkCount.value = 15;
      }
    };

    const viewHomework = () => {
      // 跳转到作业管理页面
      window.location.href = '/homework';
    };

    const generateKnowledgeSummary = () => {
      // 跳转到知识点总结页面
      window.location.href = '/knowledge-summary';
    };

    const practiceExercises = () => {
      // 跳转到习题练习页面
      window.location.href = '/knowledge-summary';
    };

    const reviewErrorQuestions = () => {
      window.location.href = '/wrong-questions';
    };

    const logout = () => {
      localStorage.removeItem('userInfo');
      localStorage.removeItem('userType');
      window.location.href = '/login';
    };

    onMounted(() => {
      loadData();
    });

    return {
      userInfo,
      pendingHomework,
      totalStudyTime,
      weekStudyTime,
      completedHomeworkCount,
      viewHomework,
      generateKnowledgeSummary,
      practiceExercises,
      reviewErrorQuestions,
      logout
    };
  }
};
</script>

<style scoped>
.student-dashboard {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  margin-bottom: 20px;
}

.stats {
  display: flex;
  justify-content: space-around;
}

.recommended-tasks {
  display: flex;
  gap: 20px;
  justify-content: center;
  padding: 20px 0;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}
</style>