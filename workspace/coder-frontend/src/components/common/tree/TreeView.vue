<script setup lang="ts">
/*****************************************************************************************
	Program ID   : TreeView
	Program Type : vue
	Program Name : 트리뷰
	Description  : 펼침/선택을 지원하는 공통 트리 컴포넌트
	Author       : ywkim
------------------------------------------------------------------------------------------
	Program History

	Date        Name      Description
	----------  --------  ----------------------------------------------------------------
	2026-10-09  ywkim     최초생성
------------------------------------------------------------------------------------------
*****************************************************************************************/
import { msg } from '@/utils/message'
import { computed } from 'vue'

export interface TreeNode {
  id: string
  label: string
  children?: TreeNode[]
}

interface VisibleNode {
  node: TreeNode
  depth: number
  hasChildren: boolean
  isExpanded: boolean
}

const props = withDefaults(
  defineProps<{
    nodes: TreeNode[]
    /** 펼쳐진 노드 id 목록 (v-model:expanded-ids) */
    expandedIds?: string[]
    selectedId?: string
    emptyText?: string
  }>(),
  { expandedIds: () => [], selectedId: '', emptyText: undefined },
)

const emit = defineEmits<{
  (e: 'update:expandedIds', ids: string[]): void
  (e: 'select', node: TreeNode): void
}>()

const expandedSet = computed(() => new Set(props.expandedIds))

// 펼쳐진 노드만 깊이와 함께 평탄화해서 렌더링한다
const visibleNodes = computed(() => {
  const result: VisibleNode[] = []
  const walk = (list: TreeNode[], depth: number) => {
    list.forEach((node) => {
      const hasChildren = !!node.children?.length
      const isExpanded = hasChildren && expandedSet.value.has(node.id)
      result.push({ node, depth, hasChildren, isExpanded })
      if (isExpanded) walk(node.children!, depth + 1)
    })
  }
  walk(props.nodes, 0)
  return result
})

/**
 * toggleNode : 자식이 있는 노드를 펼치거나 접고 update:expandedIds 이벤트를 발생
 * @param   item - 대상 노드 (깊이, 펼침 여부 포함)
 * @return  N/A
 */
function toggleNode(item: VisibleNode): void {
  if (!item.hasChildren) return
  const next = new Set(expandedSet.value)
  if (next.has(item.node.id)) next.delete(item.node.id)
  else next.add(item.node.id)
  emit('update:expandedIds', [...next])
}

/**
 * selectNode : 노드 선택 시 select 이벤트를 발생
 * @param   item - 선택된 노드 (깊이, 펼침 여부 포함)
 * @return  N/A
 */
function selectNode(item: VisibleNode): void {
  emit('select', item.node)
}
</script>

<template>
  <ul class="tree-view" role="tree">
    <li v-if="visibleNodes.length === 0" class="tree-empty">{{ emptyText ?? msg('cm.info.no.data') }}</li>
    <li
      v-for="item in visibleNodes"
      :key="item.node.id"
      class="tree-item"
      :class="{ 'is-selected': item.node.id === selectedId }"
      role="treeitem"
      :aria-expanded="item.hasChildren ? item.isExpanded : undefined"
      :aria-selected="item.node.id === selectedId"
      :style="{ paddingLeft: `${8 + item.depth * 18}px` }"
      @click="selectNode(item)"
    >
      <button
        type="button"
        class="tree-toggle"
        :class="{ 'is-leaf': !item.hasChildren }"
        :aria-label="item.isExpanded ? '접기' : '펼치기'"
        :tabindex="item.hasChildren ? 0 : -1"
        @click.stop="toggleNode(item)"
      >
        {{ item.hasChildren ? (item.isExpanded ? '−' : '+') : '' }}
      </button>
      <span class="tree-label">{{ item.node.label }}</span>
    </li>
  </ul>
</template>

<style scoped>
.tree-view {
  margin: 0;
  padding: 4px 0;
  list-style: none;
  font-size: 13px;
}

.tree-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 5px;
  padding-bottom: 5px;
  padding-right: 8px;
  border-bottom: 1px solid var(--ds-rule);
  color: var(--ds-ink);
  cursor: pointer;
  user-select: none;
  transition: background-color var(--ds-ease);
}

.tree-item:hover {
  background: var(--ds-pine-050);
}

.tree-item.is-selected {
  background: var(--ds-pine-100);
  color: var(--ds-pine-900);
  font-weight: 600;
  box-shadow: inset 3px 0 0 var(--ds-pine-700);
}

.tree-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 16px;
  height: 16px;
  padding: 0;
  font-family: inherit;
  font-size: 12px;
  line-height: 1;
  color: var(--ds-pine-700);
  background: var(--ds-surface);
  border: 1px solid var(--ds-rule-strong);
  border-radius: 2px;
  cursor: pointer;
  transition: border-color var(--ds-ease), color var(--ds-ease);
}

.tree-toggle:hover {
  border-color: var(--ds-pine-700);
}

.tree-toggle:focus-visible {
  outline: none;
  box-shadow: var(--ds-focus);
}

.tree-toggle.is-leaf {
  visibility: hidden;
}

.tree-label {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.tree-empty {
  padding: 16px;
  color: var(--ds-ink-3);
  text-align: center;
}
</style>
