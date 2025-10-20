package com.mysite.knitly.utility.test.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
public class TestController {

    @GetMapping("/")
    public String home() {
        return """
                <h1>Google OAuth2 Test</h1>
                <a href="/oauth2/authorization/google">Google 로그인</a>
                """;
    }

    @GetMapping("/login/success")
    public String loginSuccess(@RequestParam String userId,
                               @RequestParam String email,
                               @RequestParam String name,
                               @RequestParam String accessToken) {
        String decodedEmail = URLDecoder.decode(email, StandardCharsets.UTF_8);
        String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8);

        return String.format("""
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial; padding: 20px; }
                        .token { 
                            background: #f5f5f5; 
                            padding: 10px; 
                            border-radius: 5px;
                            word-break: break-all;
                            margin: 10px 0;
                        }
                    </style>
                </head>
                <body>
                    <h1>🎉 로그인 성공!</h1>
                    <p><strong>User ID:</strong> %s</p>
                    <p><strong>이메일:</strong> %s</p>
                    <p><strong>이름:</strong> %s</p>
                    
                    <h2>🔑 Access Token</h2>
                    <div class="token">%s</div>
                    
                    <p>👉 이 토큰을 복사해서 API 요청 시 사용하세요!</p>
                    <p><code>Authorization: Bearer [토큰]</code></p>
                    
                    <a href="/">홈으로 돌아가기</a>
                </body>
                </html>
                """, userId, decodedEmail, decodedName, accessToken);
    }

    @GetMapping("/login/error")
    public String loginError(@RequestParam(required = false) String message) {
        String errorMessage = message != null ?
                URLDecoder.decode(message, StandardCharsets.UTF_8) :
                "알 수 없는 오류가 발생했습니다.";

        return String.format("""
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { 
                            font-family: Arial; 
                            padding: 20px; 
                            text-align: center;
                        }
                        .error-box {
                            background: #fee;
                            border: 1px solid #fcc;
                            padding: 20px;
                            border-radius: 5px;
                            margin: 20px auto;
                            max-width: 600px;
                        }
                        .btn {
                            display: inline-block;
                            margin-top: 20px;
                            padding: 10px 20px;
                            background: #007bff;
                            color: white;
                            text-decoration: none;
                            border-radius: 5px;
                        }
                    </style>
                </head>
                <body>
                    <h1>❌ 로그인 실패</h1>
                    <div class="error-box">
                        <p><strong>오류:</strong> %s</p>
                    </div>
                    <a href="/" class="btn">다시 시도하기</a>
                </body>
                </html>
                """, errorMessage);
    }
}