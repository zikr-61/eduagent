<template>
  <div class="wrong-questions">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>错题管理</span>
          <el-button type="primary" :loading="loading" @click="loadList">刷新</el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
        title="在「知识点总结」中生成习题并作答时，答错的题目会自动进入错题本。你可以在此重新作答，答对后可移出错题本。"
      />

      <el-empty v-if="!loading && list.length === 0" description="暂无错题，去知识点总结里练一练吧" />

      <el-table v-else :data="list" stripe style="width: 100%">
        <el-table-column prop="knowledgeTitle" label="知识点" min-width="140" show-overflow-tooltip />
        <el-table-column prop="questionText" label="题目" min-width="220" show-overflow-tooltip />
        <el-table-column prop="errorCount" label="错次" width="80" />
        <el-table-column label="最近出错" width="170">
          <template #default="{ row }">
            {{ formatDt(row.lastErrorTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openRedo(row)">重做</el-button>
            <el-button type="danger" link @click="removeRow(row)">移出错题本</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="redoVisible" title="重做" width="640px" destroy-on-close @closed="closeRedo">
      <div v-if="current" class="redo-body">
        <p class="meta"><strong>知识点：</strong>{{ current.knowledgeTitle }}</p>
        <p class="q-text"><strong>题目：</strong>{{ current.questionText }}</p>
        <div v-if="current.options" class="opts">
          <p v-for="(line, i) in optionLines(current.options)" :key="i">{{ line }}</p>
        </div>
        <el-input
          v-model="redoAnswer"
          type="textarea"
          :rows="3"
          placeholder="请输入你的答案"
          style="margin-top: 12px"
        />
        <div v-if="showResult" class="result-msg" :class="redoCorrect ? 'ok' : 'bad'">
          {{ redoCorrect ? '回答正确！可将该题移出错题本。' : '仍不正确，再想想看。' }}
        </div>
      </div>
      <template #footer>
        <el-button @click="redoVisible = false">关闭</el-button>
        <el-button type="primary" @click="checkRedo">提交答案</el-button>
        <el-button v-if="showResult && redoCorrect" type="success" @click="removeAfterCorrect">移出错题本</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getErrorQuestions, removeErrorQuestion } from '@/api';

const userId = ref(1);
const loading = ref(false);
const list = ref([]);
const redoVisible = ref(false);
const current = ref(null);
const redoAnswer = ref('');
const showResult = ref(false);
const redoCorrect = ref(false);

const resolveUserId = () => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}');
    userId.value = u.id || 1;
  } catch {
    userId.value = 1;
  }
};

const formatDt = (v) => {
  if (!v) return '-';
  return new Date(v).toLocaleString('zh-CN');
};

const optionLines = (opts) => (opts || '').split('\n').filter(Boolean);

const loadList = async () => {
  loading.value = true;
  try {
    const res = await getErrorQuestions(userId.value);
    list.value = res.data.data || [];
  } catch (e) {
    console.error(e);
    ElMessage.error('加载错题失败');
  } finally {
    loading.value = false;
  }
};

const openRedo = (row) => {
  current.value = row;
  redoAnswer.value = '';
  showResult.value = false;
  redoCorrect.value = false;
  redoVisible.value = true;
};

const closeRedo = () => {
  current.value = null;
};

const norm = (s) => (s || '').trim();

const checkRedo = () => {
  if (!current.value) return;
  showResult.value = true;
  redoCorrect.value = norm(redoAnswer.value) === norm(current.value.answer);
};

const removeRow = async (row) => {
  try {
    await ElMessageBox.confirm('确定从错题本中移除该题？', '提示', { type: 'warning' });
    await removeErrorQuestion(userId.value, row.questionId);
    ElMessage.success('已移除');
    loadList();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败');
  }
};

const removeAfterCorrect = async () => {
  if (!current.value) return;
  try {
    await removeErrorQuestion(userId.value, current.value.questionId);
    ElMessage.success('已移出错题本');
    redoVisible.value = false;
    loadList();
  } catch {
    ElMessage.error('操作失败');
  }
};

onMounted(() => {
  resolveUserId();
  if (localStorage.getItem('userType') !== 'student') {
    ElMessage.warning('错题管理仅对学生开放');
    return;
  }
  loadList();
});
</script>

<style scoped>
.wrong-questions {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.meta,
.q-text {
  margin: 0 0 8px;
  line-height: 1.5;
}
.opts {
  margin: 8px 0;
  padding-left: 8px;
  color: #606266;
  font-size: 14px;
}
.result-msg {
  margin-top: 12px;
  font-weight: 600;
}
.result-msg.ok {
  color: #67c23a;
}
.result-msg.bad {
  color: #e6a23c;
}
.ans {
  color: #67c23a;
  font-weight: 600;
}
</style>
