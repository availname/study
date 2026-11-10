<template>
  <section>
    <h2>学生信息</h2>
    <p>姓名：{{ name }}</p>
    <p>成绩：{{ score }}</p>
  </section>
</template>

<script>
export default {
  props: ['name', 'score'],
}
</script>
