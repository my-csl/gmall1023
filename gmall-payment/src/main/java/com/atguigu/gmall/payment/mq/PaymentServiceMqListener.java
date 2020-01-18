package com.atguigu.gmall.payment.mq;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) {

        System.out.println("进行延迟检查，调用支付检查的接口服务");
        String out_trade_no = null;
        int count = 0;
        try {
            out_trade_no = mapMessage.getString("out_trade_no");
            count = Integer.parseInt("" + mapMessage.getInt("count"));
        } catch (JMSException e) {
            e.printStackTrace();
        }

        //调用paymentService的支付宝检查接口
        Map<String, Object> resultMap = paymentService.checkAlipayPayment(out_trade_no);

        if (resultMap != null && !resultMap.isEmpty()) {
            String trade_success = (String) resultMap.get("trade_status");
            //根据查询的支付状态结果，判断是否进行下一次的延迟任务还是支付成功更新数据和后续任务
            if (StringUtils.isNotBlank(trade_success) && trade_success.equals("TRADE_SUCCESS")) {

                //更新成功，重新发送支付队列
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setAlipayTradeNo((String) resultMap.get("trade_no"));//支付宝交易凭证号
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setOrderSn(out_trade_no);
                paymentInfo.setCallbackContent((String) resultMap.get("call_bank_content"));//回调请求字符串
                paymentInfo.setCallbackTime(new Date());
                //更新用户的支付状态
                paymentService.updatePaymentInfo(paymentInfo);
                System.out.println("支付成功，调用支付服务，修改支付信息和发送支付成功的队列");
            }
            return;
        }

        if (count > 0) {
            System.out.println("支付失败，剩余检查次数为" + count + "，继续发送延迟检查任务");
            count--;
            paymentService.sendDelayPaymentResultCheckQueue(out_trade_no, count);
        } else {
            System.out.println("支付失败，剩余检查次数为" + count + "，停止延迟检查任务");
        }

    }
}
