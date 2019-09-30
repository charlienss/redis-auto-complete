package com.home.mapper;


import com.home.page.PageObject;
import com.home.pojo.Contact;

import java.util.List;

public interface ContactMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Contact record);

    int insertSelective(Contact record);

    Contact selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Contact record);

    int updateByPrimaryKey(Contact record);

    int getAllCount();

    List<Contact> selectByPageObject(PageObject pageObject);
}