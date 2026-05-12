<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <div class="page-title">角色权限</div>
        <div class="page-desc">配置角色、权限和资源访问范围</div>
      </div>
      <a-button type="primary" @click="openCreate">新增角色</a-button>
    </div>

    <div class="role-grid">
      <a-card v-for="role in roles" :key="role.roleCode" :bordered="false" class="role-card">
        <div class="role-head">
          <div>
            <div class="role-name">{{ role.roleName }}</div>
            <div class="role-code">{{ role.roleCode }}</div>
          </div>
          <a-tag :color="role.status === 'active' ? 'green' : 'red'">{{ role.status }}</a-tag>
        </div>
        <p class="role-desc">{{ role.description || '-' }}</p>
        <a-select
          v-model:value="rolePermissionMap[role.roleCode || '']"
          mode="multiple"
          show-search
          option-filter-prop="label"
          placeholder="选择权限"
          style="width: 100%"
          :options="permissionOptions"
        />
        <div class="role-actions">
          <a-button size="small" @click="openEdit(role)">编辑</a-button>
          <a-button size="small" type="primary" @click="savePermissions(role)">保存权限</a-button>
        </div>
      </a-card>
    </div>

    <a-modal v-model:open="modalOpen" :title="editingRole.id ? '编辑角色' : '新增角色'" @ok="saveRole">
      <a-form layout="vertical">
        <a-form-item label="角色编码" required><a-input v-model:value="editingRole.roleCode" :disabled="!!editingRole.id" /></a-form-item>
        <a-form-item label="角色名称" required><a-input v-model:value="editingRole.roleName" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="editingRole.description" /></a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="editingRole.status">
            <a-select-option value="active">active</a-select-option>
            <a-select-option value="inactive">inactive</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="优先级"><a-input-number v-model:value="editingRole.priority" style="width: 100%" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { addRole, listPermissions, listRolePermissions, listRoles, setRolePermissions, updateRole } from '@/api/rbacController'

const roles = ref<API.Role[]>([])
const permissions = ref<API.Permission[]>([])
const modalOpen = ref(false)
const editingRole = reactive<API.Role>({})
const rolePermissionMap = reactive<Record<string, string[]>>({})

const permissionOptions = computed(() =>
  permissions.value.map((item) => ({
    value: item.permissionCode || '',
    label: `${item.permissionCode} ${item.permissionName || ''}`,
  })),
)

const loadData = async () => {
  const [roleRes, permissionRes] = await Promise.all([listRoles(), listPermissions()])
  if (roleRes.data.code === 0) {
    roles.value = roleRes.data.data || []
    await Promise.all(roles.value.map(async (role) => {
      if (role.roleCode && !rolePermissionMap[role.roleCode]) {
        const res = await listRolePermissions({ roleCode: role.roleCode })
        rolePermissionMap[role.roleCode] = res.data.code === 0 ? res.data.data || [] : []
      }
    }))
  }
  if (permissionRes.data.code === 0) {
    permissions.value = permissionRes.data.data || []
  }
}

const openCreate = () => {
  Object.assign(editingRole, { id: undefined, roleCode: '', roleName: '', description: '', status: 'active', priority: 0 })
  modalOpen.value = true
}

const openEdit = (role: API.Role) => {
  Object.assign(editingRole, role)
  modalOpen.value = true
}

const saveRole = async () => {
  const res = editingRole.id ? await updateRole({ ...editingRole }) : await addRole({ ...editingRole })
  if (res.data.code === 0) {
    message.success('保存成功')
    modalOpen.value = false
    await loadData()
  } else {
    message.error(res.data.message ?? '保存失败')
  }
}

const savePermissions = async (role: API.Role) => {
  if (!role.roleCode) return
  const res = await setRolePermissions({
    roleCode: role.roleCode,
    permissionCodes: rolePermissionMap[role.roleCode] || [],
  })
  if (res.data.code === 0) {
    message.success('权限已保存')
  } else {
    message.error(res.data.message ?? '保存失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-shell { max-width: 1280px; margin: 0 auto; padding: 28px 24px 40px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 16px; }
.page-title { font-size: 24px; font-weight: 700; color: #111827; }
.page-desc { margin-top: 6px; color: #6b7280; }
.role-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
.role-card { border: 1px solid #e5e7eb; }
.role-head { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.role-name { font-weight: 700; color: #111827; }
.role-code { font-size: 12px; color: #64748b; margin-top: 3px; }
.role-desc { color: #64748b; min-height: 42px; }
.role-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 14px; }
</style>
