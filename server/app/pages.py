"""보호자 폰·병원 접수함 페이지 공통 틀 (docs/design/v6/STYLE_V6.md §8). 390×844 모바일 기준, 외부 글꼴·스크립트 없음.

색은 앱과 같은 v6 토큰(라이트·다크): 파랑 = 주 버튼, 앰버 = 경고에만, 초록 = 편안·완료에만.
반려견 사진은 서버로 보내지 않으므로 페이지는 직접 그린 SVG 일러스트를 쓴다.
"""
import html

CSS = """
:root{--primary:#3478E5;--on-primary:#fff;--bg:#F5F6F8;--surface:#fff;--text:#182230;--sub:#687586;--border:#DFE4EA;
--warning:#E99532;--warning-ink:#9A5A10;--warning-soft:#FCF1E3;--comfort:#16866C;--comfort-soft:#E4F2EE;--disabled:#B9C2CD;--navy:#1C3150}
@media (prefers-color-scheme:dark){:root{--primary:#5E97F0;--on-primary:#0E1420;--bg:#0F141B;--surface:#171E27;--text:#EEF2F6;
--sub:#9AA7B6;--border:#2B3542;--warning:#F0A54B;--warning-ink:#F0A54B;--warning-soft:#33261A;--comfort:#3FB394;--comfort-soft:#15302A;--disabled:#4F5B6A;--navy:#A9BCD8}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--text);font-family:Pretendard,"Apple SD Gothic Neo","Noto Sans KR",system-ui,sans-serif;
line-height:1.5;-webkit-text-size-adjust:100%}
main{max-width:430px;margin:0 auto;padding:12px 16px 32px}
header{display:flex;align-items:center;gap:8px;height:56px;font-size:20px;font-weight:600}
.card{background:var(--surface);border:1px solid var(--border);border-radius:14px;padding:20px;margin-top:12px}
.hero{text-align:center}
.avatar{width:112px;height:112px;border-radius:50%;overflow:hidden;margin:4px auto 12px;display:block}
.title{font-size:20px;font-weight:600;margin:0;word-break:keep-all}
.big{font-size:40px;font-weight:700;line-height:48px;margin:4px 0 0;font-variant-numeric:tabular-nums;word-break:keep-all}
.tiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(96px,1fr));gap:12px;margin-top:16px}
.tile{background:var(--surface);border:1px solid var(--border);border-radius:16px;padding:16px 12px;text-align:center}
.tile .label{font-size:14px;color:var(--sub);font-weight:500}
.tile .value{font-size:28px;font-weight:700;line-height:36px;font-variant-numeric:tabular-nums}
.btn{display:block;width:100%;min-height:56px;border:0;border-radius:12px;word-break:keep-all;background:var(--primary);color:var(--on-primary);
font:inherit;font-size:16px;font-weight:600;margin-top:16px;cursor:pointer}
.btn[disabled]{background:var(--disabled);color:var(--surface)}
.banner{background:var(--warning-soft);color:var(--text);border-radius:14px;border-left:6px solid var(--warning);padding:14px 18px;margin-top:12px}
.banner b{color:var(--warning-ink);display:block;font-size:18px;margin-bottom:2px}
.banner.hot{border-left-width:10px;padding:18px 20px}
.banner.hot b{font-size:22px}
.banner.hot .big{color:var(--warning-ink)}
.ok{background:var(--comfort-soft);color:var(--comfort);border-radius:12px;padding:12px 16px;margin-top:16px;font-weight:600;text-align:center}
.note{font-size:14px;color:var(--sub);margin-top:20px}
.row{display:flex;justify-content:space-between;gap:16px;font-size:16px;padding:8px 0;border-top:1px solid var(--border)}
.row:first-child{border-top:0}
.row span:first-child{color:var(--sub);white-space:nowrap;flex:none;font-size:14px;padding-top:1px}
.row span:last-child{text-align:right;word-break:keep-all;min-width:0}
.kicker{font-size:14px;color:var(--sub);margin:0 0 4px}
.btn.alt{background:var(--surface);color:var(--text);border:1px solid var(--border)}
form{margin:0}
.bar{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:4px}
.chip{display:inline-block;border-radius:8px;padding:2px 10px;font-size:13px;font-weight:600;background:var(--warning-soft);color:var(--warning-ink)}
.snd{min-height:44px;border:1px solid var(--border);border-radius:12px;background:var(--surface);color:var(--text);font:inherit;font-size:14px;
font-weight:600;padding:0 14px;cursor:pointer}
.snd[aria-pressed=true]{border-color:var(--primary);color:var(--primary)}
.changed{border-color:var(--warning);box-shadow:inset 6px 0 0 var(--warning)}
.changed .tag{display:inline-block}
.tag{display:none;margin-left:6px;border-radius:8px;padding:0 8px;font-size:13px;font-weight:600;background:var(--warning-soft);color:var(--warning-ink)}
.demo{font-size:12px;color:var(--sub);text-align:center;margin-top:24px}
"""

LOGO = (
    "<svg width='28' height='28' viewBox='0 0 32 32' aria-hidden='true'><defs><linearGradient id='g' x1='0' y1='0' x2='0' y2='1'>"
    "<stop offset='0' stop-color='#3478E5'/><stop offset='1' stop-color='#1C53A6'/></linearGradient></defs>"
    "<rect width='32' height='32' rx='7' fill='url(#g)'/><path d='M0 27Q16 19 32 22V32H0Z' fill='#15418A'/>"
    "<circle cx='15' cy='13.5' r='7.6' fill='#fff'/><ellipse cx='8.6' cy='12' rx='2.2' ry='4.4' fill='#E6ECF7' transform='rotate(-20 8.6 12)'/>"
    "<ellipse cx='21.4' cy='12' rx='2.2' ry='4.4' fill='#E6ECF7' transform='rotate(20 21.4 12)'/>"
    "<circle cx='12.4' cy='12.5' r='.9' fill='#1A2430'/><circle cx='17.6' cy='12.5' r='.9' fill='#1A2430'/>"
    "<ellipse cx='15' cy='15.3' rx='1.3' ry='.9' fill='#1A2430'/>"
    "<rect x='17' y='22.6' width='8.4' height='2.9' rx='1' fill='#FAA627'/><rect x='19' y='20.8' width='4.6' height='2.6' rx='1' fill='#FAA627'/></svg>"
)

DOG = (
    "<svg class='avatar' viewBox='0 0 100 100' role='img' aria-label='반려견 그림'><rect width='100' height='100' fill='#DDEBFA'/>"
    "<ellipse cx='26' cy='52' rx='11' ry='21' fill='#E2C6A4' transform='rotate(-18 26 52)'/>"
    "<ellipse cx='74' cy='52' rx='11' ry='21' fill='#E2C6A4' transform='rotate(18 74 52)'/>"
    "<circle cx='50' cy='52' r='30' fill='#F1DFC8'/><ellipse cx='50' cy='68' rx='20' ry='14' fill='#FFF7EC'/>"
    "<circle cx='39' cy='48' r='4' fill='#1A2430'/><circle cx='61' cy='48' r='4' fill='#1A2430'/>"
    "<circle cx='40.2' cy='46.8' r='1.3' fill='#fff'/><circle cx='62.2' cy='46.8' r='1.3' fill='#fff'/>"
    "<ellipse cx='50' cy='61.5' rx='5' ry='3.5' fill='#1A2430'/><ellipse cx='50' cy='72' rx='4' ry='4' fill='#F08A8A'/></svg>"
)


def page(title: str, body: str, refresh_sec: int | None = None, script: str = "") -> str:
    """[script]가 있으면 그 스크립트가 새로 고침을 맡고, 새로 고침 meta는 스크립트가 꺼진 브라우저용(noscript)으로만 둔다."""
    meta = f"<meta http-equiv='refresh' content='{refresh_sec}'>" if refresh_sec else ""
    if script and meta:
        meta = f"<noscript>{meta}</noscript>"
    tail = f"<script>{script}</script>" if script else ""
    return (
        "<!doctype html><html lang='ko'><head><meta charset='utf-8'>"
        "<meta name='viewport' content='width=device-width,initial-scale=1'><meta name='robots' content='noindex,nofollow'>"
        f"<meta name='color-scheme' content='light dark'>{meta}<title>{html.escape(title)} · 같이가개</title><style>{CSS}</style></head>"
        f"<body><main><header>{LOGO}<span>같이가개</span></header>{body}</main>{tail}</body></html>"
    )
