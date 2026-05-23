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

export function formatDuration(seconds) {
  const safeSeconds = Math.max(0, Number(seconds || 0));
  const hours = Math.floor(safeSeconds / 3600);
  const minutes = Math.floor((safeSeconds % 3600) / 60);
  const remainSeconds = safeSeconds % 60;

  return [hours, minutes, remainSeconds]
    .map((item) => String(item).padStart(2, "0"))
    .join(":");
}

export function resolveErrorMessage(error, fallback = "Yêu cầu không thành công") {
  return error?.message || fallback;
}
