import json

with open('/data/data/com.termux/files/home/massive_100m_cleaned.json', 'r', encoding='utf-8') as f:
    songs = json.load(f)

print(f"Loaded {len(songs)} verified 100M+ songs.")

songs_json = json.dumps(songs, ensure_ascii=False)

html_content = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>100M+ Hits</title>
  <style>
    :root {
      --bg-base: #0a0b10;
      --bg-card: #131622;
      --bg-card-hover: #1c2030;
      --accent: #00e5ff;
      --text-main: #f8fafc;
      --text-muted: #8a96aa;
      --border: rgba(255, 255, 255, 0.07);
      --radius: 12px;
    }

    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
      -webkit-tap-highlight-color: transparent;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    }

    body {
      background-color: var(--bg-base);
      color: var(--text-main);
      min-height: 100vh;
      overflow-x: hidden;
      padding: 6px 10px 30px;
    }

    /* DIRECT LIST - NO TOP HEADER, ROW 1 STARTS AT THE VERY TOP */
    .song-list {
      display: flex;
      flex-direction: column;
      gap: 7px;
    }

    .song-item {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      padding: 8px 12px;
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      transition: background 0.15s ease, border-color 0.15s ease;
      position: relative;
    }

    .song-item:active {
      background: var(--bg-card-hover);
    }

    .song-item.active {
      background: rgba(0, 229, 255, 0.08);
      border-color: rgba(0, 229, 255, 0.5);
    }

    /* RANK NUMBER */
    .song-rank {
      font-size: 0.86rem;
      font-weight: 800;
      width: 32px;
      text-align: center;
      color: var(--text-muted);
      flex-shrink: 0;
    }

    .song-item:nth-child(1) .song-rank { color: #f59e0b; }
    .song-item:nth-child(2) .song-rank { color: #94a3b8; }
    .song-item:nth-child(3) .song-rank { color: #d97706; }

    /* THUMBNAIL */
    .thumb-box {
      width: 48px;
      height: 48px;
      border-radius: 8px;
      overflow: hidden;
      flex-shrink: 0;
      position: relative;
      background: #171b26;
    }

    .thumb-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    /* PLAYING EQUALIZER */
    .equalizer-icon {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.6);
      display: none;
      align-items: center;
      justify-content: center;
      gap: 2px;
    }

    .song-item.active.playing .equalizer-icon {
      display: flex;
    }

    .eq-bar {
      width: 3px;
      background: var(--accent);
      border-radius: 1px;
      animation: eqAnim 0.7s ease-in-out infinite alternate;
    }
    .eq-bar:nth-child(1) { height: 8px; animation-delay: 0.1s; }
    .eq-bar:nth-child(2) { height: 16px; animation-delay: 0.3s; }
    .eq-bar:nth-child(3) { height: 11px; animation-delay: 0.2s; }

    @keyframes eqAnim {
      0% { height: 4px; }
      100% { height: 18px; }
    }

    /* SONG DETAILS */
    .song-info {
      flex: 1;
      min-width: 0;
    }

    .song-title {
      font-size: 0.88rem;
      font-weight: 700;
      color: var(--text-main);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .song-artist {
      font-size: 0.74rem;
      color: var(--text-muted);
      margin-top: 2px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    /* CLEAN TEXT VIEWS TAG - NO EMOJIS OR ICONS */
    .views-tag {
      flex-shrink: 0;
      font-size: 0.76rem;
      font-weight: 700;
      color: #38bdf8;
      background: rgba(56, 189, 248, 0.08);
      border: 1px solid rgba(56, 189, 248, 0.2);
      padding: 4px 8px;
      border-radius: 6px;
    }

    /* INFINITE SCROLL SENTINEL & LOADER */
    .sentinel-box {
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-muted);
      font-size: 0.78rem;
      margin-top: 10px;
    }

    /* PRIVACY SAFETY TOAST (BLUETOOTH / EARBUDS) */
    .toast-container {
      position: fixed;
      bottom: 24px;
      left: 50%;
      transform: translateX(-50%) translateY(120px);
      background: rgba(18, 20, 29, 0.95);
      border: 1px solid rgba(0, 229, 255, 0.4);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
      backdrop-filter: blur(10px);
      padding: 12px 20px;
      border-radius: 30px;
      color: #f8fafc;
      font-size: 0.84rem;
      font-weight: 600;
      z-index: 9999;
      pointer-events: none;
      transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      display: flex;
      align-items: center;
      gap: 10px;
      max-width: 90%;
      text-align: center;
    }

    .toast-container.show {
      transform: translateX(-50%) translateY(0);
    }

    /* HIDDEN AUDIO ENGINE */
    #yt-audio-container {
      position: fixed;
      bottom: 0;
      right: 0;
      width: 1px;
      height: 1px;
      opacity: 0.01;
      pointer-events: none;
      z-index: -1;
    }
  </style>
</head>
<body>

  <!-- DIRECT SONG LIST (NO TOP HEADER) -->
  <div class="song-list" id="songList"></div>

  <!-- SENTINEL FOR INFINITE SCROLL -->
  <div class="sentinel-box" id="sentinel">Loading more 100M+ songs...</div>

  <!-- PRIVACY SAFETY FLOATING TOAST -->
  <div class="toast-container" id="toastBox">
    <span id="toastMsg">Bluetooth ba Earbud connect korun</span>
  </div>

  <!-- AUDIO ENGINE -->
  <div id="yt-audio-container">
    <div id="youtube-audio-engine"></div>
  </div>

  <script src="https://www.youtube.com/iframe_api"></script>

  <script>
    const SONGS = __SONGS_JSON__;

    let activeIndex = -1;
    let isPlaying = false;
    let ytPlayer = null;
    let renderedCount = 0;
    const BATCH_SIZE = 30;
    let toastTimeout = null;

    function showToast(msg) {
      const box = document.getElementById('toastBox');
      const text = document.getElementById('toastMsg');
      text.textContent = msg;
      box.classList.add('show');
      if (toastTimeout) clearTimeout(toastTimeout);
      toastTimeout = setTimeout(() => {
        box.classList.remove('show');
      }, 3500);
    }

    function checkHeadsetSafety() {
      if (window.AndroidMedia && window.AndroidMedia.isHeadphonesConnected) {
        try {
          const connected = window.AndroidMedia.isHeadphonesConnected();
          if (!connected) {
            showToast("Bluetooth ba Earbud connect korun. Speaker e sound hobe na.");
            if (ytPlayer && isPlaying) {
              ytPlayer.pauseVideo();
              setPlaying(false);
            }
            return false;
          }
        } catch (e) {}
      }
      return true;
    }

    // Called automatically from Android Java when earbuds are unplugged / disconnected
    window.onHeadphonesDisconnected = function() {
      if (ytPlayer) {
        try { ytPlayer.pauseVideo(); } catch (e) {}
      }
      setPlaying(false);
      showToast("Earbud disconnect hoyeche. Gaan pause kora holo.");
    };

    window.onHeadphoneStateChanged = function(connected) {
      if (!connected) {
        if (isPlaying && ytPlayer) {
          try { ytPlayer.pauseVideo(); } catch (e) {}
          setPlaying(false);
        }
        showToast("Bluetooth/Earbud disconnected (Speaker off)");
      } else {
        showToast("Bluetooth/Earbud connected");
      }
    };

    function onYouTubeIframeAPIReady() {
      ytPlayer = new YT.Player('youtube-audio-engine', {
        height: '100%',
        width: '100%',
        videoId: SONGS[0].id,
        playerVars: {
          'playsinline': 1,
          'controls': 0,
          'disablekb': 1,
          'rel': 0,
          'modestbranding': 1,
          'origin': window.location.origin
        },
        events: {
          'onStateChange': onPlayerStateChange
        }
      });
    }

    function onPlayerStateChange(event) {
      if (event.data === YT.PlayerState.PLAYING) {
        setPlaying(true);
      } else if (event.data === YT.PlayerState.PAUSED) {
        setPlaying(false);
      } else if (event.data === YT.PlayerState.ENDED) {
        playNext();
      }
    }

    function setPlaying(playing) {
      isPlaying = playing;
      updateActiveCard();

      if (window.AndroidMedia && activeIndex >= 0) {
        const s = SONGS[activeIndex];
        window.AndroidMedia.onStateChanged(isPlaying);
      }

      if ('mediaSession' in navigator && activeIndex >= 0) {
        navigator.mediaSession.playbackState = playing ? 'playing' : 'paused';
      }
    }

    function playIndex(idx) {
      if (!checkHeadsetSafety()) {
        return;
      }

      if (activeIndex === idx) {
        togglePlay();
        return;
      }

      activeIndex = idx;
      const song = SONGS[activeIndex];
      if (!song) return;

      if (ytPlayer && ytPlayer.loadVideoById) {
        ytPlayer.loadVideoById(song.id);
      }

      updateActiveCard();
      setupMediaSession(song);

      if (window.AndroidMedia) {
        window.AndroidMedia.onTrackChanged(
          song.title,
          song.artist,
          song.views,
          getThumbUrl(song.id),
          true
        );
      }
    }

    function togglePlay() {
      if (!checkHeadsetSafety()) {
        return;
      }
      if (!ytPlayer) return;
      if (isPlaying) {
        ytPlayer.pauseVideo();
      } else {
        ytPlayer.playVideo();
      }
    }

    function playNext() {
      if (SONGS.length === 0) return;
      const next = (activeIndex + 1) % SONGS.length;
      playIndex(next);
    }

    function playPrev() {
      if (SONGS.length === 0) return;
      const prev = (activeIndex - 1 + SONGS.length) % SONGS.length;
      playIndex(prev);
    }

    function updateActiveCard() {
      document.querySelectorAll('.song-item').forEach((item) => {
        const idx = parseInt(item.dataset.index, 10);
        if (idx === activeIndex) {
          item.classList.add('active');
          if (isPlaying) item.classList.add('playing');
          else item.classList.remove('playing');
        } else {
          item.classList.remove('active', 'playing');
        }
      });
    }

    function getThumbUrl(id) {
      return 'https://i.ytimg.com/vi/' + id + '/hqdefault.jpg';
    }

    function setupMediaSession(song) {
      if ('mediaSession' in navigator) {
        navigator.mediaSession.metadata = new MediaMetadata({
          title: song.title,
          artist: song.artist + ' • ' + song.views,
          album: '100M+ Hits Music',
          artwork: [
            { src: 'https://i.ytimg.com/vi/' + song.id + '/hqdefault.jpg', sizes: '480x360', type: 'image/jpeg' },
            { src: 'https://img.youtube.com/vi/' + song.id + '/mqdefault.jpg', sizes: '320x180', type: 'image/jpeg' }
          ]
        });

        navigator.mediaSession.setActionHandler('play', () => togglePlay());
        navigator.mediaSession.setActionHandler('pause', () => togglePlay());
        navigator.mediaSession.setActionHandler('previoustrack', () => playPrev());
        navigator.mediaSession.setActionHandler('nexttrack', () => playNext());
      }
    }

    // INFINITE SCROLL BATCH RENDERER
    function renderNextBatch() {
      const container = document.getElementById('songList');
      const start = renderedCount;
      const end = Math.min(start + BATCH_SIZE, SONGS.length);

      for (let i = start; i < end; i++) {
        const song = SONGS[i];
        const rank = i + 1;
        const item = document.createElement('div');
        item.className = 'song-item';
        item.dataset.index = i;
        if (i === activeIndex) {
          item.classList.add('active');
          if (isPlaying) item.classList.add('playing');
        }

        const hqThumb = 'https://i.ytimg.com/vi/' + song.id + '/hqdefault.jpg';
        const mqThumb = 'https://img.youtube.com/vi/' + song.id + '/mqdefault.jpg';

        item.innerHTML = `
          <div class="song-rank">#${rank}</div>
          <div class="thumb-box">
            <img class="thumb-img" src="${hqThumb}" alt="" loading="lazy"
                 onerror="this.onerror=null; this.src='${mqThumb}';">
            <div class="equalizer-icon">
              <div class="eq-bar"></div>
              <div class="eq-bar"></div>
              <div class="eq-bar"></div>
            </div>
          </div>
          <div class="song-info">
            <div class="song-title">${song.title}</div>
            <div class="song-artist">${song.artist}</div>
          </div>
          <div class="views-tag">${song.views}</div>
        `;

        item.addEventListener('click', () => {
          playIndex(i);
        });

        container.appendChild(item);
      }

      renderedCount = end;

      const sentinel = document.getElementById('sentinel');
      if (renderedCount >= SONGS.length) {
        sentinel.style.display = 'none';
      }
    }

    // INTERSECTION OBSERVER FOR SEAMLESS INFINITE SCROLL
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          if (renderedCount < SONGS.length) {
            renderNextBatch();
          } else if (window.AndroidMedia && window.AndroidMedia.fetchMoreOnlineSongs) {
            window.AndroidMedia.fetchMoreOnlineSongs(Math.floor(renderedCount / 30));
          }
        }
      });
    }, { rootMargin: '300px' });

    // Initial render
    renderNextBatch();
    renderNextBatch(); // initial 60 songs loaded immediately
    observer.observe(document.getElementById('sentinel'));

    // Dynamic online fetch response receiver
    window.onMoreSongsFetched = function(ytData) {
      try {
        const contents = ytData.contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents;
        let newCount = 0;
        contents.forEach(sec => {
          const items = sec.itemSectionRenderer?.contents || [];
          items.forEach(item => {
            const v = item.videoRenderer;
            if (v && v.videoId) {
              const viewText = v.viewCountText?.simpleText || v.shortViewCountText?.simpleText || '';
              const m = viewText.match(/([\\d\\.]+)\\s*([BMbm])/);
              if (m) {
                const val = parseFloat(m[1]);
                const unit = m[2].toUpperCase();
                let num = unit === 'B' ? val * 1e9 : val * 1e6;
                if (num >= 100000000 && !SONGS.some(s => s.id === v.videoId)) {
                  let title = (v.title?.runs?.[0]?.text || '').replace(/\\(.*\\)|\\[.*\\]/g, '').trim();
                  let artist = v.ownerText?.runs?.[0]?.text || 'YouTube';
                  SONGS.push({
                    id: v.videoId,
                    title: title,
                    artist: artist,
                    views: unit === 'B' ? val + 'B' : Math.round(val) + 'M',
                    viewsNum: num
                  });
                  newCount++;
                }
              }
            }
          });
        });
        if (newCount > 0) {
          const sentinel = document.getElementById('sentinel');
          sentinel.style.display = 'flex';
          renderNextBatch();
        }
      } catch (e) {}
    };
  </script>
</body>
</html>
"""

final_html = html_content.replace('__SONGS_JSON__', songs_json)

targets = [
    '/data/data/com.termux/files/home/100m_apk_build/assets/index.html',
    '/data/data/com.termux/files/home/100m_songs_app/index.html',
    '/sdcard/Download/100M_Hits_Player.html'
]

for t in targets:
    with open(t, 'w', encoding='utf-8') as f:
        f.write(final_html)
    print(f"Wrote {len(final_html)} bytes to {t}")

print("Done! index.html ready with infinite scroll & headphone safety.")
