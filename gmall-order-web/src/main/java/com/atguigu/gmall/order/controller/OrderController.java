package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;

    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, HttpServletRequest request){

        ModelAndView mv = new ModelAndView();

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        //查询交易码
        String success = orderService.checkTradCode(memberId,tradeCode);

        if (success.equals("success")){

            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setAutoConfirmDay(7);
            omsOrder.setCreateTime(new Date());
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("快点发货哦");//订单备注

            String outTradNo = "gmall";
            outTradNo = outTradNo + System.currentTimeMillis();//将毫秒时间戳拼接到外部订单号
            SimpleDateFormat sf = new SimpleDateFormat("YYYYMMDDHHmm");
            outTradNo = outTradNo+sf.format(new Date());

            omsOrder.setOrderSn(outTradNo);//外部订单号
            omsOrder.setTotalAmount(totalAmount);
            omsOrder.setOrderType(1);
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceviAddreddById(receiveAddressId);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            //当前日期加一天，一天后配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            omsOrder.setReceiveTime(time);
            omsOrder.setSourceType(0);
            omsOrder.setStatus("0");
            omsOrder.setTotalAmount(totalAmount);

            //根据用户id获得要购买的商品列表（购物车）和总价格
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
            for (OmsCartItem omsCartItem : omsCartItems) {
                if (omsCartItem.getIsChecked().equals("1")){
                    //获得订单详情列表
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    //验价
                    boolean b = skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
                    if (b==false){
                        mv = new ModelAndView("tradeFail");
                        return mv;
                    }
                    //验库存,远程调用库存系统(等库存系统做好再加上)

                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    omsOrderItem.setOrderSn(outTradNo);//外部订单号
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                    omsOrderItem.setProductSkuCode("10086111111");
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductSn("仓库对应的商品编号"); //仓库中的skuId

                    omsOrderItems.add(omsOrderItem);
                }
            }
            omsOrder.setOmsOrderItems(omsOrderItems);

            //将订单和订单详情写入数据库
            //删除购物车数据
            orderService.saveOrder(omsOrder);

            //重定向带支付系统
            mv = new ModelAndView("redirect:http://payment.gmall.com:8087/index");
            mv.addObject("outTradNo",outTradNo);
            mv.addObject("totalAmount",totalAmount);
            return mv;

        }else {
            mv = new ModelAndView("tradeFail");
            return mv;
        }

    }

    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, ModelMap modelMap){

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        //收货人地址列表
        List<UmsMemberReceiveAddress> userAddressList = userService.umsMemberReceiveAddress(memberId);
        modelMap.put("userAddressList",userAddressList);

        //将购物车列表转化为订单列表
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        for (OmsCartItem omsCartItem : omsCartItems) {
            if (omsCartItem.getIsChecked().equals("1")){
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                omsOrderItems.add(omsOrderItem);
            }
        }

        modelMap.put("totalAmount",getTotalAmount(omsCartItems));
        modelMap.put("omsOrderItems",omsOrderItems);

        //生成交易码，为了防止重复提交订单
        String tradeCode = orderService.genTradCode(memberId);
        modelMap.put("tradeCode",tradeCode);

        return "trade";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {

        BigDecimal bigDecimal = new BigDecimal("0");

        for (OmsCartItem omsCartItem : omsCartItems) {
            if (omsCartItem.getIsChecked().equals("1")){
                bigDecimal = bigDecimal.add(omsCartItem.getTotalPrice());
            }
        }

        return bigDecimal;
    }
}
