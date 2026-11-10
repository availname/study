<template>
  <main id="component-composition-lesson">
    <h1>{{ pageTitle }}</h1>
    <p>{{ lessonIntro }}</p>

    <StudentCard />
    <StudyProgress />
  </main>
</template>

<script>
import StudentCard from './StudentCard.vue'
import StudyProgress from './StudyProgress.vue'

export default {
  components: {
    StudentCard,
    StudyProgress,
  },
  data() {
    return {
      pageTitle: '根组件组合子组件',
      lessonIntro: 'Lesson.vue 负责组合页面，子组件负责自己的内容。',
    }
  },
}
</script>
