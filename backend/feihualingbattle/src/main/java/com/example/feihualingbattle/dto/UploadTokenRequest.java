package com.example.feihualingbattle.dto;

import lombok.Data;

@Data
public class UploadTokenRequest {
    private String fileType;
    private String fileName;

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
