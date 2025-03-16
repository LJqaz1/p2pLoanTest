package com.fintech.p2p.controller;

import com.fintech.p2p.dto.UserDto;
import com.fintech.p2p.model.User;
import com.fintech.p2p.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 用户注册接口
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDto userDto) {
        if (userService.findByUsername(userDto.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        User registeredUser = userService.register(userDto);
        return ResponseEntity.ok(Map.of(
                "id", registeredUser.getId(),
                "username", registeredUser.getUsername(),
                "message", "User registered successfully"
        ));
    }

    // 获取用户信息接口
    @GetMapping("/{username}")
    public ResponseEntity<Optional<User>> getUser(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByUsername(username));
    }

    // 其他用户相关的接口，如登录、更新个人资料等
}
