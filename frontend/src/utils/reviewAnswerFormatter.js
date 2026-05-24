const NOT_ANSWERED = "Chưa trả lời";

const QUESTION_TYPE_LABELS = {
  TRUE_FALSE: "Đúng/Sai",
  MULTIPLE_CHOICE: "Trắc nghiệm",
  FILL_BLANK: "Điền vào ô trống",
  MATCHING: "Nối đáp án"
};

const ANSWER_STATUS_LABELS = {
  CORRECT: "Đúng",
  WRONG: "Sai",
  NOT_ANSWERED: "Chưa trả lời",
  NOT_GRADED: "Chưa chấm"
};

export function formatQuestionType(type) {
  return QUESTION_TYPE_LABELS[type] ?? type ?? "-";
}

export function formatAnswerStatus(item) {
  if (item?.answerStatus && ANSWER_STATUS_LABELS[item.answerStatus]) {
    return ANSWER_STATUS_LABELS[item.answerStatus];
  }
  if (item?.isCorrect === true) {
    return "Đúng";
  }
  if (item?.isCorrect === false) {
    return "Sai";
  }
  return item?.answerStatus ?? "-";
}

export function shouldShowStudentAnswer(item) {
  return item?.isCorrect !== true || item?.answerStatus === "NOT_ANSWERED";
}

export function formatStudentAnswer(item) {
  const question = getQuestion(item);
  const answer = item?.studentAnswer;
  const type = getQuestionType(item);

  if (!answer) {
    return [NOT_ANSWERED];
  }

  if (type === "TRUE_FALSE") {
    return [formatTrueFalse(answer.trueFalseAnswer)];
  }

  if (type === "MULTIPLE_CHOICE") {
    if (!answer.selectedOptionId) {
      return [NOT_ANSWERED];
    }
    return [formatOptionLabel(question, answer.selectedOptionId)];
  }

  if (type === "FILL_BLANK") {
    return [hasText(answer.fillBlankText) ? answer.fillBlankText : NOT_ANSWERED];
  }

  if (type === "MATCHING") {
    const pairs = Array.isArray(answer.matchingPairs) ? answer.matchingPairs : [];
    if (pairs.length === 0) {
      return [NOT_ANSWERED];
    }
    return pairs.map((pair) => `${formatMatchingLeft(question, pair.leftId)} -> ${pair.rightText || "-"}`);
  }

  return [NOT_ANSWERED];
}

export function formatCorrectAnswer(item) {
  const question = getQuestion(item);
  const correctAnswer = item?.correctAnswer;
  const type = getQuestionType(item);

  if (!correctAnswer || Object.keys(correctAnswer).length === 0) {
    return ["Chưa có dữ liệu đáp án đúng"];
  }

  if (type === "TRUE_FALSE") {
    return [formatTrueFalse(correctAnswer.trueFalseAnswer)];
  }

  if (type === "MULTIPLE_CHOICE") {
    const optionIds = Array.isArray(correctAnswer.correctOptionIds) ? correctAnswer.correctOptionIds : [];
    if (optionIds.length === 0) {
      return ["Chưa có đáp án đúng"];
    }
    return [optionIds.map((optionId) => formatOptionLabel(question, optionId)).join(" hoặc ")];
  }

  if (type === "FILL_BLANK") {
    const acceptedAnswers = Array.isArray(correctAnswer.acceptedAnswers) ? correctAnswer.acceptedAnswers : [];
    if (acceptedAnswers.length === 0) {
      return ["Chưa có đáp án đúng"];
    }
    return [acceptedAnswers.filter(hasText).join(" hoặc ") || "Chưa có đáp án đúng"];
  }

  if (type === "MATCHING") {
    const pairs = Array.isArray(correctAnswer.pairs) ? correctAnswer.pairs : [];
    if (pairs.length === 0) {
      return ["Chưa có đáp án đúng"];
    }
    return pairs.map((pair) => `${pair.leftText || formatMatchingLeft(question, pair.matchingId)} -> ${pair.rightText || "-"}`);
  }

  return ["Chưa có dữ liệu đáp án đúng"];
}

function getQuestion(item) {
  return item?.question ?? item ?? {};
}

function getQuestionType(item) {
  return item?.type ?? item?.question?.type;
}

function formatTrueFalse(value) {
  if (value === true) {
    return "Đúng";
  }
  if (value === false) {
    return "Sai";
  }
  return NOT_ANSWERED;
}

function formatOptionLabel(question, optionId) {
  const options = Array.isArray(question?.options) ? question.options : [];
  const option = options.find((item) => item.optionId === optionId);
  if (!option) {
    return "Không tìm thấy lựa chọn";
  }
  return option.optionId ? `${option.optionId}. ${option.content}` : option.content;
}

function formatMatchingLeft(question, leftId) {
  const leftItems = Array.isArray(question?.matching?.leftItems) ? question.matching.leftItems : [];
  const leftItem = leftItems.find((item) => item.matchingId === leftId);
  return leftItem?.leftText || "Không tìm thấy vế trái";
}

function hasText(value) {
  return typeof value === "string" && value.trim().length > 0;
}
