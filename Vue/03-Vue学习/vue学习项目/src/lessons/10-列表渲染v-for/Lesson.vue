<template>
  <main id="for-lesson">
    <h1>{{ pageTitle }}</h1>
    <ul>
      <li
        v-for="student in students"
        :key="student.id"
      >
        {{ student.name }}：{{ student.score }} 分
      </li>
    </ul>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 列表渲染 v-for',
      students: [
        { id: 1, name: '小明', score: 86 },
        { id: 2, name: '小红', score: 92 },
        { id: 3, name: '小刚', score: 78 },
      ],
    }
  },
}
</script>
