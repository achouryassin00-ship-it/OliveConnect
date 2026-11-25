import { apiRequest, ApiError } from "./client";
import { bearer } from "./auth";
import type {
  ApiPage,
  RecolteCancelRequestDTO,
  RecolteCreateRequestDTO,
  RecolteDTO,
  RecolteListParams,
} from "../types/api";
import type {
  HarvestDraft,
  HarvestRequestView,
} from "../features/harvests/model";

const FIELD_MAP: Record<string, keyof HarvestDraft> = {
  vergerId: "orchardId",
  dateSouhaitee: "desiredDate",
  quantiteEstimeeKg: "estimatedKg",
  motifUrgence: "justification",
};

export function toHarvestRequestView(dto: RecolteDTO): HarvestRequestView {
  return {
    id: dto.id,
    reference: dto.reference || dto.id,
    ownerId: dto.proprietaireId,
    orchardId: dto.vergerId,
    orchardName: dto.vergerNom || "",
    orchardCode: dto.vergerCode || "",
    desiredDate: dto.dateSouhaitee,
    estimatedKg: dto.quantiteEstimeeKg ?? null,
    urgent: !!dto.urgent,
    justification: dto.motifUrgence || "",
    status: dto.statut,
    cancellable: !!dto.annulable,
    version: typeof dto.version === "number" ? dto.version : null,
    history: Array.isArray(dto.historique)
      ? dto.historique.map((h) => ({
          previousStatus: h.ancienStatut ?? null,
          nextStatus: h.nouveauStatut,
          actorId: h.acteurId,
          actorName: h.acteurNom || h.acteurId,
          actorRole: h.acteurRole || "",
          reason: h.motif || "",
          timestamp: h.horodatage,
        }))
      : [],
    createdAt: dto.createdAt,
    updatedAt: dto.updatedAt ?? null,
  };
}

function mapDraftFieldErrors(error: ApiError): ApiError {
  if (!Object.keys(error.fieldErrors).length) return error;
  const mapped: Record<string, string> = {};
  for (const [key, msg] of Object.entries(error.fieldErrors)) {
    const target = FIELD_MAP[key] || key;
    mapped[target] = msg;
  }
  return new ApiError(error.status, error.code, error.message, mapped);
}

export interface HarvestGateway {
  list(
    token: string,
    params?: RecolteListParams,
    signal?: AbortSignal,
  ): Promise<ApiPage<HarvestRequestView>>;
  get(
    token: string,
    id: string,
    signal?: AbortSignal,
  ): Promise<HarvestRequestView>;
  create(
    token: string,
    draft: HarvestDraft,
    idempotencyKey: string,
  ): Promise<HarvestRequestView>;
  cancel(
    token: string,
    id: string,
    payload?: RecolteCancelRequestDTO,
  ): Promise<HarvestRequestView>;
}

export const harvestsApi: HarvestGateway = {
  async list(token, params = {}, signal) {
    const query = new URLSearchParams();
    query.set("page", String(params.page ?? 0));
    query.set("size", String(params.size ?? 20));
    if (params.q && params.q.trim()) query.set("q", params.q.trim());
    if (params.statut) query.set("statut", params.statut);
    if (typeof params.urgent === "boolean")
      query.set("urgent", String(params.urgent));
    if (params.vergerId) query.set("vergerId", params.vergerId);

    const result = await apiRequest<ApiPage<RecolteDTO>>(
      `/recoltes?${query.toString()}`,
      {
        headers: bearer(token),
        signal,
      },
    );
    if (
      !Array.isArray(result.items) ||
      !Number.isInteger(result.totalPages) ||
      !Number.isInteger(result.page)
    ) {
      throw new ApiError(
        0,
        "INVALID_RESPONSE",
        "Le format des demandes de collecte reçu est invalide.",
      );
    }
    return {
      ...result,
      items: result.items.map(toHarvestRequestView),
    };
  },

  async get(token, id, signal) {
    const dto = await apiRequest<RecolteDTO>(
      `/recoltes/${encodeURIComponent(id)}`,
      {
        headers: bearer(token),
        signal,
      },
    );
    return toHarvestRequestView(dto);
  },

  async create(token, draft, idempotencyKey) {
    const estimatedKg = draft.estimatedKg.trim()
      ? Number(draft.estimatedKg.trim().replace(",", "."))
      : null;
    const body: RecolteCreateRequestDTO = {
      vergerId: draft.orchardId,
      dateSouhaitee: draft.desiredDate,
      quantiteEstimeeKg: estimatedKg,
      urgent: draft.urgent,
      motifUrgence: draft.urgent ? draft.justification.trim() : null,
    };
    try {
      const dto = await apiRequest<RecolteDTO>("/recoltes", {
        method: "POST",
        headers: {
          ...bearer(token),
          "Idempotency-Key": idempotencyKey,
        },
        body: JSON.stringify(body),
      });
      return toHarvestRequestView(dto);
    } catch (error) {
      if (error instanceof ApiError) throw mapDraftFieldErrors(error);
      throw error;
    }
  },

  async cancel(token, id, payload = {}) {
    const dto = await apiRequest<RecolteDTO>(
      `/recoltes/${encodeURIComponent(id)}/annuler`,
      {
        method: "POST",
        headers: bearer(token),
        body: JSON.stringify({
          version: payload.version ?? null,
          motif: payload.motif?.trim() || null,
        }),
      },
    );
    return toHarvestRequestView(dto);
  },
};

export const harvestCapabilities = {
  list: true,
  create: true,
  history: true,
  cancel: true,
} as const;
