package com.htto.backend.domain.embedded;

public class ExamAttemptFillBlankRuleSnapshot {

    private boolean ignoreCase;
    private boolean ignoreAccent;
    private boolean trimSpace;

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public boolean isIgnoreAccent() {
        return ignoreAccent;
    }

    public void setIgnoreAccent(boolean ignoreAccent) {
        this.ignoreAccent = ignoreAccent;
    }

    public boolean isTrimSpace() {
        return trimSpace;
    }

    public void setTrimSpace(boolean trimSpace) {
        this.trimSpace = trimSpace;
    }
}
