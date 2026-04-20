package com.leo.airouterbackend;// 测试代码（可放在 main 方法或单元测试中）
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DeepSeekApiTest {
    public static void main(String[] args) throws Exception {
        String apiKey = "sk-b073f7d606f1497681434472c8ed624e";
        String body = """
            {
                "model": "deepseek-chat",
                "messages": [{"role": "user", "content": "Hello"}],
                "stream": false
            }
            """;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        long start = System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long duration = System.currentTimeMillis() - start;

        System.out.println("耗时: " + duration + " ms");
        System.out.println("状态码: " + response.statusCode());
        System.out.println("响应体: " + response.body());
    }
}