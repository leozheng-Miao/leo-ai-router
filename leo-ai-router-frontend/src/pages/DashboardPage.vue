<template>
  <main class="leo-page dashboard-page">
    <section class="dashboard-page__shell">
      <h1 class="dashboard-page__title">控制台</h1>
    </section>
  </main>
</template>

<style scoped>
.dashboard-page {
  flex: 1;
}

.dashboard-page__shell {
  min-height: 220px;
  padding: 24px;
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
}

.dashboard-page__title {
  margin: 0;
  color: var(--leo-text-primary);
  font-size: 24px;
  font-weight: 800;
}
</style>
