package cn.itrip.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DefaultController {

    @RequestMapping("/hello")
    @ResponseBody
    public String helloWorld() {
        return "helloWorld";
    }
}
