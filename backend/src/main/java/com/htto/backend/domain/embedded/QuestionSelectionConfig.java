package com.htto.backend.domain.embedded;

import com.htto.backend.domain.DomainEnums.Difficulty;
import com.htto.backend.domain.DomainEnums.QuestionType;
import com.htto.backend.domain.DomainEnums.SelectionMode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class QuestionSelectionConfig {

    private SelectionMode selectionMode = SelectionMode.MANUAL;
    private int totalQuestions;
    private BigDecimal totalScore;
    private Map<QuestionType, Integer> quantityByType = new HashMap<>();
    private Map<Difficulty, Integer> quantityByDifficulty = new HashMap<>();

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public Map<QuestionType, Integer> getQuantityByType() {
        return quantityByType;
    }

    public void setQuantityByType(Map<QuestionType, Integer> quantityByType) {
        this.quantityByType = quantityByType;
    }

    public Map<Difficulty, Integer> getQuantityByDifficulty() {
        return quantityByDifficulty;
    }

    public void setQuantityByDifficulty(Map<Difficulty, Integer> quantityByDifficulty) {
        this.quantityByDifficulty = quantityByDifficulty;
    }
}
