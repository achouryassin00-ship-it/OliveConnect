import { afterEach, describe, expect, it, vi } from "vitest";
import { harvestCapabilities, harvestsApi } from "./harvests";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

const sampleDto = {
  id: "rec-1",
  reference: "REC-2026-0001",
  proprietaireId: "farmer-1",
  vergerId: "vrg-1",
  vergerNom: "Verger El Hencha",
  vergerCode: "VRG-SFX-001",
  dateSouhaitee: "2026-11-28",
  quantiteEstimeeKg: 1500.5,
  urgent: true,
  motifUrgence: "Maturité optimale",
  statut: "DEMANDEE",
  annulable: true,
  historique: [
    {
      ancienStatut: null,
      nouveauStatut: "DEMANDEE",
      acteurId: "farmer-1",
      acteurNom: "Ali",
      acteurRole: "AGRICULTEUR",
      motif: "Maturité optimale",
      horodatage: "2026-11-25T10:00:00Z",
    },
  ],
  version: 0,
  createdAt: "2026-11-25T10:00:00Z",
  updatedAt: "2026-11-25T10:00:00Z",
};

describe("Harvests API adapter", () => {
  it("declares implemented capabilities", () => {
    expect(harvestCapabilities).toEqual({
      list: true,
      create: true,
      history: true,
      cancel: true,
    });
  });

  it("lists paginated requests with query filters and maps DTOs", async () => {
    vi.stubEnv("VITE_API_ENABLED", "true");
    const fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          items: [sampleDto],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        }),
      ),
    );
    vi.stubGlobal("fetch", fetch);

    const page = await harvestsApi.list("jwt-123", {
      page: 0,
      size: 20,
      q: "Hencha",
      statut: "DEMANDEE",
      urgent: true,
    });

    const [url, init] = fetch.mock.calls[0];
    expect(url).toBe(
      "/api/recoltes?page=0&size=20&q=Hencha&statut=DEMANDEE&urgent=true",
    );
    expect(init.headers.get("Authorization")).toBe("Bearer jwt-123");
    expect(page.items[0]).toMatchObject({
      id: "rec-1",
      reference: "REC-2026-0001",
      orchardName: "Verger El Hencha",
      cancellable: true,
      history: [{ nextStatus: "DEMANDEE", actorName: "Ali" }],
    });
  });

  it("creates harvest with Idempotency-Key and maps backend fieldErrors", async () => {
    vi.stubEnv("VITE_API_ENABLED", "true");
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify(sampleDto), { status: 201 }))
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            code: "VALIDATION_ERROR",
            message: "bad",
            fieldErrors: { dateSouhaitee: "invalid", motifUrgence: "required" },
          }),
          { status: 400 },
        ),
      );
    vi.stubGlobal("fetch", fetch);

    const created = await harvestsApi.create(
      "jwt-123",
      {
        orchardId: "vrg-1",
        desiredDate: "2026-11-28",
        estimatedKg: "1500,5",
        urgent: true,
        justification: "Maturité optimale",
      },
      "idem-key-1",
    );

    expect(created.id).toBe("rec-1");
    expect(fetch.mock.calls[0][1].headers.get("Idempotency-Key")).toBe("idem-key-1");
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
      vergerId: "vrg-1",
      dateSouhaitee: "2026-11-28",
      quantiteEstimeeKg: 1500.5,
      urgent: true,
      motifUrgence: "Maturité optimale",
    });

    await expect(
      harvestsApi.create(
        "jwt-123",
        {
          orchardId: "vrg-1",
          desiredDate: "2020-01-01",
          estimatedKg: "",
          urgent: true,
          justification: "",
        },
        "idem-key-2",
      ),
    ).rejects.toMatchObject({
      status: 400,
      fieldErrors: {
        desiredDate: "Vérifiez la valeur de ce champ.",
        justification: "Vérifiez la valeur de ce champ.",
      },
    });
  });

  it("cancels harvest with version and optional motif", async () => {
    vi.stubEnv("VITE_API_ENABLED", "true");
    const fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ ...sampleDto, statut: "ANNULEE", annulable: false, version: 1 }),
      ),
    );
    vi.stubGlobal("fetch", fetch);

    const cancelled = await harvestsApi.cancel("jwt-123", "rec-1", {
      version: 0,
      motif: "Report",
    });
    expect(fetch.mock.calls[0][0]).toBe("/api/recoltes/rec-1/annuler");
    expect(cancelled.status).toBe("ANNULEE");
    expect(cancelled.cancellable).toBe(false);
  });
});
