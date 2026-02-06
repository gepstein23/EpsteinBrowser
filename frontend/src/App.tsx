import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout.tsx'
import HomePage from './pages/HomePage.tsx'
import SearchPage from './pages/SearchPage.tsx'
import DocumentPage from './pages/DocumentPage.tsx'
import DatasetsPage from './pages/DatasetsPage.tsx'

function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/documents/:id" element={<DocumentPage />} />
        <Route path="/datasets" element={<DatasetsPage />} />
      </Route>
    </Routes>
  )
}

export default App
