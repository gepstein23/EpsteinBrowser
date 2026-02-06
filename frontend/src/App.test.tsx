import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, it, expect } from 'vitest'
import App from './App.tsx'

function renderApp(route = '/') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('App', () => {
  it('renders the home page by default', () => {
    renderApp()
    expect(screen.getByRole('heading', { name: 'Epstein Browser' })).toBeInTheDocument()
  })

  it('renders the search page', () => {
    renderApp('/search')
    expect(screen.getByRole('heading', { name: 'Search' })).toBeInTheDocument()
  })

  it('renders the datasets page', () => {
    renderApp('/datasets')
    expect(screen.getByRole('heading', { name: 'Datasets' })).toBeInTheDocument()
  })

  it('renders the document page', () => {
    renderApp('/documents/123')
    expect(screen.getByRole('heading', { name: 'Document' })).toBeInTheDocument()
    expect(screen.getByText(/123/)).toBeInTheDocument()
  })
})
