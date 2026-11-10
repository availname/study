<template>
  <main id="watch-lesson">
    <h1>{{ pageTitle }}</h1>

    <p>当前成绩：{{ score }}</p>
    <p>{{ changeMessage }}</p>

    <button @click="improveScore">提高 5 分</button>
    <button @click="lowerScore">降低 5 分</button>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 监听器 watch',
      score: 88,
      changeMessage: '等待成绩发生变化',
    }
  },

  watch: {
    score(newValue, oldValue) {
      this.changeMessage = '成绩从 ' + oldValue + ' 分变为 ' + newValue + ' 分'
    },
  },

  methods: {
    improveScore() {
      this.score += 5
    },

    lowerScore() {
      this.score -= 5
    },
  },
}
</script>
