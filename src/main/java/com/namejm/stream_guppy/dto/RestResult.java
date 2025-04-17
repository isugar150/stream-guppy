package com.namejm.stream_guppy.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
// 이 클래스 내에서는 Jackson import가 더 이상 필요 없을 수 있습니다.

@Getter
@Setter
public class RestResult {
    private boolean success;
    private String message;

    private Object data;

    public void setData(Object payload) {
        this.data = payload;
    }
    public void setData(String name, Object value) {
        // 현재 data가 Map 타입인지 확인
        if (!(this.data instanceof Map)) {
            // Map이 아니거나 null이면, 새로운 HashMap을 생성하여 data 필드에 할당
            this.data = new HashMap<String, Object>();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapData = (Map<String, Object>) this.data;
            mapData.put(name, value);
        } catch (ClassCastException e) {
            System.err.println("경고: 내부 데이터가 Map<String, Object> 타입이 아니므로 키-값 쌍을 추가할 수 없습니다. data 필드를 새 Map으로 교체합니다.");
            this.data = new HashMap<String, Object>();
            ((Map<String, Object>) this.data).put(name, value);
        }
    }

    public RestResult() {}

    public RestResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static RestResult success(Object payload) {
        RestResult result = new RestResult(true, "Success");
        result.setData(payload);
        return result;
    }

    public static RestResult success(String key, Object value) {
        RestResult result = new RestResult(true, "Success");
        result.setData(key, value);
        return result;
    }

     public static RestResult successMessage(String message) {
         return new RestResult(true, message);
     }

    public static RestResult failure(String message) {
        return new RestResult(false, message);
    }
}