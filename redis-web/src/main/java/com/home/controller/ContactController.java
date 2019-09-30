package com.home.controller;

import com.home.pojo.Contact;
import com.home.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;


@Controller
@RequestMapping("/")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostConstruct
    public void init() {
        contactService.transDBToRedis(1,100);
    }

    @RequestMapping("/test")
    @ResponseBody
    public Contact test() {
        return contactService.selectByPrimaryKey(1);
    }

    /**
     * 自动补全功能
     */
    @RequestMapping("/auto_complete")
    @ResponseBody
    public List<String> autoComplete(String name) {
        return contactService.getRelatedWord(name);
    }



}
