package com.htto.backend.domain.embedded;

import java.util.ArrayList;
import java.util.List;

public class FillBlankAnswer {

    private String answerId;
    private List<String> acceptedAnswers = new ArrayList<>();
    private boolean ignoreCase = true;
    private boolean ignoreAccent = true;
    private boolean trimSpace = true;

    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    public List<String> getAcceptedAnswers() {
        return acceptedAnswers;
    }

    public void setAcceptedAnswers(List<String> acceptedAnswers) {
        this.acceptedAnswers = acceptedAnswers;
    }

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
