package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class MatchDayTest {

    // Two Manchester United fixtures four days apart, kick-offs as ESPN gives them (UTC).
    private static final MatchDay.Match COMO = new MatchDay.Match("Como", "Man United",
            "UEFA Champions League", "Giuseppe Sinigaglia", Instant.parse("2026-10-21T16:45:00Z"), "2572.png", "360.png");
    private static final MatchDay.Match BOURNEMOUTH = new MatchDay.Match("Man United", "Bournemouth",
            "Premier League", "Old Trafford", Instant.parse("2026-10-25T14:00:00Z"), "360.png", "349.png");
    // Out of order on purpose: the fixtures are sorted by kick-off.
    private static final List<MatchDay.Match> FIXTURES = Arrays.asList(BOURNEMOUTH, COMO);

    @Test
    public void nothingMoreThanTwelveHoursBeforeKickoff() {
        assertNull(MatchDay.at(FIXTURES, Instant.parse("2026-10-21T04:44:00Z")));
    }

    @Test
    public void countsDownFromTwelveHoursBefore() {
        MatchDay.State s = MatchDay.at(FIXTURES, Instant.parse("2026-10-21T04:45:00Z"));
        assertSame(COMO, s.match);
        assertFalse(s.live);
        assertEquals(Duration.ofHours(12), s.untilKickoff);
    }

    @Test
    public void liveFromKickoffForTwoHalvesAndStoppageTime() {
        MatchDay.State kickoff = MatchDay.at(FIXTURES, Instant.parse("2026-10-21T16:45:00Z"));
        assertTrue(kickoff.live);
        assertNull(kickoff.untilKickoff);
        assertTrue(MatchDay.at(FIXTURES, Instant.parse("2026-10-21T18:39:00Z")).live);
        assertTrue(MatchDay.at(FIXTURES, Instant.parse("2026-10-21T18:40:00Z")).finished());
    }

    @Test
    public void goneThreeHoursAfterKickoffUntilTheNextMatchDay() {
        assertNull(MatchDay.at(FIXTURES, Instant.parse("2026-10-21T19:45:00Z")));
        assertNull(MatchDay.at(FIXTURES, Instant.parse("2026-10-25T01:59:00Z")));
        assertSame(BOURNEMOUTH, MatchDay.at(FIXTURES, Instant.parse("2026-10-25T02:00:00Z")).match);
    }
}
