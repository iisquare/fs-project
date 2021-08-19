<template>
  <section>
    <a-divider>正则校验</a-divider>
    <draggable :value="value" @input="val => $emit('input', val)" group="regulars" chosenClass="fs-regular-chosen" animation="340">
      <div class="fs-regular-item" v-for="(item, index) in value" :key="item.id">
        <a-icon type="close" @click="removeRegular(item, index)" class="close-btn" />
        <a-form-model-item label="表达式" labelAlign="right">
          <a-auto-complete v-model="item.regular" optionLabelProp="value">
            <template slot="dataSource">
              <a-select-option v-for="(v, k) in regulars" :key="k" :value="v.name">{{ v.label }} - {{ v.name }}</a-select-option>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <a-form-model-item label="错误提示" labelAlign="right"><a-input v-model="item.tooltip" auto-complete="on" /></a-form-model-item>
      </div>
    </draggable>
    <a-row class="fs-regular-btn">
      <a-col :span="24"><a-button @click="addRegular" icon="plus-circle" type="link">添加规则</a-button></a-col>
    </a-row>
  </section>
</template>

<script>
import draggable from 'vuedraggable'
import UIUtil from '@/utils/ui'
import formRegularService from '@/service/oa/formRegular'

export default {
  name: 'Regular',
  components: { draggable },
  props: {
    value: { type: Array, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {
      regulars: []
    }
  },
  methods: {
    generateRegular () {
      return { id: this.config.uuidRegular(), regular: '', tooltip: '' }
    },
    addRegular () {
      this.value.push(this.generateRegular())
    },
    removeRegular (item, index) {
      this.value.splice(index, 1)
    },
    loadRegulars () {
      UIUtil.cache(null, () => formRegularService.all(), 0).then(result => {
        if (result.code === 0) {
          this.regulars = result.data
        }
      })
    }
  },
  mounted () {
    this.loadRegulars()
  }
}
</script>

<style lang="less" scoped>
.fs-regular-item {
  padding: 12px 10px;
  background: #f8f8f8;
  position: relative;
  border-radius: 4px;
  border: 1px dashed #ccc;
  margin-bottom: 10px;
  .close-btn {
    position: absolute;
    right: -7px;
    top: -7px;
    display: block;
    width: 16px;
    height: 16px;
    line-height: 16px;
    background: rgba(0,0,0,.2);
    border-radius: 50%;
    color: #fff;
    text-align: center;
    z-index: 1;
    cursor: pointer;
    font-size: 12px;
  }
}
.fs-regular-chosen {
  border: 1px dashed #409eff;
}
.fs-regular-btn {
  padding: 5px 0px;
}
</style>
