package com.bugboard.dto;

import java.time.LocalDateTime;

public class IssueDTO {
    private Long id;
    private String title;
    private String status;
    private String priority;
    private String reporterName;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private String attachmentPath;

    public IssueDTO() {
    }

    public IssueDTO(Long id, String title, String status, String priority, String reporterName,
                    LocalDateTime createdAt, LocalDateTime closedAt, String attachmentPath) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.priority = priority;
        this.reporterName = reporterName;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
        this.attachmentPath = attachmentPath;
    }

    // Getters
    public Long getId() {
        return id;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }
}