package com.acrom.memory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memory_games")
public class Game {

    public enum Status { PLAYING, WON }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    public Long id;

    @Column(nullable = false)
    @JsonProperty("player")
    public String player = "MISAFIR";

    @Column(nullable = false)
    @JsonProperty("pairs")
    public int pairs;

    /** rastgele karýilmiþ kart degerleri, virgülle ayrilmis — istemciye asla tam olarak donmez */
    @Column(nullable = false, length = 400)
    @JsonIgnore
    public String deckCsv;

    /** eþleþen kartlar (indeks listesi) */
    @Column(length = 200)
    @JsonIgnore
    public String matchedCsv = "";

    /** acik olup eslesmeyi bekleyen tek kart (veya null) */
    @JsonIgnore
    public Integer openIdx = null;

    @JsonProperty("hasOpenCard")
    public boolean hasOpenCard() {
        return openIdx != null;
    }

    @Column(nullable = false)
    @JsonProperty("moves")
    public int moves = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    @JsonProperty("status")
    public Status status = Status.PLAYING;

    @JsonProperty("createdAt")
    public LocalDateTime createdAt = LocalDateTime.now();

    public String[] deck() {
        return deckCsv.split(",");
    }

    public Integer[] matched() {
        if (matchedCsv.isEmpty()) return new Integer[0];
        String[] parts = matchedCsv.split(",");
        Integer[] out = new Integer[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Integer.parseInt(parts[i]);
        return out;
    }

    public void addMatched(int a, int b) {
        String entry = matchedCsv.isEmpty()
                ? (a + "," + b)
                : matchedCsv + "," + a + "," + b;
        matchedCsv = entry;
    }
}
