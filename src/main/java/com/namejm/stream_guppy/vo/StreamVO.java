package com.namejm.stream_guppy.vo;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class StreamVO {

    @Id
    private String streamKey;             // 관리용 이름

    @Column(nullable = false, length = 50)
    private String name;             // 관리용 이름

    @Column(nullable = false, length = 500)
    private String rtspUrl;          // 소스 RTS 주소

    private boolean useYn = false;   // 사용 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StreamVO() {}

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isEmpty() {
        return streamKey == null && name == null && rtspUrl == null;
    }
}
