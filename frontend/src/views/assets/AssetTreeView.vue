<template>
  <section class="page">
    <h1>资产主数据</h1>
    <table class="result-table">
      <thead>
        <tr>
          <th>节点</th>
          <th>类型</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id">
          <td>{{ `${'　'.repeat(row.level)}${row.name}` }}</td>
          <td>{{ row.type }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchAssetTree, type AssetNode } from '../../api/assets'

const assets = ref<AssetNode[]>([])

const rows = computed(() => flatten(assets.value))

onMounted(async () => {
  assets.value = await fetchAssetTree()
})

function flatten(nodes: AssetNode[], level = 0): Array<{ id: string; name: string; type: string; level: number }> {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, type: node.type ?? '', level },
    ...flatten(node.children, level + 1)
  ])
}
</script>

<style scoped>
.page {
  display: grid;
  gap: 12px;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

.result-table th,
.result-table td {
  padding: 8px;
  border: 1px solid #dcdfe6;
  text-align: left;
}
</style>
