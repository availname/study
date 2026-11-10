<template>
  <section>
    <h2>学生信息</h2>
    <p>姓名：{{ name }}</p>
    <button @click="notifyParent">告诉父组件我完成了</button>
  </section>
</template>

<script>
export default {
  props: ['name'],
  emits: ['study-complete'],
  methods: {
    notifyParent() {
      this.$emit('study-complete', '子组件报告：学习完成')
    },
  },
}
</script>
