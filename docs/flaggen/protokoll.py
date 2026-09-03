"""Macht aus einer Sitzungsdatei ein lesbares Chatprotokoll.

    python protokoll.py <sitzung.jsonl> <ziel.md>

Format wie die vorhandenen Protokolle in docs/flaggen/Chats: eine Ueberschrift je
Beitrag, Denkbloecke eingeklappt, Werkzeugaufrufe als einzeilige Marke. Werkzeug-
Ergebnisse bleiben draussen -- sie machen die Datei um ein Vielfaches groesser und
stehen ohnehin im Repo.
"""
import json
import re
import sys
from datetime import datetime, timezone

NAME = "Perminides"
# Im Repo steht nie der echte Vorname -- auch nicht, wenn er im Chat gefallen ist.
ECHTE_NAMEN = ["Thorsten"]


def zeit(iso):
    d = datetime.fromisoformat(iso.replace("Z", "+00:00")).astimezone()
    return d.strftime("%d.%m.%Y %H:%M")


def saeubere(text):
    text = re.sub(r"<system-reminder>.*?</system-reminder>", "", text, flags=re.S)
    for n in ECHTE_NAMEN:
        # Ohne IGNORECASE rutscht eine Kleinschreibung durch -- genau das ist beim ersten
        # Lauf passiert, in einem Denkblock, der ueber diese Pruefung selbst redete.
        text = re.sub(rf"\b{n}\b", NAME, text, flags=re.I)
    return text.strip()


def kurz(werkzeug, eingabe):
    """Eine Zeile je Werkzeugaufruf: was es war, wozu."""
    if not isinstance(eingabe, dict):
        return ""
    for schluessel in ("description", "query", "prompt", "title", "skill", "file_path", "url", "pattern"):
        if eingabe.get(schluessel):
            return str(eingabe[schluessel])[:110].replace("\n", " ")
    return ""


def lade(pfad):
    for zeile in open(pfad, encoding="utf-8"):
        try:
            yield json.loads(zeile)
        except json.JSONDecodeError:
            continue


def bloecke(inhalt):
    """Vereinheitlicht die zwei Formen, in denen Inhalt vorkommt: Text oder Blockliste."""
    if isinstance(inhalt, str):
        return [{"type": "text", "text": inhalt}]
    return inhalt if isinstance(inhalt, list) else []


quelle, ziel = sys.argv[1], sys.argv[2]
beitraege, n_user, n_claude, n_werkzeug = [], 0, 0, 0
erste = letzte = None

for d in lade(quelle):
    art = d.get("type")
    if art not in ("user", "assistant") or d.get("isSidechain"):
        continue
    nachricht = d.get("message") or {}
    stempel = d.get("timestamp")
    teile = []

    for b in bloecke(nachricht.get("content")):
        typ = b.get("type")
        if typ == "text":
            t = saeubere(b.get("text", ""))
            if t:
                teile.append(t)
        elif typ == "thinking":
            t = saeubere(b.get("thinking", ""))
            if t:
                teile.append(f"<details><summary>Überlegung</summary>\n\n{t}\n\n</details>")
        elif typ == "tool_use":
            n_werkzeug += 1
            k = kurz(b.get("name", ""), b.get("input"))
            teile.append(f"› **{b.get('name','')}**" + (f" — {k}" if k else ""))
        # tool_result bleibt draussen

    if not teile:
        continue
    wer = NAME if art == "user" else "Claude"
    if art == "user":
        n_user += 1
    else:
        n_claude += 1
    if stempel:
        erste = erste or stempel
        letzte = stempel
    # Aufeinanderfolgende Beitraege desselben Sprechers gehoeren unter eine Ueberschrift:
    # Claude schickt eine Antwort oft als mehrere Nachrichten, das ist keine Zaesur.
    if beitraege and beitraege[-1][0] == wer:
        beitraege[-1][2].extend(teile)
    else:
        beitraege.append([wer, zeit(stempel) if stempel else "", list(teile)])

kopf = (f"# Chatprotokoll — Flaggen-Deck\n\n"
        f"Sitzung `{quelle.rsplit('/',1)[-1].replace('.jsonl','')}` · "
        f"{zeit(erste)} bis {zeit(letzte)}\n\n"
        f"{n_user} Nachrichten von {NAME}, {n_claude} von Claude, {n_werkzeug} Werkzeugaufrufe. "
        f"Denkblöcke sind eingeklappt, Werkzeug-Ergebnisse nicht enthalten.\n\n---\n\n")

text = "\n\n".join(f"## {w} · {t}\n\n" + "\n\n".join(teile) for w, t, teile in beitraege)
open(ziel, "w", encoding="utf-8").write(kopf + text + "\n")
print(f"{len(beitraege)} Beiträge · {n_user} / {n_claude} · {n_werkzeug} Werkzeugaufrufe")
