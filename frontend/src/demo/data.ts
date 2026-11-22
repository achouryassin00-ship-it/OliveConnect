import type {
  Orchard,
  Harvest,
  FieldAlert,
  Resource,
  Tour,
  Weighing,
} from "../types/domain";
// Entirely fictional, fixed fixtures. No API, persistence, live weather or real identities.
export const orchards: Orchard[] = [
  {
    id: "demo-v1",
    name: "El Amra 12",
    zone: "Sfax Nord",
    ownerId: "demo-user",
    areaHa: 8.5,
    variety: "Chemlali",
    maturityPercent: 92,
    location: { type: "Point", coordinates: [10.85, 34.98] },
    active: true,
  },
  {
    id: "demo-v2",
    name: "Sakiet Ezzit 04",
    zone: "Sakiet Ezzit",
    ownerId: "demo-user",
    areaHa: 5.2,
    variety: "Chemlali",
    maturityPercent: 78,
    location: { type: "Point", coordinates: [10.77, 34.8] },
    active: true,
  },
  {
    id: "demo-v3",
    name: "Mahrès 09",
    zone: "Sfax Sud",
    ownerId: "demo-user",
    areaHa: 12,
    variety: "Chetoui",
    maturityPercent: 85,
    location: { type: "Point", coordinates: [10.5, 34.53] },
    active: true,
  },
  {
    id: "demo-v4",
    name: "El Hencha 03",
    zone: "El Hencha",
    ownerId: "demo-user",
    areaHa: 6.8,
    variety: "Chemlali",
    maturityPercent: null,
    location: { type: "Point", coordinates: [10.72, 35.11] },
    active: true,
  },
];
export const harvests: Harvest[] = orchards.map((v, i) => ({
  id: `COL-${204 + i}`,
  orchardId: v.id,
  estimatedKg: [2400, 1800, 3200, 2100][i],
  scheduledAt: `2026-11-${22 + i}T07:00:00Z`,
  status: (["DEMANDEE", "PLANIFIEE", "EN_COURS", "VALIDEE"] as const)[i],
  urgent: i === 0,
  version: 0,
}));
export const alerts: FieldAlert[] = [
  {
    id: "a1",
    orchardId: "demo-v1",
    title: "Maturité optimale",
    severity: "URGENTE",
    category: "MATURITE",
    createdAt: "2026-11-22T08:00:00Z",
  },
  {
    id: "a2",
    orchardId: "demo-v3",
    title: "Risque de pluie · météo simulée",
    severity: "ELEVEE",
    category: "METEO",
    createdAt: "2026-11-22T07:00:00Z",
  },
];
export const resources: Resource[] = [
  {
    id: "TR-04",
    name: "Tracteur New Holland",
    kind: "TRACTEUR",
    status: "DISPONIBLE",
  },
  {
    id: "BN-12",
    name: "Benne 2 tonnes",
    kind: "BENNE",
    status: "AFFECTEE",
    capacityKg: 2000,
  },
  {
    id: "VB-07",
    name: "Vibreur électrique",
    kind: "MATERIEL",
    status: "MAINTENANCE",
  },
];
export const tours: Tour[] = [
  {
    id: "T-001",
    name: "Circuit Sfax Nord",
    startAt: "2026-11-23T06:00:00Z",
    endAt: "2026-11-23T11:00:00Z",
    harvestIds: ["COL-205"],
    resourceIds: ["BN-12"],
    status: "BROUILLON",
    version: 0,
  },
];
export const weighings: Weighing[] = [
  {
    id: "LOT-810",
    tourId: "T-DEMO-ARCHIVE",
    millId: "demo-mill",
    netKg: 2450,
    measuredAt: "2026-11-21T10:00:00Z",
    quality: "A",
  },
];
export const weeklyVolumes = [8, 12, 10, 18, 15, 23, 19];
export const campaign = {
  activeHarvests: 24,
  collectedKg: 128000,
  openAlerts: 7,
  completionPercent: 78,
};
