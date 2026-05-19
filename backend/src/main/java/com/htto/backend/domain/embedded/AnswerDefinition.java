package com.htto.backend.domain.embedded;

import java.util.ArrayList;
import java.util.List;

public class AnswerDefinition {

    private Boolean trueFalseAnswer;
    private List<AnswerOption> options = new ArrayList<>();
    private FillBlankAnswer fillBlankAnswer;
    private List<MatchingPair> matchingPairs = new ArrayList<>();

    public Boolean getTrueFalseAnswer() {
        return trueFalseAnswer;
    }

    public void setTrueFalseAnswer(Boolean trueFalseAnswer) {
        this.trueFalseAnswer = trueFalseAnswer;
    }

    public List<AnswerOption> getOptions() {
        return options;
    }

    public void setOptions(List<AnswerOption> options) {
        this.options = options;
    }

    public FillBlankAnswer getFillBlankAnswer() {
        return fillBlankAnswer;
    }

    public void setFillBlankAnswer(FillBlankAnswer fillBlankAnswer) {
        this.fillBlankAnswer = fillBlankAnswer;
    }

    public List<MatchingPair> getMatchingPairs() {
        return matchingPairs;
    }

    public void setMatchingPairs(List<MatchingPair> matchingPairs) {
        this.matchingPairs = matchingPairs;
    }
}
