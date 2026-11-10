<template>
  <div>
    <!-- 模板中的名称会读取 data() 返回的数据。 -->
    <h1>{{ pageTitle }}</h1>
    <p>你好，{{ studentName }}！</p>
    <p>当前学习内容：{{ currentLesson }}</p>
    <p>学习次数：{{ studyCount }}</p>
  </div>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 模板语法第 2 课',
      studentName: '学习者',
      currentLesson: '插值表达式',
      studyCount: 1,
    }
  },
}
</script>
