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
                        body { 
                            font-family: Arial; 
                            padding: 20px;
                            max-width: 800px;
                            margin: 0 auto;
                        }
                        .token { 
                            background: #f5f5f5; 
                            padding: 10px; 
                            border-radius: 5px;
                            word-break: break-all;
                            margin: 10px 0;
                            font-size: 12px;
                        }
                        .info {
                            background: #e3f2fd;
                            padding: 15px;
                            border-radius: 5px;
                            margin: 20px 0;
                        }
                        .btn {
                            background: #007bff;
                            color: white;
                            border: none;
                            padding: 10px 20px;
                            border-radius: 5px;
                            cursor: pointer;
                            margin: 5px;
                        }
                        .btn:hover {
                            background: #0056b3;
                        }
                        .success {
                            background: #d4edda;
                            border: 1px solid #c3e6cb;
                            color: #155724;
                            padding: 10px;
                            border-radius: 5px;
                            margin: 10px 0;
                        }
                        .error {
                            background: #f8d7da;
                            border: 1px solid #f5c6cb;
                            color: #721c24;
                            padding: 10px;
                            border-radius: 5px;
                            margin: 10px 0;
                        }
                    </style>
                </head>
                <body>
                    <h1>🎉 로그인 성공!</h1>
                    <p><strong>User ID:</strong> %s</p>
                    <p><strong>이메일:</strong> %s</p>
                    <p><strong>이름:</strong> %s</p>
                    
                    <div class="info">
                        <h3>💡 Access Token (메모리 저장됨)</h3>
                        <p>이 토큰은 JavaScript 변수에 저장되어 있습니다.</p>
                        <p>개발자도구 Console에서 <code>window.accessToken</code>으로 확인할 수 있습니다.</p>
                    </div>
                    
                    <h2>🔑 Access Token</h2>
                    <div class="token" id="tokenDisplay">%s</div>
                    
                    <div>
                        <button class="btn" onclick="copyToken()">📋 토큰 복사</button>
                        <button class="btn" onclick="testGetUserInfo()">👤 내 정보 조회 (AT 사용)</button>
                        <button class="btn" onclick="testRefreshToken()">🔄 토큰 갱신 (RT 쿠키 사용)</button>
                        <button class="btn" onclick="checkTokenInConsole()">🔍 Console에서 AT 확인</button>
                    </div>
                    
                    <div id="result"></div>
                    
                    <h3>🍪 Refresh Token (쿠키 저장됨)</h3>
                    <p>개발자도구 > Application > Cookies에서 <code>refreshToken</code> 확인 가능</p>
                    
                    <hr>
                    <a href="/">홈으로 돌아가기</a>
                    
                    <script>
                        // Access Token을 전역 변수(메모리)에 저장
                        window.accessToken = "%s";
                        
                        console.log("✅ Access Token이 메모리에 저장되었습니다!");
                        console.log("👉 window.accessToken으로 확인하세요");
                        
                        function copyToken() {
                            navigator.clipboard.writeText(window.accessToken);
                            showResult('✅ 토큰이 클립보드에 복사되었습니다!', 'success');
                        }
                        
                        function checkTokenInConsole() {
                            console.log("=== Access Token (메모리) ===");
                            console.log(window.accessToken);
                            showResult('✅ Console을 확인하세요! (F12)', 'success');
                        }
                        
                        async function testGetUserInfo() {
                            try {
                                const response = await fetch('http://localhost:8080/users/me', {
                                    method: 'GET',
                                    headers: {
                                        'Authorization': 'Bearer ' + window.accessToken
                                    }
                                });
                                
                                if (response.ok) {
                                    const data = await response.json();
                                    showResult('✅ 내 정보 조회 성공!<br>' + JSON.stringify(data, null, 2), 'success');
                                    console.log("User Info:", data);
                                } else {
                                    showResult('❌ 조회 실패: ' + response.status, 'error');
                                }
                            } catch (error) {
                                showResult('❌ 에러: ' + error.message, 'error');
                            }
                        }
                        
                        async function testRefreshToken() {
                            try {
                                const response = await fetch('http://localhost:8080/auth/refresh', {
                                    method: 'POST',
                                    credentials: 'include'  // 쿠키 포함
                                });
                                
                                if (response.ok) {
                                    const data = await response.json();
                                    // 새로운 Access Token으로 업데이트
                                    window.accessToken = data.accessToken;
                                    document.getElementById('tokenDisplay').textContent = data.accessToken;
                                    
                                    showResult('✅ 토큰 갱신 성공!<br>새로운 AT가 메모리에 저장되었습니다.', 'success');
                                    console.log("New Access Token:", data.accessToken);
                                } else {
                                    showResult('❌ 갱신 실패: ' + response.status, 'error');
                                }
                            } catch (error) {
                                showResult('❌ 에러: ' + error.message, 'error');
                            }
                        }
                        
                        function showResult(message, type) {
                            const resultDiv = document.getElementById('result');
                            resultDiv.innerHTML = '<div class="' + type + '">' + message + '</div>';
                        }
                    </script>
                </body>
                </html>
                """, userId, decodedEmail, decodedName, accessToken, accessToken);
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