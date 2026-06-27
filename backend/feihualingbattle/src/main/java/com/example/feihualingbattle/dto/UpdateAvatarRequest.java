package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class UpdateAvatarRequest {
    private String avatarUrl;

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
