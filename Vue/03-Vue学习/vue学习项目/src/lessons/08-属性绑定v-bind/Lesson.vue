<template>
  <main id="bind-lesson">
    <h1>{{ pageTitle }}</h1>
    <p :title="tipText">这是一个动态 title 属性。</p>

    <input
      :placeholder="placeholderText"
      :disabled="isDisabled"
    />
    <button @click="toggleDisabled">
      切换输入框状态
    </button>

    <p>输入框是否禁用：{{ isDisabled }}</p>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 属性绑定 v-bind',
      tipText: 'title 属性来自 Vue 数据',
      placeholderText: '请输入学习内容',
      isDisabled: false,
    }
  },
  methods: {
    toggleDisabled() {
      this.isDisabled = !this.isDisabled
    },
  },
}
</script>
