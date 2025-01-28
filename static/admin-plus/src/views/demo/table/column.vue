<script setup lang="ts">
import { ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import TableColumnSetting from '@/components/Table/TableColumnSetting.vue';
import type { TableInstance } from 'element-plus';

const tableRef = ref<TableInstance>()
const searchable = ref(true)
const columns = ref([
  { prop: 'date', label: 'Date', width: '150' },
  { label: 'Delivery Info', children: [
    { prop: 'name', label: 'Name', width: '120' },
    { label: 'Address Info', children: [
      { prop: 'state', label: 'State', width: '120' },
      { prop: 'city', label: 'City', width: '120', hide: true },
      { prop: 'address', label: 'Address', slot: 'address' },
      { prop: 'zip', label: 'Zip', width: '150', slot: 'zip' },
    ] },
  ] },
])

const rows = ref([
  {
    date: '2016-05-03',
    name: 'Tom',
    state: 'California',
    city: 'Los Angeles',
    address: 'No. 189, Grove St, Los Angeles',
    zip: 'CA 90036',
  },
  {
    date: '2016-05-02',
    name: 'Tom',
    state: 'California',
    city: 'Los Angeles',
    address: 'No. 189, Grove St, Los Angeles',
    zip: 'CA 90036',
  },
  {
    date: '2016-05-04',
    name: 'Tom',
    state: 'California',
    city: 'Los Angeles',
    address: 'No. 189, Grove St, Los Angeles',
    zip: 'CA 90036',
  },
  {
    date: '2016-05-01',
    name: 'Tom',
    state: 'California',
    city: 'Los Angeles',
    address: 'No. 189, Grove St, Los Angeles',
    zip: 'CA 90036',
  },
  {
    date: '2016-05-08',
    name: 'Tom',
    state: 'California',
    city: 'Los Angeles',
    address: 'No. 189, Grove St, Los Angeles',
    zip: 'CA 90036',
  },
  {
    date: '2016-05-06',
    name: 'Tom',
    state: 'California',
    city: 'Los Angeles',
    address: 'No. 189, Grove St, Los Angeles',
    zip: 'CA 90036',
  },
  {
    date: '2016-05-07',
    name: 'Tom',
    state: 'California',
    city: 'Los Angeles',
    address: 'No. 189, Grove St, Los Angeles',
    zip: 'CA 90036',
  },
])
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">查询</el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add />
        <button-edit />
        <button-delete />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh />
        <TableColumnSetting v-model="columns" :table="tableRef" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="record => record.id"
      :border="true"
      table-layout="auto"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #zip="scope">
          <el-switch v-model="scope.row.checked" inline-prompt :active-text="scope.row.zip" />
        </template>
        <template #address="scope">
          <el-button :icon="ElementPlusIcons.MapLocation">{{ scope.row.address }}</el-button>
        </template>
      </TableColumn>
    </el-table>
    <el-pagination
      :background="true"
      :page-sizes="[5, 10, 15, 20, 25, 30, 35, 45, 50, 60, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="600"
    />
  </el-card>
</template>

<style lang="scss" scoped>
</style>
