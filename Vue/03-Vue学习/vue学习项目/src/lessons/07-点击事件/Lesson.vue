<template>
  <main id="click-lesson">
    <h1>{{ pageTitle }}</h1>
    <p>当前点击次数：{{ count }}</p>

    <button @click="count++">直接增加 1</button>
    <button @click="increase">通过方法增加 1</button>
    <button @click="resetCount">重置次数</button>

    <p>最后操作：{{ lastAction }}</p>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 点击事件 @click',
      count: 0,
      lastAction: '还没有点击按钮',
    }
  },
  methods: {
    increase() {
      this.count++
      this.lastAction = '点击了通过方法增加按钮'
    },
    resetCount() {
      this.count = 0
      this.lastAction = '点击了重置按钮'
    },
  },
}
</script>
