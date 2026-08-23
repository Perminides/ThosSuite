"""Baut flaggen-signaturen.html aus dem Flaggen-Sheet.

Holt die Tabelle als CSV, gruppiert die Flaggen nach identischer Attributsignatur
und schreibt die Uebersichtsseite daneben. Die Seite liest das Sheet spaeter selbst
live nach; neu gebaut werden muss sie nur, wenn sich Spaltennamen oder -positionen
aendern.

    python build-signaturseite.py

Spaltenannahmen: 0 = Bildpfad, 2 = Land, 7 = Signatur, 9..24 = die 16 Attribute.
"""

import csv, json, io, sys, datetime, collections, urllib.request
from pathlib import Path

SHEET = "1FX8SgpOr9G_Ss030KkQDAtOUE3AbEHuMPfxpl3PBQdE"
CSV_URL = f"https://docs.google.com/spreadsheets/d/{SHEET}/export?format=csv"
ATTR_START = 9          # erste Attributspalte; wie viele es sind, sagt die Signatur
HERE = Path(__file__).resolve().parent

sys.stdout.reconfigure(encoding="utf-8")

raw = urllib.request.urlopen(CSV_URL, timeout=60).read().decode("utf-8")
(HERE / "flaggen.csv").write_text(raw, encoding="utf-8")
rows = list(csv.reader(io.StringIO(raw)))

# Kopfzeile suchen statt annehmen: es ist die, die in Spalte 7 "Signatur" traegt.
HDR = next(i for i, r in enumerate(rows) if len(r) > 7 and r[7].strip() == "Signatur")

FLAGS = [[r[0], r[2].strip(), r[7].strip()] for r in rows[HDR + 1:]
         if len(r) > 7 and r[2].strip() and (r[0].startswith("..") or r[0].startswith("http"))]

# Attributzahl aus der haeufigsten Signaturlaenge ableiten, damit Spaltenaenderungen
# im Sheet hier nichts kaputtmachen.
laengen = collections.Counter(len(s.split("|")) for _, _, s in FLAGS if "|" in s)
N_ATTR = laengen.most_common(1)[0][0] if laengen else 0
ATTR = [h.strip() for h in rows[HDR][ATTR_START:ATTR_START + N_ATTR]]

# ---- Bericht ----------------------------------------------------------------
g = collections.defaultdict(list)
for _, land, sig in FLAGS:
    g[sig].append(land)
items = sorted(g.items(), key=lambda kv: -len(kv[1]))
singles = sum(1 for _, m in items if len(m) == 1)
print(f"{len(FLAGS)} Flaggen, {len(items)} Signaturen, {singles} Einzelgaenger")
kaputt = [s for s in g if not s or "#" in s]
print("Fehlerhafte Signaturen:", kaputt if kaputt else "keine")
print()
for sig, mem in items:
    dec = "; ".join(f"{ATTR[i]}={v}" for i, v in enumerate(sig.split("|"))
                    if v not in ("0", "") and i < len(ATTR))
    print(f"[{len(mem):>2}] {dec if dec else '(alles null)'}")
    print(f"     {', '.join(sorted(mem))}")

# ---- Seite ------------------------------------------------------------------
TPL = r'''<!doctype html><html lang="de"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Flaggen-Signaturen</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;600&family=IBM+Plex+Sans+Condensed:wght@600;700&family=IBM+Plex+Sans:wght@400;500;600&display=swap">
<style>
:root{--paper:#F3F4F7;--card:#fff;--ink:#1A1E28;--soft:#59616F;--rule:#DBDFE6;--accent:#3B4A78;--tint:#E9ECF4;--sh:0 1px 2px rgba(26,30,40,.06),0 6px 16px rgba(26,30,40,.05)}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--paper:#131519;--card:#1B1E25;--ink:#E5E8ED;--soft:#98A0AE;--rule:#2A2F38;--accent:#93A6DA;--tint:#232936;--sh:0 1px 2px rgba(0,0,0,.4)}}
:root[data-theme="dark"]{--paper:#131519;--card:#1B1E25;--ink:#E5E8ED;--soft:#98A0AE;--rule:#2A2F38;--accent:#93A6DA;--tint:#232936;--sh:0 1px 2px rgba(0,0,0,.4)}
*{box-sizing:border-box}
body{margin:0;background:var(--paper);color:var(--ink);font:400 15px/1.55 "IBM Plex Sans",system-ui,sans-serif;-webkit-font-smoothing:antialiased}
.wrap{max-width:1180px;margin:0 auto;padding:0 20px 72px}
header.top{border-bottom:1px solid var(--rule);background:var(--paper);padding:28px 0 18px;margin-bottom:22px}
h1{font:700 30px/1.1 "IBM Plex Sans Condensed",system-ui,sans-serif;letter-spacing:.01em;margin:0 0 6px;text-wrap:balance}
.sub{color:var(--soft);margin:0 0 14px;max-width:62ch}
.status{font:400 12px/1.4 "IBM Plex Mono",monospace;color:var(--soft);margin:0 0 18px}
.status b{color:var(--accent);font-weight:600}
.stats{display:flex;flex-wrap:wrap;gap:26px;margin-bottom:20px}
.stat b{display:block;font:600 26px/1 "IBM Plex Mono",monospace;font-variant-numeric:tabular-nums}
.stat span{font-size:12px;letter-spacing:.07em;text-transform:uppercase;color:var(--soft)}
details.leg{border:1px solid var(--rule);border-radius:8px;background:var(--card);padding:10px 14px;margin-bottom:18px}
details.leg summary{cursor:pointer;font-weight:600;font-size:13px}
details.leg ol{list-style:none;padding:12px 0 4px;margin:0;display:grid;gap:4px 18px;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));font-size:13px;color:var(--soft)}
.pos{display:inline-block;min-width:22px;font:600 12px/1 "IBM Plex Mono",monospace;color:var(--accent)}
.index{display:flex;flex-wrap:wrap;gap:5px}
.ix{display:flex;align-items:baseline;gap:5px;padding:3px 8px;border:1px solid var(--rule);border-radius:6px;background:var(--card);text-decoration:none;color:var(--ink);font:600 12px/1 "IBM Plex Mono",monospace;font-variant-numeric:tabular-nums}
.ix i{font-style:normal;color:var(--soft);font-weight:400}
.ix-1{opacity:.6}
.ix:hover,.ix:focus-visible{border-color:var(--accent);color:var(--accent);outline:none}
.cl{display:grid;grid-template-columns:172px 1fr;gap:24px;background:var(--card);border:1px solid var(--rule);border-radius:12px;padding:18px 20px;margin-bottom:14px;box-shadow:var(--sh);scroll-margin-top:14px}
.cl-1{background:transparent;box-shadow:none;border-style:dashed}
.rail{border-right:1px solid var(--rule);padding-right:20px}
.rank{font:600 12px/1 "IBM Plex Mono",monospace;color:var(--accent);letter-spacing:.08em}
.cnt{font:600 40px/1.05 "IBM Plex Mono",monospace;font-variant-numeric:tabular-nums;margin-top:4px}
.cnt-l{font-size:11px;letter-spacing:.09em;text-transform:uppercase;color:var(--soft)}
.sig{display:block;margin-top:12px;font:400 11px/1.5 "IBM Plex Mono",monospace;color:var(--soft);word-break:break-all}
.chips{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:16px}
.chip{display:inline-flex;align-items:center;gap:7px;background:var(--tint);border-radius:5px;padding:3px 9px;font-size:12.5px}
.chip-k{color:var(--soft)}
.chip-v{font:600 12px/1 "IBM Plex Mono",monospace;color:var(--accent)}
.chip-none{color:var(--soft);font-style:italic}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(92px,1fr));gap:14px 12px}
.t{margin:0;text-align:center}
.t img{width:100%;max-width:92px;aspect-ratio:3/2;object-fit:contain;border:1px solid var(--rule);border-radius:3px;background:var(--tint);display:block;margin:0 auto;padding:2px}
.t figcaption{margin-top:5px;font-size:11.5px;line-height:1.3;color:var(--soft);hyphens:auto}
@media(max-width:680px){.cl{grid-template-columns:1fr;gap:14px}.rail{border-right:0;border-bottom:1px solid var(--rule);padding:0 0 12px;display:flex;align-items:baseline;gap:10px;flex-wrap:wrap}.cnt{font-size:26px}.sig{margin-top:0;flex-basis:100%}}
</style></head><body><div class="wrap">
<header class="top"><h1>Flaggen-Signaturen</h1>
<p class="sub">Nationalflaggen, gruppiert nach identischer Attributsignatur. Jede Gruppe ist ein Kandidat fuer <em>einen</em> gemeinsamen Sketch &mdash; gestrichelte Karten sind Einzelgaenger und brauchen ohnehin einen eigenen.</p>
<p class="status" id="status">Lade aktuellen Stand aus dem Sheet &hellip;</p>
<div class="stats" id="stats"></div>
<details class="leg"><summary>Attributreihenfolge in der Signatur</summary><ol id="leg"></ol></details>
<nav class="index" id="index"></nav></header>
<main id="out"></main>
</div>
<script>
const SHEET="__SHEET__";
let ATTR=__ATTR__, FLAGS=__FLAGS__;
const esc=s=>String(s).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]));
const url=p=>p.replace("..","https://www.sciencekids.co.nz");

function render(){
  const g=new Map();
  for(const f of FLAGS){ const s=f[2]; if(!g.has(s)) g.set(s,[]); g.get(s).push([f[1],f[0]]); }
  const items=[...g.entries()].sort((a,b)=>b[1].length-a[1].length||a[0].localeCompare(b[0]));
  const singles=items.filter(x=>x[1].length===1).length;
  const top10=items.slice(0,10).reduce((n,x)=>n+x[1].length,0);
  document.getElementById("stats").innerHTML=[
    [FLAGS.length,"Flaggen"],[items.length,"Signaturen"],
    [items.length-singles,"Cluster ≥ 2"],[singles,"Einzelgänger"],[top10,"in den Top 10"]
  ].map(p=>'<div class="stat"><b>'+p[0]+'</b><span>'+p[1]+'</span></div>').join("");
  document.getElementById("leg").innerHTML=ATTR.map((a,i)=>'<li><span class="pos">'+(i+1)+'</span>'+esc(a)+'</li>').join("");
  document.getElementById("index").innerHTML=items.map((x,i)=>
    '<a class="ix'+(x[1].length===1?" ix-1":"")+'" href="#c'+(i+1)+'"><b>'+(i+1)+'</b><i>'+x[1].length+'</i></a>').join("");
  document.getElementById("out").innerHTML=items.map((x,i)=>{
    const sig=x[0], mem=x[1], n=i+1, one=mem.length===1;
    const chips=sig.split("|").map((v,j)=>(v!=="0"&&v!=="")
      ? '<span class="chip"><span class="chip-k">'+esc(ATTR[j]||"?")+'</span><span class="chip-v">'+esc(v)+'</span></span>':"")
      .filter(Boolean).join("") || '<span class="chip chip-none">keine Merkmale gesetzt</span>';
    const tiles=mem.slice().sort((a,b)=>a[0].localeCompare(b[0])).map(m=>
      '<figure class="t"><img loading="lazy" src="'+esc(url(m[1]))+'" alt="'+esc(m[0])+'"><figcaption>'+esc(m[0].replace(/_/g," "))+'</figcaption></figure>').join("");
    return '<section class="cl'+(one?" cl-1":"")+'" id="c'+n+'">'+
      '<div class="rail"><div class="rank">'+String(n).padStart(2,"0")+'</div><div class="cnt">'+mem.length+'</div>'+
      '<div class="cnt-l">'+(one?"Flagge":"Flaggen")+'</div><code class="sig">'+esc(sig)+'</code></div>'+
      '<div class="body"><div class="chips">'+chips+'</div><div class="grid">'+tiles+'</div></div></section>';
  }).join("");
}

const SNAP="__SNAP__";
function setStatus(t,live){ document.getElementById("status").innerHTML = live
  ? '<b>Live aus dem Sheet</b> · geladen '+t : 'Eingebauter Stand · '+t; }

render(); setStatus(SNAP,false);

window.flagData=function(res){
  try{
    const rows=res.table.rows.map(r=>r.c.map(c=>(c&&c.v!=null)?String(c.v):""));
    const hdr=rows.findIndex(r=>(r[7]||"").trim()==="Signatur");
    if(hdr<0) throw new Error("Kopfzeile nicht gefunden");
    const n=ATTR.length;
    const attr=rows[hdr].slice(9,9+n).map(s=>s.trim());
    const flags=rows.slice(hdr+1).filter(r=>r[2]&&r[2].trim()&&r[0]&&
                     (r[0].indexOf("..")===0||r[0].indexOf("http")===0))
                     .map(r=>[r[0],r[2].trim(),r[7].trim()]);
    if(flags.length<100) throw new Error("unplausible Daten");
    if(attr.length===n && attr.every(function(a){return a;})) ATTR=attr;
    FLAGS=flags; render();
    setStatus(new Date().toLocaleString("de-DE"),true);
  }catch(e){ document.getElementById("status").innerHTML=
      'Eingebauter Stand · '+SNAP+' · <span title="'+esc(e.message)+'">Sheet-Abgleich fehlgeschlagen</span>'; }
};
const sc=document.createElement("script");
sc.src="https://docs.google.com/spreadsheets/d/"+SHEET+"/gviz/tq?tqx=out:json;responseHandler:flagData&headers=0&tq="+encodeURIComponent("select *");
sc.onerror=function(){ setStatus(SNAP+" · Sheet nicht erreichbar",false); };
document.head.appendChild(sc);
</script></body></html>'''

out = (TPL.replace("__SHEET__", SHEET)
          .replace("__ATTR__", json.dumps(ATTR, ensure_ascii=False))
          .replace("__FLAGS__", json.dumps(FLAGS, ensure_ascii=False))
          .replace("__SNAP__", datetime.datetime.now().strftime("%d.%m.%Y %H:%M")))
(HERE / "flaggen-signaturen.html").write_text(out, encoding="utf-8")
print(f"\nflaggen-signaturen.html geschrieben: {len(out)} Zeichen, {len(ATTR)} Attribute")
