import axios from 'axios'

export interface AssetNode {
  id: string
  name: string
  type?: string
  children: AssetNode[]
}

interface AssetNodeRaw {
  id?: string | number
  name?: string
  nodeName?: string
  type?: string
  nodeType?: string
  children?: AssetNodeRaw[]
}

interface AssetTreeResponse {
  data?: AssetNodeRaw[]
}

function normalizeNode(raw: AssetNodeRaw): AssetNode {
  return {
    id: String(raw.id ?? ''),
    name: raw.name ?? raw.nodeName ?? '',
    type: raw.type ?? raw.nodeType ?? '',
    children: Array.isArray(raw.children) ? raw.children.map(normalizeNode) : []
  }
}

export async function fetchAssetTree() {
  const { data } = await axios.get<AssetTreeResponse>('/api/assets/tree')
  return Array.isArray(data.data) ? data.data.map(normalizeNode) : []
}
