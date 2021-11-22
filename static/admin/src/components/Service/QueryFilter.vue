<template>
  <section>
    <a-space style="margin-bottom: 20px;">
      <a-button @click="appendOperation()">添加条件</a-button>
      <a-button @click="appendRelation()">添加关系</a-button>
      <a-button type="danger" @click="clear()">清空全部</a-button>
    </a-space>
    <a-table
      :bordered="true"
      :columns="columns"
      :dataSource="rows"
      :pagination="false"
      :defaultExpandAllRows="true"
      :expandIconColumnIndex="1"
      :expandedRowKeys.sync="expandedRowKeys"
      :rowKey="record=> record.id">
      <span slot="state" slot-scope="text, record">
        <a-checkbox v-model="record.enabled" />
      </span>
      <span slot="icon" slot-scope="text, record">
        <a-icon :type="types[record.type].icon" />
      </span>
      <span slot="expression" slot-scope="text, record">
        <template v-if="record.type === 'RELATION'">
          <a-select v-model="record.value" placeholder="请选择关系">
            <a-select-option :value="v.value" :key="v.value" v-for="v in relations">{{ v.label }}</a-select-option>
          </a-select>
        </template>
        <template v-else>
          <a-space>
            <a-auto-complete
              v-model="record.left"
              style="width: 230px;"
              optionLabelProp="value"
              :allowClear="true"
              :filterOption="UIUtil.filterOption"
              v-if="operations[record.value].valuable">
              <template slot="dataSource">
                <a-select-option :key="k" :value="v.value" v-for="(v, k) in fields">{{ v.label }}</a-select-option>
              </template>
            </a-auto-complete>
            <a-select
              v-model="record.value"
              style="width: 170px;"
              placeholder="请选择条件">
              <a-select-option :value="v.value" :key="v.value" v-for="v in operations">{{ v.label }}</a-select-option>
            </a-select>
            <a-auto-complete
              v-model="record.right"
              style="width: 230px;"
              optionLabelProp="value"
              :allowClear="true"
              :filterOption="UIUtil.filterOption"
              v-if="operations[record.value].valuable">
              <template slot="dataSource">
                <a-select-option :key="k" :value="v.value" v-for="(v, k) in fields">{{ v.label }}</a-select-option>
              </template>
            </a-auto-complete>
          </a-space>
        </template>
      </span>
      <span slot="action" slot-scope="text, record, index">
        <a-button-group>
          <a-button type="link" @click="() => appendRelation(index, record)" v-if="record.type === 'RELATION'">关系</a-button>
          <a-button type="link" @click="() => appendOperation(index, record)" v-if="record.type === 'RELATION'">条件</a-button>
          <a-button type="link" @click="() => remove(index, record)">删除</a-button>
          <a-button type="link" @click="() => clear(index, record)" v-if="record.type === 'RELATION'">清空</a-button>
        </a-button-group>
      </span>
    </a-table>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'

export default {
  name: 'QueryFilter',
  props: {
    value: { type: Array, required: true },
    fields: { type: Array, required: true },
    volatile: { type: [Object, String, Number], default: null }
  },
  data () {
    return {
      UIUtil,
      rows: [],
      expandedRowKeys: [],
      columns: [
        { title: '', scopedSlots: { customRender: 'state' }, width: 50 },
        { title: '类型', scopedSlots: { customRender: 'icon' }, width: 150 },
        { title: '表达式', scopedSlots: { customRender: 'expression' } },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 272 }
      ],
      types: { RELATION: { name: '关系', icon: 'fork' }, OPERATION: { name: '条件', icon: 'filter' } },
      relations: {
        AND: { label: '并且（AND）', value: 'AND' },
        OR: { label: '或者（OR）', value: 'OR' }
      },
      operations: {
        EQUAL: { label: '等于（=）', value: 'EQUAL', valuable: true },
        NOT_EQUAL: { label: '不等于（!=）', value: 'NOT_EQUAL', valuable: true },
        LESS_THAN: { label: '小于（<）', value: 'LESS_THAN', valuable: true },
        LESS_THAN_OR_EQUAL: { label: '小于等于（<=）', value: 'LESS_THAN_OR_EQUAL', valuable: true },
        GREATER_THAN: { label: '大于（>）', value: 'GREATER_THAN', valuable: true },
        GREATER_THAN_OR_EQUAL: { label: '大于等于（>=）', value: 'GREATER_THAN_OR_EQUAL', valuable: true },
        IS_NULL: { label: '为空（is null）', value: 'IS_NULL', valuable: false },
        IS_NOT_NULL: { label: '不为空（is not null）', value: 'IS_NOT_NULL', valuable: false },
        LIKE: { label: '包含（like）', value: 'LIKE', valuable: true },
        NOT_LIKE: { label: '不包含（not like）', value: 'NOT_LIKE', valuable: true },
        IN: { label: '在列表中（in）', value: 'IN', valuable: true },
        NOT_IN: { label: '不在列表中（not in）', value: 'NOT_IN', valuable: true }
      }
    }
  },
  watch: {
    volatile: {
      handler () {
        this.rows = this.value
      },
      immediate: true
    }
  },
  methods: {
    generateFilterRelation () {
      return { id: UIUtil.uuid('relation-'), enabled: true, type: 'RELATION', value: 'AND', children: [] }
    },
    generateFilterOperation () {
      return { id: UIUtil.uuid('operation-'), enabled: true, type: 'OPERATION', value: 'EQUAL', left: '', right: '' }
    },
    appendRelation (index, item) {
      const relation = this.generateFilterRelation()
      if (item) {
        item.children.push(relation)
      } else {
        this.rows.push(relation)
      }
      this.expandedRowKeys.push(relation.id)
      this.$emit('input', this.rows)
    },
    appendOperation (index, item) {
      const operation = this.generateFilterOperation()
      if (item) {
        item.children.push(operation)
      } else {
        this.rows.push(operation)
      }
      this.$emit('input', this.rows)
    },
    remove (index, item) {
      this.removeById(this.rows, item.id)
      this.$emit('input', this.rows)
    },
    removeById (data, id) {
      for (const index in data) {
        const item = data[index]
        if (id === item.id) {
          data.splice(index, 1)
          return true
        }
        if (item.children && this.removeById(item.children, id)) return true
      }
      return false
    },
    clear (index, item) {
      if (item) {
        item.children = []
      } else {
        this.rows = []
      }
      this.$emit('input', this.rows)
    }
  },
  mounted () {}
}
</script>

<style lang="less" scoped>
</style>
