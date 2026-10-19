package com.acrom.memory.service;

import com.acrom.memory.model.Game;
import com.acrom.memory.model.Score;
import com.acrom.memory.repository.GameRepository;
import com.acrom.memory.repository.ScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class MemoryService {

    private static final Object[] EMOJIS = {
            "🍉", "🚗", "🐝", "🎸", "🚀", "🐙", "🌵", "🍩",
            "⚽", "🦊", "☂️", "🎈", "🔑", "🧲", "🍄", "🦄"
    };

    public static final int[] ALLOWED_PAIRS = {6, 8, 12, 16};

    private final GameRepository games;
    private final ScoreRepository scores;
    private final Random rnd = new Random();

    public MemoryService(GameRepository games, ScoreRepository scores) {
        this.games = games;
        this.scores = scores;
    }

    @Transactional
    public Map<String, Object> create(String player, Integer pairsReq) {
        int pairs = 8; // varsayilan
        if (pairsReq != null) {
            boolean ok = false;
            for (int a : ALLOWED_PAIRS) {
                if (a == pairsReq) { ok = true; break; }
            }
            if (!ok) throw new IllegalArgumentException("pairs: 6, 8, 12 veya 16 olmali");
            pairs = pairsReq;
        }
        if (player != null && !player.trim().isEmpty()) {
            player = player.trim();
            if (player.length() > 20) player = player.substring(0, 20);
        } else {
            player = "MISAFIR";
        }

        List<String> chosen = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            chosen.add(String.valueOf(EMOJIS[i]));
        }
        chosen.addAll(new ArrayList<>(chosen));
        Collections.shuffle(chosen, rnd);

        Game g = new Game();
        g.player = player;
        g.pairs = pairs;
        g.deckCsv = String.join(",", chosen);
        games.save(g);

        return toState(g, null, null);
    }

    @Transactional
    public Map<String, Object> flip(Long id, Integer index) {
        Game g = games.findById(id).orElse(null);
        if (g == null) throw new IllegalArgumentException("Oyun bulunamadı");
        if (g.status != Game.Status.PLAYING) {
            throw new IllegalArgumentException("Oyun zaten bitti");
        }
        if (index == null || index < 0 || index >= g.deck().length) {
            throw new IllegalArgumentException("Geçersiz kart indeksi: " + index);
        }
        if (contains(g.matched(), index)) {
            throw new IllegalArgumentException("Bu kart zaten eŞleşmiş");
        }

        String value = g.deck()[index];
        Integer flipped = index;
        Integer[] newlyMatched = new Integer[0];
        String message;

        if (g.openIdx == null) {
            g.openIdx = index;
            message = "İkinci kartı seç";
        } else {
            g.moves++;
            String openValue = g.deck()[g.openIdx];
            if (openValue.equals(value)) {
                Integer a = g.openIdx, b = index;
                g.addMatched(a, b);
                newlyMatched = new Integer[]{a, b};
                int found = g.matched().length / 2;
                message = "Eşleşme! " + found + "/" + g.pairs;
                if (found == g.pairs) {
                    g.status = Game.Status.WON;
                    message = "Kazandın! Tüm kartlar eşleşti.";
                }
                g.openIdx = null;
            } else {
                message = "Olmadı — kapatabilirsin";
                g.openIdx = null;
            }
        }

        games.save(g);
        return toState(g, flipped, value, newlyMatched, message);
    }

    public Map<String, Object> state(Long id) {
        Game g = games.findById(id).orElse(null);
        if (g == null) throw new IllegalArgumentException("Oyun bulunamadı");
        return toState(g, null, null);
    }

    @Transactional
    public Map<String, Object> finish(Long id, Integer seconds) {
        Game g = games.findById(id).orElse(null);
        if (g == null) throw new IllegalArgumentException("Oyun bulunamadı");
        if (g.status != Game.Status.WON) {
            throw new IllegalArgumentException("Oyun henüz kazanılmadı");
        }
        if (seconds == null) seconds = 0;
        seconds = Math.max(0, Math.min(seconds, 24 * 3600));

        Score s = new Score();
        s.player = g.player;
        s.pairs = g.pairs;
        s.moves = g.moves;
        s.seconds = seconds;
        scores.save(s);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("saved", Boolean.TRUE);
        out.put("scoreId", s.id);
        out.put("player", s.player);
        out.put("moves", s.moves);
        out.put("seconds", s.seconds);
        return out;
    }

    @Transactional(readOnly = true)
    public List<Score> leaderboard() {
        return scores.findTop10ByOrderByMovesAscSecondsAsc();
    }

    private static boolean contains(Integer[] arr, Integer v) {
        for (Integer x : arr) {
            if (x.intValue() == v.intValue()) return true;
        }
        return false;
    }

    /** oyun durumunu istemci formatına döndürür: kart değerleri yalnızca açılanlar/eşleşenler */
    static Map<String, Object> toState(Game g, Integer flipped, String flipValue) {
        return toState(g, flipped, flipValue, new Integer[0], "");
    }

    static Map<String, Object> toState(Game g, Integer flipped, String flipValue, Integer[] newlyMatched, String message) {
        Integer[] matched = g.matched();
        String[] deck = g.deck();

        List<Integer> matchedList = new ArrayList<>();
        for (Integer m : matched) matchedList.add(m);

        // eşleşen kartların değerleri: istemci tahtayı yeniden kurabilsin (zaten ifşa olmuş)
        Map<Integer, String> matchedValues = new LinkedHashMap<>();
        for (Integer m : matched) {
            matchedValues.put(m, deck[m]);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", g.id);
        out.put("player", g.player);
        out.put("pairs", g.pairs);
        out.put("moves", g.moves);
        out.put("matched", matchedList);
        out.put("matchedValues", matchedValues);
        out.put("matchedCount", matchedList.size() / 2);
        out.put("hasOpenCard", g.hasOpenCard());
        out.put("status", g.status.name());
        out.put("message", message != null ? message : "");
        out.put("cards", Integer.valueOf(deck.length));

        if (g.hasOpenCard() && g.openIdx != null) {
            out.put("openIndex", g.openIdx);
            out.put("openValue", deck[g.openIdx]);
        }

        if (flipped != null) {
            out.put("flipped", (Integer) flipped);
            out.put("value", flipValue);
        }
        if (newlyMatched != null && newlyMatched.length > 0) {
            List<Integer> nm = new ArrayList<>();
            for (Integer m : newlyMatched) nm.add(m);
            out.put("newlyMatched", nm);
        }
        if (g.status != Game.Status.PLAYING) {
            out.put("deck", deck); // oyun bitti, tüm kartlar acik
        }
        return out;
    }
}
