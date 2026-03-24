<template>
  <div class="teacher-dashboard">
    <el-card class="dashboard-card">
      <template #header>
        <div class="card-header">
          <span>教师管理</span>
          <el-button type="danger" text @click="logout">退出登录</el-button>
        </div>
      </template>

      <div class="user-info">
        <h3>欢迎，{{ userInfo?.name }}</h3>
        <p>学科：{{ userInfo?.subject }} | 学校：{{ userInfo?.school }}</p>
      </div>

      <el-divider />

      <el-row :gutter="20">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>学生管理</span>
            </template>
            <div v-if="students.length > 0">
              <el-table :data="students" stripe>
                <el-table-column prop="id" label="学生ID" width="80" />
                <el-table-column prop="name" label="姓名" />
                <el-table-column prop="grade" label="年级" />
                <el-table-column label="操作">
                  <template #default="scope">
                    <el-button type="primary" size="small" @click="viewStudent(scope.row)">
                      查看
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else class="empty-state">
              暂无学生
            </div>
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span>作业管理</span>
            </template>
            <div class="homework-stats">
              <el-statistic title="已布置作业" :value="homeworkCount" suffix="份" />
              <el-statistic title="待批改" :value="pendingCorrection" suffix="份" />
              <el-statistic title="完成率" :value="completionRate" suffix="%" />
            </div>
            <el-button type="success" style="margin-top: 20px;" @click="createHomework">
              布置作业
            </el-button>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <el-card shadow="hover">
            <template #header>
              <span>学生学习情况</span>
            </template>
            <div class="class-stats">
              <el-table :data="studentStats" stripe>
                <el-table-column prop="name" label="学生" />
                <el-table-column prop="totalStudyTime" label="总学习时长(分钟)" />
                <el-table-column prop="weeklyStudyTime" label="本周学习时长(分钟)" />
                <el-table-column prop="completedHomework" label="已完成作业(份)" />
              </el-table>
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
import { getTeacherHomeworkList } from '@/api';

export default {
  name: 'TeacherDashboard',
  setup() {
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo')));
    const students = ref([]);
    const homeworkCount = ref(0);
    const pendingCorrection = ref(0);
    const completionRate = ref(0);
    const studentStats = ref([]);

    const loadData = async () => {
      try {
        const teacherId = userInfo.value?.id;
        if (!teacherId) {
          ElMessage.warning('用户信息不存在');
          return;
        }

        // 获取教师的作业列表
        const homeworkRes = await getTeacherHomeworkList(teacherId);
        const homeworks = homeworkRes.data.data || [];
        homeworkCount.value = homeworks.length;

        // 模拟获取学生列表（实际应该从API获取）
        students.value = [
          { id: 1, name: '张三', grade: '高一' },
          { id: 2, name: '李四', grade: '高二' },
          { id: 3, name: '王五', grade: '高三' }
        ];

        // 模拟获取学生学习情况（实际应该从API获取）
        studentStats.value = [
          { name: '张三', totalStudyTime: 1250, weeklyStudyTime: 120, completedHomework: 15 },
          { name: '李四', totalStudyTime: 1100, weeklyStudyTime: 100, completedHomework: 12 },
          { name: '王五', totalStudyTime: 950, weeklyStudyTime: 80, completedHomework: 10 }
        ];

        // 计算待批改作业数量和完成率（实际应该从API获取）
        pendingCorrection.value = 3;
        completionRate.value = 85;
      } catch (error) {
        console.error('加载数据失败:', error);
        ElMessage.error('加载数据失败');
        // 加载失败时使用模拟数据
        students.value = [
          { id: 1, name: '张三', grade: '高一' },
          { id: 2, name: '李四', grade: '高二' },
          { id: 3, name: '王五', grade: '高三' }
        ];
        homeworkCount.value = 12;
        pendingCorrection.value = 3;
        completionRate.value = 85;
        studentStats.value = [
          { name: '张三', totalStudyTime: 1250, weeklyStudyTime: 120, completedHomework: 15 },
          { name: '李四', totalStudyTime: 1100, weeklyStudyTime: 100, completedHomework: 12 },
          { name: '王五', totalStudyTime: 950, weeklyStudyTime: 80, completedHomework: 10 }
        ];
      }
    };

    const viewStudent = (student) => {
      // 查看学生详情
      console.log('查看学生:', student);
    };

    const createHomework = () => {
      // 跳转到作业管理页面
      window.location.href = '/homework';
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
      students,
      homeworkCount,
      pendingCorrection,
      completionRate,
      studentStats,
      viewStudent,
      createHomework,
      logout
    };
  }
};
</script>

<style scoped>
.teacher-dashboard {
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

.homework-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}
</style>