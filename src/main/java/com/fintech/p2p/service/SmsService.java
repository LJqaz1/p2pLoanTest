package com.fintech.p2p.service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SmsService {

    public static String sendSMS(String phoneNumber, String message, String apiKey) {
        try {
            String urlParameters = "phone=" + URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8.toString())
                    + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
                    + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8.toString());

            URL url = new URL("https://textbelt.com/text");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); // 设置请求头

            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(urlParameters);
                wr.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return "{\"success\":false,\"error\":\"HTTP Error: " + responseCode + "\"}";
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }
            return parseJsonResponse(response.toString()); // 解析 JSON 响应
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    // 解析 JSON 响应
    private static String parseJsonResponse(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            boolean success = jsonNode.get("success").asBoolean();
            int quotaRemaining = jsonNode.has("quotaRemaining") ? jsonNode.get("quotaRemaining").asInt() : -1;
            return "成功与否: " + success + ", 剩余配额: " + quotaRemaining;
        } catch (Exception e) {
            return "JSON 解析失败: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        String result = sendSMS("+08064233598", "(test)【重要】您的贷款已逾期，请尽快还款！", "textbelt"); // 不换API Key的话，一天只能发一条
        System.out.println("結果:" + result);
    }
}