package com.home.service.impl;

import com.alibaba.druid.support.opds.udf.ExportSelectListColumns;
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
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

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

        int posn = VALID_CHARACTERS.indexOf(prefix.charAt(prefix.length() - 1));    //查找出前缀字符串最后一个字符在列表中的位置
        char suffix = VALID_CHARACTERS.charAt(posn > 0 ? posn - 1 : 0);                //找出前驱字符
        String start = prefix.substring(0, prefix.length() - 1) + suffix + 'g';        //生成前缀字符串的前驱字符串
        String end = prefix + 'g';                                                    //生成前缀字符串的后继字符串
        return new String[]{start, end};
    }
}
