package com.htto.backend.domain;

public final class DomainEnums {

    private DomainEnums() {
    }

    public enum AcademicStatus {
        ACTIVE,
        INACTIVE,
        GRADUATED,
        SUSPENDED
    }

    public enum ProfileStatus {
        ACTIVE,
        INACTIVE
    }

    public enum ClassStatus {
        ACTIVE,
        INACTIVE
    }

    public enum EnrollmentStatus {
        ACTIVE,
        REMOVED
    }

    public enum SubjectStatus {
        ACTIVE,
        INACTIVE
    }

    public enum AssignmentStatus {
        ACTIVE,
        INACTIVE
    }

    public enum QuestionBankStatus {
        ACTIVE,
        INACTIVE
    }

    public enum QuestionStatus {
        ACTIVE,
        INACTIVE
    }

    public enum QuestionType {
        TRUE_FALSE,
        MULTIPLE_CHOICE,
        FILL_BLANK,
        MATCHING
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public enum SelectionMode {
        MANUAL,
        RANDOM
    }

    public enum ExamStatus {
        DRAFT,
        PUBLISHED,
        OPEN,
        CLOSED,
        CANCELLED
    }

    public enum ExamSessionStatus {
        NOT_OPENED,
        IN_PROGRESS,
        ENDED,
        CANCELLED
    }

    public enum SubmissionStatus {
        IN_PROGRESS,
        SUBMITTED,
        OVERDUE,
        CANCELLED
    }

    public enum GradingStatus {
        CORRECT,
        INCORRECT,
        PARTIAL,
        UNGRADED
    }

    public enum ResultPublishStatus {
        NOT_PUBLISHED,
        UNPUBLISHED,
        PUBLISHED
    }

    public enum NotificationStatus {
        UNREAD,
        READ
    }
}
