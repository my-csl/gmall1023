package com.atguigu.gmall.payment.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;


    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String aliPayCallBankReturn(HttpServletRequest request) {

        //回调请求中获取支付宝参数
        String sign = request.getParameter("sign"); //签名
        String trade_no = request.getParameter("trade_no"); //支付宝交易凭证号
        String out_trade_no = request.getParameter("out_trade_no"); //原支付请求的商户订单号
        String total_amount = request.getParameter("total_amount");
        String call_bank_content = request.getQueryString();

        //通过支付宝的paramsMap进行验签，由于2.0版本的接口将paramsMap参数去掉了，导致同步请求无法验签
        //我们设定只要sign不为空即为验签成功
        if (StringUtils.isNotBlank(sign)){
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setTotalAmount(new BigDecimal(total_amount));
            paymentInfo.setAlipayTradeNo(trade_no);//支付宝交易凭证号
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setCallbackContent(call_bank_content);//回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            //更新用户的支付状态
            paymentService.updatePaymentInfo(paymentInfo);
        }

        return "finish";
    }

    @RequestMapping("mx/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String mx(String outTradNo, String totalAmount) {

        return null;
    }

    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String alipay(String outTradNo, BigDecimal totalAmount) {

        //获得一个支付宝请求的客户端（他并不是一个链接，而是一个封装好的http表单请求）

        String form = null;
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        //回调函数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", outTradNo);
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", 0.01);
        map.put("subject", "cc");

        String param = JSON.toJSONString(map);
        alipayRequest.setBizContent(param);

        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
            System.out.println(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        //生成并且保存用户的支付信息
        OmsOrder omsOrder = orderService.getOrderByOutTradNo(outTradNo);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderSn(outTradNo);
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setSubject("谷粒商城商品一件");
        paymentInfo.setTotalAmount(totalAmount);
        paymentService.savePaymentInfo(paymentInfo);

        //向消息队列发送一个检查支付状态（支付服务消费）的延迟消息队列
        paymentService.sendDelayPaymentResultCheckQueue(outTradNo,5);

        //提交请求到支付宝
        return form;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradNo, String totalAmount, ModelMap modelMap, HttpServletRequest request) {

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        modelMap.put("outTradNo", outTradNo);
        modelMap.put("totalAmount", totalAmount);
        modelMap.put("nickname", nickname);

        return "index";
    }
}
