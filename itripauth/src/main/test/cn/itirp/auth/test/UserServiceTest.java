package cn.itirp.auth.test;

import cn.itrip.auth.service.UserService;
import cn.itrip.beans.pojo.ItripUser;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;

public class UserServiceTest {

    @Test
    public void testInsertUser() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext-mybatis.xml");
        UserService userService = (UserService)ctx.getBean("userService");
        ItripUser user = new ItripUser();
        user.setUserCode("itripczkt@aliyun.com");
        user.setUserName("小明");
        try {
            userService.insertUser(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testActivateUser() {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext-mybatis.xml");
        UserService userService = (UserService)ctx.getBean("userService");
        try {
            userService.activate("itripczkt@aliyun.com", "5bfc386f3abe350c930e96a12014db2f");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[]args) {
        System.out.println(new Date().toLocaleString());
    }
}
