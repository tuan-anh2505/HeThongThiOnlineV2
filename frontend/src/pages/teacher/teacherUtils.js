export function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

export function formatNumber(value) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }

  return Number(value).toLocaleString("vi-VN", {
    maximumFractionDigits: 2
  });
}

export function resolveErrorMessage(error, fallback = "Yêu cầu không thành công") {
  return error?.message || fallback;
}

export function toDateTimeLocal(value) {
  if (!value) {
    return "";
  }

  const date = new Date(value);
  const offset = date.getTimezoneOffset();
  const localDate = new Date(date.getTime() - offset * 60 * 1000);
  return localDate.toISOString().slice(0, 16);
}

export function fromDateTimeLocal(value) {
  if (!value) {
    return "";
  }

  return new Date(value).toISOString();
}

export function getSubjectLabel(subject) {
  if (!subject) {
    return "-";
  }

  return `${subject.subjectCode || ""}${subject.subjectCode ? " - " : ""}${subject.subjectName || subject.subjectId}`;
}

export function getClassLabel(schoolClass) {
  if (!schoolClass) {
    return "-";
  }

  return `${schoolClass.classCode || ""}${schoolClass.classCode ? " - " : ""}${schoolClass.className || schoolClass.classId}`;
}
