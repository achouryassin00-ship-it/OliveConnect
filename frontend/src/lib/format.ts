export const number = (value: number) =>
  new Intl.NumberFormat("fr-TN").format(value);
export const date = (value: string) =>
  new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "short",
    timeZone: "Africa/Tunis",
  }).format(new Date(value));
export const dateTime = (value: string) =>
  new Intl.DateTimeFormat("fr-FR", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: "Africa/Tunis",
  }).format(new Date(value));
