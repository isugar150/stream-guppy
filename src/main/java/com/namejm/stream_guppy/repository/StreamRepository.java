package com.namejm.stream_guppy.repository;

import com.namejm.stream_guppy.vo.StreamVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreamRepository extends JpaRepository<StreamVO, String> {
    List<StreamVO> findAll();
    Optional<StreamVO> findByStreamKey(String streamKey);
    List<StreamVO> findAllByUseYn(boolean useYn);

}