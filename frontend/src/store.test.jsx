import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const { apiRequest } = vi.hoisted(() => ({ apiRequest: vi.fn() }));

vi.mock('axios', () => ({
  default: {
    create: () => ({ request: apiRequest }),
  },
}));

import Store from './store.jsx';

const product = {
  id: 1,
  name: 'Forma Ceramic Vase',
  category: 'Home & Living',
  price: 68,
  rating: 4.9,
  reviews: 124,
  image: 'photo-1578500494198-246f612d3b3d',
  stock: 18,
};

describe('storefront', () => {
  beforeEach(() => {
    localStorage.clear();
    apiRequest.mockReset();
    apiRequest.mockImplementation(({ url }) => {
      if (url.startsWith('/products')) return Promise.resolve({ status: 200, data: { content: [product] } });
      if (url === '/categories') return Promise.resolve({ status: 200, data: [{ id: 1, name: 'Home & Living' }] });
      return Promise.resolve({ status: 200, data: {} });
    });
  });

  it('renders products returned by the catalog API', async () => {
    render(<Store />);
    expect(await screen.findByText('Forma Ceramic Vase')).toBeInTheDocument();
  });

  it('adds a guest item to the bag and persists the guest cart', async () => {
    render(<Store />);
    await waitFor(() => expect(screen.getAllByRole('button', { name: 'Add to bag' })).toHaveLength(1));
    fireEvent.click(screen.getByRole('button', { name: 'Add to bag' }));
    fireEvent.click(screen.getByRole('button', { name: 'Shopping bag, 1 items' }));

    expect(await screen.findByText('Your bag')).toBeInTheDocument();
    expect(screen.getAllByText('Forma Ceramic Vase')).toHaveLength(2);
    expect(JSON.parse(localStorage.getItem('atelier-cart'))[0].quantity).toBe(1);
  });
});
