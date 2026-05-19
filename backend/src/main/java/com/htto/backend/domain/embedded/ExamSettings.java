package com.htto.backend.domain.embedded;

public class ExamSettings {

    private boolean showScoreImmediately;
    private boolean allowReview;
    private boolean showCorrectAnswers;
    private boolean shuffleQuestions;
    private boolean shuffleAnswers;

    public boolean isShowScoreImmediately() {
        return showScoreImmediately;
    }

    public void setShowScoreImmediately(boolean showScoreImmediately) {
        this.showScoreImmediately = showScoreImmediately;
    }

    public boolean isAllowReview() {
        return allowReview;
    }

    public void setAllowReview(boolean allowReview) {
        this.allowReview = allowReview;
    }

    public boolean isShowCorrectAnswers() {
        return showCorrectAnswers;
    }

    public void setShowCorrectAnswers(boolean showCorrectAnswers) {
        this.showCorrectAnswers = showCorrectAnswers;
    }

    public boolean isShuffleQuestions() {
        return shuffleQuestions;
    }

    public void setShuffleQuestions(boolean shuffleQuestions) {
        this.shuffleQuestions = shuffleQuestions;
    }

    public boolean isShuffleAnswers() {
        return shuffleAnswers;
    }

    public void setShuffleAnswers(boolean shuffleAnswers) {
        this.shuffleAnswers = shuffleAnswers;
    }
}
