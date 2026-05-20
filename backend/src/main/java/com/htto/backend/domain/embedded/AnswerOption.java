package com.htto.backend.domain.embedded;

public class AnswerOption {

    private String optionId;
    private String content;
    private boolean isCorrect;
    private int orderIndex;

    public AnswerOption() {
    }

    public AnswerOption(String optionId, String content, boolean isCorrect, int orderIndex) {
        this.optionId = optionId;
        this.content = content;
        this.isCorrect = isCorrect;
        this.orderIndex = orderIndex;
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
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
