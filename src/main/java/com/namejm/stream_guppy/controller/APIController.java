package com.namejm.stream_guppy.controller;

import com.namejm.stream_guppy.dto.RestResult;
import com.namejm.stream_guppy.repository.StreamRepository;
import com.namejm.stream_guppy.vo.StreamVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class APIController {

    private final StreamRepository streamRepository;

    @GetMapping("/stream")
    public RestResult getStreamList() throws Exception {
        RestResult result = new RestResult();
        result.setData(streamRepository.findAll());
        result.setSuccess(true);
        return result;
    }

    @GetMapping("/stream/{streamKey}")
    public RestResult getStreamList(@PathVariable String streamKey) throws Exception {
        RestResult result = new RestResult();
        if(streamKey == null || streamKey.isEmpty()) {
            result.setData("message", "streamKey is null or empty");
            return result;
        }
        result.setData(streamRepository.findByStreamKey(streamKey).orElse(new StreamVO()));
        result.setSuccess(true);
        return result;
    }

    @RequestMapping(value = "/stream/{streamKey}", method = {RequestMethod.POST, RequestMethod.PUT})
    public RestResult saveStreamList(@PathVariable String streamKey, @RequestBody StreamVO streamVO) throws Exception {
        RestResult result = new RestResult();
        streamVO.setStreamKey(streamKey);
        String isValidCheck = streamVO.isValidCheck();
        if(!isValidCheck.isEmpty()) {
            result.setData(isValidCheck);
            return result;
        }

        streamRepository.save(streamVO);
        result.setSuccess(true);
        return result;
    }

    @DeleteMapping("/stream/{streamKey}")
    public RestResult deleteStreamList(@PathVariable String streamKey) throws Exception {
        RestResult result = new RestResult();
        if(streamKey == null || streamKey.isEmpty()) {
            result.setData("message", "streamKey is null or empty");
            return result;
        }
        streamRepository.deleteById(streamKey);
        result.setSuccess(true);
        return result;
    }
}
