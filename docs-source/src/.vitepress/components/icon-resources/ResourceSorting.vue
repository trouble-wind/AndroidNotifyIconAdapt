<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { DownOutlined, SortAscendingOutlined, SortDescendingOutlined } from '@ant-design/icons-vue';
import type { IconResourceSortMode } from './utils/state';

type SortGroup = 'name' | 'added';

interface SortingText {
    sort: string;
    sortOptions: {
        added: string;
        ascending: string;
        descending: string;
        name: string;
    };
}

const props = defineProps<{
    modelValue: IconResourceSortMode;
    text: SortingText;
}>();

const emit = defineEmits<{
    'update:modelValue': [mode: IconResourceSortMode];
}>();

const sortGroups: SortGroup[] = ['name', 'added'];
const sortRoot = ref<HTMLElement>();
const menuOpen = ref(false);
const activeGroup = computed(() => props.modelValue.startsWith('name') ? 'name' : 'added');
const descending = computed(() => props.modelValue.endsWith('Desc'));
const resolveSortMode = (group: SortGroup, useDescending: boolean) => group === 'name'
    ? useDescending ? 'nameDesc' : 'nameAsc'
    : useDescending ? 'addedDesc' : 'addedAsc';
const toggleDirection = () => {
    emit('update:modelValue', resolveSortMode(activeGroup.value, !descending.value));
    menuOpen.value = false;
};
const selectSortGroup = (group: SortGroup) => {
    emit('update:modelValue', resolveSortMode(group, descending.value));
    menuOpen.value = false;
};
const closeMenuOnDocumentClick = (event: MouseEvent) => {
    if (!sortRoot.value?.contains(event.target as Node)) menuOpen.value = false;
};
const closeMenuOnEscape = (event: KeyboardEvent) => {
    if (event.key === 'Escape') menuOpen.value = false;
};

onMounted(() => {
    document.addEventListener('click', closeMenuOnDocumentClick);
    document.addEventListener('keydown', closeMenuOnEscape);
});
onBeforeUnmount(() => {
    document.removeEventListener('click', closeMenuOnDocumentClick);
    document.removeEventListener('keydown', closeMenuOnEscape);
});
</script>

<template>
    <div ref="sortRoot" class="sort-control">
        <button type="button" class="sort-trigger sort-direction-trigger"
            :title="`${text.sort}: ${text.sortOptions[descending ? 'descending' : 'ascending']}`"
            :aria-label="`${text.sort}: ${text.sortOptions[descending ? 'descending' : 'ascending']}`"
            @click="toggleDirection">
            <SortDescendingOutlined v-if="descending" aria-hidden="true" />
            <SortAscendingOutlined v-else aria-hidden="true" />
        </button>
        <button type="button" class="sort-trigger sort-field-trigger" :class="{ active: menuOpen }" aria-haspopup="menu"
            :aria-expanded="menuOpen" :title="`${text.sort}: ${text.sortOptions[activeGroup]}`"
            :aria-label="`${text.sort}: ${text.sortOptions[activeGroup]}`" @click="menuOpen = !menuOpen">
            <DownOutlined aria-hidden="true" />
        </button>
        <Transition name="sort-menu">
            <div v-if="menuOpen" class="sort-menu" role="menu">
                <button v-for="group in sortGroups" :key="group" type="button" role="menuitemradio"
                    :aria-checked="activeGroup === group" :class="{ active: activeGroup === group }"
                    @click="selectSortGroup(group)">
                    {{ text.sortOptions[group] }}
                </button>
            </div>
        </Transition>
    </div>
</template>

<style scoped lang="scss">
.sort-control {
    position: relative;
    display: flex;
    flex: 0 0 auto;
}

.sort-trigger {
    display: grid;
    height: 38px;
    margin: 0;
    padding: 0;
    border: 1px solid var(--vp-c-divider);
    place-items: center;
    background: var(--vp-c-bg-soft);
    color: var(--vp-c-text-2);
    cursor: pointer;

    :deep(.anticon) {
        font-size: 18px;
    }

    &:hover,
    &.active {
        position: relative;
        z-index: 1;
        border-color: var(--vp-c-brand-1);
        color: var(--vp-c-brand-1);
    }

    &.active {
        background: var(--vp-c-brand-soft);
    }
}

.sort-direction-trigger {
    width: 38px;
    border-radius: 9px 0 0 9px;
}

.sort-field-trigger {
    width: 30px;
    margin-left: -1px;
    border-radius: 0 9px 9px 0;

    :deep(.anticon) {
        font-size: 12px;
        transition: transform 0.15s ease;
    }

    &[aria-expanded='true'] :deep(.anticon) {
        transform: rotate(180deg);
    }
}

.sort-menu {
    position: absolute;
    z-index: 20;
    top: calc(100% + 8px);
    right: 0;
    display: grid;
    min-width: 130px;
    padding: 8px;
    border: 1px solid var(--vp-c-divider);
    border-radius: 10px;
    background: var(--vp-c-bg-elv);
    box-shadow: var(--vp-shadow-3);

    button {
        width: 100%;
        padding: 7px 10px;
        border: 0;
        border-radius: 6px;
        background: transparent;
        color: var(--vp-c-text-2);
        font: inherit;
        text-align: left;
        cursor: pointer;

        &:hover {
            background: var(--vp-c-bg-soft);
            color: var(--vp-c-text-1);
        }

        &.active {
            background: var(--vp-c-brand-soft);
            color: var(--vp-c-brand-1);
        }
    }
}

.sort-menu-enter-active,
.sort-menu-leave-active {
    transition: opacity 0.15s ease, transform 0.15s ease;
}

.sort-menu-enter-from,
.sort-menu-leave-to {
    opacity: 0;
    transform: translateY(-6px);
}
</style>