package cn.itrip.trade.controller;

import cn.itrip.beans.dto.Dto;
import cn.itrip.beans.pojo.ItripHotelOrder;
import cn.itrip.common.DtoUtil;
import cn.itrip.trade.config.WxConfig;
import cn.itrip.trade.service.OrderService;
import cn.itrip.trade.wx.WXPayConstants;
import cn.itrip.trade.wx.WXPayUtil;
import cn.itrip.trade.wx.WxPayRequest;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/wx/")
public class WxPaymentController {

    @Resource
    private OrderService orderService;

    @Resource
    private WxConfig wxConfig;

    Logger logger = Logger.getLogger(WxPaymentController.class);

    /**
     * 生成微信支付二维码的方法
     * @param orderNo 订单编号
     * @return
     */
    @RequestMapping(value = "/createCode/{orderNo}", method = RequestMethod.GET)
    @ResponseBody
    public Dto createCode(@PathVariable String orderNo) {
        try {
            //根据订单编号查询订单详情对象信息。
            ItripHotelOrder order = orderService.loadItripHotelOrder(orderNo);
            //1、构造参数
            Map<String,String> data  = new HashMap<String, String>();
            data.put("body", "腾讯充值中心-QQ会员充值");
            data.put("out_trade_no",orderNo); //商户订单号
            data.put("device_info", "");
            data.put("fee_type", "CNY");
            //支付金额这块，因为要将payAmount的数值转换成"分"，所以要在原金额的基础上再乘以100
            data.put("total_fee", order.getPayAmount().multiply(new BigDecimal(100)).toBigInteger().toString()); //订单的金额
            data.put("spbill_create_ip", "123.12.12.123");
            data.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
            //data.put("notify_url", "http://itripczkt.free.idcfengye.com/itriptrade/api/wx/notify");
            data.put("notify_url", "http://itriptest.free.idcfengye.com/itriptrade/api/wx/notify");
            data.put("trade_type", "NATIVE");
            data.put("product_id", "12");
            data.put("appid", wxConfig.getAppid());//appid暂时用的是测试用的appid
            data.put("mch_id", wxConfig.getMchid());
            data.put("sign_type", "HMAC-SHA256");

            //2、转成XML请求微信支付平台   key API密钥
            String reqXml = WXPayUtil.generateSignedXml(data, wxConfig.getKey(), WXPayConstants.SignType.HMACSHA256);
            //3、请求微信支付平台，获取预支付交易链接
            //下面网址获取位置，https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=9_1
            String respXml = WxPayRequest.requestWx("https://api.mch.weixin.qq.com/pay/unifiedorder", reqXml);
            Map<String,String> resultMap = WXPayUtil.xmlToMap(respXml);//将字符串转换成Map集合。
            if (resultMap.get("return_code").equals("SUCCESS") && resultMap.get("result_code").equals("SUCCESS")) {
                Map<String, String> result = new HashMap<String,String>();
                result.put("code_url", resultMap.get("code_url"));
                return DtoUtil.returnDataSuccess(result);
            } else {
                return DtoUtil.returnFail(resultMap.get("return_msg"), "100002");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //3、获取code_url 返回给前端
        return DtoUtil.returnFail("生成微信支付二维码时发生错误","100001");
    }

    /**
     * 定义接收来自微信平台的异步通知的方法
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/notify")
    public void wxNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //1、从request获取XML流，转化成为MAP数据。
        logger.info("=====notify方法成功被调用=====");
        try {
            StringBuffer sb = new StringBuffer();
            InputStream inputStream = request.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String temp;
            while((temp = reader.readLine()) != null) {
                sb.append(temp);
            }
            reader.close();
            inputStream.close();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(sb.toString());
            boolean flag = WXPayUtil.isSignatureValid(resultMap, wxConfig.getKey() , WXPayConstants.SignType.HMACSHA256);
            //2、识别是否成功，修改订单状态
            if (flag) {
                logger.info("=====notify 签名验证 成功通过=====");
                if (resultMap.get("return_code").equals("SUCCESS") && resultMap.get("result_code").equals("SUCCESS")) {
                    logger.info("=====notify 通知， 订单支付成功！=====");
                    String out_trade_no = resultMap.get("out_trade_no");
                    String trade_no = resultMap.get("prepay_id");
                    if (!orderService.processed(out_trade_no)) {
                        orderService.paySuccess(out_trade_no, 2 , trade_no);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("=====notify 通知， 出现异常，异常信息：！=====" + e.getMessage());
        } finally {
            logger.info("===== 成功返回数据 =====");
            //3、给微信返回结果
            Map<String,String> returnMap = new HashMap<String,String>();
            returnMap.put("return_code", "SUCCESS");
            returnMap.put("return_msg", "SUCCESS");
            String respXml = WXPayUtil.generateSignedXml(returnMap, wxConfig.getKey());
            response.getWriter().write(respXml);
            response.getWriter().flush();
        }
    }

    @RequestMapping(value="/queryOrderStatus/{orderNo}", method=RequestMethod.GET)
    @ResponseBody
    public Dto queryOrderStatus(@PathVariable String orderNo) {
        //将订单查询出来
        try {
            ItripHotelOrder order = orderService.loadItripHotelOrder(orderNo);
            return DtoUtil.returnDataSuccess(order);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DtoUtil.returnFail("查询失败", "100003");
    }
}
