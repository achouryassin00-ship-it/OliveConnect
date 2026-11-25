import type { HarvestStatus } from "../../types/domain";

export const MAX_ESTIMATED_KG = 500_000;

export interface HarvestDraft {
  orchardId: string;
  desiredDate: string;
  estimatedKg: string;
  urgent: boolean;
  justification: string;
}

export interface OrchardChoice {
  id: string;
  name: string;
  code: string;
  ownerId: string;
  active: boolean;
}

export interface HarvestHistoryEntry {
  previousStatus: HarvestStatus | null;
  nextStatus: HarvestStatus;
  actorId: string;
  actorName: string;
  actorRole: string;
  reason: string;
  timestamp: string;
}

export interface HarvestRequestView {
  id: string;
  reference: string;
  ownerId: string;
  orchardId: string;
  orchardName: string;
  orchardCode: string;
  desiredDate: string;
  estimatedKg: number | null;
  urgent: boolean;
  justification: string;
  status: HarvestStatus;
  cancellable: boolean;
  version: number | null;
  history: HarvestHistoryEntry[];
  createdAt: string;
  updatedAt: string | null;
}

export const emptyDraft: HarvestDraft = {
  orchardId: "",
  desiredDate: "",
  estimatedKg: "",
  urgent: false,
  justification: "",
};

export const statusLabels: Record<HarvestStatus, string> = {
  DEMANDEE: "À valider",
  VALIDEE: "Validée",
  PLANIFIEE: "Planifiée",
  EN_COURS: "En cours",
  LIVREE: "Livrée",
  PESEE: "Pesée",
  ARCHIVEE: "Archivée",
  REFUSEE: "Refusée",
  ANNULEE: "Annulée",
  INTERROMPUE: "Interrompue",
};

export function tunisToday(now = new Date()): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Africa/Tunis",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(now);
}

export function displayDay(value: string) {
  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "medium",
    timeZone: "UTC",
  }).format(new Date(`${value}T12:00:00Z`));
}

export function validateDraft(
  draft: HarvestDraft,
  orchards: OrchardChoice[],
  ownerId: string,
  today = tunisToday(),
) {
  const errors: Partial<Record<keyof HarvestDraft, string>> = {};
  if (
    !orchards.some(
      (v) => v.id === draft.orchardId && v.ownerId === ownerId && v.active,
    )
  )
    errors.orchardId = "Sélectionnez un de vos vergers actifs.";
  const parsed = new Date(`${draft.desiredDate}T12:00:00Z`);
  if (
    !/^\d{4}-\d{2}-\d{2}$/.test(draft.desiredDate) ||
    !Number.isFinite(parsed.getTime()) ||
    parsed.toISOString().slice(0, 10) !== draft.desiredDate ||
    draft.desiredDate < today
  )
    errors.desiredDate =
      "Choisissez une date valide, aujourd’hui ou plus tard (heure de Tunis).";
  if (draft.estimatedKg.trim()) {
    const numeric = Number(draft.estimatedKg.trim().replace(",", "."));
    if (
      !/^\d+(?:[.,]\d{1,3})?$/.test(draft.estimatedKg.trim()) ||
      !Number.isFinite(numeric) ||
      numeric <= 0 ||
      numeric > MAX_ESTIMATED_KG
    ) {
      errors.estimatedKg = `Indiquez un poids positif en kg (3 décimales max, plafond ${MAX_ESTIMATED_KG} kg).`;
    }
  }
  if (draft.urgent && !draft.justification.trim())
    errors.justification = "Précisez le motif de l’urgence.";
  if (draft.justification.length > 500)
    errors.justification = "Limitez le motif à 500 caractères.";
  return errors;
}
