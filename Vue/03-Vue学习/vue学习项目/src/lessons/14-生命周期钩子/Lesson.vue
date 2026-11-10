<template>
  <main id="lifecycle-lesson">
    <h1>{{ pageTitle }}</h1>

    <p>当前数量：{{ count }}</p>
    <p>{{ lifecycleMessage }}</p>

    <button @click="increaseCount">增加数量</button>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 生命周期钩子',
      count: 0,
      lifecycleMessage: '组件正在创建',
    }
  },

  created() {
    this.lifecycleMessage = 'created：数据已经准备好'
    console.log('created：组件已经创建')
  },

  mounted() {
    this.lifecycleMessage = 'mounted：组件已经显示到页面'
    console.log('mounted：组件已经显示到页面')
  },

  updated() {
    console.log('updated：页面已经更新')
  },

  unmounted() {
    console.log('unmounted：组件已经被移除')
  },

  methods: {
    increaseCount() {
      this.count += 1
    },
  },
}
</script>
