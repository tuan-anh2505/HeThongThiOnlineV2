package com.htto.backend.domain.embedded;

import java.util.ArrayList;
import java.util.List;

public class ExamAttemptMatchingSnapshot {

    private List<ExamAttemptMatchingLeftSnapshot> leftItems = new ArrayList<>();
    private List<ExamAttemptMatchingRightSnapshot> rightItems = new ArrayList<>();

    public List<ExamAttemptMatchingLeftSnapshot> getLeftItems() {
        return leftItems;
    }

    public void setLeftItems(List<ExamAttemptMatchingLeftSnapshot> leftItems) {
        this.leftItems = leftItems;
    }

    public List<ExamAttemptMatchingRightSnapshot> getRightItems() {
        return rightItems;
    }

    public void setRightItems(List<ExamAttemptMatchingRightSnapshot> rightItems) {
        this.rightItems = rightItems;
    }
}
