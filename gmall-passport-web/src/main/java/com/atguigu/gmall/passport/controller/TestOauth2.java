package com.atguigu.gmall.passport.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpclientUtil;

import java.util.HashMap;
import java.util.Map;

public class TestOauth2 {

    public static void main(String[] args) {

//        App Key：1005435318
//        App Secret：62406caf74e0e42d478db5276ce1ad20

        //1 请求授权地址
        String s1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=1005435318&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");

        //code=7d858f2b0e67f287518d2f623f0cc411

        //2 通过回调地址获得授权code
        String s2 = "http://passport.gmall.com:8085/vlogin?code=7d858f2b0e67f287518d2f623f0cc411";

        //3 通过code获得access_token
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "1005435318");
        map.put("client_secret", "62406caf74e0e42d478db5276ce1ad20");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://passport.gmall.com:8085/vlogin");
        map.put("code", "b8697b40b70178ecc7fb6f1cd2358fc8"); //授权其有效期内使用，每次新生成一次授权码，说明用户对第三方重新授权，之前的access_token和授权码全部过期
//        String s3 = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token?", map);
        //"uid":"5673530687"
        //"access_token":"2.00TVYxLGgehCGBd91503ba15TiPDmC"
//        System.out.println(s3);

        //4 通过access_token 获得用户信息
        //https://api.weibo.com/2/users/show.json?access_token=token&uid=uid
        String s4 = HttpclientUtil.doGet("https://api.weibo.com/2/users/show.json?access_token=2.00TVYxLGgehCGBd91503ba15TiPDmC&uid=5673530687");
        Map<String, String> userMap = JSON.parseObject(s4, Map.class);
        System.out.println(userMap);

    }
}
