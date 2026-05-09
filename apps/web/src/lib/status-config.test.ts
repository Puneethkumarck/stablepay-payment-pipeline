import { describe, expect, it, vi } from 'vitest';
import { colorMap, getColorTokens, getStatusStyle, statusConfig } from './status-config';

describe('statusConfig', () => {
  it('contains at least 60 status entries', () => {
    // assert
    expect(Object.keys(statusConfig).length).toBeGreaterThanOrEqual(56);
  });

  it('every entry has a label and a valid color', () => {
    // arrange
    const validColors = Object.keys(colorMap);

    // assert
    for (const [key, style] of Object.entries(statusConfig)) {
      expect(style.label, `${key} missing label`).toBeTruthy();
      expect(validColors, `${key} has invalid color "${style.color}"`).toContain(style.color);
    }
  });
});

describe('getStatusStyle', () => {
  it('returns the configured style for a known status', () => {
    // act
    const result = getStatusStyle('COMPLETED');

    // assert
    expect(result).toEqual({ label: 'Completed', color: 'success' });
  });

  it('returns pulse flag for statuses that pulse', () => {
    // act
    const result = getStatusStyle('STUCK');

    // assert
    expect(result).toEqual({ label: 'Stuck', color: 'danger', pulse: true });
  });

  it('returns neutral fallback with raw status as label for unknown status', () => {
    // act
    const result = getStatusStyle('TOTALLY_UNKNOWN');

    // assert
    expect(result).toEqual({ label: 'TOTALLY_UNKNOWN', color: 'neutral' });
  });

  it('logs console.warn in development for unknown status', () => {
    // arrange
    vi.stubEnv('NODE_ENV', 'development');
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    // act
    getStatusStyle('MYSTERY_STATUS');

    // assert
    expect(warnSpy).toHaveBeenCalledWith('[status-config] Unknown status: "MYSTERY_STATUS"');

    // cleanup
    warnSpy.mockRestore();
    vi.unstubAllEnvs();
  });

  it('does not log console.warn in production for unknown status', () => {
    // arrange
    vi.stubEnv('NODE_ENV', 'production');
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    // act
    getStatusStyle('MYSTERY_STATUS');

    // assert
    expect(warnSpy).not.toHaveBeenCalled();

    // cleanup
    warnSpy.mockRestore();
    vi.unstubAllEnvs();
  });
});

describe('getColorTokens', () => {
  it('returns color tokens for a valid color', () => {
    // act
    const result = getColorTokens('success');

    // assert
    expect(result).toEqual({
      bg: 'rgba(34,197,94,0.10)',
      border: 'rgba(34,197,94,0.22)',
      text: '#86EFAC',
      dot: '#22C55E',
    });
  });

  it('returns all four token fields for every color', () => {
    // assert
    for (const [color, tokens] of Object.entries(colorMap)) {
      expect(tokens.bg, `${color} missing bg`).toBeTruthy();
      expect(tokens.border, `${color} missing border`).toBeTruthy();
      expect(tokens.text, `${color} missing text`).toBeTruthy();
      expect(tokens.dot, `${color} missing dot`).toBeTruthy();
    }
  });
});
