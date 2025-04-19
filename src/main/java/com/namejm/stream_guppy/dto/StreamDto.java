package com.namejm.stream_guppy.dto;

import lombok.Getter;
import lombok.Setter;

public class StreamDto {

    @Getter
    @Setter
    public static class StreamReqDto {
        private String streamKey;
    }

    @Getter
    @Setter
    public static class StreamResDto {
        private String streamKey;
        private String name;
        private String rtspUrl;
        private String createdAt;
        private String updatedAt;
    }

    @Getter
    @Setter
    public static class StreamSaveDto {
        private String streamKey;
        private String name;
        private String rtspUrl;
    }

}
