#!/usr/bin/env python3
"""Put a football club's upcoming fixtures into a theme, for the launcher's match-day panel.

    football_fixtures.py <theme folder>              Manchester United, every competition
    football_fixtures.py <theme folder> --team 364   another club by its ESPN team id (364 is Liverpool)

It writes football_fixtures into <theme folder>/config.json, keeping every other key. Then push the theme as
usual: tools/theme.sh <theme folder>. The launcher never goes online; this script is the only thing that does,
once, on your computer. The data comes from ESPN's public site API: no key, no account, but not a documented
service, so it can change without notice. Run it again every few weeks, or when a kick-off is moved for TV.
Matches that are already over are left out.
"""
import argparse
import datetime
import json
from pathlib import Path
import sys
import urllib.request

API = "https://site.api.espn.com/apis/site/v2/sports/soccer/all/teams/{team}/schedule?fixture=true"
MANCHESTER_UNITED = "360"


def kickoff(event):
    """ESPN's "2026-10-25T14:00Z" as a full UTC timestamp the launcher's Instant.parse accepts."""
    when = datetime.datetime.fromisoformat(event["date"].replace("Z", "+00:00"))
    return when.astimezone(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"), when


def name(competitor):
    team = competitor.get("team", {})
    return team.get("shortDisplayName") or team.get("displayName") or "?"


def fixtures(events, now):
    out = []
    for event in events:
        competitions = event.get("competitions") or []
        if not competitions or not event.get("date"):
            continue
        match = competitions[0]
        sides = {c.get("homeAway"): c for c in match.get("competitors", [])}
        if "home" not in sides or "away" not in sides:
            continue
        start, when = kickoff(event)
        # The launcher keeps a match on screen for three hours after kick-off; a day covers that.
        if when < now - datetime.timedelta(days=1):
            continue
        league = event.get("league") or {}
        out.append({
            "home": name(sides["home"]),
            "away": name(sides["away"]),
            "competition": league.get("shortName") or league.get("name") or "",
            "venue": (match.get("venue") or {}).get("fullName", ""),
            "kickoff": start,
        })
    out.sort(key=lambda m: m["kickoff"])
    return out


def main():
    parser = argparse.ArgumentParser(description="Put a club's upcoming fixtures into a theme.")
    parser.add_argument("theme", help="theme folder with a config.json (created if missing)")
    parser.add_argument("--team", default=MANCHESTER_UNITED, help="ESPN team id, default 360 (Manchester United)")
    args = parser.parse_args()

    config = Path(args.theme) / "config.json"
    theme = json.loads(config.read_text(encoding="utf-8")) if config.is_file() else {}
    request = urllib.request.Request(API.format(team=args.team), headers={"User-Agent": "cleanyourtv"})
    with urllib.request.urlopen(request, timeout=30) as response:
        data = json.load(response)
    club = (data.get("team") or {}).get("displayName", f"team {args.team}")
    now = datetime.datetime.now(datetime.timezone.utc)
    matches = fixtures(data.get("events", []), now)
    if not matches:
        sys.exit(f"no upcoming fixtures for {club}")
    theme["football_fixtures"] = matches
    config.parent.mkdir(parents=True, exist_ok=True)
    config.write_text(json.dumps(theme, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    first = matches[0]
    print(f"{len(matches)} {club} fixtures -> {config}")
    print(f"next: {first['home']} vs {first['away']}, {first['competition']}, {first['kickoff']}")


if __name__ == "__main__":
    main()
