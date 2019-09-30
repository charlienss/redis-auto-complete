# 使用Redis实现中英文自动补全功能详解

#### 1.Redis自动补全功能介绍:

​		Redis可以帮我们实现很多种功能,今天这里着重介绍的是Redis的自动补全功能的实现.我们使用有序集合，并score都为0，这样就按元素值的字典序排序.然后我们可以根据排序号的字符,进行添加前缀和后缀的方式,找到我们想要的区间内容.下面介绍一个简单的Zset的排序内容和思路,以便后续的理解:

**名称为redis_concat的Zset集合元素如下:**

| 编号 | 数值   | 分值 |
| ---- | ------ | ---- |
| 1    | a      | 0    |
| 2    | ab     | 0    |
| 3    | abcd   | 0    |
| 4    | abef   | 0    |
| 5    | hjk    | 0    |
| 6    | dbfgll | 0    |
| 7    | efhuo  | 0    |
| 8    | iop    | 0    |
| 9    | lkj    | 0    |
| 10   | ghu    | 0    |

​		当所有的数值分值为0的时候,Zset会按照字典升序排列,这里我们如果需要查找上面的a,就应该能找出[ a, ab,abcd,abef]这四个元素,查找上面的ab,就应该能找出[ab,abcd,abef]这三个元素,其他同理.这个时候我们只要想办法在这个搜索条件查找元素的前面后最后都筛选出想要的数据即可:

- Ascii码里小写字母a的前面是`,z的后面是{
- 于是我们查找ab匹配的元素,插入 aa{ 和 ab{ 即可( 或者" ab` "和" ab{ " )
- 找到aa{ 和 ab{ 的下标,通过Zrange()得出相关区间的内容
- 如果是中文,建议全部将支付转为16进制字符来进行存储,取出时候再转码

#### 2.相关Demo分享

​		基于此本人建立了一个前后端分离的利用Redis自动补全联系人姓名的项目,前端采用的是Vue,后端采用Java的Spring框架,这个示例功能单一,有好的建议和想法都可以给我留言评论,多加以改进,另外项目GitHub地址在文末,喜欢请关注.下面是项目的简单演示:

![uNnnKI.gif](https://s2.ax1x.com/2019/10/01/uNnnKI.gif)

##### 项目结构如下:

```java
├─src
│  └─main
│      ├─java
│      │  └─com
│      │      └─home
│      │          ├─config
│      │          ├─constants
│      │          ├─controller
│      │          ├─mapper
│      │          ├─page
│      │          ├─pojo
│      │          └─service
│      │              └─impl
│      ├─resources
│      │  ├─mapper
│      │  └─properties
│      └─webapp
│          └─WEB-INF
│              └─views
│                  └─vue
└─target
    ├─classes
    │  ├─com
    │  │  └─home
    │  │      ├─config
    │  │      ├─constants
    │  │      ├─controller
    │  │      ├─mapper
    │  │      ├─page
    │  │      ├─pojo
    │  │      └─service
    │  │          └─impl
    │  ├─mapper
    │  └─properties
    ├─generated-sources
    │  └─annotations
    ├─qfang-agent-online-mass-client
    │  ├─META-INF
    │  └─WEB-INF
    │      ├─classes
    │      │  ├─com
    │      │  │  └─home
    │      │  │      ├─controller
    │      │  │      ├─mapper
    │      │  │      ├─pojo
    │      │  │      └─service
    │      │  │          └─impl
    │      │  └─mapper
    │      └─lib
    └─redis-web-1.0-SNAPSHOT
        ├─META-INF
        └─WEB-INF
            ├─classes
            │  ├─com
            │  │  └─home
            │  │      ├─config
            │  │      ├─constants
            │  │      ├─controller
            │  │      ├─mapper
            │  │      ├─page
            │  │      ├─pojo
            │  │      └─service
            │  │          └─impl
            │  ├─mapper
            │  └─properties
            ├─lib
            └─views

```

##### Vue的构建步骤:

```java
# install dependencies
npm install

# serve with hot reload at localhost:8080
npm run dev

# build for production with minification
npm run build

# build for production and view the bundle analyzer report
npm run build --report

# run unit tests
npm run unit

# run e2e tests
npm run e2e

# run all tests
npm test
```

##### Java_Service中相关的方法:

- 1.分页获取前100条数据,如果Redis中不存该联系人在就放入redis中
- 2.放入前使用 unicode编码,位于coding方法中,取出相关的数据后记得使用decoding方法解码
- 3.获得相关数据后删除放入的前缀和后缀,这里都加了UUID,防止有相同的查询带有前后缀的数据被误删(如查找 ab ,数据中本身就含有  ab{ 等)
- 4.获得前5条或者前10条相关匹配的数据给前台(这里自定义即可,查看注释地方)

##### 相关类详情:

```java
package com.home.service.impl;

import com.home.constants.RedisExpireUtil;
import com.home.mapper.ContactMapper;
import com.home.page.PageObject;
import com.home.page.PageResult;
import com.home.pojo.Contact;
import com.home.service.ContactService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

/**
 * 改造为中英文皆可匹配
 * 这里全部转为16进制存入到redis中
 */
@Service
public class ContactServiceImpl implements ContactService {
    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private JedisPool jedisPool;

    private static final String REDIS_CONCAT = "redis_concat";

    private static final String VALID_CHARACTERS = "0123456789abcdefg";

    @Override
    public Contact selectByPrimaryKey(int i) {
        return contactMapper.selectByPrimaryKey(i);
    }

    /**
     * 获取相应的条数的数据
     */
    @Override
    public PageResult getDateSplitByNum(PageObject pageObject) {
        // 获取总条数
        int totalCount = contactMapper.getAllCount();
        List<Contact> contactList = contactMapper.selectByPageObject(pageObject);
        return new PageResult(contactList, totalCount, pageObject);
    }

    @Override
    public List<String> getRelatedWord(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        Jedis jedis = jedisPool.getResource();
        if (jedis.exists(REDIS_CONCAT)) {
            // 拼接字段
            String[] prefixRange = findPrefixRange(coding(name));
            // 放入到redis中
            List<String> strFinds = putIntoRedisAndFind(prefixRange);
            return strFinds;
        } else {
            // 从数据库放入
            transDBToRedis(1, 100);
            // 拼接字段
            String[] prefixRange = findPrefixRange(name);
            // 放入到redis中
            List<String> strFinds = putIntoRedisAndFind(prefixRange);
            return strFinds;
        }
    }

    /**
     * 把数据放入到redis
     *
     * @return
     */
    public void transDBToRedis(int currentPage, int pageSize) {
        Jedis jedis = jedisPool.getResource();
        PageObject po = new PageObject();
        po.setCurrentPage(currentPage);
        po.setPageSize(pageSize);
        // 1.取出前100条数据
        PageResult pageResult = getDateSplitByNum(po);
        List<Contact> contactList = (List<Contact>) pageResult.getData();
        // 2.放入前100条数据到redis
        for (Contact contact : contactList) {
            String contactName = coding(contact.getContactName());
            if (jedis.zrank(REDIS_CONCAT, contactName) == null) {
                jedis.zadd(REDIS_CONCAT, 0D, contactName);
            }
        }
        if (jedis.ttl(REDIS_CONCAT).intValue() == RedisExpireUtil.NEVER_EXPIRE) {
            // 放入redis有效期一小时
            jedis.expire(REDIS_CONCAT, RedisExpireUtil.ONE_HOUR);
        }
    }


    /**
     * 将相关的参数放入redis中
     *
     * @param prefixRange
     */
    private List<String> putIntoRedisAndFind(String[] prefixRange) {
        Jedis jedis = jedisPool.getResource();
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        List<String> list = new ArrayList();
        try {
            jedis.watch(REDIS_CONCAT);
            String start = prefixRange[0] + uuid;
            String end = prefixRange[1] + uuid;


            // 1.放入redis
            jedis.zadd(REDIS_CONCAT, 0, start);
            jedis.zadd(REDIS_CONCAT, 0, end);

            // 2.得到索引的位置
            int begin_index = jedis.zrank(REDIS_CONCAT, start).intValue();
            int end_index = jedis.zrank(REDIS_CONCAT, end).intValue();
            //  3.删除这两个放入的值
            jedis.zrem(REDIS_CONCAT, start);
            jedis.zrem(REDIS_CONCAT, end);
            // 3.因为最多展示5个，所以计算出结束为止
            int erange = Math.min(begin_index + 4, end_index - 2);
            if(begin_index>erange){
                return null;
            }
            // 4.获得其中的值
            Set<String> zrange = jedis.zrange(REDIS_CONCAT, begin_index, erange);
            if (zrange == null) {
                return null;
            }

            list.addAll(zrange);
            ListIterator<String> it = list.listIterator();
            while (it.hasNext()) {
                String next = it.next();
                if (next.indexOf("g") != -1) {
                    it.remove();
                } else {
                    it.set(decoding(next));//把16进制字符串转换回来
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.unwatch();
        }

        return list;

    }


//    这个方法仅仅适用于匹配英文字符
//    public String[] findPrefixRange(String prefix) {
//        String start = prefix + '`';
//        String end = prefix + '{';
//        System.out.println(prefix + "  " + start + "---" + end);
//        return new String[]{start, end};
//    }


    //unicode编码
    private String coding(String s) {
        char[] chars = s.toCharArray();
        StringBuffer buffer = new StringBuffer();
        for (char aChar : chars) {
            String s1 = Integer.toString(aChar, 16);
            buffer.append("-" + s1);
        }
        String encoding = buffer.toString();
        return encoding;
    }

    //unicode解码
    private String decoding(String s) {
        String[] split = s.split("-");
        StringBuffer buffer = new StringBuffer();

        for (String s1 : split) {
            if (!s1.trim().equals("")) {
                char i = (char) Integer.parseInt(s1, 16);
                buffer.append(i);
            }
        }
        return buffer.toString();
    }


    private String[] findPrefixRange(String prefix) {
         //查找出前缀字符串最后一个字符在列表中的位置
        int posn = VALID_CHARACTERS.indexOf(prefix.charAt(prefix.length() - 1));
        //找出前驱字符
        char suffix = VALID_CHARACTERS.charAt(posn > 0 ? posn - 1 : 0); 
        //生成前缀字符串的前驱字符串
        String start = prefix.substring(0, prefix.length() - 1) + suffix + 'g'; 
        //生成前缀字符串的后继字符串
        String end = prefix + 'g';                                                    
        return new String[]{start, end};
    }
}

```

#### 3.项目git地址

<font color=#68228B  size=3>(喜欢记得点星支持哦,谢谢!)</font> 

<font color=#EEB422   size=4>[https://github.com/fengcharly/redis-auto-complete](https://github.com/fengcharly/redis-auto-complete)</font>

