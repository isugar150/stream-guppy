package com.namejm.stream_guppy.config;

import lombok.Data;

@Data
public class StreamConfig {
    private Long id;                 // 고유 아이디
    private String streamKey;             // 관리용 이름
    private String name;             // 관리용 이름
    private String rtspUrl;          // 소스 RTSP 주소
    private boolean useYn = false;   // 사용 여부

    public boolean isEmpty() {
        return id == null && streamKey == null && name == null && rtspUrl == null;
    }
}
