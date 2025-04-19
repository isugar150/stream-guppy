
package com.namejm.stream_guppy.config;

import com.namejm.stream_guppy.repository.StreamRepository;
import com.namejm.stream_guppy.vo.StreamVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private final StreamRepository streamRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("Checking and inserting initial stream data if necessary...");

        if (streamRepository.count() == 0) {
            log.info("No existing stream data found. Inserting initial data.");
            int[] array = new int[]{1, 3, 7};
            for (int i = 0; i < array.length; i++) {
                StreamVO vo = new StreamVO();
                String key = "CCTV" + (i + 1);
                vo.setStreamKey(key);
                vo.setName("CCTV " + (i + 1));
                vo.setRtspUrl("rtsp://210.99.70.120:1935/live/cctv00" + array[i] + ".stream");
                streamRepository.save(vo);
                log.info("Saved initial data for stream key: {}", vo.getStreamKey());
            }
            log.info("Initial stream data inserted successfully.");
        } else {
            log.info("Initial stream data already exists ({}) count. Skipping insertion.", streamRepository.count());
        }
    }
}