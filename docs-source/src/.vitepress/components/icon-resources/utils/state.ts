import { ref } from 'vue';
import type { CategoryId, LoadedCategory } from '../../../data/icon-resources';

/** Sort modes supported by the icon resources view. */
export type IconResourceSortMode = 'nameAsc' | 'nameDesc' | 'addedAsc' | 'addedDesc';

/** Keeps the icon resources view state while VitePress swaps a localized page. */
export const iconResourcesState = {
    activeCategoryId: ref<CategoryId>('app'),
    categories: ref<LoadedCategory[]>([]),
    fatalError: ref(''),
    initialized: ref(false),
    loading: ref(true),
    query: ref(''),
    sortMode: ref<IconResourceSortMode>('nameAsc')
};

/** Clears view state when the visitor leaves the icon resources page. */
export const resetIconResourcesState = () => {
    iconResourcesState.activeCategoryId.value = 'app';
    iconResourcesState.categories.value = [];
    iconResourcesState.fatalError.value = '';
    iconResourcesState.initialized.value = false;
    iconResourcesState.loading.value = true;
    iconResourcesState.query.value = '';
    iconResourcesState.sortMode.value = 'nameAsc';
};