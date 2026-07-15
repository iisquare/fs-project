<script setup lang="ts">
import { ref, watch, computed } from 'vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const candidacy = computed(() => {
  // UserTask is candidacy-configured unless directly connected from StartEvent
  return true
})

const handleDelete = () => {
  props.instance.flow.remove(model.value)
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <el-form-item label="" class="title">
          <span>基础信息</span>
          <el-popconfirm title="确认删除该元素？" @confirm="handleDelete" width="180">
            <template #reference>
              <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </el-form-item>
        <el-form-item label="类型">bpmn:UserTask</el-form-item>
        <el-form-item label="标识">
          <el-input v-model="model.id" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="model.data.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.data.documentation" />
        </el-form-item>
        <el-divider v-if="candidacy">任务指派</el-divider>
        <el-form-item label="候选组" v-if="candidacy">
          <el-input v-model="model.data.candidateGroups" placeholder="候选用户/角色，多个用逗号分隔" />
        </el-form-item>
        <el-divider>表单权限</el-divider>
        <el-form-item label="">
          <el-input type="textarea" v-model="model.data.authority" placeholder="表单字段权限JSON配置" :rows="4" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="多方会签" name="multiInstance" v-if="candidacy">
      <el-form :model="model" label-position="top">
        <el-form-item label="启用会签">
          <el-switch v-model="model.data.loopCharacteristics" />
        </el-form-item>
        <template v-if="model.data.loopCharacteristics">
          <el-form-item label="顺序执行">
            <el-checkbox v-model="model.data.isSequential" />
          </el-form-item>
          <el-form-item label="循环基数">
            <el-input v-model="model.data.loopCardinality" placeholder="Loop Cardinality" />
          </el-form-item>
          <el-form-item label="集合变量">
            <el-input v-model="model.data.collection" placeholder="Collection" />
          </el-form-item>
          <el-form-item label="元素变量">
            <el-input v-model="model.data.elementVariable" placeholder="Element Variable" />
          </el-form-item>
          <el-form-item label="完成条件">
            <el-tooltip placement="top">
              <template #content>
                nrOfInstances - 实例总数<br/>
                nrOfActiveInstances - 活动中<br/>
                nrOfCompletedInstances - 已完成
              </template>
              <el-input type="textarea" v-model="model.data.completionCondition" placeholder="Completion Condition" />
            </el-tooltip>
          </el-form-item>
        </template>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
