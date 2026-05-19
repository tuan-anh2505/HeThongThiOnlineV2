package com.htto.backend.domain.embedded;

import java.util.ArrayList;
import java.util.List;

public class FillBlankAnswer {

    private List<String> acceptedAnswers = new ArrayList<>();
    private boolean caseSensitive;
    private boolean ignoreAccents = true;
    private boolean fuzzyMatching;

    public List<String> getAcceptedAnswers() {
        return acceptedAnswers;
    }

    public void setAcceptedAnswers(List<String> acceptedAnswers) {
        this.acceptedAnswers = acceptedAnswers;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isIgnoreAccents() {
        return ignoreAccents;
    }

    public void setIgnoreAccents(boolean ignoreAccents) {
        this.ignoreAccents = ignoreAccents;
    }

    public boolean isFuzzyMatching() {
        return fuzzyMatching;
    }

    public void setFuzzyMatching(boolean fuzzyMatching) {
        this.fuzzyMatching = fuzzyMatching;
    }
}
