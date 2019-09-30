<template>
  <div class="test">
    <h1>这里是搜索框</h1>
    <el-autocomplete
      v-model="state"
      :fetch-suggestions="querySearchAsync"
      placeholder="请输入内容"
      @select="handleSelect"
    ></el-autocomplete>

  </div>
</template>

<script>
  import $ from 'jquery'
  //      请求远端的数据
  function getResult(str) {
    let list = [];
    function content(value) {
      this.value = value;
    }
    $.ajax({
      type: "POST",
      url: "http://127.0.0.1:8080/auto_complete?name=" + str,
      success: function (msg) {
        for (let i = 0; i < msg.length; i++) {
          list.push(new content(msg[i]));
          console.log(msg[i]);
        }
      }
    })
    return list;
  }

  export default {
    name: 'autoComplete',
    data() {
      return {
        restaurants: [],
        state: '',
        word: 'a'

      };
    },
    methods: {
      loadAll() {
        console.log("loadAll=>"+this.word);

      },
      querySearchAsync(queryString, cb) {
        this.word = queryString;
        console.log(this.word);
        var restaurants = this.restaurants;
        if (queryString == null || queryString == '') {
          this.word ='a';
//          return;
        }

        var results =getResult(queryString);

//        queryString ? restaurants.filter(this.createStateFilter(queryString)) : restaurants;
        clearTimeout(this.timeout);
        this.timeout = setTimeout(() => {
          cb(results);
        }, 100 * Math.random());
      },
      createStateFilter(queryString) {
        return (state) => {
          return (state.value.toLowerCase().indexOf(queryString.toLowerCase()) === 0);
        };
      },
      handleSelect(item) {
        console.log(item);
      }
    },
    mounted() {
      this.restaurants = this.loadAll();
    }
  };


</script>

<style scoped>
  h1, h2 {
    font-weight: normal;
  }
</style>
