package com.fintech.p2p.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    // 构造函数
    public UserDto() {
    }

    public UserDto(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public @NotBlank(message = "Email cannot be empty") @Email(message = "Invalid email format") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email cannot be empty") @Email(message = "Invalid email format") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Username cannot be empty") @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters") String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank(message = "Username cannot be empty") @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters") String username) {
        this.username = username;
    }

    public @NotBlank(message = "Password cannot be empty") @Size(min = 6, message = "Password must be at least 6 characters") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Password cannot be empty") @Size(min = 6, message = "Password must be at least 6 characters") String password) {
        this.password = password;
    }
}