package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Reference
    CartService cartService;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String checkTradCode(String memberId, String tradeCode) {
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();
            String tradKey = "user:" + memberId + ":tradCode";

//            String tradCode = jedis.get(tradKey);

            //使用lua脚本在发现key的同时把key删除，防止并发订单攻击
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradKey), Collections.singletonList(tradeCode));
            /*StringUtils.isNotBlank(tradCode) && tradCode.equals(tradeCode)*/
            if (eval != null && eval != 0) {
//                jedis.del(tradKey);
                return "success";
            }

            return "fail";

        } finally {
            jedis.close();
        }
    }

    @Override
    public String genTradCode(String memberId) {
        Jedis jedis = null;
        String tradKey = "user:" + memberId + ":tradCode";
        String tradCode = UUID.randomUUID().toString();

        try {
            jedis = redisUtil.getJedis();
            jedis.setex(tradKey, 60 * 15, tradCode);
        } finally {
            jedis.close();
        }

        return tradCode;
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
        //保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        //保存订单详情
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
//            cartService.delBySkuId(omsOrderItem.getProductSkuId(), omsOrder.getMemberId());
        }
    }

    @Override
    public OmsOrder getOrderByOutTradNo(String outTradNo) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradNo);
        OmsOrder omsOrder1 = omsOrderMapper.selectOne(omsOrder);
        return omsOrder1;
    }

    @Override
    public void updateOrder(OmsOrder omsOrder) {
        Example e = new Example(OmsOrder.class);
        omsOrder.setStatus("1");
        e.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());

        //发送一个订单已支付的mq队列，提供给库存消费
        Connection connection = null;
        Session session = null;

        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Queue order_pay_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(order_pay_queue);
//            MapMessage mapMessage = new ActiveMQMapMessage();
            TextMessage textMessage = new ActiveMQTextMessage();

            //查询订单的对象，转化成json字符串，存入ORDER_PAY_QUEUE的消息队列
            OmsOrder omsOrderParam = new OmsOrder();
            omsOrderParam.setOrderSn(omsOrder.getOrderSn());
            OmsOrder omsOrderResponse = omsOrderMapper.selectOne(omsOrderParam);

            OmsOrderItem omsOrderItemParam = new OmsOrderItem();
            omsOrderItemParam.setOrderSn(omsOrderParam.getOrderSn());
            List<OmsOrderItem> select = omsOrderItemMapper.select(omsOrderItemParam);
            omsOrderResponse.setOmsOrderItems(select);

            omsOrderMapper.updateByExampleSelective(omsOrder,e);
            textMessage.setText(JSON.toJSONString(omsOrderResponse));
            producer.send(textMessage);
            session.commit();

        } catch (JMSException e1) {
            try {
                session.rollback();
            } catch (JMSException e2) {
                e2.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }

    }
}
