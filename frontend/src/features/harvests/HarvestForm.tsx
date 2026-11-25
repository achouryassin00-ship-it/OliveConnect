import { useRef, useState, type FormEvent } from "react";
import { ApiError } from "../../api/client";
import {
  tunisToday,
  validateDraft,
  type HarvestDraft,
  type OrchardChoice,
} from "./model";
export function HarvestForm({
  draft,
  onChange,
  orchards,
  ownerId,
  demo,
  disabled,
  onSubmit,
}: {
  draft: HarvestDraft;
  onChange: (draft: HarvestDraft) => void;
  orchards: OrchardChoice[];
  ownerId: string;
  demo: boolean;
  disabled: boolean;
  onSubmit: (draft: HarvestDraft) => Promise<void>;
}) {
  const [errors, setErrors] = useState<
    Partial<Record<keyof HarvestDraft, string>>
  >({});
  const [failure, setFailure] = useState("");
  const [busy, setBusy] = useState(false);
  const lock = useRef(false);
  const form = useRef<HTMLFormElement>(null);
  function update<K extends keyof HarvestDraft>(
    key: K,
    value: HarvestDraft[K],
  ) {
    onChange({ ...draft, [key]: value });
    setErrors((e) => ({ ...e, [key]: undefined }));
    setFailure("");
  }
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (lock.current || disabled) return;
    const found = validateDraft(draft, orchards, ownerId);
    setErrors(found);
    setFailure("");
    if (Object.keys(found).length) {
      requestAnimationFrame(() =>
        form.current
          ?.querySelector<HTMLElement>('[aria-invalid="true"]')
          ?.focus(),
      );
      return;
    }
    lock.current = true;
    setBusy(true);
    try {
      await onSubmit(draft);
    } catch (e) {
      setFailure(
        e instanceof ApiError
          ? e.message
          : "Opération impossible. Votre saisie est conservée.",
      );
      if (e instanceof ApiError) setErrors(e.fieldErrors);
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }
  const own = orchards.filter((v) => v.active && v.ownerId === ownerId);
  return (
    <form
      ref={form}
      className="harvest-form"
      onSubmit={submit}
      noValidate
      aria-busy={busy}
    >
      <p className="muted">
        Les champs marqués * sont nécessaires pour vérifier ce brouillon. Aucune
        règle serveur de récolte n’est encore disponible.
      </p>
      {failure && (
        <div className="form-error" role="alert">
          {failure} Votre saisie reste dans ce formulaire.
        </div>
      )}
      <fieldset disabled={busy || disabled}>
        <label htmlFor="orchardId">Votre verger *</label>
        <select
          id="orchardId"
          value={draft.orchardId}
          onChange={(e) => update("orchardId", e.target.value)}
          aria-invalid={!!errors.orchardId}
          aria-describedby={errors.orchardId ? "orchard-error" : undefined}
        >
          <option value="">Sélectionner un verger</option>
          {own.map((v) => (
            <option key={v.id} value={v.id}>
              {v.name} · {v.code}
            </option>
          ))}
        </select>
        {errors.orchardId && (
          <span id="orchard-error" className="field-error">
            {errors.orchardId}
          </span>
        )}
        {!own.length && (
          <p>
            Aucun verger actif disponible pour ce compte. Contactez la
            coopérative.
          </p>
        )}
        <div className="form-columns">
          <div>
            <label htmlFor="desiredDate">Date souhaitée *</label>
            <input
              id="desiredDate"
              type="date"
              min={tunisToday()}
              value={draft.desiredDate}
              onChange={(e) => update("desiredDate", e.target.value)}
              aria-invalid={!!errors.desiredDate}
              aria-describedby="date-hint date-error"
            />
            <small id="date-hint">
              Date locale à Tunis ; créneau à confirmer par la coopérative.
            </small>
            {errors.desiredDate && (
              <span id="date-error" className="field-error">
                {errors.desiredDate}
              </span>
            )}
          </div>
          <div>
            <label htmlFor="estimatedKg">Quantité estimée (kg)</label>
            <input
              id="estimatedKg"
              inputMode="decimal"
              value={draft.estimatedKg}
              onChange={(e) => update("estimatedKg", e.target.value)}
              placeholder="Ex. 2 400 → saisir 2400"
              aria-invalid={!!errors.estimatedKg}
              aria-describedby="weight-hint weight-error"
            />
            <small id="weight-hint">
              Facultative dans cet aperçu ; obligation à valider au Jour 5.
            </small>
            {errors.estimatedKg && (
              <span id="weight-error" className="field-error">
                {errors.estimatedKg}
              </span>
            )}
          </div>
        </div>
        <label htmlFor="requestType">Type de demande *</label>
        <select
          id="requestType"
          value={draft.urgent ? "urgent" : "normal"}
          onChange={(e) => update("urgent", e.target.value === "urgent")}
        >
          <option value="normal">Normale</option>
          <option value="urgent">Urgente</option>
        </select>
        {draft.urgent && (
          <>
            <label htmlFor="justification">Motif de l’urgence *</label>
            <textarea
              id="justification"
              rows={3}
              maxLength={500}
              value={draft.justification}
              onChange={(e) => update("justification", e.target.value)}
              aria-invalid={!!errors.justification}
              aria-describedby="justification-error"
            />
            {errors.justification && (
              <span id="justification-error" className="field-error">
                {errors.justification}
              </span>
            )}
          </>
        )}
        <div className="form-actions">
          <button type="submit" disabled={busy || disabled || own.length === 0}>
            {busy
              ? "Vérification en cours…"
              : demo
                ? "Ajouter un exemple local"
                : "Vérifier le brouillon"}
          </button>
          <span className="muted">
            {demo
              ? "Simulation en mémoire · aucun envoi"
              : "Transmission indisponible au Jour 4"}
          </span>
        </div>
      </fieldset>
    </form>
  );
}
