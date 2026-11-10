<template>
  <section>
    <h2>学习操作</h2>
    <button @click="studyStore.completeLesson">完成一课</button>
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
