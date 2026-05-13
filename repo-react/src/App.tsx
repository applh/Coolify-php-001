import { useState } from 'react'

function App() {
  const [count, setCount] = useState(0)

  return (
    <div style={{ padding: '2rem', textAlign: 'center', fontFamily: 'sans-serif' }}>
      <h1>Vite + React</h1>
      <div style={{ margin: '2rem' }}>
        <button onClick={() => setCount((count) => count + 1)} style={{ padding: '0.5rem 1rem', fontSize: '1.2rem', cursor: 'pointer' }}>
          count is {count}
        </button>
      </div>
      <p style={{ color: '#666' }}>
        Edit <code>src/App.tsx</code> and save to test HMR
      </p>
    </div>
  )
}

export default App
