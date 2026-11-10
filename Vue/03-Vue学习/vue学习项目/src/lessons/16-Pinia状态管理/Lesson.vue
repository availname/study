<template>
  <main id="pinia-lesson">
    <h1>Pinia 状态管理</h1>

    <StudyProgress />
    <StudyControls />
  </main>
</template>

<script>
import StudyProgress from './StudyProgress.vue'
import StudyControls from './StudyControls.vue'

export default {
  components: {
    StudyProgress,
    StudyControls,
  },
}
</script>
