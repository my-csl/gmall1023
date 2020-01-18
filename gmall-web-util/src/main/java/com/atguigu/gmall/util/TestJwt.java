package com.atguigu.gmall.util;

import java.util.HashMap;
import java.util.Map;

public class TestJwt {

    public static void main(String[] args) {
        Map<String,Object> map = new HashMap<>();
        map.put("memberId","1");
        String encode = JwtUtil.encode("2019gmall0105", map, "127.0.0.1");
        System.err.println(encode);
    }
}
