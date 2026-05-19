package com.htto.backend.domain.embedded;

public class AnswerOption {

    private String optionId;
    private String content;
    private boolean correct;
    private int displayOrder;

    public AnswerOption() {
    }

    public AnswerOption(String optionId, String content, boolean correct, int displayOrder) {
        this.optionId = optionId;
        this.content = content;
        this.correct = correct;
        this.displayOrder = displayOrder;
    }

    public String getOptionId() {
        return optionId;
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
