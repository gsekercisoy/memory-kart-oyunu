package com.acrom.memory.repository;

import com.acrom.memory.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findTop10ByOrderByMovesAscSecondsAsc();
}
