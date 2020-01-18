package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    //umsMember表的增删改查
    @Override
    public List<UmsMember> getAllUser() {
        List<UmsMember> userMappers = userMapper.selectAll();
        return userMappers;
    }

    @Override
    public UmsMember selectById(String id) {
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    public void insertUmsMember(UmsMember umsMember) {
        userMapper.insert(umsMember);
    }

    @Override
    public void upUmsMember(UmsMember umsMember) {
        userMapper.updateByPrimaryKey(umsMember);
    }

    @Override
    public void deleteUmsMember(String id) {
        userMapper.deleteByPrimaryKey(id);
    }


    @Override
    public List<UmsMemberReceiveAddress> umsMemberReceiveAddress(String memberId) {

        //封装的参数对象
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);

        List<UmsMemberReceiveAddress> select = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);

        return select;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {

        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            if (jedis != null) {
                //查询缓存
                String umsMemberStr = jedis.get("user:" + umsMember.getPassword()+umsMember.getUsername() + ":info");

                if (StringUtils.isNotBlank(umsMemberStr)) {
                    //密码正确
                    UmsMember umsMemberFromCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                }

            }

            //查询数据库
            UmsMember umsMemberFromDb = umsMemberFromDb(umsMember);
            if (umsMemberFromDb != null) {
                jedis.setex("user:" + umsMemberFromDb.getPassword()+umsMemberFromDb.getUsername() + ":info", 60 * 60 * 24, JSON.toJSONString(umsMemberFromDb));
            }
            return umsMemberFromDb;

        } finally {
            jedis.close();
        }
    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis = redisUtil.getJedis();

        jedis.setex("user:" + memberId + ":token", 60 * 60 * 2, token);

        jedis.close();
    }

    @Override
    public UmsMember addOauth2(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);

        return umsMember;
    }

    @Override
    public UmsMember checkOauthUser(UmsMember umsMemberCheck) {

        UmsMember umsMember = userMapper.selectOne(umsMemberCheck);

        return umsMember;
    }

    @Override
    public UmsMemberReceiveAddress getReceviAddreddById(String receiveAddressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        UmsMemberReceiveAddress umsMemberReceiveAddress1 = umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
        return umsMemberReceiveAddress1;
    }


    private UmsMember umsMemberFromDb(UmsMember umsMember) {

        List<UmsMember> members = userMapper.select(umsMember);
        if (!members.isEmpty()){
            return members.get(0);
        }

        return null;
    }
}
