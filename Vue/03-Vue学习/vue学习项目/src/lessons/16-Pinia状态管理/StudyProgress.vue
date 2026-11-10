<template>
  <section>
    <h2>学习进度</h2>
    <p>学生：{{ studyStore.studentName }}</p>
    <p>{{ studyStore.progressText }}</p>
  </section>
</template>

<script>
import { useStudyStore } from './stores/study.js'

export default {
  setup() {
    return {
      studyStore: useStudyStore(),
    }
  },
}
</script>
