package com.namejm.stream_guppy.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Data
public class StreamVO {

    @Id
    private String streamKey;             // 관리용 이름

    @Column(nullable = false, length = 50)
    private String name;             // 관리용 이름

    @Column(nullable = false, length = 500)
    private String rtspUrl;          // 소스 RTS 주소

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
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

    public String isValidCheck() {
        if(streamKey == null || streamKey.isEmpty()) return "streamKey값은 필수입니다.";
        if(streamKey.length() < 3 || streamKey.length() > 15) return "streamKey는 3자 이상 15자 이하여야 합니다.";
        String regex = "^[a-zA-Z0-9]+$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(streamKey);
        if(!matcher.matches()) return "streamKey는 대/소문자 영문, 숫자만 입력 가능합니다.";

        if(name == null || name.isEmpty()) return "name값은 필수입니다.";
        if(name.length() < 3 || name.length() > 15) return "name은 3자 이상 15자 이하여야 합니다.";

        if(rtspUrl == null || rtspUrl.isEmpty()) return "rtspUrl값은 필수입니다.";
        String rtspRegex = "^rtsps?:\\/\\/(([^:@\\/]+(:[^:@\\/]+)?)@)?([^:\\/?#]+)(:(\\d+))?(\\/.*)?$";
        Pattern pattern2 = Pattern.compile(rtspRegex);
        Matcher matcher2 = pattern2.matcher(rtspUrl);
        if(!matcher2.matches()) return "rtspUrl 형식이 올바르지 않습니다.";

        return "";
    }

    public boolean isEmpty() {
        return streamKey == null && name == null && rtspUrl == null;
    }
}
