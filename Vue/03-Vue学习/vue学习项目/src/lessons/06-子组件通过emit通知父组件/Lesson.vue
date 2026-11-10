<template>
  <main id="emit-lesson">
    <h1>{{ pageTitle }}</h1>
    <StudentCard
      :name="studentName"
      @study-complete="handleStudyComplete"
    />
    <p>{{ studyMessage }}</p>
  </main>
</template>

<script>
import StudentCard from './StudentCard.vue'

export default {
  components: {
    StudentCard,
  },
  data() {
    return {
      pageTitle: '子组件通知父组件',
      studentName: '小明',
      studyMessage: '等待子组件通知',
    }
  },
  methods: {
    handleStudyComplete(message) {
      this.studyMessage = message
    },
  },
}
</script>
