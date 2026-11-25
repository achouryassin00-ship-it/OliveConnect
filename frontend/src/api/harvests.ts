import { ApiError } from "./client";
import type {
  HarvestDraft,
  HarvestRequestView,
} from "../features/harvests/model";
/** Explicit capability boundary. Implement this port only after J05 DTO/routes are validated. */
export interface HarvestGateway {
  list(signal?: AbortSignal): Promise<HarvestRequestView[]>;
  get(id: string, signal?: AbortSignal): Promise<HarvestRequestView>;
  create(
    draft: HarvestDraft,
    idempotencyKey: string,
  ): Promise<HarvestRequestView>;
}
async function unavailable(): Promise<never> {
  throw new ApiError(
    0,
    "HARVEST_API_UNAVAILABLE",
    "Le service de demandes de collecte n’est pas encore disponible. Aucun envoi effectué.",
  );
}
export const harvestsApi: HarvestGateway = {
  list: unavailable,
  get: unavailable,
  create: unavailable,
};
export const harvestCapabilities = {
  list: false,
  create: false,
  history: false,
  cancel: false,
} as const;
