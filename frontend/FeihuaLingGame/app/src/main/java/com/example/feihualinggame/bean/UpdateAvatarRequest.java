package com.example.feihualinggame.bean;

/**
 * 更新用户头像请求DTO
 */
public class UpdateAvatarRequest {
    private String avatarUrl;

    public UpdateAvatarRequest() {}

    public UpdateAvatarRequest(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
