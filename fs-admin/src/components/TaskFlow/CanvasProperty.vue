<script setup lang="ts">
import { ref } from 'vue';
import { CronExpressionParser } from 'cron-parser';
import LayoutIcon from '../Layout/LayoutIcon.vue';
import DateUtil from '@/utils/DateUtil';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config: any,
  instance: any,
}>()

const expressionVisible = ref(false)
const simulation = ref('')
const handleExpression = () => {
  if (model.value.expression) {
    try {
      const interval = CronExpressionParser.parse(model.value.expression)
      const result = []
      for (let i = 0; i < 5; i++) {
        result.push(DateUtil.format(interval.next()))
      }
      simulation.value = result.join('\r')
    } catch (e: any) {
      simulation.value = e.message
  }
  } else {
    simulation.value = ''
  }
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="流程属性" name="property">
      <el-form :model="model">
        <el-form-item label="" class="title">基础信息</el-form-item>
        <el-form-item label="流程标识">{{ model.id }}</el-form-item>
        <el-form-item label="流程名称">
          <el-input v-model="model.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="model.sort" :controls="false" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="model.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.description" />
        </el-form-item>
        <el-form-item label="" class="title">流程配置</el-form-item>
        <el-form-item label="并发度">
          <el-input-number v-model="model.concurrent" :controls="false" />
        </el-form-item>
        <el-form-item label="并发策略">
          <el-select v-model="model.concurrency" placeholder="请选择">
            <el-option v-for="item in config.concurrences" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="失败策略">
          <el-select v-model="model.failure" placeholder="请选择">
            <el-option v-for="item in config.failures" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="" class="title">定时调度</el-form-item>
        <el-form-item label="表达式">
          <el-input v-model="model.expression">
            <template #append><el-button @click="expressionVisible = true"><LayoutIcon name="Timer" /></el-button></template>
          </el-input>
        </el-form-item>
        <el-form-item label="" class="title">默认参数</el-form-item>
        <el-form-item label="">
          <el-input type="textarea" v-model="model.data" :rows="5" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="消息通知" name="notify">
      <el-form :model="model">
        <el-form-item label="" class="title">通知配置</el-form-item>
        <el-form-item label="触发时机">
          <el-checkbox-group v-model="model.notify.stage">
            <el-checkbox :label="item.label" :value="item.value" v-for="item in config.stages" :key="item.value" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="失败通知">
          <el-input type="textarea" v-model="model.notify.failure" placeholder="采用英文逗号分隔" :rows="5" />
        </el-form-item>
        <el-form-item label="成功通知">
          <el-input type="textarea" v-model="model.notify.success" placeholder="采用英文逗号分隔" :rows="5" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
  <el-dialog v-model="expressionVisible" title="定时调度" draggable>
    <el-form :model="model" label-position="top">
      <el-form-item label="定时任务">
        <el-input v-model="model.expression">
          <template #append><el-button @click="handleExpression">解析</el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item label="模拟触发">
        <el-input type="textarea" v-model="simulation" placeholder="输入表达式，点击解析按钮模拟触发结果" :rows="5" disabled />
      </el-form-item>
    </el-form>
    <el-alert title="表达式格式：{秒} {分} {时} {日期} {月份} {星期} {年份}" type="primary" show-icon>
      <p>（1）*：表示匹配该域的任意值。</p>
      <p>（2）?：只能用在{日期}和{星期}两个域，表示不为该域指定值。</p>
      <p>（3）-：表示范围。</p>
      <p>（4）/：表示起始时间开始触发，然后每隔固定时间触发一次。</p>
      <p>（5）,：表示列出枚举值。</p>
      <p>（6）L：表示最后，只能出现在{日期}和{星期}两个域，在最近有效工作日触发。</p>
      <p>（7）W：表示有效工作日(周一到周五),只能出现在{星期}域，在最近有效工作日触发。</p>
      <p>（8）LW：这两个字符可以连用，表示在某个月最后一个工作日，即最后一个星期五。</p>
      <p>（9）#：用于指定月份中的第几周的哪一天，只能出现在{星期}域。</p>
    </el-alert>
  </el-dialog>
</template>

<style lang="scss" scoped>
.el-alert {
  align-items: flex-start;
}
</style>
