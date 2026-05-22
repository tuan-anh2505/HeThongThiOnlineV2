export function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(new Date(value));
}

export function formatDate(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short"
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

export function getAccountName(account) {
  if (!account) {
    return "-";
  }

  return account.fullName || account.username || account.email || "-";
}

export function getTeacherName(teacher) {
  if (!teacher) {
    return "-";
  }

  return teacher.account?.fullName || teacher.teacherCode || teacher.teacherId || "-";
}

export function getStudentName(student) {
  if (!student) {
    return "-";
  }

  return student.account?.fullName || student.studentCode || student.studentId || "-";
}
