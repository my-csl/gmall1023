package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {

    @Autowired
    OrderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void updateProcessOrderStatus(MapMessage mapMessage){

        try {
            String out_trade_no = mapMessage.getString("out_trade_no");
            //消费mq的支付队列，更新订单状态业务
            OmsOrder omsOrderUpdate = new OmsOrder();
            omsOrderUpdate.setOrderSn(out_trade_no);
            orderService.updateOrder(omsOrderUpdate);

//            System.out.println(out_trade_no);
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
