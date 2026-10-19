package com.acrom.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memory_scores")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    public Long id;

    @Column(nullable = false, length = 20)
    @JsonProperty("player")
    public String player;

    @Column(nullable = false)
    @JsonProperty("pairs")
    public int pairs;

    @Column(nullable = false)
    @JsonProperty("moves")
    public int moves;

    @Column(nullable = false)
    @JsonProperty("seconds")
    public int seconds;

    @JsonProperty("createdAt")
    public LocalDateTime createdAt = LocalDateTime.now();
}
