package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;

import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("vlogin")
    public String vlogin(String code,HttpServletRequest request) {

        //授权码换取access_token
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "1005435318");
        map.put("client_secret", "62406caf74e0e42d478db5276ce1ad20");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://passport.gmall.com:8085/vlogin");
        map.put("code", code);
        String access_token = HttpclientUtil.doPost("https://api.weibo.com/oauth2/access_token?", map);
        Map<String, Object> accessTokenMap = JSON.parseObject(access_token, Map.class);

        //access_token换取用户信息
        String uid = (String) accessTokenMap.get("uid");
        String accessToken = (String) accessTokenMap.get("access_token");
        String userInfo = HttpclientUtil.doGet("https://api.weibo.com/2/users/show.json?access_token=" + accessToken + "&uid=" + uid);
        Map<String, Object> userMap = JSON.parseObject(userInfo, Map.class);

        //将用户信息存入数据库，用户类型设置为微博用户
        UmsMember umsMember = new UmsMember();
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(accessToken);
        String g = "0";//0为女
        if (userMap.get("gender").equals("m")) {
            g = "1";//1为男
        }
        umsMember.setGender(g);
        umsMember.setSourceType(2);//2为微博用户
        umsMember.setSourceUid((String) userMap.get("idstr"));
        umsMember.setNickname((String) userMap.get("screen_name"));
        umsMember.setCity((String) userMap.get("location"));

        //查询是否第三方登录过
        UmsMember umsMemberCheck = new UmsMember();
        umsMemberCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember checkOauthUser = userService.checkOauthUser(umsMemberCheck);
        if (checkOauthUser == null) {
            //没登录过把用户数据存入数据库
            umsMember = userService.addOauth2(umsMember); //rpc的主键返回策略失效
        }else {
            umsMember = checkOauthUser;
        }

        //生成jwt的token，重定向到首页，携带该token
        String token = getToken(umsMember.getId(), umsMember.getNickname(), request);


        return "redirect:http://search.gmall.com:8083/index?token="+token;
    }


    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token, String currentIp) {

        //通过jwt验证token真假
        Map<String, String> map = new HashMap<>();
        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall0105", currentIp);

        if (decode != null) {
            map.put("status", "success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));
        } else {
            map.put("status", "fail");
        }

        return JSON.toJSONString(map);
    }

    @RequestMapping("login")
    @ResponseBody
    public String token(UmsMember umsMember, HttpServletRequest request) {

        //调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);

        String token = "";
        if (umsMemberLogin != null) {
            //登录成功
            token = getToken(umsMemberLogin.getId(), umsMemberLogin.getNickname(), request);

//            //用jwt制作token
//            Map<String, Object> map = new HashMap<>();
//            String memberId = umsMemberLogin.getId();
//            String nickname = umsMemberLogin.getnickname();
//            map.put("memberId", memberId);
//            map.put("nickname", nickname);
//
//            //盐值
//            String ip = request.getHeader("x-forwarded-for");//从nginx转发的客户端ip
//            if (StringUtils.isBlank(ip)) {
//                ip = request.getRemoteAddr(); //从request获取的ip
//                if (StringUtils.isBlank(ip)) {
//                    ip = "127.0.0.1";
//                }
//            }
//
//            //按照设计的算法对参数进行加密后，生成token
//            //2019gmall0105为密钥  map为用户信息 ip为盐值
//            token = JwtUtil.encode("2019gmall0105", map, ip);
//
//            //将token存入redis一份
//            userService.addUserToken(token, memberId);

        } else {
            //登录失败
            token = "fail";
        }

        return token;
    }

    public String getToken(String memberId, String nickname, HttpServletRequest request) {

        String token = "";

        //用jwt制作token
        Map<String, Object> map = new HashMap<>();
        map.put("memberId", memberId);
        map.put("nickname", nickname);

        //盐值
        String ip = request.getHeader("x-forwarded-for");//从nginx转发的客户端ip
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr(); //从request获取的ip
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.1";
            }
        }

        //按照设计的算法对参数进行加密后，生成token
        //2019gmall0105为密钥  map为用户信息 ip为盐值
        token = JwtUtil.encode("2019gmall0105", map, ip);

        //将token存入redis一份
        userService.addUserToken(token, memberId);

        return token;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index(String ReturnUrl, ModelMap modelMap) {

        modelMap.put("ReturnUrl", ReturnUrl);

        return "index";
    }
}
