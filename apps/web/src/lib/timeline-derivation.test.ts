import { describe, expect, it } from 'vitest';
import { deriveTimeline } from './timeline-derivation';

describe('deriveTimeline', () => {
  it('marks steps before current as done, current as live, rest as pending', () => {
    // arrange & act
    const steps = deriveTimeline('SCREENING_IN_PROGRESS', 'FIAT', 'PAYIN');

    // assert
    expect(steps.map((s) => s.state)).toEqual([
      'done',
      'done',
      'done',
      'done',
      'live',
      'pending',
      'pending',
      'pending',
      'pending',
    ]);
  });

  it('marks all steps as done and last as live when COMPLETED', () => {
    // arrange & act
    const steps = deriveTimeline('COMPLETED', 'FIAT', 'PAYIN');

    // assert
    const last = steps[steps.length - 1]!;
    expect(last.label).toBe('Completed');
    expect(last.state).toBe('live');
    expect(steps.slice(0, -1).every((s) => s.state === 'done')).toBe(true);
  });

  it('returns correct lifecycle for crypto payout', () => {
    // arrange & act
    const steps = deriveTimeline('BROADCASTING', 'CRYPTO', 'PAYOUT');

    // assert
    expect(steps.find((s) => s.label === 'Broadcasting')?.state).toBe('live');
    expect(steps.find((s) => s.label === 'Signed')?.state).toBe('done');
    expect(steps.find((s) => s.label === 'Confirming on-chain')?.state).toBe('pending');
  });

  it('returns correct lifecycle for fiat payout', () => {
    // arrange & act
    const steps = deriveTimeline('APPROVED', 'FIAT', 'PAYOUT');

    // assert
    expect(steps.find((s) => s.label === 'Approved')?.state).toBe('live');
    expect(steps.find((s) => s.label === 'Pending approval')?.state).toBe('done');
    expect(steps.find((s) => s.label === 'Pending screening')?.state).toBe('pending');
  });

  it('returns correct lifecycle for crypto payin', () => {
    // arrange & act
    const steps = deriveTimeline('CONFIRMING', 'CRYPTO', 'PAYIN');

    // assert
    expect(steps.find((s) => s.label === 'Confirming on-chain')?.state).toBe('live');
    expect(steps.find((s) => s.label === 'Detected')?.state).toBe('done');
  });

  it('handles terminal failure status with done steps before it', () => {
    // arrange & act
    const steps = deriveTimeline('FAILED', 'FIAT', 'PAYIN');

    // assert
    const lastStep = steps[steps.length - 1]!;
    expect(lastStep.label).toBe('failed');
    expect(lastStep.state).toBe('live');
    expect(steps.slice(0, -1).every((s) => s.state === 'done')).toBe(true);
  });

  it('handles CANCELLED terminal status', () => {
    // arrange & act
    const steps = deriveTimeline('CANCELLED', 'CRYPTO', 'PAYOUT');

    // assert
    const lastStep = steps[steps.length - 1]!;
    expect(lastStep.label).toBe('cancelled');
    expect(lastStep.state).toBe('live');
  });

  it('handles unknown status with a single live step', () => {
    // arrange & act
    const steps = deriveTimeline('SOME_UNKNOWN_STATUS', 'FIAT', 'PAYIN');

    // assert
    expect(steps).toHaveLength(1);
    expect(steps[0]?.state).toBe('live');
    expect(steps[0]?.label).toBe('some unknown status');
  });

  it('marks INITIATED as live with all others pending', () => {
    // arrange & act
    const steps = deriveTimeline('INITIATED', 'FIAT', 'PAYIN');

    // assert
    expect(steps[0]?.label).toBe('Initiated');
    expect(steps[0]?.state).toBe('live');
    expect(steps.slice(1).every((s) => s.state === 'pending')).toBe(true);
  });
});
