package com.example.feihualingbattle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String username;
    private String identityCode;
    private String token;
    private String sessionId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getIdentityCode() { return identityCode; }
    public void setIdentityCode(String identityCode) { this.identityCode = identityCode; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
