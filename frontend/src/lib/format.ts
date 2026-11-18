/**
 * Formatting for everything the backend sends (FE-66, FE-67, FE-70). The frontend only formats:
 * it never works out "today" or a period by itself (FE-68).
 *
 * Numbers and money take the Indonesian form (`1.250.000`, `Rp 1.250.000`). Dates and times take
 * the Indonesian order with English month names and a colon, to match the English UI (FE-65):
 * `16 Nov 2026`, `14:05`.
 */

/** Money as the API sends it: a decimal string and an ISO 4217 currency (ENG-80). */
export interface Money {
  amount: string;
  currency: string;
}

/** A business date as the API sends it, such as `2026-11-16` (ENG-80). */
export type BusinessDate = string;

/** A moment as the API sends it: ISO 8601 in UTC, such as `2026-11-16T03:15:00Z` (ENG-80). */
export type Moment = string;

const NUMBER_LOCALE = "id-ID";
// US English has the three-letter month names (British English writes "Sept"); its parts are put
// in the day-month-year order by dayMonthYear() below.
const DATE_LOCALE = "en-US";
const TIME_LOCALE = "en-GB";
const JAKARTA = "Asia/Jakarta";

const numberFormat = new Intl.NumberFormat(NUMBER_LOCALE);

const dateParts = { day: "numeric", month: "short", year: "numeric" } as const;
const hourMinute = { hour: "2-digit", minute: "2-digit", hourCycle: "h23" } as const;

// A business date is a calendar day with no zone, so it is read and written in UTC and never
// shifts by a day in a browser that is not in Jakarta.
const businessDateFormat = new Intl.DateTimeFormat(DATE_LOCALE, { ...dateParts, timeZone: "UTC" });
const timeFormat = new Intl.DateTimeFormat(TIME_LOCALE, { ...hourMinute, timeZone: JAKARTA });
const momentDateFormat = new Intl.DateTimeFormat(DATE_LOCALE, { ...dateParts, timeZone: JAKARTA });

const moneyFormats = new Map<string, Intl.NumberFormat>();

/** `1250000` as `1.250.000`. */
export function formatNumber(value: number): string {
  return numberFormat.format(value);
}

/** `2026-11-16` as `16 Nov 2026`. */
export function formatDate(date: BusinessDate): string {
  return dayMonthYear(businessDateFormat, new Date(`${date}T00:00:00Z`));
}

/** `2026-11-16T07:05:00Z` as `14:05`, the 24-hour time in Jakarta. */
export function formatTime(moment: Moment): string {
  return timeFormat.format(new Date(moment));
}

/** `2026-11-16T03:15:00Z` as `16 Nov 2026 10:15`, the date and time in Jakarta. */
export function formatMoment(moment: Moment): string {
  const at = new Date(moment);
  return `${dayMonthYear(momentDateFormat, at)} ${timeFormat.format(at)}`;
}

/**
 * `{ amount: "1250000.00", currency: "IDR" }` as `Rp 1.250.000`, and any other currency with its
 * own symbol. Rupiah are shown in whole rupiah, rounded half up, since sen are not in use; another
 * currency keeps its own minor unit. The decimal string goes to `Intl` as it is, so no amount loses precision.
 */
export function formatMoney({ amount, currency }: Money): string {
  let format = moneyFormats.get(currency);
  if (format === undefined) {
    format = new Intl.NumberFormat(NUMBER_LOCALE, { style: "currency", currency });
    moneyFormats.set(currency, format);
  }
  // Intl formats a decimal string exactly. TypeScript types that as a numeric string literal, which
  // an amount from the API is.
  return format.format(amount as `${number}`);
}

/** A date as `16 Nov 2026`, whatever order the format's locale writes its parts in. */
function dayMonthYear(format: Intl.DateTimeFormat, at: Date): string {
  const parts = format.formatToParts(at);
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((candidate) => candidate.type === type)?.value ?? "";
  return `${part("day")} ${part("month")} ${part("year")}`;
}
