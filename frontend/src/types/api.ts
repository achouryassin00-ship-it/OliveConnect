import type { HarvestStatus, User } from "./domain";
/** DTOs validés avec le backend Spring Boot (J02/J03/J05). */
export interface AuthResponse {
  token: string;
  user: User;
}
export interface ApiPage<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface VergerDTO {
  id: string;
  code: string;
  nom: string;
  proprietaireId: string;
  commune: string | null;
  longitude: number | null;
  latitude: number | null;
  precisionGpsMetres: number | null;
  sourcePosition: "AUTO" | "MANUEL";
  surfaceHa: number;
  nombreArbres: number | null;
  anneePlantation: number | null;
  actif: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
  surfaceParcellesTotale: number;
}
export interface HistoriqueStatutDTO {
  ancienStatut: HarvestStatus | null;
  nouveauStatut: HarvestStatus;
  acteurId: string;
  acteurNom: string;
  acteurRole: string;
  motif: string | null;
  horodatage: string;
}
export interface RecolteDTO {
  id: string;
  reference: string;
  proprietaireId: string;
  vergerId: string;
  vergerNom: string | null;
  vergerCode: string | null;
  dateSouhaitee: string;
  quantiteEstimeeKg: number | null;
  urgent: boolean;
  motifUrgence: string | null;
  statut: HarvestStatus;
  annulable: boolean;
  historique: HistoriqueStatutDTO[];
  version: number;
  createdAt: string;
  updatedAt: string | null;
}
export interface RecolteCreateRequestDTO {
  vergerId: string;
  dateSouhaitee: string;
  quantiteEstimeeKg: number | null;
  urgent: boolean;
  motifUrgence: string | null;
}
export interface RecolteCancelRequestDTO {
  version?: number | null;
  motif?: string | null;
}
export interface RecolteListParams {
  page?: number;
  size?: number;
  q?: string;
  statut?: HarvestStatus | "";
  urgent?: boolean | "";
  vergerId?: string;
}
