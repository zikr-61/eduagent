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
        <el-tab-pane label="作业列表" name="list">
          <el-radio-group v-model="filterStatus" @change="loadHomework">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="pending">待完成</el-radio-button>
            <el-radio-button label="completed">已完成</el-radio-button>
          </el-radio-group>

          <el-table :data="filteredHomework" style="width: 100%; margin-top: 20px;" stripe>
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column prop="priority" label="优先级" width="100">
              <template #default="scope">
                <el-tag :type="getPriorityType(scope.row.priority)">
                  {{ getPriorityText(scope.row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dueDate" label="截止日期" width="120">
              <template #default="scope">
                {{ scope.row.dueDate || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="isCompleted" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.isCompleted ? 'success' : 'warning'">
                  {{ scope.row.isCompleted ? '已完成' : '待完成' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250">
              <template #default="scope">
                <el-button
                  v-if="!scope.row.isCompleted"
                  type="success"
                  size="small"
                  @click="handleComplete(scope.row.id)"
                >
                  完成
                </el-button>
                <el-button
                  v-else
                  type="info"
                  size="small"
                  @click="handleIncomplete(scope.row.id)"
                >
                  撤销
                </el-button>
                <el-button type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
                <el-button type="danger" size="small" @click="handleDelete(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="添加作业" name="add">
          <el-card shadow="hover">
            <el-form :model="homeworkForm" label-width="120px">
              <el-form-item label="标题" required>
                <el-input v-model="homeworkForm.title" placeholder="请输入作业标题" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="homeworkForm.description" type="textarea" :rows="3" placeholder="请输入作业描述" />
              </el-form-item>
              <el-form-item label="优先级">
                <el-select v-model="homeworkForm.priority" placeholder="请选择优先级">
                  <el-option label="低" :value="1" />
                  <el-option label="中" :value="2" />
                  <el-option label="高" :value="3" />
                </el-select>
              </el-form-item>
              <el-form-item label="截止日期">
                <el-date-picker
                  v-model="homeworkForm.dueDate"
                  type="date"
                  placeholder="选择日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item label="提醒时间">
                <el-date-picker
                  v-model="homeworkForm.reminderTime"
                  type="datetime"
                  placeholder="选择提醒时间"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DDTHH:mm:ss"
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

    <el-dialog v-model="editDialogVisible" title="编辑作业" width="50%">
      <el-form :model="editForm" label-width="120px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="editForm.priority">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="editForm.dueDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveEdit">保存</el-button>
          <el-button @click="editDialogVisible = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getHomeworkList,
  createHomework,
  updateHomework,
  deleteHomework as deleteHomeworkApi,
  completeHomework,
  incompleteHomework
} from '@/api';

const userId = ref(1);
const activeTab = ref('list');
const filterStatus = ref('all');
const homeworkList = ref([]);
const editDialogVisible = ref(false);
const editForm = ref({
  id: null,
  title: '',
  description: '',
  priority: 1,
  dueDate: ''
});

const homeworkForm = ref({
  title: '',
  description: '',
  priority: 1,
  dueDate: '',
  reminderTime: ''
});

const filteredHomework = computed(() => {
  if (filterStatus.value === 'all') {
    return homeworkList.value;
  } else if (filterStatus.value === 'pending') {
    return homeworkList.value.filter(h => !h.isCompleted);
  } else {
    return homeworkList.value.filter(h => h.isCompleted);
  }
});

const loadHomework = async () => {
  try {
    const res = await getHomeworkList(userId.value);
    homeworkList.value = res.data.data || [];
  } catch (error) {
    console.error('加载作业失败:', error);
    ElMessage.error('加载作业失败');
  }
};

const addHomework = async () => {
  if (!homeworkForm.value.title) {
    ElMessage.warning('请输入作业标题');
    return;
  }

  try {
    await createHomework(
      userId.value,
      homeworkForm.value.title,
      homeworkForm.value.description,
      homeworkForm.value.priority,
      homeworkForm.value.dueDate
    );
    ElMessage.success('添加成功');
    loadHomework();
    resetForm();
    activeTab.value = 'list';
  } catch (error) {
    console.error('添加失败:', error);
    ElMessage.error('添加失败');
  }
};

const resetForm = () => {
  homeworkForm.value = {
    title: '',
    description: '',
    priority: 1,
    dueDate: '',
    reminderTime: ''
  };
};

const openEditDialog = (homework) => {
  editForm.value = {
    id: homework.id,
    title: homework.title,
    description: homework.description,
    priority: homework.priority,
    dueDate: homework.dueDate
  };
  editDialogVisible.value = true;
};

const saveEdit = async () => {
  try {
    await updateHomework(
      editForm.value.id,
      editForm.value.title,
      editForm.value.description,
      editForm.value.priority,
      editForm.value.dueDate
    );
    ElMessage.success('保存成功');
    editDialogVisible.value = false;
    loadHomework();
  } catch (error) {
    console.error('保存失败:', error);
    ElMessage.error('保存失败');
  }
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

const handleComplete = async (id) => {
  try {
    await completeHomework(id);
    ElMessage.success('标记完成');
    loadHomework();
  } catch (error) {
    console.error('操作失败:', error);
    ElMessage.error('操作失败');
  }
};

const handleIncomplete = async (id) => {
  try {
    await incompleteHomework(id);
    ElMessage.success('撤销完成');
    loadHomework();
  } catch (error) {
    console.error('操作失败:', error);
    ElMessage.error('操作失败');
  }
};

const getPriorityType = (priority) => {
  const types = { 1: 'info', 2: 'warning', 3: 'danger' };
  return types[priority] || 'info';
};

const getPriorityText = (priority) => {
  const texts = { 1: '低', 2: '中', 3: '高' };
  return texts[priority] || '低';
};

onMounted(() => {
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
