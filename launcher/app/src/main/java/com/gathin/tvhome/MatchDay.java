package com.gathin.tvhome;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Match-day mode. A theme's config.json can carry a club's fixtures ({@code football_fixtures}, written by
 * tools/football_fixtures.py); from half a day before kick-off until the result has settled, Home shows the
 * match in the race-weekend panel with a countdown. Like {@link RaceWeekend} it is only the clock against data
 * the owner pushed: no network, no Android types, so it is tested on the JVM.
 */
final class MatchDay {

    /** The panel shows from this long before kick-off... */
    static final Duration LEAD = Duration.ofHours(12);
    /** ...counts as live for two halves, half time and stoppage time... */
    static final Duration LIVE = Duration.ofMinutes(115);
    /** ...and stays until this long after kick-off, reading "full time" once the match is over. */
    static final Duration TAIL = Duration.ofHours(3);

    static final class Match {
        final String home;
        final String away;
        final String competition;
        final String venue;
        final Instant kickoff;
        /** File names in the theme's crests/ folder; empty when the fixture names none. */
        final String homeCrest;
        final String awayCrest;

        Match(String home, String away, String competition, String venue, Instant kickoff,
              String homeCrest, String awayCrest) {
            this.home = home;
            this.away = away;
            this.competition = competition;
            this.venue = venue;
            this.kickoff = kickoff;
            this.homeCrest = homeCrest;
            this.awayCrest = awayCrest;
        }
    }

    /** What the panel shows at one moment of a match day. */
    static final class State {
        final Match match;
        final boolean live;
        /** Time until kick-off; null once the match has kicked off. */
        final Duration untilKickoff;

        State(Match match, boolean live, Duration untilKickoff) {
            this.match = match;
            this.live = live;
            this.untilKickoff = untilKickoff;
        }

        boolean finished() {
            return !live && untilKickoff == null;
        }
    }

    private MatchDay() {
    }

    /** The match {@code now} falls near, or null when none is. Two matches a day apart never overlap. */
    static State at(List<Match> fixtures, Instant now) {
        List<Match> sorted = new ArrayList<>(fixtures);
        Collections.sort(sorted, (a, b) -> a.kickoff.compareTo(b.kickoff));
        for (Match m : sorted) {
            if (now.isBefore(m.kickoff.minus(LEAD)) || !now.isBefore(m.kickoff.plus(TAIL))) continue;
            if (now.isBefore(m.kickoff)) return new State(m, false, Duration.between(now, m.kickoff));
            return new State(m, now.isBefore(m.kickoff.plus(LIVE)), null);
        }
        return null;
    }
}
