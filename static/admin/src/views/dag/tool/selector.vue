<template>
  <section>
    <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-model-item label="选择模式" prop="mode">
        <a-select v-model="value.mode" placeholder="mode">
          <a-select-option v-for="(item, key) in options.modes" :key="key" :value="item.mode">{{ item.mode }} - {{ item.label }}</a-select-option>
        </a-select>
      </a-form-model-item>
    </a-form-model>
    <a-radio-group v-model="value.type" @change="handleTypeChange" size="small">
      <a-radio-button value="static">静态数据</a-radio-button>
      <a-radio-button value="dictionary" disabled>数据字典</a-radio-button>
      <a-radio-button value="api">远端数据</a-radio-button>
    </a-radio-group>
    <a-divider orientation="left"><i>配置项</i></a-divider>
    <section v-if="value.type == 'static'">
      <draggable v-model="value.items" group="items" handle=".fs-selector-sort" chosenClass="fs-selector-drop" animation="340">
        <a-row class="fs-selector-item" type="flex" v-for="(item, index) in value.items" :key="index">
          <a-col flex="32px"><a-button type="link" icon="deployment-unit" class="fs-selector-sort" /></a-col>
          <a-col flex="95px"><a-input v-model="item.value" auto-complete="on" placeholder="value" /></a-col>
          <a-col flex="3px"></a-col>
          <a-col flex="95px"><a-input v-model="item.label" auto-complete="on" placeholder="label" /></a-col>
          <a-col flex="32px"><a-button @click="itemDelete(item, index)" type="link" icon="minus-circle" class="fs-selector-delete" /></a-col>
        </a-row>
      </draggable>
      <a-row class="fs-selector-item">
        <a-col :span="24"><a-button @click="itemAdd" icon="plus-circle" type="link">添加选项</a-button></a-col>
      </a-row>
    </section>
    <a-form-model v-else-if="value.type == 'api'" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-model-item label="接口地址" prop="apiUrl">
        <a-input v-model="value.apiUrl" auto-complete="on" placeholder="数据接口地址"></a-input>
      </a-form-model-item>
      <a-form-model-item label="数据字段" prop="fieldData">
        <a-input v-model="value.fieldData" auto-complete="on" placeholder="接口数据所在字段路径"></a-input>
      </a-form-model-item>
      <a-form-model-item label="取值字段" prop="fieldValue">
        <a-input v-model="value.fieldValue" auto-complete="on" placeholder="接口Value字段名称"></a-input>
      </a-form-model-item>
      <a-form-model-item label="标签字段" prop="fieldLabel">
        <a-input v-model="value.fieldLabel" auto-complete="on" placeholder="接口Lable字段名称"></a-input>
      </a-form-model-item>
    </a-form-model>
  </section>
</template>

<script>
import draggable from 'vuedraggable'

export default {
  name: 'DagToolSelector',
  components: { draggable },
  props: {
    value: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      options: {
        modes: [{
          label: '默认',
          mode: 'default'
        }, {
          label: '多选',
          mode: 'multiple'
        }, {
          label: '标签',
          mode: 'tags'
        }, {
          label: '组合',
          mode: 'combobox'
        }]
      }
    }
  },
  methods: {
    handleTypeChange () {
      this.$emit('input', this.formatted(this.value))
    },
    formatted (obj) {
      const result = {
        mode: obj.mode || 'default',
        type: obj.type || 'static'
      }
      switch (result.type) {
        case 'static':
          result.items = obj.items || []
          break
        case 'api':
          result.apiUrl = obj.apiUrl || ''
          result.fieldData = obj.fieldData || ''
          result.fieldValue = obj.fieldValue || ''
          result.fieldLabel = obj.fieldLabel || ''
          break
      }
      return result
    },
    itemData () {
      return { value: '', label: '' }
    },
    itemAdd () {
      this.value.items.push(this.itemData())
    },
    itemDelete (item, index) {
      this.value.items.splice(index, 1)
    }
  },
  mounted () {
    this.$emit('input', this.formatted(this.value))
  }
}
</script>

<style lang="less" scoped>
.fs-selector-item {
  padding: 3px 0px 3px 0px;
  border:dashed 1px white;
  .fs-selector-sort {
    color: lightslategray;
    cursor: move;
  }
  .fs-selector-delete {
    color: rgb(211, 69, 69);
  }
}
.fs-selector-drop {
  border:dashed 1px lightblue;
}
</style>
