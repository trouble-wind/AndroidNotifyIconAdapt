<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import type { CategoryId, CategorySpec } from '../../data/icon-resources';

interface NavigationText {
    categories: Record<CategoryId, string>;
    system: string;
}

const props = defineProps<{
    activeCategoryId: CategoryId;
    categoryCount: (id: CategoryId) => number;
    systemCategories: CategorySpec[];
    systemMenuOpen: boolean;
    systemButtonLabel: string;
    text: NavigationText;
}>();

const emit = defineEmits<{
    select: [id: CategoryId];
    'update:systemMenuOpen': [open: boolean];
}>();

const navigationRoot = ref<HTMLElement>();
const systemTrigger = ref<HTMLButtonElement>();
const systemMenuPosition = ref({
    left: '0px',
    maxWidth: 'calc(100vw - 16px)',
    top: '0px'
});
const activeSystemCategory = computed(() =>
    props.systemCategories.find((category) => category.id === props.activeCategoryId)
);
const updateSystemMenuPosition = () => {
    const trigger = systemTrigger.value;
    if (!trigger) return;
    const triggerBounds = trigger.getBoundingClientRect();
    const left = Math.max(8, Math.min(triggerBounds.left, window.innerWidth - 168));
    systemMenuPosition.value = {
        left: `${left}px`,
        maxWidth: `${window.innerWidth - left - 8}px`,
        top: `${triggerBounds.bottom + 8}px`
    };
};
const updateOpenSystemMenuPosition = () => {
    if (props.systemMenuOpen) updateSystemMenuPosition();
};
const toggleSystemMenu = () => {
    const open = !props.systemMenuOpen;
    if (open) updateSystemMenuPosition();
    emit('update:systemMenuOpen', open);
};
const closeSystemMenuOnDocumentClick = (event: MouseEvent) => {
    if (!navigationRoot.value?.contains(event.target as Node)) emit('update:systemMenuOpen', false);
};
const closeSystemMenuOnEscape = (event: KeyboardEvent) => {
    if (event.key === 'Escape') emit('update:systemMenuOpen', false);
};

onMounted(() => {
    document.addEventListener('click', closeSystemMenuOnDocumentClick);
    document.addEventListener('keydown', closeSystemMenuOnEscape);
    window.addEventListener('resize', updateOpenSystemMenuPosition);
    window.addEventListener('scroll', updateOpenSystemMenuPosition, true);
});
onBeforeUnmount(() => {
    document.removeEventListener('click', closeSystemMenuOnDocumentClick);
    document.removeEventListener('keydown', closeSystemMenuOnEscape);
    window.removeEventListener('resize', updateOpenSystemMenuPosition);
    window.removeEventListener('scroll', updateOpenSystemMenuPosition, true);
});
</script>

<template>
    <div ref="navigationRoot" class="category-tabs">
        <button type="button" class="category-tab" :aria-pressed="activeCategoryId === 'app'"
            :class="{ active: activeCategoryId === 'app' }" @click="emit('select', 'app')">
            {{ text.categories.app }}
            <span class="count">{{ categoryCount('app') }}</span>
        </button>
        <button type="button" class="category-tab" :aria-pressed="activeCategoryId === 'game'"
            :class="{ active: activeCategoryId === 'game' }" @click="emit('select', 'game')">
            {{ text.categories.game }}
            <span class="count">{{ categoryCount('game') }}</span>
        </button>
        <div class="system-menu-wrapper">
            <button ref="systemTrigger" type="button" class="system-trigger" :class="{ active: activeSystemCategory }"
                aria-haspopup="menu" :aria-expanded="systemMenuOpen" @click="toggleSystemMenu">
                {{ systemButtonLabel }}
                <span v-if="activeSystemCategory" class="count">{{ categoryCount(activeSystemCategory.id) }}</span>
                <span class="vpi-chevron-down system-chevron" aria-hidden="true" />
            </button>
            <Teleport to="body">
                <Transition name="system-menu">
                    <div v-if="systemMenuOpen" class="system-menu" role="menu" :style="systemMenuPosition">
                        <button v-for="category in systemCategories" :key="category.id" type="button"
                            role="menuitemradio" :aria-checked="activeCategoryId === category.id"
                            :class="{ active: activeCategoryId === category.id }" @click="emit('select', category.id)">
                            <span class="category-label">{{ text.categories[category.id] }}</span>
                            <span class="count">{{ categoryCount(category.id) }}</span>
                        </button>
                    </div>
                </Transition>
            </Teleport>
        </div>
    </div>
</template>

<style scoped lang="scss">
.category-tabs {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding-bottom: 6px;

    .category-tab,
    .system-trigger {
        display: inline-flex;
        align-items: center;
        flex: 0 0 auto;
        gap: 7px;
        min-height: 38px;
        padding: 0 13px;
        border: 1px solid var(--vp-c-divider);
        border-radius: 9px;
        background: var(--vp-c-bg-soft);
        color: var(--vp-c-text-2);
        font: inherit;
        cursor: pointer;

        .count {
            color: var(--vp-c-text-3);
            font-size: 12px;
        }

        &.active {
            border-color: var(--vp-c-brand-1);
            background: var(--vp-c-brand-soft);
            color: var(--vp-c-brand-1);
        }
    }
}

.system-menu-wrapper {
    position: relative;
    flex: 0 0 auto;
}

.system-chevron {
    color: var(--vp-c-text-3);
    font-size: 14px;
    transition: transform 0.2s ease;
}

.system-trigger[aria-expanded='true'] .system-chevron {
    transform: rotate(180deg);
}

.system-menu {
    position: fixed;
    z-index: 100;
    display: grid;
    min-width: 160px;
    padding: 8px;
    border: 1px solid var(--vp-c-divider);
    border-radius: 10px;
    background: var(--vp-c-bg-elv);
    box-shadow: var(--vp-shadow-3);

    button {
        display: grid;
        align-items: center;
        grid-template-columns: minmax(0, 1fr) auto;
        gap: 20px;
        min-width: 0;
        padding: 7px 10px;
        border: 0;
        border-radius: 6px;
        background: transparent;
        color: var(--vp-c-text-2);
        font: inherit;
        text-align: left;
        cursor: pointer;

        .category-label {
            overflow: hidden;
            color: inherit;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .count {
            color: var(--vp-c-text-3);
            font-size: 12px;
            white-space: nowrap;
        }

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

.system-menu-enter-active,
.system-menu-leave-active {
    transition: opacity 0.15s ease, transform 0.15s ease;
}

.system-menu-enter-from,
.system-menu-leave-to {
    opacity: 0;
    transform: translateY(-6px);
}
</style>