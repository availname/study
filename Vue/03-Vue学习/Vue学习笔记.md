# Vue 学习笔记

这份笔记记录当前 Vue 学习项目中的知识、语法、运行过程和案例。

当前学习方式：一个 Vite 项目，多个课程文件。每一课只切换 `src/App.vue` 导入的课程组件，不重新创建项目，也不重复安装依赖。

## 学习状态

| 状态 | 内容 |
| --- | --- |
| 已完成 | 第 1 课：Vue、Vite 和项目启动 |
| 已完成 | 第 2 课：插值表达式 `{{ }}` |
| 已完成 | 第 3 课：多个标签与 `data()` 数据管理 |
| 已完成 | 第 4 课：根组件组合子组件 |
| 已完成 | 第 5 课：父组件通过 `props` 向子组件传递数据 |
| 已完成 | 第 6 课：子组件通过 `emit` 通知父组件 |
| 已完成 | 第 7 课：Vue 点击事件 `@click` |
| 已完成 | 第 8 课：Vue 属性绑定 `v-bind` |
| 已完成 | 第 9 课：条件渲染 `v-if` |
| 已完成 | 第 10 课：列表渲染 `v-for` |
| 已完成 | 第 11 课：表单双向绑定 `v-model` |
| 已完成 | 第 12 课：计算属性 `computed` |
| 已完成 | 第 13 课：监听器 `watch` |
| 已完成 | 第 14 课：生命周期钩子 |
| 已完成 | 第 15 课：Vue Router 路由 |
| 正在学习 | 第 16 课：Pinia 状态管理 |
| 下一课 | Vue 使用 `fetch` 加载数据 |

## 一、学习项目基础

### 1. Vue 是什么

Vue 是一个用于构建网页用户界面的 JavaScript 框架。

Vue 主要帮助我们管理：

- 页面结构
- 页面数据
- 用户操作
- 组件之间的关系

Vue 的核心思路是：

```text
数据 → 模板 → 页面
事件 → 修改数据 → 页面自动更新
```

### 2. Vite 是什么

Vite 是 Vue 项目的开发工具，不是 Vue 语法本身。

Vite 负责：

- 启动开发服务器
- 处理 `.vue` 文件
- 处理 `import` 和 `export`
- 编译 TypeScript
- 保存代码后自动更新页面
- 构建用于发布的文件

可以这样区分：

```text
Vue 负责页面的写法和功能
Vite 负责项目的启动、编译和开发环境
```

### 3. 当前项目结构

```text
vue学习项目/
├─ package.json       项目依赖和命令
├─ node_modules/      已安装的依赖
├─ index.html         浏览器入口
└─ src/
   ├─ main.js         Vue 启动入口
   ├─ App.vue         项目根组件
   └─ lessons/        每个知识点的课程文件
```

当前课程文件：

```text
src/lessons/
├─ 01-Vue是什么与Vite项目/Lesson.vue
├─ 02-插值表达式/Lesson.vue
├─ 03-多个标签与数据管理/Lesson.vue
├─ 04-根组件与子组件/
│  ├─ Lesson.vue
│  ├─ StudentCard.vue
│  └─ StudyProgress.vue
└─ 05-父组件通过props传递数据/
   ├─ Lesson.vue
   └─ StudentCard.vue
```

### 4. 常用命令

在 `vue学习项目/` 目录执行：

```bash
npm install
npm run dev
npm run build
```

- `npm install`：根据 `package.json` 安装依赖。
- `npm run dev`：启动开发服务器。
- `npm run build`：把项目编译成发布文件。

### 5. 多个知识点共用一个项目

学习不同知识点时，不需要每次重新创建：

```text
package.json
node_modules/
```

只需要在 `src/lessons/` 下增加课程文件，然后修改根组件的导入：

```javascript
import Lesson from './lessons/05-父组件通过props传递数据/Lesson.vue'
```

这样可以保持一个项目、一份依赖和多份课程示例。

## 二、Vue 项目的启动机制

### 1. 入口链路

```text
index.html
    ↓ 加载
src/main.js
    ↓ 导入
src/App.vue
    ↓ 挂载
页面上的 #app
```

### 2. `index.html`

```html
<div id="app"></div>
<script type="module" src="/src/main.js"></script>
```

```html
<div id="app"></div>
```

这是 Vue 的挂载位置。它开始时是一个空容器。

```html
<script type="module" src="/src/main.js"></script>
```

这句告诉浏览器加载 `main.js`。

### 3. `type="module"`

`type` 是 HTML `<script>` 标签的属性。

```html
type="module"
```

表示按照 JavaScript 模块处理这个文件，因此可以使用：

```javascript
import App from './App.vue'
export default {}
```

它不是专门表示 Vue，也不是专门表示 TypeScript。

```text
type="module" → 使用 JavaScript 模块语法
main.ts       → 文件使用 TypeScript
Vite          → 编译 TypeScript 和 Vue 文件
```

### 4. `main.js`

普通 Vue 项目的启动文件：

```javascript
import { createApp } from 'vue'
import App from './App.vue'

const app = createApp(App)
app.mount('#app')
```

逐句理解：

```javascript
import { createApp } from 'vue'
```

从 Vue 中导入创建应用的方法。

```javascript
import App from './App.vue'
```

导入项目根组件 `App.vue`。

```javascript
createApp(App)
```

使用 `App.vue` 创建 Vue 应用。此时只是创建，还没有放到页面中。

```javascript
.mount('#app')
```

把 Vue 应用挂载到 `index.html` 中的 `#app` 元素。

完整含义：

```text
使用 App.vue 创建 Vue 应用
把这个应用放到 #app 元素中
```

### 5. 根组件是什么

`App.vue` 是整个 Vue 应用的根组件。

```text
main.js
  ↓
App.vue
  ↓
其他子组件
```

根组件不是每个页面都要重新创建的应用。普通单页面项目通常只创建一次：

```javascript
createApp(App).mount('#app')
```

子组件不需要自己调用：

```javascript
createApp()
```

也不需要自己调用：

```javascript
mount()
```

### 6. 完整案例

```vue
<!-- App.vue -->
<template>
  <main>
    <h1>Vue 页面</h1>
  </main>
</template>

<script>
export default {}
</script>
```

[第 1 课源文件](./vue学习项目/src/lessons/01-Vue是什么与Vite项目/Lesson.vue)

## 三、模板与数据

### 第 2 课：插值表达式 `{{ }}`

#### 这是什么

插值表达式是 Vue 模板中显示数据的写法：

```vue
{{ message }}
```

它不是普通 HTML 标签，也不是普通 JavaScript 对象，而是 Vue 模板语法。

#### 作用

- 把 JavaScript 数据显示到 HTML 模板中。
- 数据变化后让 Vue 自动更新页面。
- 减少手动查找 DOM 和修改文字的代码。

#### 解决的问题

原生 JavaScript 需要手动修改 DOM：

```javascript
document.querySelector('#message').textContent = message
```

Vue 可以直接写：

```vue
<p>{{ message }}</p>
```

#### 语法

```vue
<p>{{ message }}</p>
```

- `<p>`：HTML 段落标签。
- `{{`：插值表达式开始。
- `message`：要读取的数据名称。
- `}}`：插值表达式结束。

数据需要在组件中提供：

```javascript
export default {
  data() {
    return {
      message: 'Hello Vue',
    }
  },
}
```

模板中的名字必须和数据名称对应：

```text
{{ message }}  ↔  message: 'Hello Vue'
```

#### 完整案例

[第 2 课源文件](./vue学习项目/src/lessons/02-插值表达式/Lesson.vue)

### 第 3 课：多个标签与 `data()` 数据管理

#### 这是什么

一个 Vue 组件可以管理很多 HTML 标签，每个标签可以显示 `data()` 中不同的数据。

```vue
<h1>{{ pageTitle }}</h1>
<p>姓名：{{ studentName }}</p>
<p>成绩：{{ studentScore }}</p>
```

#### 作用

- 一个页面显示多个数据。
- 让数据集中放在组件的 `data()` 中。
- 让模板负责页面结构和数据显示。

#### `id` 和数据的区别

```vue
<p id="student-name">姓名：{{ studentName }}</p>
```

这句话包含两件事：

```text
id="student-name" → 这个 HTML 标签是谁
{{ studentName }}  → 这个标签显示什么数据
```

`id` 不负责提供数据，插值表达式才负责读取数据。

#### 语法

```vue
<template>
  <main id="page">
    <h1 id="page-title">{{ pageTitle }}</h1>
    <p id="student-name">姓名：{{ studentName }}</p>
    <p id="student-score">成绩：{{ studentScore }}</p>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: '学生学习信息',
      studentName: '小明',
      studentScore: 86,
    }
  },
}
</script>
```

#### 完整案例

[第 3 课源文件](./vue学习项目/src/lessons/03-多个标签与数据管理/Lesson.vue)

## 四、组件

### 第 4 课：根组件组合子组件

#### 这是什么

Vue 可以把页面拆成多个组件，再由父组件组合子组件。

本课的组件关系：

```text
main.js
  ↓
App.vue                 项目根组件
  ↓
Lesson.vue              当前课程的父组件
  ↓
StudentCard.vue         学生信息子组件
StudyProgress.vue       学习进度子组件
```

要区分：

- `App.vue` 是整个 Vue 应用的根组件。
- `Lesson.vue` 是 `App.vue` 使用的子组件。
- 在本课中，`Lesson.vue` 又是 `StudentCard.vue` 和 `StudyProgress.vue` 的父组件。

#### 作用

- 拆分复杂页面。
- 让每个组件负责相对独立的内容。
- 让组件可以重复使用。
- 让父组件负责页面组合，子组件负责自己的内容。

#### 解决的问题

如果所有内容都放在 `App.vue` 中，文件会越来越长。

拆分后：

```text
页面外壳       → App.vue
课程内容       → Lesson.vue
学生信息       → StudentCard.vue
学习进度       → StudyProgress.vue
```

#### 导入子组件

```javascript
import StudentCard from './StudentCard.vue'
import StudyProgress from './StudyProgress.vue'
```

`./` 表示当前文件所在目录。

#### 注册子组件

```javascript
export default {
  components: {
    StudentCard,
    StudyProgress,
  },
}
```

`components` 是 Vue 的组件配置项，表示当前组件要使用哪些子组件。

这段简写等同于：

```javascript
components: {
  StudentCard: StudentCard,
  StudyProgress: StudyProgress,
}
```

#### 使用子组件

```vue
<template>
  <main>
    <StudentCard />
    <StudyProgress />
  </main>
</template>
```

`<StudentCard />` 看起来像 HTML 标签，但它实际对应 `StudentCard.vue` 文件。

#### `export default` 和 `import`

```javascript
export default {}
```

表示把当前组件作为默认内容导出。

```javascript
import Lesson from './Lesson.vue'
```

表示导入默认导出的组件。

```text
export default → 默认导出
import Name    → 导入默认内容
```

`export default` 是 JavaScript 模块语法，不是 TypeScript 专用语法，也不是 Power 平台专用语法。

#### 每个组件可以有自己的数据

```javascript
export default {
  data() {
    return {
      studentName: '小明',
    }
  },
}
```

这里的 `studentName` 属于当前组件，不会自动变成父组件的数据。

#### 完整案例

[第 4 课父组件](./vue学习项目/src/lessons/04-根组件与子组件/Lesson.vue)

[第 4 课学生信息子组件](./vue学习项目/src/lessons/04-根组件与子组件/StudentCard.vue)

[第 4 课学习进度子组件](./vue学习项目/src/lessons/04-根组件与子组件/StudyProgress.vue)

### 第 5 课：父组件通过 `props` 向子组件传递数据

#### 这是什么

`props` 是父组件向子组件传递数据的方式。

```text
父组件 data()
    ↓ props
子组件 props
    ↓
子组件模板
```

#### 作用

- 让子组件显示父组件提供的数据。
- 让同一个子组件接收不同数据。
- 让父组件负责数据，子组件负责显示。

#### 解决的问题

如果子组件把内容写死：

```vue
<p>姓名：小明</p>
```

它只能显示小明的信息。

使用 `props` 后，同一个子组件可以显示不同学生：

```vue
<StudentCard :name="firstStudent.name" :score="firstStudent.score" />
<StudentCard :name="secondStudent.name" :score="secondStudent.score" />
```

#### 父组件语法

```vue
<script>
import StudentCard from './StudentCard.vue'

export default {
  components: {
    StudentCard,
  },
  data() {
    return {
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

<template>
  <StudentCard
    :name="firstStudent.name"
    :score="firstStudent.score"
  />
  <StudentCard
    :name="secondStudent.name"
    :score="secondStudent.score"
  />
</template>
```

#### `:` 是什么

```vue
:name="firstStudent.name"
```

可以拆成：

- `:`：`v-bind` 的简写，表示动态绑定。
- `name`：传给子组件的 prop 名称。
- `firstStudent.name`：父组件中的数据表达式。

如果不写冒号：

```vue
<StudentCard name="studentName" />
```

传递的是普通字符串 `studentName`。

写冒号：

```vue
<StudentCard :name="studentName" />
```

传递的是变量 `studentName` 的值。

#### 子组件接收数据

```vue
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
```

```javascript
props: ['name', 'score']
```

表示子组件声明自己可以接收 `name` 和 `score` 两个数据。

接收后，子组件模板可以直接使用：

```vue
{{ name }}
{{ score }}
```

#### 数据方向

```text
父组件 → 子组件
```

这是单向数据流。子组件可以读取 `props`，但不能直接修改父组件传来的 `props`。

#### 完整案例

[第 5 课父组件](./vue学习项目/src/lessons/05-父组件通过props传递数据/Lesson.vue)

[第 5 课子组件](./vue学习项目/src/lessons/05-父组件通过props传递数据/StudentCard.vue)

### 第 6 课：子组件通过 `emit` 通知父组件

#### 这是什么

`emit` 是子组件向父组件发送自定义事件的方式。

```text
父组件通过 props 传数据给子组件
子组件通过 emit 通知父组件
```

#### 有什么作用

- 让子组件把用户操作通知给父组件。
- 让父组件决定收到通知后如何修改自己的数据。
- 保持父子组件之间清晰的数据方向。

#### 解决了什么问题

子组件不能直接修改父组件的数据。例如，父组件有：

```javascript
studyMessage: '等待通知'
```

子组件点击按钮后，不能直接写：

```javascript
this.studyMessage = '学习完成'
```

因为 `studyMessage` 属于父组件。

正确方式是：

```text
子组件发出事件
    ↓
父组件监听事件
    ↓
父组件修改自己的数据
```

#### 父组件语法

```vue
<template>
  <main>
    <h1>{{ pageTitle }}</h1>
    <StudentCard
      :name="studentName"
      @study-complete="handleStudyComplete"
    />
    <p>{{ studyMessage }}</p>
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
      pageTitle: '子组件通知父组件',
      studentName: '小明',
      studyMessage: '等待子组件通知',
    }
  },
  methods: {
    handleStudyComplete(message) {
      this.studyMessage = message
    },
  },
}
</script>
```

```vue
@study-complete="handleStudyComplete"
```

可以拆成：

- `@`：监听事件的简写。
- `study-complete`：事件名称。
- `handleStudyComplete`：父组件收到事件后执行的方法。

#### 子组件语法

```vue
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
```

```javascript
emits: ['study-complete']
```

表示当前子组件会发出一个叫 `study-complete` 的自定义事件。

```javascript
this.$emit('study-complete', '子组件报告：学习完成')
```

逐部分理解：

- `this.$emit`：Vue 提供的发出事件的方法。
- `'study-complete'`：事件名称，必须和父组件监听的名称对应。
- `'子组件报告：学习完成'`：传给父组件的数据，也叫事件参数或负载。

父组件的方法接收这个参数：

```javascript
handleStudyComplete(message) {
  this.studyMessage = message
}
```

#### 完整运行过程

```text
用户点击子组件按钮
    ↓
执行 notifyParent()
    ↓
this.$emit('study-complete', message)
    ↓
父组件的 @study-complete 被触发
    ↓
执行 handleStudyComplete(message)
    ↓
父组件修改 studyMessage
    ↓
页面自动更新
```

#### 数据方向

```text
父组件 ──props──> 子组件
父组件 <─emit──── 子组件
```

`props` 负责传数据，`emit` 负责发消息。两者一起构成 Vue 基础的父子通信。

#### 完整案例

[第 6 课父组件](./vue学习项目/src/lessons/06-子组件通过emit通知父组件/Lesson.vue)

[第 6 课子组件](./vue学习项目/src/lessons/06-子组件通过emit通知父组件/StudentCard.vue)

本课只学习子组件发出事件，下一课专门学习 Vue 的点击事件 `@click`。

### 第 7 课：Vue 点击事件 `@click`

#### 这是什么

`@click` 是 Vue 监听鼠标点击事件的语法。

```vue
<button @click="count++">点击次数：{{ count }}</button>
```

用户点击按钮时，Vue 会执行 `@click` 后面的代码。

#### 有什么作用

- 监听按钮、链接等元素的点击。
- 点击后修改组件数据。
- 点击后调用 `methods` 中的方法。
- 让页面交互不需要手动查找 DOM。

#### 解决了什么问题

原生 JavaScript 需要这样监听点击：

```javascript
button.addEventListener('click', () => {
  count++
})
```

Vue 可以直接在模板中声明：

```vue
<button @click="count++">点击次数：{{ count }}</button>
```

Vue 会负责找到这个按钮并监听点击事件。

#### `@click` 和 `v-on:click`

```vue
@click="count++"
```

是下面写法的简写：

```vue
v-on:click="count++"
```

其中：

- `v-on`：Vue 的事件监听指令。
- `click`：浏览器的点击事件名称。
- `@`：`v-on:` 的简写。
- `count++`：点击后要执行的 JavaScript 表达式。

#### 直接执行表达式

```vue
<button @click="count++">增加</button>
<button @click="count = 0">重置</button>
```

组件数据：

```javascript
data() {
  return {
    count: 0,
  }
}
```

点击第一个按钮时：

```text
count：0 → 1 → 2 → 3
```

点击第二个按钮时：

```text
count：当前值 → 0
```

#### 点击后调用方法

模板：

```vue
<button @click="increase">调用方法增加</button>
<button @click="resetCount">调用方法重置</button>
```

JavaScript：

```javascript
methods: {
  increase() {
    this.count++
    this.lastAction = '点击了增加按钮'
  },
  resetCount() {
    this.count = 0
    this.lastAction = '点击了重置按钮'
  },
}
```

```text
@click="increase"
    ↓
点击时执行 methods 中的 increase()
```

在模板中写方法名时，Vue 会在点击发生时调用这个方法。

#### 事件和数据更新流程

```text
用户点击按钮
    ↓
Vue 监听到 click
    ↓
执行表达式或 methods 方法
    ↓
修改 data() 中的数据
    ↓
插值表达式自动更新页面
```

#### 完整案例

[第 7 课源文件](./vue学习项目/src/lessons/07-点击事件/Lesson.vue)

本课只学习 Vue 的点击事件，下一课学习 Vue 的属性绑定 `v-bind`。

### 第 8 课：Vue 属性绑定 `v-bind`

#### 这是什么

`v-bind` 用来把 Vue 中的数据绑定到 HTML 元素的属性上。

```vue
<input :placeholder="placeholderText" />
```

这里的 `placeholder` 属性值来自 Vue 数据 `placeholderText`。

#### 有什么作用

- 动态设置 HTML 属性。
- 根据数据改变输入框提示、按钮状态、链接地址等。
- 让 HTML 属性跟着 Vue 数据自动更新。

#### 解决什么问题

普通 HTML 的属性通常是固定的：

```html
<input placeholder="请输入内容" />
```

如果提示内容来自 Vue 数据，可以写：

```vue
<input :placeholder="placeholderText" />
```

当 `placeholderText` 改变时，HTML 的 `placeholder` 属性也会更新。

#### 完整语法

```vue
<元素 v-bind:属性名="数据或表达式" />
```

例如：

```vue
<input v-bind:placeholder="placeholderText" />
```

#### 简写语法

```vue
<input :placeholder="placeholderText" />
```

冒号 `:` 是 `v-bind:` 的简写：

```text
:placeholder="placeholderText"
      ↓
v-bind:placeholder="placeholderText"
```

#### 绑定不同属性

```vue
<input :placeholder="placeholderText" />
<input :disabled="isDisabled" />
<p :title="tipText">把鼠标放到这里</p>
<div :id="elementId">动态 id</div>
```

左边是 HTML 属性，右边是 Vue 中的数据或表达式。

#### 布尔属性

```vue
<button :disabled="isDisabled">提交</button>
```

组件数据：

```javascript
data() {
  return {
    isDisabled: false,
  }
}
```

- `false`：按钮可以使用。
- `true`：按钮被禁用。

这里必须使用动态绑定：

```vue
:disabled="isDisabled"
```

如果写成：

```vue
disabled="isDisabled"
```

那只是一个普通字符串属性，不会读取 `isDisabled` 变量。

#### 和普通属性的区别

固定属性：

```html
<input placeholder="请输入内容" />
```

动态属性：

```vue
<input :placeholder="placeholderText" />
```

```text
没有冒号 → 属性值是普通文字
有冒号   → 属性值是 Vue 表达式
```

#### 完整案例

```vue
<template>
  <main>
    <h1>{{ pageTitle }}</h1>
    <p :title="tipText">这是一个动态 title 属性。</p>
    <input
      :placeholder="placeholderText"
      :disabled="isDisabled"
    />
    <button @click="toggleDisabled">
      切换输入框状态
    </button>
    <p>输入框是否禁用：{{ isDisabled }}</p>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 属性绑定 v-bind',
      tipText: 'title 属性来自 Vue 数据',
      placeholderText: '请输入学习内容',
      isDisabled: false,
    }
  },
  methods: {
    toggleDisabled() {
      this.isDisabled = !this.isDisabled
    },
  },
}
</script>
```

这个案例中：

```vue
:placeholder="placeholderText"
```

把数据绑定到 `placeholder`。

```vue
:disabled="isDisabled"
```

把数据绑定到 `disabled`。

点击按钮后，`isDisabled` 改变，输入框状态也跟着改变。

#### 和 `v-model` 的区别

```vue
<input :value="inputValue" />
```

这是单向绑定：数据传给输入框。

```vue
<input v-model="inputValue" />
```

这是双向绑定：输入框变化时，数据也会变化。

上一课只学习 `v-bind`，本课开始学习 `v-model`。

[第 8 课源文件](./vue学习项目/src/lessons/08-属性绑定v-bind/Lesson.vue)

本课只学习属性绑定，下一课学习条件渲染 `v-if`。

### 第 9 课：条件渲染 `v-if`

#### 这是什么

`v-if` 是 Vue 根据条件决定是否显示元素的指令。

```vue
<p v-if="isVisible">这段内容可以显示或隐藏</p>
```

当 `isVisible` 为 `true` 时，元素会显示；当它为 `false` 时，元素不会被渲染到页面中。

#### 有什么作用

- 根据数据决定页面内容是否显示。
- 显示登录状态对应的内容。
- 显示加载中、成功或失败的不同提示。
- 根据权限决定是否显示按钮。

#### 解决什么问题

如果使用原生 JavaScript，需要手动查找元素并修改 DOM：

```javascript
element.style.display = 'none'
```

Vue 可以直接在模板中描述条件：

```vue
<p v-if="isVisible">欢迎回来</p>
```

数据改变后，Vue 会自动决定这个元素是否存在。

#### 基本语法

```vue
<元素 v-if="条件表达式">内容</元素>
```

例如：

```vue
<p v-if="score >= 60">考试通过</p>
```

`score >= 60` 是 JavaScript 条件表达式，结果是 `true` 或 `false`。

#### `v-if` 和数据

```javascript
data() {
  return {
    isVisible: true,
  }
}
```

```vue
<p v-if="isVisible">学习内容</p>
```

当数据变化：

```javascript
this.isVisible = false
```

Vue 会移除这段内容对应的 DOM。

再次改为：

```javascript
this.isVisible = true
```

Vue 会重新创建并显示这段内容。

#### `v-else`

`v-else` 表示条件不成立时显示的内容：

```vue
<p v-if="isVisible">内容正在显示</p>
<p v-else>内容已经隐藏</p>
```

`v-else` 必须紧跟在 `v-if` 或 `v-else-if` 后面。

#### `v-else-if`

多个条件可以使用 `v-else-if`：

```vue
<p v-if="score >= 90">优秀</p>
<p v-else-if="score >= 60">及格</p>
<p v-else>不及格</p>
```

Vue 会从上到下判断，显示第一个成立的条件。

#### 完整案例

```vue
<template>
  <main>
    <h1>{{ pageTitle }}</h1>
    <button @click="toggleVisible">切换内容显示</button>

    <p v-if="isVisible">这段内容当前正在显示。</p>
    <p v-else>这段内容当前已经隐藏。</p>

    <h2>成绩判断</h2>
    <p v-if="score >= 90">成绩优秀</p>
    <p v-else-if="score >= 60">成绩及格</p>
    <p v-else>还需要继续学习</p>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 条件渲染 v-if',
      isVisible: true,
      score: 86,
    }
  },
  methods: {
    toggleVisible() {
      this.isVisible = !this.isVisible
    },
  },
}
</script>
```

#### `v-if` 的运行过程

```text
读取 isVisible
    ↓
判断 true 或 false
    ↓
决定是否创建元素
    ↓
数据改变后重新判断
```

#### 和 `v-show` 的区别

```vue
<p v-if="isVisible">内容</p>
<p v-show="isVisible">内容</p>
```

- `v-if`：条件为假时，元素不会存在于 DOM 中。
- `v-show`：元素仍然存在，只是通过 CSS 隐藏。

当前先学习 `v-if`，`v-show` 后面再单独比较。

[第 9 课源文件](./vue学习项目/src/lessons/09-条件渲染v-if/Lesson.vue)

本课只学习条件渲染，下一课学习列表渲染 `v-for`。

### 第 10 课：列表渲染 `v-for`

#### 这是什么

`v-for` 是 Vue 根据数组或对象重复生成页面元素的指令。

```vue
<li v-for="student in students" :key="student.id">
  {{ student.name }}
</li>
```

数组中有几条数据，Vue 就可以生成几个 `<li>`。

#### 有什么作用

- 把数组数据显示成列表。
- 避免手动重复书写相同的 HTML。
- 当数组数据变化时，让页面列表自动更新。

#### 解决什么问题

如果有三个学生，原生 HTML 可能要重复写三次：

```html
<li>小明</li>
<li>小红</li>
<li>小刚</li>
```

使用 `v-for` 后，只需要准备数组：

```javascript
students: [
  { id: 1, name: '小明', score: 86 },
  { id: 2, name: '小红', score: 92 },
  { id: 3, name: '小刚', score: 78 },
]
```

然后循环生成页面。

#### 基本语法

```vue
<元素 v-for="临时变量 in 数组" :key="唯一值">
  {{ 临时变量 }}
</元素>
```

例如：

```vue
<li v-for="student in students" :key="student.id">
  {{ student.name }}
</li>
```

逐部分理解：

- `student`：每次循环得到的一条数据。
- `in`：表示从数组中逐条取数据。
- `students`：组件中的数组。
- `:key="student.id"`：给每一条循环数据一个唯一标识。

#### 数组数据

```javascript
data() {
  return {
    students: [
      { id: 1, name: '小明', score: 86 },
      { id: 2, name: '小红', score: 92 },
    ],
  }
}
```

数组中的每一项都是一个对象：

```text
students[0] → { id: 1, name: '小明', score: 86 }
students[1] → { id: 2, name: '小红', score: 92 }
```

循环时，`student` 依次代表每一个对象。

#### `key` 是什么

```vue
:key="student.id"
```

`key` 用来告诉 Vue：

> 当前这一行对应数组中的哪一条数据。

`key` 应该使用稳定且唯一的值，例如：

```vue
:key="student.id"
```

不建议直接使用可能重复的姓名，也不建议优先使用数组下标作为 key。

#### 获取下标

如果还需要下标，可以写：

```vue
<li v-for="(student, index) in students" :key="student.id">
  第 {{ index + 1 }} 名：{{ student.name }}
</li>
```

```text
student → 当前数据
index   → 当前下标
```

#### 完整案例

```vue
<template>
  <main>
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
```

#### `v-for` 的运行过程

```text
读取 students 数组
    ↓
逐条取出 student
    ↓
为每条数据生成一个 li
    ↓
使用 student.id 识别每一行
    ↓
显示 student.name 和 student.score
```

#### 完整案例

[第 10 课源文件](./vue学习项目/src/lessons/10-列表渲染v-for/Lesson.vue)

本课只学习数组列表渲染，下一课学习表单双向绑定 `v-model`。

### 第 11 课：表单双向绑定 `v-model`

#### 这是什么

`v-model` 是 Vue 用来绑定表单数据的指令。

```vue
<input v-model="studentName" />
```

它会建立输入框和组件数据之间的双向联系。

#### 有什么作用

- 把组件数据显示到输入框。
- 把用户输入的内容自动保存回组件数据。
- 减少手动读取 `input.value` 和监听输入事件的代码。

#### 解决什么问题

原生 JavaScript 需要手动处理：

```javascript
input.value = studentName
input.addEventListener('input', (event) => {
  studentName = event.target.value
})
```

Vue 可以直接写：

```vue
<input v-model="studentName" />
```

#### 双向绑定是什么

```text
组件数据 → 输入框
输入框 → 组件数据
```

所以它叫“双向绑定”。

#### 文本输入框

数据：

```javascript
data() {
  return {
    studentName: '小明',
  }
}
```

模板：

```vue
<input v-model="studentName" />
<p>当前姓名：{{ studentName }}</p>
```

页面开始时输入框显示“小明”。用户修改输入框后，下面的插值内容也会同步更新。

#### 复选框

```javascript
data() {
  return {
    hasAgreed: false,
  }
}
```

```vue
<label>
  <input type="checkbox" v-model="hasAgreed" />
  我同意学习规则
</label>
<p>是否同意：{{ hasAgreed }}</p>
```

复选框的 `v-model` 通常对应布尔值：

```text
未选中 → false
选中   → true
```

#### 和 `v-bind` 的区别

`v-bind` 通常是单向绑定：

```vue
<input :value="studentName" />
```

数据可以显示到输入框，但用户输入不会自动修改 `studentName`。

`v-model` 是双向绑定：

```vue
<input v-model="studentName" />
```

用户输入会自动修改 `studentName`。

```text
v-bind → 数据传给元素
v-model → 数据和表单相互同步
```

#### 完整案例

```vue
<template>
  <main>
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
```

#### `v-model` 的运行过程

```text
读取 studentName
    ↓
显示到 input
    ↓
用户修改输入框
    ↓
studentName 自动更新
    ↓
其他 {{ studentName }} 自动更新
```

#### 完整案例

[第 11 课源文件](./vue学习项目/src/lessons/11-表单双向绑定v-model/Lesson.vue)

本课只学习文本输入框和复选框的双向绑定，下一课学习计算属性 `computed`。

### 第 12 课：计算属性 `computed`

#### 这是什么

`computed` 是 Vue 中用来定义“根据已有数据计算出新数据”的功能。

例如，姓和名是两个原始数据，完整姓名是根据它们计算出来的结果：

```javascript
firstName: '张',
lastName: '三',
```

```javascript
computed: {
  fullName() {
    return this.firstName + this.lastName
  },
}
```

#### 解决了什么问题

如果在模板中直接拼接，表达式会越来越复杂：

```vue
<p>{{ firstName + lastName }}</p>
```

计算属性可以把这段计算逻辑集中放在 `computed` 中：

```vue
<p>{{ fullName }}</p>
```

这样模板负责显示，`computed` 负责计算，代码更容易阅读和维护。

#### 基本语法

```javascript
export default {
  data() {
    return {
      firstName: '张',
      lastName: '三',
    }
  },

  computed: {
    fullName() {
      return this.firstName + this.lastName
    },
  },
}
```

模板中使用计算属性时，不需要括号：

```vue
<p>{{ fullName }}</p>
```

不要写成：

```vue
<p>{{ fullName() }}</p>
```

因为 `fullName` 是计算属性，不是普通方法。

#### 它是如何重新计算的

```text
firstName 或 lastName 发生变化
          ↓
Vue 发现 fullName 依赖的数据变化
          ↓
重新计算 fullName
          ↓
更新使用 fullName 的页面内容
```

计算属性会记住计算结果。只有它依赖的数据变化时，Vue 才会重新计算。

#### `computed` 和 `methods` 的区别

计算属性：

```javascript
computed: {
  fullName() {
    return this.firstName + this.lastName
  },
}
```

使用时：

```vue
{{ fullName }}
```

方法：

```javascript
methods: {
  getFullName() {
    return this.firstName + this.lastName
  },
}
```

使用时：

```vue
{{ getFullName() }}
```

简单记忆：

```text
computed：像数据一样使用，适合计算结果
methods：像函数一样调用，适合执行动作
```

#### 完整案例

```vue
<template>
  <main>
    <h1>{{ pageTitle }}</h1>

    <p>姓：{{ firstName }}</p>
    <p>名：{{ lastName }}</p>
    <p>完整姓名：{{ fullName }}</p>
    <p>成绩：{{ score }}</p>
    <p>成绩等级：{{ scoreLevel }}</p>

    <button @click="improveScore">提高 5 分</button>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 计算属性 computed',
      firstName: '张',
      lastName: '三',
      score: 88,
    }
  },

  computed: {
    fullName() {
      return this.firstName + this.lastName
    },

    scoreLevel() {
      if (this.score >= 90) {
        return '优秀'
      }

      if (this.score >= 60) {
        return '及格'
      }

      return '需要继续学习'
    },
  },

  methods: {
    improveScore() {
      this.score += 5
    },
  },
}
</script>
```

[第 12 课源文件](./vue学习项目/src/lessons/12-计算属性computed/Lesson.vue)

本课学习根据已有数据计算新数据，下一课学习监听数据变化的 `watch`。

### 第 13 课：监听器 `watch`

#### 这是什么

`watch` 是 Vue 中用来监听数据变化的功能。当指定的数据发生变化时，Vue 会执行对应的监听函数。

例如，监听 `score`：

```javascript
watch: {
  score(newValue, oldValue) {
    console.log(newValue, oldValue)
  },
}
```

其中：

```text
newValue：变化之后的新值
oldValue：变化之前的旧值
```

#### 解决了什么问题

有些数据变化后，我们需要执行额外动作，例如：

```text
成绩变化后显示提示
搜索关键字变化后请求数据
表单内容变化后保存草稿
用户设置变化后保存到本地
```

这些动作不是单纯计算一个显示结果，因此适合使用 `watch`。

#### 基本语法

```javascript
export default {
  data() {
    return {
      score: 88,
    }
  },

  watch: {
    score(newValue, oldValue) {
      console.log('成绩发生变化')
      console.log('旧成绩：' + oldValue)
      console.log('新成绩：' + newValue)
    },
  },
}
```

当代码修改 `score` 时，监听函数会自动执行：

```javascript
this.score += 5
```

#### 完整案例

```vue
<template>
  <main>
    <h1>{{ pageTitle }}</h1>

    <p>当前成绩：{{ score }}</p>
    <p>{{ changeMessage }}</p>

    <button @click="improveScore">提高 5 分</button>
    <button @click="lowerScore">降低 5 分</button>
  </main>
</template>

<script>
export default {
  data() {
    return {
      pageTitle: 'Vue 监听器 watch',
      score: 88,
      changeMessage: '等待成绩发生变化',
    }
  },

  watch: {
    score(newValue, oldValue) {
      this.changeMessage = '成绩从 ' + oldValue + ' 分变为 ' + newValue + ' 分'
    },
  },

  methods: {
    improveScore() {
      this.score += 5
    },

    lowerScore() {
      this.score -= 5
    },
  },
}
</script>
```

点击“提高 5 分”时，运行过程是：

```text
点击按钮
  ↓
improveScore 修改 score
  ↓
score 发生变化
  ↓
watch.score 自动执行
  ↓
changeMessage 被修改
  ↓
页面显示成绩变化记录
```

#### `computed` 和 `watch` 的区别

```text
computed：根据已有数据计算一个新结果
watch：数据变化后执行一段动作
```

例如：

```javascript
computed: {
  scoreLevel() {
    return this.score >= 60 ? '及格' : '不及格'
  },
},

watch: {
  score(newValue, oldValue) {
    console.log('成绩发生变化')
  },
},
```

`scoreLevel` 是计算结果，适合 `computed`；打印日志、保存数据、请求接口等动作，适合 `watch`。

#### 默认执行时机

普通 `watch` 只有在数据变化后才执行，不会在组件刚创建时执行。

如果希望组件创建时也执行一次，可以写成：

```javascript
watch: {
  score: {
    handler(newValue, oldValue) {
      console.log(newValue, oldValue)
    },
    immediate: true,
  },
}
```

[第 13 课源文件](./vue学习项目/src/lessons/13-监听器watch/Lesson.vue)

本课学习监听数据变化并执行动作，下一课学习 Vue 的生命周期钩子。

### 第 14 课：生命周期钩子

#### 这是什么

组件从创建到销毁，会经历一系列固定阶段。Vue 在这些阶段提供了可以执行代码的函数，这些函数叫生命周期钩子。

可以把组件理解成一个网页对象：

```text
创建组件
  ↓
组件显示到页面
  ↓
数据变化，页面更新
  ↓
组件从页面移除
```

生命周期钩子就是让我们在这些阶段执行代码。

#### 常用钩子

```text
created：组件数据和方法已经准备好，但页面元素还没有生成
mounted：组件已经显示到页面，DOM 元素可以使用
updated：数据变化并且页面更新完成
unmounted：组件已经从页面移除，可以清理资源
```

#### 基本语法

```javascript
export default {
  created() {
    console.log('组件已经创建')
  },

  mounted() {
    console.log('组件已经显示到页面')
  },

  updated() {
    console.log('页面已经更新')
  },

  unmounted() {
    console.log('组件已经被移除')
  },
}
```

这些名称是 Vue 规定的生命周期钩子名称，不是我们自己随意定义的普通方法名。

#### `created` 和 `mounted` 的区别

```javascript
created() {
  this.message = '数据已经准备好'
},

mounted() {
  const title = this.$el.querySelector('h1')
  console.log(title.textContent)
},
```

`created` 阶段可以访问 `data`、`computed` 和 `methods`，但 DOM 还没有准备好。

`mounted` 阶段组件已经进入页面，可以操作当前组件的 DOM。通常可以在这里初始化第三方控件、读取元素尺寸或开始加载数据。

#### `updated` 的注意事项

```javascript
updated() {
  console.log('页面更新完成')
}
```

`updated` 中不要再次无条件修改响应式数据，否则可能形成：

```text
修改数据
  ↓
触发 updated
  ↓
updated 再修改数据
  ↓
再次触发 updated
```

#### 完整案例

```vue
<template>
  <main>
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
```

点击按钮后：

```text
increaseCount 修改 count
        ↓
Vue 更新页面上的数量
        ↓
页面更新完成
        ↓
updated 执行
```

#### 生命周期的使用场景

```text
created：准备初始数据、执行不依赖 DOM 的逻辑
mounted：操作 DOM、初始化插件、开始请求数据
updated：观察页面更新完成
unmounted：清除定时器、取消事件监听、销毁插件
```

[第 14 课源文件](./vue学习项目/src/lessons/14-生命周期钩子/Lesson.vue)

本课学习组件生命周期，下一课学习 Vue Router 路由。

### 第 15 课：Vue Router 路由

#### 这是什么

Vue Router 是 Vue 官方的路由工具。它根据浏览器地址，决定显示哪个 Vue 组件。

例如：

```text
/       → HomePage.vue
/students → StudentPage.vue
```

#### 解决了什么问题

如果没有路由，想显示不同页面，通常需要在一个组件中写很多 `v-if`：

```vue
<HomePage v-if="currentPage === 'home'" />
<StudentPage v-if="currentPage === 'students'" />
```

路由可以直接根据地址管理页面：

```text
浏览器地址变化
      ↓
Vue Router 查找对应的 routes
      ↓
显示对应的页面组件
```

#### 创建路由

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import HomePage from './HomePage.vue'
import StudentPage from './StudentPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: HomePage,
    },
    {
      path: '/students',
      component: StudentPage,
    },
  ],
})

export default router
```

这里：

```text
createRouter：创建路由对象
createWebHistory：使用浏览器地址栏管理路由
routes：路由规则数组
path：浏览器地址路径
component：这个路径要显示的组件
```

#### 把路由安装到 Vue 应用

路由创建出来后，还要在 `main.js` 中安装到 Vue 应用：

```javascript
import { createApp } from 'vue'
import App from './App.vue'
import router from './lessons/15-VueRouter路由/router.js'

const app = createApp(App)
app.use(router)
app.mount('#app')
```

```javascript
app.use(router)
```

表示把路由功能安装到当前 Vue 应用中。安装之后，组件里才能使用 `RouterLink` 和 `RouterView`。

#### 页面导航

```vue
<nav>
  <RouterLink to="/">首页</RouterLink>
  <RouterLink to="/students">学生页面</RouterLink>
</nav>
```

`RouterLink` 是 Vue Router 提供的导航组件。点击它时，地址会变化，页面不会整体刷新。

#### 显示当前页面

```vue
<RouterView />
```

`RouterView` 是路由页面的显示位置。

```text
RouterLink：改变地址
RouterView：显示当前地址对应的组件
```

#### 完整案例

课程组件：

```vue
<template>
  <main>
    <h1>Vue Router 路由</h1>

    <nav>
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/students">学生页面</RouterLink>
    </nav>

    <RouterView />
  </main>
</template>
```

首页组件：

```vue
<template>
  <section>
    <h2>首页</h2>
    <p>这里是首页组件。</p>
  </section>
</template>
```

学生页组件：

```vue
<template>
  <section>
    <h2>学生页面</h2>
    <p>这里是学生页面组件。</p>
  </section>
</template>
```

点击“学生页面”后：

```text
地址：/ → /students
      ↓
路由匹配 /students
      ↓
StudentPage.vue 被放入 RouterView
```

#### 和普通组件的关系

路由页面本质上仍然是 Vue 组件：

```text
App.vue
  ↓
Lesson.vue
  ↓
RouterView
  ↓
HomePage.vue 或 StudentPage.vue
```

路由不是重新创建 Vue 应用，而是在同一个 Vue 应用中切换显示的组件。

[第 15 课路由入口](./vue学习项目/src/lessons/15-VueRouter路由/Lesson.vue)

[第 15 课路由配置](./vue学习项目/src/lessons/15-VueRouter路由/router.js)

本课学习地址与页面组件的对应关系，下一课学习 Pinia 状态管理。

### 第 16 课：Pinia 状态管理

#### 这是什么

Pinia 是 Vue 的状态管理工具。它可以把多个组件需要共同使用的数据集中放在一个 store 中。

```text
store：集中保存共享数据和操作数据的方法
```

#### 解决了什么问题

如果两个很远的组件需要使用同一份数据，只靠 `props` 和 `emit`，数据可能要经过很多层组件：

```text
父组件 → 子组件 → 孙组件 → 目标组件
```

Pinia 让不同组件直接使用同一个 store：

```text
组件 A ─┐
        ├→ Pinia store
组件 B ─┘
```

组件 A 修改 store 后，组件 B 使用到的内容也会自动更新。

#### 创建 store

```javascript
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
```

这里：

```text
defineStore：创建一个 store
'study'：这个 store 的唯一名称
state：共享的数据
getters：根据 state 计算出来的数据
actions：修改数据或执行业务动作的方法
```

#### 把 Pinia 安装到 Vue 应用

在 `main.js` 中：

```javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './lessons/15-VueRouter路由/router.js'

const app = createApp(App)

app.use(router)
app.use(createPinia())

app.mount('#app')
```

```javascript
app.use(createPinia())
```

表示把 Pinia 安装到当前 Vue 应用中。安装后，组件才能使用 `useStudyStore()`。

#### 组件中使用 store

```javascript
import { useStudyStore } from './stores/study.js'

export default {
  setup() {
    const studyStore = useStudyStore()

    return {
      studyStore,
    }
  },
}
```

```text
useStudyStore()：获取 study store
return：把 store 暴露给 template
```

模板中使用：

```vue
<p>{{ studyStore.studentName }}</p>
<p>{{ studyStore.progressText }}</p>
<button @click="studyStore.completeLesson">完成一课</button>
```

注意：`progressText` 是 getter，像数据一样使用；`completeLesson` 是 action，点击时执行。

#### 完整案例

进度展示组件：

```vue
<template>
  <section>
    <h2>学习进度</h2>
    <p>学生：{{ studyStore.studentName }}</p>
    <p>{{ studyStore.progressText }}</p>
  </section>
</template>

<script>
import { useStudyStore } from './stores/study.js'

export default {
  setup() {
    return {
      studyStore: useStudyStore(),
    }
  },
}
</script>
```

操作组件：

```vue
<template>
  <section>
    <h2>学习操作</h2>
    <button @click="studyStore.completeLesson">完成一课</button>
  </section>
</template>

<script>
import { useStudyStore } from './stores/study.js'

export default {
  setup() {
    return {
      studyStore: useStudyStore(),
    }
  },
}
</script>
```

这两个组件分别调用 `useStudyStore()`，拿到的是同一个 store。点击操作组件后，进度展示组件会自动更新。

[第 16 课入口](./vue学习项目/src/lessons/16-Pinia状态管理/Lesson.vue)

[第 16 课 store](./vue学习项目/src/lessons/16-Pinia状态管理/stores/study.js)

本课学习多个组件共享状态，下一课学习 Vue 使用 `fetch` 加载数据。

## 五、普通 Vue 与 Power 平台封装

### 普通 Vue

普通 Vue 3 项目通常这样启动：

```javascript
import { createApp } from 'vue'
import App from './App.vue'

createApp(App).mount('#app')
```

### `power-web`

`power-web` 使用平台自己的封装：

```typescript
import { createApp } from '@power/vue'

createApp({
  modules: [...],
  App,
})
```

需要区分：

```text
import、对象、数组、函数调用 → JavaScript/TypeScript 语法
@power/vue 的 createApp     → Power 平台提供的方法
modules、App 配置结构       → Power 平台约定的参数
```

平台的 `createApp` 内部仍然会使用 Vue，但额外处理了：

- 路由
- Pinia
- 页面模块
- 布局
- 全局组件
- 项目初始化

因此它不是修改了 Vue 原来的 `createApp`，而是提供了一个同名的封装方法。

## 六、Vue 多页面项目案例

### 这是什么

多页面项目是在同一个 Vite 项目中放置多个 HTML 入口：

```text
index.html  → 首页
about.html  → 关于页
```

这和单页面多个组件不同：

```text
多页面：多个 HTML 文件
单页面：一个 HTML 文件，内部切换组件或路由
```

### 项目结构

```text
vue多页面项目/
├─ index.html
├─ about.html
├─ vite.config.js
└─ src/pages/
   ├─ home/
   │  ├─ main.js
   │  └─ App.vue
   └─ about/
      ├─ main.js
      └─ App.vue
```

### 首页入口

```html
<div id="home-app"></div>
<script type="module" src="/src/pages/home/main.js"></script>
```

```javascript
import { createApp } from 'vue'
import App from './App.vue'

createApp(App).mount('#home-app')
```

### 关于页入口

```html
<div id="about-app"></div>
<script type="module" src="/src/pages/about/main.js"></script>
```

```javascript
import { createApp } from 'vue'
import App from './App.vue'

createApp(App).mount('#about-app')
```

### Vite 多入口配置

```javascript
export default defineConfig({
  plugins: [vue()],
  build: {
    rolldownOptions: {
      input: {
        home: fileURLToPath(new URL('./index.html', import.meta.url)),
        about: fileURLToPath(new URL('./about.html', import.meta.url)),
      },
    },
  },
})
```

这里的 `input` 告诉 Vite 需要构建哪些 HTML 入口。

### 完整案例

[Vue 多页面项目首页](./vue多页面项目/index.html)

[Vue 多页面项目关于页](./vue多页面项目/about.html)

开发地址：

```text
http://127.0.0.1:5176/
http://127.0.0.1:5176/about.html
```

## 七、当前学习路线

已经完成：

1. Vue、Vite 和项目启动
2. 插值表达式 `{{ }}`
3. 多个标签与 `data()` 数据管理
4. 根组件组合子组件

已完成：

5. 父组件通过 `props` 向子组件传递数据

6. 子组件通过 `emit` 通知父组件

7. Vue 点击事件 `@click`

8. Vue 属性绑定 `v-bind`

9. 条件渲染 `v-if`

正在学习：

11. 表单双向绑定 `v-model`

接下来：

1. `computed` 计算属性
2. `watch` 监听数据
3. Vue Router
4. Pinia 状态管理
