import urllib.request, urllib.parse, json, re, time

queries = [
    'most viewed songs youtube all time',
    'billion views club music video',
    'most viewed music videos in the world',
    'highest view song in youtube',
    'billboard hot 100 all time hits',
    'arijit singh most viewed songs',
    'shreya ghoshal top songs 100m views',
    'neha kakkar most viewed songs',
    'jubin nautiyal 100m hits',
    'badshah most viewed songs',
    'honey singh top viewed songs',
    'diljit dosanjh top hits 100m',
    'sidhu moose wala top hits',
    'guru randhawa top hits',
    'bengali songs 100 million views',
    'bangla most viewed music video',
    'arman alif top hits',
    'imran mahmudul 100m views',
    'ed sheeran official music video',
    'taylor swift official music video',
    'justin bieber official music video',
    'bruno mars official music video',
    'katy perry official music video',
    'shakira official music video',
    'eminem official music video',
    'maroon 5 official music video',
    'alan walker official music video',
    'the weeknd official music video',
    'dua lipa official music video',
    'imagine dragons official music video',
    'linkin park official music video',
    'queen official music video',
    'coldplay official music video',
    'billie eilish official music video',
    'bts official music video',
    'blackpink official music video',
    'psy official music video',
    'j balvin official music video',
    'bad bunny official music video',
    'daddy yankee official music video',
    'coke studio top songs 100m',
    'atif aslam top songs',
    'rahat fateh ali khan 100m',
    'ariana grande official music video',
    'adele official music video',
    'avicii official music video',
    'shawn mendes official music video',
    'charlie puth official music video'
]

# Load existing songs if available
existing_songs = {}
try:
    with open('/data/data/com.termux/files/home/final_songs.json') as f:
        for s in json.load(f):
            if s.get('viewsNum', 0) >= 100000000:
                existing_songs[s['id']] = s
except Exception as e:
    print("Error loading final_songs:", e)

def parse_views(v_str):
    if not v_str: return 0
    v_str = v_str.lower().strip()
    # Check for B (Billion)
    m_b = re.search(r'([\d\.]+)\s*b', v_str)
    if m_b:
        try: return int(float(m_b.group(1)) * 1000000000)
        except: pass
    # Check for M (Million)
    m_m = re.search(r'([\d\.]+)\s*m', v_str)
    if m_m:
        try: return int(float(m_m.group(1)) * 1000000)
        except: pass
    # Check raw digits
    digits = re.sub(r'[^\d]', '', v_str)
    if digits:
        try: return int(digits)
        except: pass
    return 0

def format_views(num):
    if num >= 1000000000:
        return f"{num / 1000000000:.1f}B"
    elif num >= 1000000:
        return f"{num / 1000000:.0f}M"
    return str(num)

def clean_title_and_artist(raw_title, channel_title=""):
    title = raw_title
    # Remove unwanted tags
    title = re.sub(r'\(Official (Music )?Video\)', '', title, flags=re.I)
    title = re.sub(r'\[Official (Music )?Video\]', '', title, flags=re.I)
    title = re.sub(r'\(Official HD Video\)', '', title, flags=re.I)
    title = re.sub(r'\(Official Audio\)', '', title, flags=re.I)
    title = re.sub(r'\[Official Audio\]', '', title, flags=re.I)
    title = re.sub(r'\(Lyric Video\)', '', title, flags=re.I)
    title = re.sub(r'\(Lyrics\)', '', title, flags=re.I)
    title = re.sub(r'\|.*$', '', title)
    title = title.strip(' -|[]()')
    
    artist = channel_title.replace(' - Topic', '').replace('VEVO', '').strip()
    if ' - ' in title:
        parts = title.split(' - ', 1)
        artist = parts[0].strip()
        title = parts[1].strip()
    elif not artist:
        artist = "Various Artists"
    
    return title, artist

for idx, q in enumerate(queries):
    url = 'https://www.youtube.com/results?search_query=' + urllib.parse.quote(q)
    try:
        req = urllib.request.Request(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept-Language': 'en-US,en;q=0.9'
        })
        html = urllib.request.urlopen(req, timeout=6).read().decode('utf-8', errors='ignore')
        m = re.search(r'var ytInitialData = ({.*?});</script>', html)
        if m:
            data = json.loads(m.group(1))
            contents = data['contents']['twoColumnSearchResultsRenderer']['primaryContents']['sectionListRenderer']['contents']
            found_count = 0
            for section in contents:
                items = section.get('itemSectionRenderer', {}).get('contents', [])
                for item in items:
                    v = item.get('videoRenderer')
                    if v:
                        vid = v.get('videoId')
                        if not vid: continue
                        raw_title = v.get('title', {}).get('runs', [{}])[0].get('text', '')
                        channel = v.get('ownerText', {}).get('runs', [{}])[0].get('text', '')
                        views_text = v.get('viewCountText', {}).get('simpleText', '')
                        if not views_text:
                            # Try aria-label or shortViewCountText
                            views_text = v.get('shortViewCountText', {}).get('simpleText', '')
                        num = parse_views(views_text)
                        
                        # Fallback: if already exists, keep or update
                        if num < 100000000 and vid in existing_songs:
                            num = existing_songs[vid].get('viewsNum', 0)
                        
                        if num >= 100000000:
                            clean_t, clean_a = clean_title_and_artist(raw_title, channel)
                            existing_songs[vid] = {
                                "id": vid,
                                "title": clean_t,
                                "artist": clean_a,
                                "views": format_views(num),
                                "viewsNum": num
                            }
                            found_count += 1
            print(f"[{idx+1}/{len(queries)}] Query '{q}': found {found_count} 100M+ songs. Total unique: {len(existing_songs)}")
    except Exception as e:
        print(f"[{idx+1}/{len(queries)}] Error querying '{q}': {e}")
    time.sleep(0.1)

# Sort all songs strictly descending by viewsNum
all_songs = sorted(existing_songs.values(), key=lambda s: s['viewsNum'], reverse=True)

print(f"\nFinal catalog has {len(all_songs)} songs with >= 100M views.")
print("Top 5 songs:")
for s in all_songs[:5]:
    print(f"  #{s['viewsNum']} - {s['title']} ({s['views']})")

with open('/data/data/com.termux/files/home/massive_100m.json', 'w', encoding='utf-8') as f:
    json.dump(all_songs, f, ensure_ascii=False, indent=2)
print("Saved to /data/data/com.termux/files/home/massive_100m.json")
