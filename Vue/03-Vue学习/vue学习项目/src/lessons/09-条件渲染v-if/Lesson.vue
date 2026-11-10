<template>
  <main id="if-lesson">
    <h1>{{ pageTitle }}</h1>
    <button @click="toggleVisible">切换内容显示</button>

    <p v-if="isVisible">这段内容当前正在显示。</p>
    <p v-else>这段内容当前已经隐藏。</p>

    <h2>成绩判断</h2>
    <p v-if="score >= 90">成绩优秀</p>
    <p v-else-if="score >= 60">成绩及格</p>
    <p v-else>还需要继续学习</p>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 条件渲染 v-if',
      isVisible: true,
      score: 86,
    }
  },
  methods: {
    toggleVisible() {
      this.isVisible = !this.isVisible
    },
  },
}
</script>
