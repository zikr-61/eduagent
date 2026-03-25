<template>
  <div class="knowledge-summary">
    <!-- ══ 教师端：教案生成 ══ -->
    <el-card v-if="userType === 'teacher'" class="box-card">
      <template #header>
        <div class="card-header"><span>教案生成</span></div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="生成教案" name="generate">
          <el-form :model="planForm" label-width="120px">
            <el-form-item label="教学主题">
              <el-input v-model="planForm.title" placeholder="例：牛顿第二定律、一元二次方程" />
            </el-form-item>
            <el-form-item label="目标年级">
              <el-input v-model="planForm.gradeLevel" placeholder="例：高一、初三" />
            </el-form-item>
            <el-form-item label="内容要点">
              <el-input v-model="planForm.content" type="textarea" :rows="6"
                placeholder="请输入本节课的主要内容、知识点或教学要求..." />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="generatePlan">生成教案</el-button>
            </el-form-item>
          </el-form>

          <el-divider v-if="planResult" />

          <div v-if="planResult" class="result-content">
            <ExecutionVisualizer
              v-if="executionSteps.length > 0"
              :steps="executionSteps"
              :total-duration="totalDuration"
            />

            <el-descriptions title="教案详情" :column="1" border class="plan-desc">
              <el-descriptions-item label="教学目标">
                <div class="plan-text">{{ planResult.objectives }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="重点难点">
                <div class="plan-text">{{ planResult.keyPoints }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="知识讲解框架">
                <div class="plan-text">{{ planResult.framework }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="例题（含解析）">
                <div class="plan-text">{{ planResult.examples }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="课后作业建议">
                <div class="plan-text">{{ planResult.homework }}</div>
              </el-descriptions-item>
            </el-descriptions>

            <el-button type="success" style="margin-top:16px" @click="downloadPlan">下载教案</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="教案历史" name="history">
          <el-table :data="lessonPlanHistory" style="width:100%" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="教学主题" />
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="scope">{{ formatDate(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="scope">
                <el-button type="primary" size="small" @click="viewPlanDetail(scope.row)">查看</el-button>
                <el-button type="success" size="small" @click="downloadPlanRecord(scope.row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 教案详情对话框 -->
    <el-dialog v-model="planDialogVisible" title="教案详情" width="70%" destroy-on-close>
      <div v-if="currentPlan">
        <h3>{{ currentPlan.title }}</h3>
        <el-divider />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="教学目标"><div class="plan-text">{{ currentPlan.objectives }}</div></el-descriptions-item>
          <el-descriptions-item label="重点难点"><div class="plan-text">{{ currentPlan.keyPoints }}</div></el-descriptions-item>
          <el-descriptions-item label="知识讲解框架"><div class="plan-text">{{ currentPlan.framework }}</div></el-descriptions-item>
          <el-descriptions-item label="例题（含解析）"><div class="plan-text">{{ currentPlan.examples }}</div></el-descriptions-item>
          <el-descriptions-item label="课后作业建议"><div class="plan-text">{{ currentPlan.homework }}</div></el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- ══ 学生端：知识点总结 + 习题 ══ -->
    <el-card v-if="userType !== 'teacher'" class="box-card">
      <template #header>
        <div class="card-header"><span>生成知识点总结</span></div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="生成总结" name="generate">
          <el-form :model="form" label-width="120px">
            <el-form-item label="标题">
              <el-input v-model="form.title" placeholder="请输入知识点标题" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入知识点内容" />
            </el-form-item>
            <el-form-item label="上传文件">
              <el-upload ref="upload" :auto-upload="false" :limit="5" :on-change="handleFileChange" accept=".txt,.doc,.docx,.pdf">
                <el-button type="primary">选择文件</el-button>
                <template #tip>
                  <div class="el-upload__tip">支持txt, doc, docx, pdf文件，单次最多5个文件</div>
                </template>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="generateSummary">生成总结和习题</el-button>
            </el-form-item>
          </el-form>

          <el-divider v-if="result" />

          <div v-if="result" class="result-content">
            <ExecutionVisualizer
              v-if="executionSteps.length > 0"
              :steps="executionSteps"
              :total-duration="totalDuration"
            />

            <el-card shadow="hover">
              <template #header><span>生成的摘要</span></template>
              <p>{{ result.summary }}</p>
            </el-card>

            <el-card shadow="hover">
              <template #header><span>生成的习题 (共{{ result.questions.length }}道)</span></template>
              <div v-for="(q, index) in result.questions" :key="q.id || index" class="question-item">
                <p><strong>{{ index + 1 }}. {{ q.questionText }}</strong></p>
                <div v-if="q.options" class="options">
                  <p v-for="(opt, i) in q.options.split('\n')" :key="i">{{ opt }}</p>
                </div>
                <div class="practice-section">
                  <el-input v-model="userAnswers[index]" placeholder="请输入你的答案" style="margin:10px 0" />
                  <el-button type="primary" @click="checkAnswer(index)">检查答案</el-button>
                  <p v-if="showAnswers[index]" class="answer">答案: {{ q.answer }}</p>
                  <p v-if="showAnswers[index] && userAnswers[index] === q.answer" class="correct">回答正确！</p>
                  <p v-if="showAnswers[index] && userAnswers[index] !== q.answer" class="incorrect">回答错误，正确答案是: {{ q.answer }}</p>
                </div>
                <el-divider />
              </div>
              <el-button type="success" @click="downloadQuestions">下载习题</el-button>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="历史记录" name="history">
          <el-table :data="knowledgePoints" style="width:100%" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="scope">{{ formatDate(scope.row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="scope">
                <el-button type="primary" size="small" @click="viewDetail(scope.row)">查看</el-button>
                <el-button type="danger" size="small" @click="downloadResult(scope.row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 学生知识点详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="知识点详情" width="70%">
      <div v-if="currentDetail">
        <h3>{{ currentDetail.title }}</h3>
        <el-divider />
        <h4>摘要:</h4>
        <p>{{ currentDetail.summary }}</p>
        <el-divider />
        <h4>习题:</h4>
        <div v-for="(q, index) in currentDetail.questions" :key="index" class="question-item">
          <p><strong>{{ index + 1 }}. {{ q.questionText }}</strong></p>
          <div v-if="q.options" class="options">
            <p v-for="(opt, i) in q.options.split('\n')" :key="i">{{ opt }}</p>
          </div>
          <p class="answer">答案: {{ q.answer }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getKnowledgePoints, generateSummaryAndQuestions, generateLessonPlan, getQuestions, startLearning, endLearning, recordErrorQuestion } from '@/api';
import ExecutionVisualizer from '@/components/ExecutionVisualizer.vue';

const userId = ref(1);
const userType = ref(localStorage.getItem('userType') || 'student');
const activeTab = ref('generate');
const loading = ref(false);
const executionSteps = ref([]);
const totalDuration = ref('');

// ── 教师端状态 ──
const planForm = ref({ title: '', gradeLevel: '', content: '' });
const planResult = ref(null);
const lessonPlanHistory = ref([]);
const planDialogVisible = ref(false);
const currentPlan = ref(null);

// ── 学生端状态 ──
const form = ref({ title: '', content: '', fileName: '' });
const result = ref(null);
const knowledgePoints = ref([]);
const detailDialogVisible = ref(false);
const currentDetail = ref(null);
const learningStartTime = ref(null);
const userAnswers = ref([]);
const showAnswers = ref([]);

const resolveUserId = () => {
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}');
    userId.value = u.id || 1;
  } catch { userId.value = 1; }
};

// ── 教师端方法 ──
const generatePlan = async () => {
  if (!planForm.value.title || !planForm.value.content) {
    ElMessage.warning('请填写教学主题和内容要点');
    return;
  }
  loading.value = true;
  executionSteps.value = [];
  totalDuration.value = '';
  try {
    const res = await generateLessonPlan(
      userId.value,
      planForm.value.title,
      planForm.value.gradeLevel,
      planForm.value.content
    );
    const data = res.data.data;
    planResult.value = data.lessonPlan;
    if (data.executionSteps) executionSteps.value = data.executionSteps;
    if (data.totalDuration) totalDuration.value = data.totalDuration;
    ElMessage.success('教案生成成功');
    loadPlanHistory();
  } catch (error) {
    console.error('生成失败:', error);
    ElMessage.error('生成失败');
  } finally {
    loading.value = false;
  }
};

const loadPlanHistory = async () => {
  try {
    const res = await getKnowledgePoints(userId.value);
    const all = res.data.data || [];
    lessonPlanHistory.value = all.filter(kp => kp.fileName === 'LESSON_PLAN');
  } catch (error) {
    console.error('加载教案历史失败:', error);
  }
};

const parsePlanSummary = (summary) => {
  try {
    return JSON.parse(summary);
  } catch {
    return { objectives: summary, keyPoints: '', framework: '', examples: '', homework: '' };
  }
};

const viewPlanDetail = (row) => {
  currentPlan.value = { title: row.title, ...parsePlanSummary(row.summary) };
  planDialogVisible.value = true;
};

const downloadPlan = () => {
  if (!planResult.value) return;
  const p = planResult.value;
  const content = `教案：${planForm.value.title}\n年级：${planForm.value.gradeLevel}\n\n` +
    `【教学目标】\n${p.objectives}\n\n【重点难点】\n${p.keyPoints}\n\n` +
    `【知识讲解框架】\n${p.framework}\n\n【例题（含解析）】\n${p.examples}\n\n` +
    `【课后作业建议】\n${p.homework}`;
  downloadFile(`${planForm.value.title}_教案.txt`, content);
};

const downloadPlanRecord = (row) => {
  const p = parsePlanSummary(row.summary);
  const content = `教案：${row.title}\n\n` +
    `【教学目标】\n${p.objectives}\n\n【重点难点】\n${p.keyPoints}\n\n` +
    `【知识讲解框架】\n${p.framework}\n\n【例题（含解析）】\n${p.examples}\n\n` +
    `【课后作业建议】\n${p.homework}`;
  downloadFile(`${row.title}_教案.txt`, content);
};

// ── 学生端方法 ──
const startLearningSession = async () => {
  try {
    const res = await startLearning(userId.value, 'knowledge', null);
    learningStartTime.value = res.data.startTime;
  } catch (error) { console.error('开始学习失败:', error); }
};

const endLearningSession = async () => {
  if (learningStartTime.value) {
    try {
      await endLearning(userId.value, 'knowledge', null, learningStartTime.value);
    } catch (error) { console.error('结束学习失败:', error); }
  }
};

const generateSummary = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题和内容');
    return;
  }
  await startLearningSession();
  loading.value = true;
  executionSteps.value = [];
  totalDuration.value = '';
  try {
    const res = await generateSummaryAndQuestions(
      userId.value, form.value.title, form.value.content, form.value.fileName
    );
    result.value = res.data.data;
    if (res.data.data.executionSteps) executionSteps.value = res.data.data.executionSteps;
    if (res.data.data.totalDuration) totalDuration.value = res.data.data.totalDuration;
    ElMessage.success('生成成功');
    loadHistory();
  } catch (error) {
    console.error('生成失败:', error);
    ElMessage.error('生成失败');
  } finally {
    loading.value = false;
    await endLearningSession();
  }
};

const loadHistory = async () => {
  try {
    const res = await getKnowledgePoints(userId.value);
    const all = res.data.data || [];
    knowledgePoints.value = all.filter(kp => kp.fileName !== 'LESSON_PLAN');
  } catch (error) { console.error('加载历史失败:', error); }
};

const viewDetail = async (row) => {
  try {
    const res = await getQuestions(row.id);
    currentDetail.value = { ...row, questions: res.data.data || [] };
    detailDialogVisible.value = true;
  } catch (error) {
    ElMessage.error('加载详情失败');
  }
};

const downloadQuestions = () => {
  if (!result.value) return;
  const content = `知识点: ${form.value.title}\n\n摘要:\n${result.value.summary}\n\n习题:\n`;
  const q = result.value.questions.map((q, i) =>
    `${i + 1}. ${q.questionText}\n${q.options || ''}\n答案: ${q.answer}\n`
  ).join('\n');
  downloadFile(`${form.value.title}_习题.txt`, content + q);
};

const downloadResult = async (row) => {
  try {
    const res = await getQuestions(row.id);
    const questions = res.data.data || [];
    const content = `知识点: ${row.title}\n\n摘要:\n${row.summary}\n\n习题:\n`;
    const q = questions.map((q, i) =>
      `${i + 1}. ${q.questionText}\n${q.options || ''}\n答案: ${q.answer}\n`
    ).join('\n');
    downloadFile(`${row.title}_习题.txt`, content + q);
  } catch (error) {
    ElMessage.error('下载失败');
  }
};

const handleFileChange = (file) => {
  const ext = file.name.split('.').pop().toLowerCase();
  if (ext === 'txt') {
    form.value.fileName = file.name;
    const reader = new FileReader();
    reader.onload = (e) => { form.value.content = e.target.result; };
    reader.readAsText(file.raw, 'utf-8');
  } else {
    ElMessage.warning('暂不支持非文本文件的直接解析，请手动输入内容');
  }
};

const norm = (s) => (s == null ? '' : String(s).trim());

const checkAnswer = async (index) => {
  showAnswers.value[index] = true;
  const q = result.value?.questions?.[index];
  if (!q || !q.id) return;
  const ok = norm(userAnswers.value[index]) === norm(q.answer);
  if (ok || userType.value !== 'student') return;
  try {
    const r = await recordErrorQuestion(userId.value, q.id);
    if (r.data?.success !== false) ElMessage.success('已记录到错题本');
  } catch (e) { console.error(e); }
};

// ── 通用工具 ──
const downloadFile = (filename, content) => {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('zh-CN');
};

onMounted(() => {
  resolveUserId();
  if (userType.value === 'teacher') {
    loadPlanHistory();
  } else {
    loadHistory();
  }
});
</script>

<style scoped>
.knowledge-summary { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.result-content { margin-top: 20px; }
.question-item { margin-bottom: 15px; }
.options { margin-left: 20px; color: #606266; }
.answer { color: #67c23a; font-weight: bold; }
.correct { color: #67c23a; font-weight: bold; margin-top: 10px; }
.incorrect { color: #f56c6c; font-weight: bold; margin-top: 10px; }
.practice-section { margin-top: 15px; }
.plan-desc { margin-top: 8px; }
.plan-text { white-space: pre-wrap; line-height: 1.8; font-size: 14px; }
</style>
