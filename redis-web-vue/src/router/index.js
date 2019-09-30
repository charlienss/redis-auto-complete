import Vue from 'vue'
import Router from 'vue-router'
import HelloWorld from '@/components/HelloWorld'
import test from '@/components/Test'
import autoComplete from '@/components/autoComplete'

Vue.use(Router)




export default new Router({
  routes: [
    {
      path: '/',
      name: 'HelloWorld',
      component: HelloWorld
    },
    {
      path: '/test',
      name: 'test',
      component: test
    },
    {
      path: '/autoComplete',
      name: 'autoComplete',
      component: autoComplete
    }
  ]
})
