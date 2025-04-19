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
    public List<StreamDto.StreamResDto> getStreamList(StreamDto.StreamReqDto streamReqDto) throws Exception {
        return streamRepository.findAll()
                .stream().map((obj) -> mapper.map(obj, StreamDto.StreamResDto.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/{streamKey}")
    public StreamDto.StreamResDto getStreamList(@PathVariable String streamKey) throws Exception {
        if(streamKey == null || streamKey.isEmpty()) {
            throw new IllegalArgumentException("streamKey is null or empty");
        }
        return mapper.map(streamRepository.findByStreamKey(streamKey).orElse(new StreamVO()), StreamDto.StreamResDto.class);
    }

    @PostMapping
    public void insertStreamList(@PathVariable String streamKey, @RequestBody StreamDto.StreamSaveDto streamSaveDto) throws Exception {
        StreamVO streamVO = mapper.map(streamSaveDto, StreamVO.class);

        streamVO.setStreamKey(streamKey);
        String isValidCheck = streamVO.isValidCheck();
        if(!isValidCheck.isEmpty()) {
            throw new IllegalArgumentException(isValidCheck);
        }
        streamRepository.save(streamVO);
    }

    @PutMapping("/{streamKey}")
    public void updateStreamList(@PathVariable String streamKey, @RequestBody StreamDto.StreamSaveDto streamSaveDto) throws Exception {
        StreamVO streamVO = mapper.map(streamSaveDto, StreamVO.class);
        streamVO.setStreamKey(streamKey);
        String isValidCheck = streamVO.isValidCheck();
        if(!isValidCheck.isEmpty()) {
            throw new IllegalArgumentException(isValidCheck);
        }
        streamRepository.save(streamVO);
    }

    @DeleteMapping("/{streamKey}")
    public void deleteStreamList(@PathVariable String streamKey) throws Exception {
        if(streamKey == null || streamKey.isEmpty()) {
            throw new IllegalArgumentException("streamKey is null or empty");
        }
        streamRepository.deleteById(streamKey);
    }
}
