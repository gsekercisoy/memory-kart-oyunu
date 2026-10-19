# Hafıza Kart Oyunu v2

React (Vite) ön yüz + Java (Spring Boot 2.7) REST API + H2 veritabanı.
Kart destesi sunucuda karıştırılır ve **değerleri gizli tutulur** — istemci her kart
açılışını `POST /flip` isteğiyle sorar; değerler yalnızca açıldığında/eşleştiğinde ifşa olur.

## Özellikler

- 6 / 8 / 12 / 16 çift seçilebilir deste (emoji kartlar)
- Oyun durumu sunucuda: açık kart, eşleşenler, hareket sayısı, durum makinesi
- Eşleşen kartların değerleri state yanıtında döner → sayfa yenilense de tahta geri kurulur
- Her harekette doğrulama: geçersiz indeks / tekrar açılan kart / biten oyun → 400
- Kazanınca `POST /finish` ile süre kaydı → rekor listesi (az hareket + az süre sırası)

## Çalıştırma

```bash
cd backend && mvn spring-boot:run     # API :8080
cd frontend && npm install && npm run dev   # UI :5173 (proxy → 8080)
```

## API

| Metot | Yol | Açıklama |
|---|---|---|
| POST | `/api/games` | `{player?, pairs?: 6\|8\|12\|16}` → `{id, cards, pairs, ...}` (deste gizli) |
| GET | `/api/games/{id}` | Mevcut durum (kart değerleri gizli) |
| POST | `/api/games/{id}/flip` | `{index}` → `{value?, newlyMatched?, matched, moves, status, message}` |
| POST | `/api/games/{id}/finish` | `{seconds}` — kazanan oyunu rekorlara yaz |
| GET | `/api/leaderboard` | Top-10 rekor (hareket↑, süre↑) |

v1 tek dosyalık HTML sürümü git geçmişinde korunmaktadır.
