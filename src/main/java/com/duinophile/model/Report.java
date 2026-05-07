package com.duinophile.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "reports")
public class Report {
    @Id
    private String id;
    private String reporterId;
    private String reporterName;
    private String commentId;
    private String postId;
    private String commentContent;
    private String commentAuthorName;
    private String reason;
    private String status = "PENDING"; // PENDING, ACCEPTED, REJECTED
    private Date createdAt = new Date();

    public Report() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getCommentContent() { return commentContent; }
    public void setCommentContent(String commentContent) { this.commentContent = commentContent; }

    public String getCommentAuthorName() { return commentAuthorName; }
    public void setCommentAuthorName(String commentAuthorName) { this.commentAuthorName = commentAuthorName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
