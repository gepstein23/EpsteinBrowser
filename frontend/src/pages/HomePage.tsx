import epsteinPhoto from '../assets/jeffrey-epstein-0zm.png'

export default function HomePage() {
  return (
    <div>
      <img
        src={epsteinPhoto}
        alt="Jeffrey Epstein"
        className="w-40 rounded-lg shadow-md mb-6"
      />
      <h1 className="text-3xl font-bold text-gray-900">Epstein Browser</h1>
      <p className="mt-4 text-gray-600">
        Explore the Epstein files from government FOIA releases.
      </p>
    </div>
  )
}
