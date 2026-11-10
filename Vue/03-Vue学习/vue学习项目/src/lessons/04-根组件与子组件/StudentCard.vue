<template>
  <section>
    <h2>学生信息子组件</h2>
    <p>姓名：{{ studentName }}</p>
    <p>成绩：{{ studentScore }}</p>
  </section>
</template>

<script>
export default {
  data() {
    return {
      studentName: '小明',
      studentScore: 86,
    }
  },
}
</script>
