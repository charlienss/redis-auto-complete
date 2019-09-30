<template>
  <div id="app">
    <!--<img src="./assets/logo.png">-->
    <router-link to="/">Go to /hello</router-link>
    <br/> <br/>
    <router-link to="/autoComplete">Go to /autoComplete</router-link>
    <br/> <br/>
    <router-link to="/test">Go to /test</router-link>
    <router-view/>
  </div>
</template>

<script>
  export default {
    name: 'App'
  }
</script>

<style>
  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-align: center;
    color: white;
    margin-top: 190px;
  }
  a{
    color: white;
  }

  body {
    background: url("image/back.jpg") no-repeat center;
    background-size:100% 100%;
    background-attachment: fixed;
  }




</style>
