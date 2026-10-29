"""보호자 폰 페이지 공통 틀 (6번 화면, docs/design/v5/STYLE_V5.md). 390×844 모바일 기준, 외부 글꼴·스크립트 없음.

색은 v5 토큰(라이트·다크)을 그대로 쓴다: 파랑 = 주 버튼, 앰버 = 경고에만, 민트 = 편안에만.
반려견 사진은 서버로 보내지 않으므로 페이지는 직접 그린 SVG 일러스트를 쓴다.
"""
import html

CSS = """
:root{--primary:#286FE5;--on-primary:#fff;--bg:#F4F5F7;--surface:#fff;--text:#1A2430;--sub:#667384;--border:#D6DBE2;
--warning:#F59E0B;--warning-ink:#A85C10;--warning-soft:#FDE8CC;--comfort:#137A60;--comfort-soft:#DDF3EC;--disabled:#AEB6C2}
@media (prefers-color-scheme:dark){:root{--primary:#69A9FF;--on-primary:#10151D;--bg:#10151D;--surface:#19212D;--text:#F4F6F8;
--sub:#AAB5C3;--border:#3B4757;--warning:#F4A344;--warning-ink:#F4A344;--warning-soft:#3A2A17;--comfort:#59C9A5;--comfort-soft:#173A33;--disabled:#5A6677}}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--text);font-family:Pretendard,"Apple SD Gothic Neo","Noto Sans KR",system-ui,sans-serif;
line-height:1.5;-webkit-text-size-adjust:100%}
main{max-width:430px;margin:0 auto;padding:16px 24px 32px}
header{display:flex;align-items:center;gap:8px;height:56px;font-size:20px;font-weight:600}
.card{background:var(--surface);border:1px solid var(--border);border-radius:16px;padding:20px;margin-top:16px}
.hero{text-align:center}
.avatar{width:112px;height:112px;border-radius:50%;overflow:hidden;margin:4px auto 12px;display:block}
.title{font-size:20px;font-weight:600;margin:0}
.big{font-size:44px;font-weight:700;line-height:52px;margin:4px 0 0;font-variant-numeric:tabular-nums}
.tiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(96px,1fr));gap:12px;margin-top:16px}
.tile{background:var(--surface);border:1px solid var(--border);border-radius:16px;padding:16px 12px;text-align:center}
.tile .label{font-size:14px;color:var(--sub);font-weight:500}
.tile .value{font-size:28px;font-weight:700;line-height:36px;font-variant-numeric:tabular-nums}
.btn{display:block;width:100%;min-height:56px;border:0;border-radius:12px;background:var(--primary);color:var(--on-primary);
font:inherit;font-size:16px;font-weight:600;margin-top:16px;cursor:pointer}
.btn[disabled]{background:var(--disabled);color:var(--surface)}
.banner{background:var(--warning-soft);color:var(--text);border-radius:16px;padding:16px 20px;margin-top:16px}
.banner b{color:var(--warning-ink);display:block;font-size:20px;margin-bottom:4px}
.ok{background:var(--comfort-soft);color:var(--comfort);border-radius:12px;padding:12px 16px;margin-top:16px;font-weight:600;text-align:center}
.note{font-size:14px;color:var(--sub);margin-top:20px}
.row{display:flex;justify-content:space-between;gap:12px;font-size:16px;padding:6px 0}
.row span:first-child{color:var(--sub)}
"""

LOGO = (
    "<svg width='28' height='28' viewBox='0 0 32 32' aria-hidden='true'><defs><linearGradient id='g' x1='0' y1='0' x2='0' y2='1'>"
    "<stop offset='0' stop-color='#286FE5'/><stop offset='1' stop-color='#1C53A6'/></linearGradient></defs>"
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


def page(title: str, body: str, refresh_sec: int | None = None) -> str:
    meta = f"<meta http-equiv='refresh' content='{refresh_sec}'>" if refresh_sec else ""
    return (
        "<!doctype html><html lang='ko'><head><meta charset='utf-8'>"
        "<meta name='viewport' content='width=device-width,initial-scale=1'><meta name='robots' content='noindex,nofollow'>"
        f"<meta name='color-scheme' content='light dark'>{meta}<title>{html.escape(title)} · 같이가개</title><style>{CSS}</style></head>"
        f"<body><main><header>{LOGO}<span>같이가개</span></header>{body}</main></body></html>"
    )
