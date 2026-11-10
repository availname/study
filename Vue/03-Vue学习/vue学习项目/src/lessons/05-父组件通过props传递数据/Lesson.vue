<template>
  <main id="props-lesson">
    <h1>{{ pageTitle }}</h1>
    <p>{{ lessonIntro }}</p>

    <StudentCard
      :name="firstStudent.name"
      :score="firstStudent.score"
    />
    <StudentCard
      :name="secondStudent.name"
      :score="secondStudent.score"
    />
  </main>
</template>

<script>
import StudentCard from './StudentCard.vue'

export default {
  components: {
    StudentCard,
  },
  data() {
    return {
      pageTitle: '父组件通过 props 向子组件传递数据',
      lessonIntro: '同一个 StudentCard 组件可以接收不同学生的数据。',
      firstStudent: {
        name: '小明',
        score: 86,
      },
      secondStudent: {
        name: '小红',
        score: 92,
      },
    }
  },
}
</script>
