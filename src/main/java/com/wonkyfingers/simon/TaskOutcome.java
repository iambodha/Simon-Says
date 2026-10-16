package com.wonkyfingers.simon;

/** Resolves a task once; null means no new outcome to deliver. */
final class TaskOutcome {
    private boolean resolved;

    boolean isResolved() { return resolved; }

    Boolean observe(boolean actionCompleted, boolean simonSays, boolean deadlineReached) {
        if (resolved || (!actionCompleted && !deadlineReached)) return null;
        resolved = true;
        return actionCompleted == simonSays;
    }
}
