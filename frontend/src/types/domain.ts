export type Role =
  | "AGRICULTEUR"
  | "RESPONSABLE_COLLECTE"
  | "CHEF_EQUIPE"
  | "RESPONSABLE_LOGISTIQUE"
  | "RESPONSABLE_PRESSOIR"
  | "ADMINISTRATEUR";
export type HarvestStatus =
  | "DEMANDEE"
  | "VALIDEE"
  | "PLANIFIEE"
  | "EN_COURS"
  | "LIVREE"
  | "PESEE"
  | "ARCHIVEE"
  | "REFUSEE"
  | "ANNULEE"
  | "INTERROMPUE";
export interface GeoPoint {
  type: "Point";
  coordinates: [longitude: number, latitude: number];
}
export interface User {
  id: string;
  name: string;
  email: string;
  roles: Role[];
  active: boolean;
}
export interface Orchard {
  id: string;
  name: string;
  zone: string;
  ownerId: string;
  areaHa: number;
  variety: string;
  maturityPercent: number | null;
  location: GeoPoint;
  active: boolean;
}
export interface Harvest {
  id: string;
  orchardId: string;
  estimatedKg: number;
  scheduledAt: string;
  status: HarvestStatus;
  urgent: boolean;
  version: number;
}
export interface FieldAlert {
  id: string;
  orchardId: string;
  title: string;
  severity: "URGENTE" | "ELEVEE" | "MODEREE";
  category: "MATURITE" | "METEO" | "PROBLEME";
  createdAt: string;
}
export interface Resource {
  id: string;
  name: string;
  kind: "OUVRIER" | "TRACTEUR" | "BENNE" | "MATERIEL";
  status: "DISPONIBLE" | "AFFECTEE" | "MAINTENANCE";
  capacityKg?: number;
}
export interface Tour {
  id: string;
  name: string;
  startAt: string;
  endAt: string;
  harvestIds: string[];
  resourceIds: string[];
  status: "BROUILLON" | "PLANIFIEE" | "EN_COURS" | "TERMINEE";
  version: number;
}
export interface Mill {
  id: string;
  name: string;
  availableCapacityKg: number;
}
export interface Weighing {
  id: string;
  tourId: string;
  millId: string;
  netKg: number;
  measuredAt: string;
  quality: string;
  replacesId?: string;
}
export interface PageResult<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}
