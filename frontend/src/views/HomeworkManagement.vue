<template>
  <div class="homework-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>作业管理</span>
          <el-button type="primary" @click="loadHomework">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 学生端 - 作业列表 -->
        <el-tab-pane v-if="userType === 'student'" label="我的作业" name="student-list">
          <el-radio-group v-model="filterStatus" @change="loadHomework">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="pending">待完成</el-radio-button>
            <el-radio-button value="completed">已完成</el-radio-button>
          </el-radio-group>

          <el-table :data="filteredHomework" style="width: 100%; margin-top: 20px;" stripe>
            <el-table-column prop="homework.title" label="标题" />
            <el-table-column prop="homework.description" label="描述" show-overflow-tooltip />
            <el-table-column prop="homework.difficulty" label="难度" width="100">
              <template #default="scope">
                <el-tag :type="getDifficultyType(scope.row.homework.difficulty)">
                  {{ getDifficultyText(scope.row.homework.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="homework.dueDate" label="截止日期" width="120">
              <template #default="scope">
                {{ scope.row.homework.dueDate || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'completed' ? 'success' : 'warning'">
                  {{ scope.row.status === 'completed' ? '已完成' : '待完成' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button
                  v-if="scope.row.status === 'pending'"
                  type="success"
                  size="small"
                  @click="submitHomework(scope.row)"
                >
                  完成
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  size="small"
                  @click="viewHomework(scope.row)"
                >
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 教师端 - 作业列表 -->
        <el-tab-pane v-if="userType === 'teacher'" label="作业管理" name="teacher-list">
          <el-table :data="homeworkList" style="width: 100%; margin-top: 20px;" stripe>
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column prop="difficulty" label="难度" width="100">
              <template #default="scope">
                <el-tag :type="getDifficultyType(scope.row.difficulty)">
                  {{ getDifficultyText(scope.row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dueDate" label="截止日期" width="120" />
            <el-table-column label="操作" width="320">
              <template #default="scope">
                <el-button type="primary" size="small" @click="openCompletion(scope.row)">完成情况</el-button>
                <el-button type="success" size="small" @click="assignHomeworkToAll(scope.row)">发布给全班</el-button>
                <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 教师端 - 添加作业 -->
        <el-tab-pane v-if="userType === 'teacher'" label="添加作业" name="add">
          <el-card shadow="hover">
            <el-form :model="homeworkForm" label-width="120px">
              <el-form-item label="标题" required>
                <el-input v-model="homeworkForm.title" placeholder="请输入作业标题" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="homeworkForm.description" type="textarea" :rows="3" placeholder="请输入作业描述" />
              </el-form-item>
              <el-form-item label="难度">
                <el-select v-model="homeworkForm.difficulty" placeholder="请选择难度">
                  <el-option label="简单" :value="1" />
                  <el-option label="中等" :value="2" />
                  <el-option label="困难" :value="3" />
                </el-select>
              </el-form-item>
              <el-form-item label="截止日期" required>
                <el-date-picker
                  v-model="homeworkForm.dueDate"
                  type="date"
                  placeholder="选择日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="addHomework">添加作业</el-button>
                <el-button @click="resetForm">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 完成作业：记录本次用时，并计入学习成果统计 -->
    <el-dialog v-model="submitDialogVisible" title="完成作业" width="50%">
      <el-form :model="submitForm" label-width="140px">
        <el-form-item label="作业标题">
          <el-input v-model="submitForm.title" disabled />
        </el-form-item>
        <el-form-item label="本次作业用时(分钟)" required>
          <el-input-number v-model="submitForm.completionTime" :min="1" :max="600" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="confirmSubmit">确认完成</el-button>
          <el-button @click="submitDialogVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 教师：查看各学生完成情况 -->
    <el-dialog v-model="completionDialogVisible" title="作业完成情况" width="720px">
      <p v-if="completionHomeworkTitle" style="margin-bottom: 12px">
        <strong>{{ completionHomeworkTitle }}</strong>
      </p>
      <el-table v-loading="completionLoading" :data="completionRows" stripe max-height="400">
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'completed' ? 'success' : 'warning'">
              {{ row.status === 'completed' ? '已完成' : '待完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completionTime" label="用时(分钟)" width="110">
          <template #default="{ row }">{{ row.completionTime != null ? row.completionTime : '-' }}</template>
        </el-table-column>
        <el-table-column prop="submitTime" label="完成时间" min-width="170">
          <template #default="{ row }">{{ formatSubmitTime(row.submitTime) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="completionDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createHomework,
  getTeacherHomeworkList,
  getStudentHomeworkList,
  getPendingStudentHomework,
  deleteHomework as deleteHomeworkApi,
  submitHomework as submitHomeworkApi,
  assignHomeworkToAll as assignHomeworkToAllApi,
  getHomeworkCompletionDetail
} from '@/api';

const userId = ref(1);
const userType = ref(localStorage.getItem('userType') || 'student');
const activeTab = ref(userType.value === 'teacher' ? 'teacher-list' : 'student-list');
const filterStatus = ref('all');
const homeworkList = ref([]);
const studentHomeworkList = ref([]);
const submitDialogVisible = ref(false);
const completionDialogVisible = ref(false);
const completionLoading = ref(false);
const completionRows = ref([]);
const completionHomeworkTitle = ref('');

const homeworkForm = ref({
  title: '',
  description: '',
  difficulty: 1,
  dueDate: ''
});

const submitForm = ref({
  title: '',
  homeworkId: null,
  completionTime: 30
});

const resolveUserId = () => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}');
    userId.value = u.id || 1;
  } catch {
    userId.value = 1;
  }
};

const filteredHomework = computed(() => {
  if (filterStatus.value === 'all') {
    return studentHomeworkList.value;
  } else if (filterStatus.value === 'pending') {
    return studentHomeworkList.value.filter(h => h.status === 'pending');
  } else {
    return studentHomeworkList.value.filter(h => h.status === 'completed');
  }
});

const loadHomework = async () => {
  try {
    if (userType.value === 'teacher') {
      const res = await getTeacherHomeworkList(userId.value);
      homeworkList.value = res.data.data || [];
    } else {
      if (filterStatus.value === 'pending') {
        const res = await getPendingStudentHomework(userId.value);
        studentHomeworkList.value = res.data.data || [];
      } else {
        const res = await getStudentHomeworkList(userId.value);
        studentHomeworkList.value = res.data.data || [];
      }
    }
  } catch (error) {
    console.error('加载作业失败:', error);
    ElMessage.error('加载作业失败');
  }
};

const addHomework = async () => {
  if (!homeworkForm.value.title || !homeworkForm.value.dueDate) {
    ElMessage.warning('请输入作业标题和截止日期');
    return;
  }

  try {
    await createHomework(
      userId.value,
      homeworkForm.value.title,
      homeworkForm.value.description,
      homeworkForm.value.difficulty,
      homeworkForm.value.dueDate
    );
    ElMessage.success('添加成功');
    loadHomework();
    resetForm();
    activeTab.value = 'teacher-list';
  } catch (error) {
    console.error('添加失败:', error);
    ElMessage.error('添加失败');
  }
};

const resetForm = () => {
  homeworkForm.value = {
    title: '',
    description: '',
    difficulty: 1,
    dueDate: ''
  };
};

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个作业吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteHomeworkApi(id);
    ElMessage.success('删除成功');
    loadHomework();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

const submitHomework = (homework) => {
  submitForm.value = {
    title: homework.homework.title,
    homeworkId: homework.homework.id,
    completionTime: 30
  };
  submitDialogVisible.value = true;
};

const confirmSubmit = async () => {
  try {
    await submitHomeworkApi(
      userId.value,
      submitForm.value.homeworkId,
      submitForm.value.completionTime
    );
    ElMessage.success('已记录完成，学习统计已增加本次用时');
    submitDialogVisible.value = false;
    loadHomework();
  } catch (error) {
    console.error('提交失败:', error);
    ElMessage.error('提交失败');
  }
};

const assignHomeworkToAll = async (homework) => {
  try {
    await ElMessageBox.confirm(
      '将向系统中所有学生发布该作业（已发布过的学生不会重复创建）。是否继续？',
      '发布给全班学生',
      { type: 'warning' }
    );
    const res = await assignHomeworkToAllApi(homework.id, userId.value);
    if (res.data.success === false) {
      ElMessage.error(res.data.message || '发布失败');
      return;
    }
    ElMessage.success('已发布给全班学生');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发布失败:', error);
      ElMessage.error('发布失败');
    }
  }
};

const openCompletion = async (homework) => {
  completionHomeworkTitle.value = homework.title;
  completionDialogVisible.value = true;
  completionLoading.value = true;
  completionRows.value = [];
  try {
    const res = await getHomeworkCompletionDetail(homework.id, userId.value);
    if (res.data.success === false) {
      ElMessage.warning(res.data.message || '无权限或作业不存在');
      completionRows.value = [];
      return;
    }
    completionRows.value = res.data.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error('加载完成情况失败');
  } finally {
    completionLoading.value = false;
  }
};

const formatSubmitTime = (t) => {
  if (!t) return '-';
  return new Date(t).toLocaleString('zh-CN');
};

const viewHomework = (row) => {
  const h = row.homework;
  if (!h) return;
  ElMessageBox.alert(
    `${h.description || '无描述'}\n\n截止：${h.dueDate || '-'}\n用时：${row.completionTime != null ? row.completionTime + ' 分钟' : '-'}`,
    h.title,
    { confirmButtonText: '关闭' }
  );
};

const getDifficultyType = (difficulty) => {
  const types = { 1: 'success', 2: 'warning', 3: 'danger' };
  return types[difficulty] || 'info';
};

const getDifficultyText = (difficulty) => {
  const texts = { 1: '简单', 2: '中等', 3: '困难' };
  return texts[difficulty] || '中等';
};

onMounted(() => {
  resolveUserId();
  loadHomework();
});
</script>

<style scoped>
.homework-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
