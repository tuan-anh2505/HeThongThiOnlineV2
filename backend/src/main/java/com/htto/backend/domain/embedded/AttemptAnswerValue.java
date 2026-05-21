package com.htto.backend.domain.embedded;

import java.util.ArrayList;
import java.util.List;

public class AttemptAnswerValue {

    private Boolean trueFalseAnswer;
    private String selectedOptionId;
    private String fillBlankText;
    private List<AttemptMatchingPairAnswer> matchingPairs = new ArrayList<>();

    public Boolean getTrueFalseAnswer() {
        return trueFalseAnswer;
    }

    public void setTrueFalseAnswer(Boolean trueFalseAnswer) {
        this.trueFalseAnswer = trueFalseAnswer;
    }

    public String getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(String selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public String getFillBlankText() {
        return fillBlankText;
    }

    public void setFillBlankText(String fillBlankText) {
        this.fillBlankText = fillBlankText;
    }

    public List<AttemptMatchingPairAnswer> getMatchingPairs() {
        return matchingPairs;
    }

    public void setMatchingPairs(List<AttemptMatchingPairAnswer> matchingPairs) {
        this.matchingPairs = matchingPairs;
    }
}
