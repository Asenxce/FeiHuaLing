package com.example.feihualinggame.bean;

/**
 * 获取OSS上传凭证请求DTO
 */
public class UploadTokenRequest {
    private String fileType;
    private String fileName;

    public UploadTokenRequest() {}

    public UploadTokenRequest(String fileType) {
        this.fileType = fileType;
    }

    public UploadTokenRequest(String fileType, String fileName) {
        this.fileType = fileType;
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
