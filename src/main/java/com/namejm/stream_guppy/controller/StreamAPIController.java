package com.namejm.stream_guppy.controller;

import com.namejm.stream_guppy.dto.RestResult;
import com.namejm.stream_guppy.dto.StreamDto;
import com.namejm.stream_guppy.repository.StreamRepository;
import com.namejm.stream_guppy.vo.StreamVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/stream")
public class StreamAPIController {

    private final StreamRepository streamRepository;
    private final ModelMapper mapper;

    @GetMapping
    public RestResult getStreamList(StreamDto.StreamReqDto streamReqDto) throws Exception {
        RestResult result = new RestResult();

        List<StreamDto.StreamResDto> list = streamRepository.findAll()
                .stream().map((obj) -> mapper.map(obj, StreamDto.StreamResDto.class))
                .collect(Collectors.toList());

        result.setData(list);
        result.setSuccess(true);
        return result;
    }

    @GetMapping("/{streamKey}")
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

    @PostMapping
    public RestResult insertStreamList(@PathVariable String streamKey, @RequestBody StreamVO streamVO) throws Exception {
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

    @PutMapping("/{streamKey}")
    public RestResult updateStreamList(@PathVariable String streamKey, @RequestBody StreamVO streamVO) throws Exception {
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

    @DeleteMapping("/{streamKey}")
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
