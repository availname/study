<template>
  <main id="computed-lesson">
    <h1>{{ pageTitle }}</h1>

    <p>姓：{{ firstName }}</p>
    <p>名：{{ lastName }}</p>
    <p>完整姓名：{{ fullName }}</p>
    <p>成绩：{{ score }}</p>
    <p>成绩等级：{{ scoreLevel }}</p>

    <button @click="improveScore">提高 5 分</button>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 计算属性 computed',
      firstName: '张',
      lastName: '三',
      score: 88,
    }
  },

  computed: {
    fullName() {
      return this.firstName + this.lastName
    },

    scoreLevel() {
      if (this.score >= 90) {
        return '优秀'
      }

      if (this.score >= 60) {
        return '及格'
      }

      return '需要继续学习'
    },
  },

  methods: {
    improveScore() {
      this.score += 5
    },
  },
}
</script>
