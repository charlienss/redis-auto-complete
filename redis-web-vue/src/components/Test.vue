<template>
  <div class="test">
    <h1>{{ msg }}</h1>
    <el-button type="primary">
      test
    </el-button>
  </div>
</template>

<script>
  export default {
    name: 'Test',
    data () {
      return {
        msg: 'Welcome to Your test'
      }
    }
  }
</script>

<style scoped>
  h1, h2 {
    font-weight: normal;
  }
</style>
