import { useParams } from 'react-router-dom'

export default function DocumentPage() {
  const { id } = useParams()

  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900">Document</h1>
      <p className="mt-4 text-gray-600">Viewing document {id}.</p>
    </div>
  )
}
