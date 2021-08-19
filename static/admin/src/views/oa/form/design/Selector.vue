<template>
  <section>
    <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-model-item label="数据来源">
        <a-radio-group v-model="value.type" @change="handleTypeChange" size="small">
          <a-radio-button value="static">静态数据</a-radio-button>
          <a-radio-button value="dictionary">数据字典</a-radio-button>
          <a-radio-button value="api">远端数据</a-radio-button>
        </a-radio-group>
      </a-form-model-item>
    </a-form-model>
    <a-divider orientation="center">配置项</a-divider>
    <section v-if="value.type == 'static'">
      <draggable v-model="value.items" group="items" handle=".fs-selector-sort" chosenClass="fs-selector-drop" animation="340">
        <a-row class="fs-selector-item" type="flex" v-for="(item, index) in value.items" :key="index">
          <a-col flex="32px"><a-button type="link" icon="deployment-unit" class="fs-selector-sort" /></a-col>
          <a-col flex="1"><a-input v-model="item.value" auto-complete="on" placeholder="value" /></a-col>
          <a-col flex="3px"></a-col>
          <a-col flex="1"><a-input v-model="item.label" auto-complete="on" placeholder="label" /></a-col>
          <a-col flex="32px" class="fs-selector-delete"><a-icon @click="itemDelete(item, index)" type="minus-circle" /></a-col>
        </a-row>
      </draggable>
      <a-row class="fs-selector-item">
        <a-col :span="24"><a-button @click="itemAdd" icon="plus-circle" type="link">添加选项</a-button></a-col>
      </a-row>
    </section>
    <a-form-model v-else-if="value.type == 'dictionary'" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-form-model-item label="数据字典">
        <a-select v-model="value.dictionary" placeholder="请选择" :allowClear="true">
          <a-select-option v-for="(v, k) in dictionaries" :key="k" :value="v.value">{{ v.label }} - {{ v.value }}</a-select-option>
        </a-select>
      </a-form-model-item>
    </a-form-model>
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
import UIUtil from '@/utils/ui'
import dictionaryService from '@/service/member/dictionary'

export default {
  name: 'Selector',
  components: { draggable },
  props: {
    value: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {
      dictionaries: []
    }
  },
  watch: {
    'activeItem.id': {
      handler () {
        this.$emit('input', this.formatted(this.value))
      },
      immediate: true
    }
  },
  methods: {
    handleTypeChange () {
      this.$emit('input', this.formatted(this.value))
    },
    formatted (obj) {
      const result = {
        type: obj.type || 'static'
      }
      result.items = obj.items || []
      switch (result.type) {
        case 'dictionary':
          result.dictionary = obj.dictionary || ''
          break
        case 'api':
          result.apiUrl = obj.apiUrl || ''
          result.fieldData = obj.fieldData || ''
          result.fieldValue = obj.fieldValue || ''
          result.fieldLabel = obj.fieldLabel || ''
          break
      }
      return Object.assign({}, obj, result)
    },
    itemData () {
      return { value: '', label: '' }
    },
    itemAdd () {
      this.value.items.push(this.itemData())
    },
    itemDelete (item, index) {
      this.value.items.splice(index, 1)
    },
    loadDictionary () {
      UIUtil.cache(null, () => dictionaryService.available({ formatArray: true }), 0).then(result => {
        if (result.code === 0) {
          this.dictionaries = result.data
        }
      })
    }
  },
  mounted () {
    this.loadDictionary()
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
    text-align: center;
    line-height: 32px;
    vertical-align: middle;
    color: rgb(211, 69, 69);
  }
}
.fs-selector-drop {
  border:dashed 1px lightblue;
}
</style>
