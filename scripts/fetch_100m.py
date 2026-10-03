import urllib.request, urllib.parse, json, re

queries = [
    'top most viewed songs all time',
    'billion views club songs',
    'most viewed songs of all time youtube',
    'most viewed bollywood songs all time',
    'arijit singh hits 100m',
    'most viewed bengali song',
    'bengali hit songs 100m views',
    'punjabi top songs all time',
    'punjabi hits 500m',
    'taylor swift most viewed',
    'alan walker most viewed songs',
    'bts blackpink most viewed',
    'neha kakkar top songs 100m',
    'shakira top songs',
    'ed sheeran hits',
    'justin bieber hits',
    'coke studio top songs all time',
    'latin hits 1 billion views',
    'arman alif songs',
    'imran mahmudul top hits'
]

raw_songs = {}

def parse_views(v_str):
    if not v_str: return 0
    digits = re.sub(r'[^\d]', '', v_str)
    if digits:
        try:
            return int(digits)
        except:
            pass
    return 0

for q in queries:
    url = 'https://www.youtube.com/results?search_query=' + urllib.parse.quote(q)
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        html = urllib.request.urlopen(req, timeout=6).read().decode('utf-8', errors='ignore')
        m = re.search(r'var ytInitialData = ({.*?});</script>', html)
        if m:
            data = json.loads(m.group(1))
            contents = data['contents']['twoColumnSearchResultsRenderer']['primaryContents']['sectionListRenderer']['contents']
            for section in contents:
                items = section.get('itemSectionRenderer', {}).get('contents', [])
                for item in items:
                    v = item.get('videoRenderer')
                    if v:
                        vid = v.get('videoId')
                        title = v.get('title', {}).get('runs', [{}])[0].get('text', '')
                        views_text = v.get('viewCountText', {}).get('simpleText', '')
                        num = parse_views(views_text)
                        if num >= 100000000:
                            raw_songs[vid] = (title, num)
    except Exception as e:
        pass

print(f'Discovered {len(raw_songs)} videos with >= 100M views.')

# Now verify thumbnails for all of them
verified = []
for vid, (title, num) in raw_songs.items():
    url = f'https://i.ytimg.com/vi/{vid}/hqdefault.jpg'
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req, timeout=3)
        if res.status == 200 and int(res.headers.get('Content-Length', 0)) > 2000:
            verified.append({'id': vid, 'title': title, 'viewsNum': num})
    except:
        pass

print(f'Verified {len(verified)} videos with working thumbnails.')
with open('/data/data/com.termux/files/home/verified_100m.json', 'w') as f:
    json.dump(verified, f)
