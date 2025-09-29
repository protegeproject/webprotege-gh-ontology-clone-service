package edu.stanford.protege.github.cloneservice.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "webprotege.minio")
public class MinioProperties {

    private String accessKey;

    private String secretKey;

    private String endPoint;

    private String projectHistoryDocumentsBucketName;

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public String getProjectHistoryDocumentsBucketName() {
        return projectHistoryDocumentsBucketName;
    }

    public void setProjectHistoryDocumentsBucketName(String projectHistoryDocumentsBucketName) {
        this.projectHistoryDocumentsBucketName = projectHistoryDocumentsBucketName;
    }
}
