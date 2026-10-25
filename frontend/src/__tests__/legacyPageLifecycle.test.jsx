import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { render, screen } from '@testing-library/react';
import { LegacyPage } from '../App.jsx';

describe('LegacyPage', () => {
  it('deshabilita el adaptador antiguo y no carga HTML ni ejecuta scripts externos', () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch');

    render(<LegacyPage />);

    expect(screen.getByRole('heading', { name: 'Esta vista antigua ya no está disponible' })).toBeInTheDocument();
    expect(fetchSpy).not.toHaveBeenCalled();
    expect(document.querySelector('#legacy-root')).toBeNull();
  });
});
