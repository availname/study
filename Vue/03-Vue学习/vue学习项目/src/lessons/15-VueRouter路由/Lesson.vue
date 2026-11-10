<template>
  <main id="router-lesson">
    <h1>Vue Router 路由</h1>

    <nav>
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/students">学生页面</RouterLink>
    </nav>

    <RouterView />
  </main>
</template>
