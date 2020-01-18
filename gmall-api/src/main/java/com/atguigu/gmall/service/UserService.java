package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    //UmsMember表增删改查
    List<UmsMember> getAllUser();
    UmsMember selectById(String id);
    void insertUmsMember(UmsMember umsMember);
    void upUmsMember(UmsMember umsMember);
    void deleteUmsMember(String id);

    //UmsMemberReceiveAddress表增删改查
    List<UmsMemberReceiveAddress> umsMemberReceiveAddress(String memberId);


    UmsMember login(UmsMember umsMember);

    void addUserToken(String token, String memberId);

    UmsMember addOauth2(UmsMember umsMember);

    UmsMember checkOauthUser(UmsMember umsMemberCheck);

    UmsMemberReceiveAddress getReceviAddreddById(String receiveAddressId);
}
