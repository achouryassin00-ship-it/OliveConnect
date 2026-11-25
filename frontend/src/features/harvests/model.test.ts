import { describe, expect, it } from "vitest";
import { emptyDraft, tunisToday, validateDraft } from "./model";
import { demoFarmerId, demoOrchardChoices } from "../../demo/harvests";
const valid = {
  ...emptyDraft,
  orchardId: "demo-v1",
  desiredDate: "2026-11-25",
};
const validate = (changes = {}) =>
  validateDraft(
    { ...valid, ...changes },
    demoOrchardChoices,
    demoFarmerId,
    "2026-11-25",
  );
describe("Farmer harvest draft validation", () => {
  it("accepts own active orchard, today and optional quantity", () =>
    expect(validate()).toEqual({}));
  it.each(["", "demo-inactive", "demo-other", "missing"])(
    "rejects unavailable orchard %s",
    (orchardId) => expect(validate({ orchardId }).orchardId).toBeTruthy(),
  );
  it.each(["", "2026-11-24", "2026-02-30", "invalid"])(
    "rejects date %s",
    (desiredDate) => expect(validate({ desiredDate }).desiredDate).toBeTruthy(),
  );
  it.each(["0", "-12", "NaN", "Infinity", "1e9", "2.1234"])(
    "rejects invalid weight %s",
    (estimatedKg) => expect(validate({ estimatedKg }).estimatedKg).toBeTruthy(),
  );
  it("accepts decimal comma", () =>
    expect(validate({ estimatedKg: "1250,5" })).toEqual({}));
  it("requires a reason only for urgency", () => {
    expect(validate({ urgent: true }).justification).toBeTruthy();
    expect(validate({ urgent: true, justification: "Olives mûres" })).toEqual(
      {},
    );
  });
  it("limits text length", () =>
    expect(
      validate({ justification: "x".repeat(501) }).justification,
    ).toBeTruthy());
  it("uses Tunis day across UTC midnight", () =>
    expect(tunisToday(new Date("2026-11-24T23:30:00Z"))).toBe("2026-11-25"));
});
