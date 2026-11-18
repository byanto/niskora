import { describe, expect, it } from "vitest";
import { formatDate, formatMoment, formatMoney, formatNumber, formatTime } from "./format";

// Intl puts a no-break space between "Rp" and the amount, so the line never breaks inside it.
const NBSP = " ";

describe("formatNumber", () => {
  it("groups thousands with dots", () => {
    expect(formatNumber(1250000)).toBe("1.250.000");
  });

  it("writes decimals with a comma", () => {
    expect(formatNumber(0.25)).toBe("0,25");
  });
});

describe("formatDate", () => {
  it("writes a business date as day, short month and year", () => {
    expect(formatDate("2026-11-16")).toBe("16 Nov 2026");
  });

  it("uses English month names", () => {
    expect(formatDate("2026-05-01")).toBe("1 May 2026");
  });

  it("writes every month in three letters", () => {
    // British English writes September as "Sept"; niskora keeps every month to three letters.
    expect(formatDate("2026-09-16")).toBe("16 Sep 2026");
    expect(formatMoment("2026-09-16T03:15:00Z")).toBe("16 Sep 2026 10:15");
  });

  it("keeps the date whatever zone the browser is in", () => {
    // A plain date is a calendar day, not a moment, so it never shifts a day.
    expect(formatDate("2026-01-01")).toBe("1 Jan 2026");
    expect(formatDate("2026-12-31")).toBe("31 Dec 2026");
  });
});

describe("formatTime", () => {
  it("shows a UTC moment as a 24-hour time in Jakarta", () => {
    expect(formatTime("2026-11-16T07:05:00Z")).toBe("14:05");
  });

  it("writes midnight as 00:00", () => {
    expect(formatTime("2026-11-16T17:00:00Z")).toBe("00:00");
  });
});

describe("formatMoment", () => {
  it("shows the Jakarta date and time of a UTC moment", () => {
    expect(formatMoment("2026-11-16T03:15:00Z")).toBe("16 Nov 2026 10:15");
  });

  it("moves to the next day when Jakarta is already past midnight", () => {
    expect(formatMoment("2026-11-16T18:30:00Z")).toBe("17 Nov 2026 01:30");
  });
});

describe("formatMoney", () => {
  it("shows rupiah with Rp and no decimals", () => {
    expect(formatMoney({ amount: "1250000.00", currency: "IDR" })).toBe(`Rp${NBSP}1.250.000`);
  });

  it("rounds rupiah to the whole rupiah", () => {
    expect(formatMoney({ amount: "1250000.50", currency: "IDR" })).toBe(`Rp${NBSP}1.250.001`);
  });

  it("shows another currency with its own symbol", () => {
    expect(formatMoney({ amount: "12.50", currency: "USD" })).toBe("US$12,50");
  });

  it("keeps a large amount exact", () => {
    // Beyond what a JavaScript number holds exactly: the decimal string is formatted as it is.
    expect(formatMoney({ amount: "90071992547409930.00", currency: "IDR" })).toBe(
      `Rp${NBSP}90.071.992.547.409.930`,
    );
  });
});
