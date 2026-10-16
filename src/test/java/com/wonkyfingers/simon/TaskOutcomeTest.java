package com.wonkyfingers.simon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskOutcomeTest {
    @Test void realTaskAllowsTheWholeWindow() {
        TaskOutcome outcome = new TaskOutcome();
        assertNull(outcome.observe(false, true, false));
        assertEquals(Boolean.TRUE, outcome.observe(true, true, false));
        assertNull(outcome.observe(false, true, true));
    }
    @Test void realTaskFailsOnlyAtDeadline() {
        TaskOutcome outcome = new TaskOutcome();
        assertNull(outcome.observe(false, true, false));
        assertEquals(Boolean.FALSE, outcome.observe(false, true, true));
        assertNull(outcome.observe(false, true, true));
    }
    @Test void fakeTaskFailsOnceAndCannotBecomeSuccess() {
        TaskOutcome outcome = new TaskOutcome();
        assertEquals(Boolean.FALSE, outcome.observe(true, false, false));
        assertNull(outcome.observe(false, false, true));
    }
    @Test void ignoringFakeTaskSucceedsAtDeadline() {
        TaskOutcome outcome = new TaskOutcome();
        assertNull(outcome.observe(false, false, false));
        assertEquals(Boolean.TRUE, outcome.observe(false, false, true));
        assertNull(outcome.observe(false, false, true));
    }
    @Test void borderReachesTargetInConfiguredSeconds() {
        double radius = 150, target = 105, speed = (radius-target)/90;
        for (int second=0; second<90; second++) radius=Game_Setup.advanceRadius(radius, target, speed);
        assertEquals(target, radius, 0.000001);
        assertEquals(target, Game_Setup.advanceRadius(radius,target,speed));
    }
}
