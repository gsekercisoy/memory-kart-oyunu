import React, { useEffect, useRef, useState } from 'react';
import { api } from './api.js';

export default function App() {
  const [game, setGame] = useState(null);       // sunucu durumu: id, cards, pairs, moves...
  const [cards, setCards] = useState([]);       // istemci görünümü: {val, matched}
  const [firstIdx, setFirstIdx] = useState(null);
  const [message, setMessage] = useState('');
  const [board, setBoard] = useState([]);
  const [player, setPlayer] = useState(() => localStorage.getItem('memoryPlayer') || '');
  const [pairs, setPairs] = useState(8);
  const [seconds, setSeconds] = useState(0);
  const [err, setErr] = useState('');
  const busy = useRef(false);
  const firstRef = useRef(null);
  const startedAt = useRef(0);
  const finished = useRef(false);

  const loadBoard = () => api.get('/leaderboard').then(setBoard).catch(() => {});

  useEffect(() => { loadBoard(); }, []);

  // devam eden oyunu geri getir
  useEffect(() => {
    const id = localStorage.getItem('memoryGameId');
    if (!id) return;
    api.get(`/games/${id}`).then(s => {
      rebuild(s);
    }).catch(() => {
      localStorage.removeItem('memoryGameId');
    });
  }, []);

  const rebuild = (s) => {
    setGame(s);
    const mv = s.matchedValues || {};
    const arr = Array.from({ length: s.cards }, (_, i) => ({
      val: mv[i] !== undefined ? mv[i] : null,
      matched: (s.matched || []).includes(i)
    }));
    if (s.hasOpenCard && s.openIndex !== undefined) {
      arr[s.openIndex] = { val: s.openValue, matched: false };
      firstRef.current = s.openIndex;
      setFirstIdx(s.openIndex);
    }
    setCards(arr);
    startedAt.current = Date.now() - (s.moves * 5000); // yaklaşık devam süresi
    finished.current = false;
  };

  const newGame = async () => {
    setErr('');
    busy.current = true;
    try {
      const s = await api.post('/games', { player: player.trim() || 'MISAFIR', pairs });
      if (player.trim()) localStorage.setItem('memoryPlayer', player.trim());
      localStorage.setItem('memoryGameId', String(s.id));
      finished.current = false;
      startedAt.current = Date.now();
      firstRef.current = null;
      setFirstIdx(null);
      setMessage('');
      setCards(Array.from({ length: s.cards }, () => ({ val: null, matched: false })));
      setGame(s);
    } catch (e) { setErr(e.message); }
    busy.current = false;
  };

  const applyMatched = (resp) => {
    setCards(prev => prev.map((c, i) => {
      if (resp.newlyMatched.includes(i)) return { ...c, matched: true };
      return c;
    }));
  };

  const flip = async (i) => {
    if (!game || game.status !== 'PLAYING') return;
    if (busy.current) return;
    if (cards[i].matched) return;
    if (firstRef.current === i) return;

    busy.current = true;
    try {
      const resp = await api.post(`/games/${game.id}/flip`, { index: i });
      setMessage(resp.message || '');

      if (resp.newlyMatched && resp.newlyMatched.length > 0) {
        // eşleşme: kartları kalıcı aç
        setCards(prev => prev.map((c, k) => {
          if (resp.newlyMatched.includes(k)) {
            const known = k === i ? resp.value : c.val;
            return { val: known, matched: true };
          }
          return c;
        }));
        firstRef.current = null;
        setFirstIdx(null);
        if (resp.status === 'WIN' || resp.status === 'WON') {
          setGame(g => ({ ...g, status: 'WON', moves: resp.moves, matchedCount: resp.matchedCount }));
          const secs = Math.max(1, Math.round((Date.now() - startedAt.current) / 1000));
          setSeconds(secs);
          if (!finished.current) {
            finished.current = true;
            api.post(`/games/${game.id}/finish`, { seconds: secs })
              .then(() => { loadBoard(); localStorage.removeItem('memoryGameId'); })
              .catch(() => {});
          }
        } else {
          setGame(g => ({ ...g, moves: resp.moves, matchedCount: resp.matchedCount }));
        }
      } else if (firstRef.current === null && resp.value) {
        // ilk kart açıldı
        const idx = resp.fl !== undefined ? resp.fl : resp.flipped;
        setCards(prev => prev.map((c, k) => (k === idx ? { val: resp.value, matched: false } : c)));
        firstRef.current = idx;
        setFirstIdx(idx);
        setGame(g => ({ ...g, moves: resp.moves }));
      } else {
        // ikinci kart, eşleşmedi: kısa süre açık kalacak
        const idx = resp.flipped;
        setGame(g => ({ ...g, moves: resp.moves }));
        setCards(prev => prev.map((c, k) => (k === idx ? { val: resp.value, matched: false } : c)));
        const a = firstRef.current;
        setTimeout(() => {
          setCards(prev => prev.map((c, k) => {
            if (k === idx || k === a) return { val: null, matched: c.matched };
            return c;
          }));
          firstRef.current = null;
          setFirstIdx(null);
        }, 850);
      }
    } catch (e) {
      setErr(e.message);
    }
    busy.current = false;
  };

  // play view
  if (game) {
    const cols = game.cards <= 16 ? 4 : game.cards <= 24 ? 6 : 8;
    const matchedCount = (game.matchedCount !== undefined ? game.matchedCount : cards.filter(c => c.matched).length / 2);
    const won = game.status === 'WON' || (message || '').indexOf('Kazand') === 0;

    return (
      <div className="app">
        <h1>🧠 Hafıza Kart Oyunu v2</h1>
        <p className="lead">Kart değerleri sunucuda gizli tutulur — her açılış bir POST isteğidir.</p>

        <div className="hud">
          <span>{game.player}</span>
          <span>Hareket: <b>{game.moves}</b></span>
          <span>Eşleşme: <b>{matchedCount}/{game.pairs}</b></span>
          <span>Süre: <b>{won ? seconds + ' sn' : '⏱'}</b></span>
        </div>

        {won && <div className="banner">🎉 Kazandın! {game.moves} hareket, {seconds} saniye</div>}
        {message && !won && <div className="msgline">{message}</div>}
        {err && <div className="err">{err}</div>}

        <div className="board" style={{ gridTemplateColumns: `repeat(${cols}, 1fr)` }}>
          {cards.map((c, i) => (
            <button
              key={i}
              className={`card ${c.val ? 'open' : ''} ${c.matched ? 'matched' : ''}`}
              onClick={() => flip(i)}
              disabled={c.matched || game.status !== 'PLAYING'}
            >
              {c.val || ''}
            </button>
          ))}
        </div>

        <div className="actions">
          <button className="btn primary" onClick={newGame}>Yeni Oyun</button>
        </div>

        <Leaders board={board} />
      </div>
    );
  }

  // start view
  return (
    <div className="app">
      <h1>🧠 Hafıza Kart Oyunu v2</h1>
      <p className="lead">Karıştırma ve eşleşme mantığı Java REST API'de; istemci kartları göremez.</p>
      {err && <div className="err">{err}</div>}

      <div className="panel start">
        <label>Oyuncu adı</label>
        <input
          value={player}
          onChange={e => setPlayer(e.target.value)}
          placeholder="MISAFIR"
          maxLength={20}
        />
        <label>Çift sayısı</label>
        <div className="pairsr">
          {[6, 8, 12, 16].map(n => (
            <button key={n} className={n === pairs ? 'active' : ''} onClick={() => setPairs(n)}>
              {n} çift ({n * 2} kart)
            </button>
          ))}
        </div>
        <button className="btn primary" onClick={newGame}>Oyunu Başlat</button>
      </div>

      <Leaders board={board} />
    </div>
  );
}

function Leaders({ board }) {
  return (
    <div className="panel">
      <h2>🏆 Rekorlar (sunucudan — az hareket, az süre)</h2>
      <table className="tbl">
        <thead><tr><th>#</th><th>Oyuncu</th><th>Çift</th><th>Hareket</th><th>Süre</th></tr></thead>
        <tbody>
          {board.map((s, i) => (
            <tr key={s.id}>
              <td>{i + 1}</td>
              <td>{s.player}</td>
              <td>{s.pairs}</td>
              <td className="num">{s.moves}</td>
              <td>{s.seconds} sn</td>
            </tr>
          ))}
          {board.length === 0 && <tr><td colSpan={5} className="muted">Henüz rekor yok</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
