import { Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import CreateInvite from './pages/CreateInvite'
import InvitePage from './pages/InvitePage'
import Dashboard from './pages/Dashboard'

export default function App() {
  return (
    <Routes>
      <Route path="/"              element={<Home />} />
      <Route path="/create"        element={<CreateInvite />} />
      <Route path="/i/:slug"       element={<InvitePage />} />
      <Route path="/dashboard/:id" element={<Dashboard />} />
    </Routes>
  )
}
