<%--
  Created by IntelliJ IDEA.
  User: wenjie
  Date: 2019-02-17
  Time: 23:52
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>生成二维码</title>
    <script src="js/jquery-1.8.3.min.js" type="text/javascript"></script>
    <script src="js/qrcode.min.js" type="text/javascript"></script>
</head>
<body>
    <div id="qrcode"></div>
    <script>
        $.ajax({
            url : "api/wx/createCode/D100000120170627141912fe12dd",
            method : "GET",
            success : function(data) {
                new QRCode(document.getElementById('qrcode'), data.data.code_url);
            }
        });

        //查询订单
        function queryOrder() {
            $.ajax({
                url : "api/wx/queryOrderStatus/D100000120170627141912fe12dd",
                method : "GET",
                success : function(result) {
                    if (result.success == 'true') {
                        var orderStatus = result.data.orderStatus;
                        if (orderStatus == 2) {
                            window.location.href="http://www.baidu.com";
                        }
                    }
                }
            });
        }
        //每隔5秒查看下订单的状态
        setInterval(queryOrder, 5000);
    </script>
</body>
</html>
