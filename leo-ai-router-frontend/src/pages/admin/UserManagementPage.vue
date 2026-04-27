<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <div class="page-title">用户管理</div>
        <div class="page-desc">管理用户状态、配额和详细使用分析</div>
      </div>
      <a-button type="primary" size="large" @click="openCreateModal">新增用户</a-button>
    </div>

    <a-card :bordered="false" class="filter-card">
      <a-form layout="vertical">
        <div class="filter-grid">
          <a-form-item label="用户名"><a-input v-model:value="query.userName" allow-clear /></a-form-item>
          <a-form-item label="账号"><a-input v-model:value="query.userAccount" allow-clear /></a-form-item>
          <a-form-item label="角色">
            <a-select v-model:value="query.userRole" allow-clear placeholder="全部角色">
              <a-select-option value="admin">admin</a-select-option>
              <a-select-option value="user">user</a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div class="filter-actions">
          <a-space>
            <a-button type="primary" @click="handleSearch">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>

    <a-card :bordered="false">
      <a-table row-key="id" :columns="columns" :data-source="users" :loading="loading" :pagination="pagination" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userName'">
            <div class="user-cell">
              <span class="user-main">{{ record.userName || '未命名用户' }}</span>
              <span class="user-sub">{{ record.userAccount || '-' }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'userRole'">
            <a-tag :color="record.userRole === 'admin' ? 'purple' : 'blue'">{{ record.userRole }}</a-tag>
          </template>
          <template v-else-if="column.key === 'userStatus'">
            <a-tag :color="record.userStatus === 'disabled' ? 'red' : 'green'">
              {{ record.userStatus === 'disabled' ? '禁用' : '正常' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'quota'">
            {{ record.tokenQuota === -1 ? '无限制' : `${(record.tokenQuota ?? 0).toLocaleString('zh-CN')}` }}
          </template>
          <template v-else-if="column.key === 'balance'">
            ¥{{ Number(record.balance ?? 0).toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap>
              <a-button type="link" @click="openEditModal(record)">编辑</a-button>
              <a-button type="link" @click="openAnalysis(record)">分析</a-button>
              <a-button type="link" @click="openQuota(record)">配额</a-button>
              <a-popconfirm v-if="record.userStatus !== 'disabled'" title="确认禁用该用户？" @confirm="toggleUserStatus(record, false)">
                <a-button type="link" danger>禁用</a-button>
              </a-popconfirm>
              <a-button v-else type="link" @click="toggleUserStatus(record, true)">启用</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="editModalOpen" :title="editingId ? '编辑用户' : '新增用户'" :confirm-loading="saving" @ok="handleSubmit">
      <a-form layout="vertical">
        <a-form-item label="用户名" required><a-input v-model:value="form.userName" /></a-form-item>
        <a-form-item label="账号" required v-if="!editingId"><a-input v-model:value="form.userAccount" /></a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="form.userRole">
            <a-select-option value="user">user</a-select-option>
            <a-select-option value="admin">admin</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="头像"><a-input v-model:value="form.userAvatar" /></a-form-item>
        <a-form-item label="简介"><a-textarea v-model:value="form.userProfile" :auto-size="{ minRows: 3, maxRows: 5 }" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="analysisOpen" title="用户使用分析" :footer="null" width="720px">
      <a-descriptions bordered :column="2" size="small" v-if="analysis">
        <a-descriptions-item label="用户">{{ analysis.userName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="账号">{{ analysis.userAccount || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ analysis.userStatus || '-' }}</a-descriptions-item>
        <a-descriptions-item label="角色">{{ analysis.userRole || '-' }}</a-descriptions-item>
        <a-descriptions-item label="配额">{{ formatQuota(analysis.tokenQuota) }}</a-descriptions-item>
        <a-descriptions-item label="剩余">{{ formatQuota(analysis.remainingQuota) }}</a-descriptions-item>
        <a-descriptions-item label="已用 Token">{{ formatNumber(analysis.usedTokens) }}</a-descriptions-item>
        <a-descriptions-item label="累计 Token">{{ formatNumber(analysis.totalTokens) }}</a-descriptions-item>
        <a-descriptions-item label="总请求">{{ formatNumber(analysis.totalRequests) }}</a-descriptions-item>
        <a-descriptions-item label="成功请求">{{ formatNumber(analysis.successRequests) }}</a-descriptions-item>
        <a-descriptions-item label="总费用">{{ formatCurrency(analysis.totalCost) }}</a-descriptions-item>
        <a-descriptions-item label="今日费用">{{ formatCurrency(analysis.todayCost) }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <a-modal v-model:open="quotaOpen" title="设置用户配额" :confirm-loading="quotaSaving" @ok="handleQuotaSubmit">
      <a-form layout="vertical">
        <a-form-item label="Token 配额">
          <a-input-number v-model:value="quotaForm.tokenQuota" :min="-1" :step="1000" style="width: 100%" />
        </a-form-item>
        <a-space><a-button @click="resetQuotaUsage">重置已用配额</a-button></a-space>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { addUser, disableUser, enableUser, getUserAnalysis, listUserVoByPage, resetUserQuota, setUserQuota, updateUser } from '@/api/userController'

const loading = ref(false)
const saving = ref(false)
const quotaSaving = ref(false)
const editModalOpen = ref(false)
const analysisOpen = ref(false)
const quotaOpen = ref(false)
const editingId = ref<number>()
const quotaUserId = ref<number>()
const users = ref<API.UserVO[]>([])
const analysis = ref<API.UserAnalysisVO>()

const query = reactive<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  userAccount: '',
  userRole: undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const form = reactive<API.UserAddRequest & API.UserUpdateRequest>({
  userName: '',
  userAccount: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
})

const quotaForm = reactive<API.QuotaUpdateRequest>({
  userId: undefined,
  tokenQuota: 0,
})

const columns = [
  { title: '用户', key: 'userName', width: 220 },
  { title: '邮箱', dataIndex: 'userEmail', key: 'userEmail', width: 180 },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 100 },
  { title: '状态', dataIndex: 'userStatus', key: 'userStatus', width: 100 },
  { title: '配额', key: 'quota', width: 120 },
  { title: '余额', key: 'balance', width: 120 },
  { title: '操作', key: 'action', width: 320 },
]

const formatNumber = (value?: number) => (value ?? 0).toLocaleString('zh-CN')
const formatCurrency = (value?: number) => `¥${Number(value ?? 0).toFixed(2)}`
const formatQuota = (value?: number) => (value === -1 ? '无限制' : formatNumber(value))

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({
      ...query,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      users.value = res.data.data.records ?? []
      pagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载用户失败')
    }
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingId.value = undefined
  form.userName = ''
  form.userAccount = ''
  form.userAvatar = ''
  form.userProfile = ''
  form.userRole = 'user'
}

const openCreateModal = () => {
  resetForm()
  editModalOpen.value = true
}

const openEditModal = (record: API.UserVO) => {
  editingId.value = record.id
  form.userName = record.userName ?? ''
  form.userAccount = record.userAccount ?? ''
  form.userAvatar = record.userAvatar ?? ''
  form.userProfile = record.userProfile ?? ''
  form.userRole = record.userRole ?? 'user'
  editModalOpen.value = true
}

const handleSubmit = async () => {
  if (!form.userName?.trim()) {
    message.warning('请输入用户名')
    return
  }
  if (!editingId.value && !form.userAccount?.trim()) {
    message.warning('请输入账号')
    return
  }
  saving.value = true
  try {
    const payload = {
      userName: form.userName?.trim(),
      userAccount: form.userAccount?.trim(),
      userAvatar: form.userAvatar?.trim(),
      userProfile: form.userProfile?.trim(),
      userRole: form.userRole,
    }
    const res = editingId.value ? await updateUser({ id: editingId.value, ...payload }) : await addUser(payload)
    if (res.data.code === 0) {
      message.success(editingId.value ? '用户更新成功' : '用户创建成功')
      editModalOpen.value = false
      await loadUsers()
    } else {
      message.error(res.data.message ?? '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const openAnalysis = async (record: API.UserVO) => {
  if (!record.id) return
  const res = await getUserAnalysis({ userId: record.id })
  if (res.data.code === 0) {
    analysis.value = res.data.data
    analysisOpen.value = true
  } else {
    message.error(res.data.message ?? '加载分析失败')
  }
}

const openQuota = (record: API.UserVO) => {
  quotaUserId.value = record.id
  quotaForm.userId = record.id
  quotaForm.tokenQuota = record.tokenQuota ?? 0
  quotaOpen.value = true
}

const handleQuotaSubmit = async () => {
  if (!quotaForm.userId || quotaForm.tokenQuota === undefined) return
  quotaSaving.value = true
  try {
    const res = await setUserQuota({ userId: quotaForm.userId, tokenQuota: quotaForm.tokenQuota })
    if (res.data.code === 0) {
      message.success('配额设置成功')
      quotaOpen.value = false
      await loadUsers()
    } else {
      message.error(res.data.message ?? '配额设置失败')
    }
  } finally {
    quotaSaving.value = false
  }
}

const resetQuotaUsage = async () => {
  if (!quotaUserId.value) return
  const res = await resetUserQuota({ userId: quotaUserId.value })
  if (res.data.code === 0) {
    message.success('已重置已用配额')
  } else {
    message.error(res.data.message ?? '重置失败')
  }
}

const toggleUserStatus = async (record: API.UserVO, enable: boolean) => {
  if (!record.id) return
  const res = enable ? await enableUser({ userId: record.id }) : await disableUser({ userId: record.id })
  if (res.data.code === 0) {
    message.success(enable ? '用户已启用' : '用户已禁用')
    await loadUsers()
  } else {
    message.error(res.data.message ?? '操作失败')
  }
}

const handleSearch = async () => {
  pagination.current = 1
  await loadUsers()
}

const handleReset = async () => {
  query.userName = ''
  query.userAccount = ''
  query.userRole = undefined
  pagination.current = 1
  await loadUsers()
}

const handleTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  void loadUsers()
}

onMounted(() => {
  void loadUsers()
})
</script>

<style scoped>
.page-shell { max-width: 1280px; margin: 0 auto; padding: 28px 24px 40px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 16px; }
.page-title { font-size: 24px; font-weight: 700; color: #111827; }
.page-desc { margin-top: 6px; color: #6b7280; }
.filter-card { margin-bottom: 16px; }
.filter-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px 16px; }
.filter-actions { margin-top: 16px; display: flex; justify-content: flex-end; }
.user-cell { display: flex; flex-direction: column; }
.user-main { color: #111827; font-weight: 600; }
.user-sub { font-size: 12px; color: #94a3b8; }
@media (max-width: 768px) { .filter-grid { grid-template-columns: 1fr; } .filter-actions { justify-content: flex-start; } }
</style>
