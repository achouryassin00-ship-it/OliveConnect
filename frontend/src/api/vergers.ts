import { apiRequest, ApiError } from "./client";
import { bearer } from "./auth";
import type { ApiPage, VergerDTO } from "../types/api";
export const vergersApi = {
  list: (token: string, page: number, signal?: AbortSignal) =>
    apiRequest<ApiPage<VergerDTO>>(`/vergers?page=${page}&size=100`, {
      headers: bearer(token),
      signal,
    }),
  get: (token: string, id: string) =>
    apiRequest<VergerDTO>(`/vergers/${encodeURIComponent(id)}`, {
      headers: bearer(token),
    }),
};
/** Server filters ownership; this additional filter prevents mixed-role staff data entering the farmer form. */
export async function loadOwnedVergers(
  token: string,
  ownerId: string,
  signal?: AbortSignal,
): Promise<VergerDTO[]> {
  const owned: VergerDTO[] = [];
  let page = 0;
  while (true) {
    const result = await vergersApi.list(token, page, signal);
    if (
      !Array.isArray(result.items) ||
      !Number.isInteger(result.totalPages) ||
      result.page !== page
    )
      throw new ApiError(
        0,
        "INVALID_RESPONSE",
        "Le format des vergers reçu est invalide.",
      );
    owned.push(
      ...result.items.filter((v) => v.actif && v.proprietaireId === ownerId),
    );
    page += 1;
    if (page >= result.totalPages) break;
    if (page >= 100)
      throw new ApiError(
        0,
        "TOO_MANY_PAGES",
        "Trop de vergers à charger. Contactez la coopérative.",
      );
  }
  return owned;
}
