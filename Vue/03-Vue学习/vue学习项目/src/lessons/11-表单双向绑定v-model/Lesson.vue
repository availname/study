<template>
  <main id="model-lesson">
    <h1>{{ pageTitle }}</h1>

    <label>
      学生姓名：
      <input v-model="studentName" />
    </label>
    <p>当前姓名：{{ studentName }}</p>

    <label>
      <input type="checkbox" v-model="hasAgreed" />
      我同意学习规则
    </label>
    <p>是否同意：{{ hasAgreed }}</p>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 表单双向绑定 v-model',
      studentName: '小明',
      hasAgreed: false,
    }
  },
}
</script>
