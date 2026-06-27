package com.example.feihualingbattle.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class MarkReadRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long mailId;

    public Long getMailId() { return mailId; }
    public void setMailId(Long mailId) { this.mailId = mailId; }
}
