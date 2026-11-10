<template>
  <section>
    <h2>学习进度子组件</h2>
    <p>当前课程：{{ currentLesson }}</p>
    <p>学习状态：{{ studyStatus }}</p>
  </section>
</template>

<script>
export default {
  data() {
    return {
      currentLesson: '根组件与子组件',
      studyStatus: '正在学习',
    }
  },
}
</script>
