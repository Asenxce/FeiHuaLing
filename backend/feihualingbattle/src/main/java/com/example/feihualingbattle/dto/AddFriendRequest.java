package com.example.feihualingbattle.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddFriendRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String currentUsername;
    private String targetIdentityCode;

    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String currentUsername) { this.currentUsername = currentUsername; }
    public String getTargetIdentityCode() { return targetIdentityCode; }
    public void setTargetIdentityCode(String targetIdentityCode) { this.targetIdentityCode = targetIdentityCode; }
}
