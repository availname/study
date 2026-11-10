import { defineStore } from 'pinia'

export const useStudyStore = defineStore('study', {
  state: () => ({
    studentName: '小明',
    completedLessons: 15,
  }),

  getters: {
    progressText(state) {
      return '已经完成 ' + state.completedLessons + ' 节课'
    },
  },

  actions: {
    completeLesson() {
      this.completedLessons += 1
    },
  },
})
