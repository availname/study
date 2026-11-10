<template>
  <main id="page">
    <h1 id="page-title">{{ pageTitle }}</h1>
    <p id="student-name">姓名：{{ studentName }}</p>
    <p id="student-score">成绩：{{ studentScore }}</p>
    <p id="lesson-name">课程：{{ lessonName }}</p>
    <p id="study-status">状态：{{ studyStatus }}</p>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: '学生学习信息',
      studentName: '小明',
      studentScore: 86,
      lessonName: 'Vue 模板语法',
      studyStatus: '正在学习',
    }
  },
}
</script>
