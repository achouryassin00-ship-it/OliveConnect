import type { User } from "./domain";
/** DTOs inspected in Spring Boot J02/J03; do not reuse J01 demo Orchard. */
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
