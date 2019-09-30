package com.home.service;

import com.home.page.PageObject;
import com.home.page.PageResult;
import com.home.pojo.Contact;

import java.util.List;
import java.util.Set;

public interface ContactService {
    Contact selectByPrimaryKey(int i);

    PageResult getDateSplitByNum(PageObject i);

    List<String> getRelatedWord(String name);

    void transDBToRedis(int i, int i1);
}
