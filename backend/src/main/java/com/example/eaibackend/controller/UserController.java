package com.example.eaibackend.controller;

import com.example.eaibackend.model.User;
import com.example.eaibackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // 用户注册接口
    @PostMapping("/register")
    public User register(@RequestParam String username, 
                        @RequestParam String password, 
                        @RequestParam String userType, 
                        @RequestParam String name, 
                        @RequestParam(required = false) String grade, 
                        @RequestParam(required = false) String subject, 
                        @RequestParam(required = false) String school) {
        return userService.registerUser(username, password, userType, name, grade, subject, school);
    }

    // 用户登录接口
    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String username, @RequestParam String password) {
        User user = userService.loginUser(username, password);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("user", user);
        response.put("userType", user.getUserType());
        return response;
    }

    // 获取用户信息
    @GetMapping("/info/{id}")
    public User getUserInfo(@PathVariable Integer id) {
        return userService.getUserById(id);
    }
}

