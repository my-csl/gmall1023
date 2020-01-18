package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

public interface OrderService {

    String checkTradCode(String memberId, String tradeCode);

    String genTradCode(String memberId);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByOutTradNo(String outTradNo);

    void updateOrder(OmsOrder omsOrder);
}
