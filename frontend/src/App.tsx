import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { MainLayout } from './components/MainLayout'
import { useAuth } from './context/AuthContext'
import { DashboardPage } from './pages/DashboardPage'
import { CareAssistantPage } from './pages/CareAssistantPage'
import { BloodSupportPage } from './pages/BloodSupportPage'
import { AmbulancePage } from './pages/AmbulancePage'
import { DoctorDirectoryPage } from './pages/DoctorDirectoryPage'
import { DiagnosticsPage } from './pages/DiagnosticsPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { LiveQueuePage } from './pages/LiveQueuePage'
import { MedicalRecordsPage } from './pages/MedicalRecordsPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { NavigationPage } from './pages/NavigationPage'
import { QueueBookingPage } from './pages/QueueBookingPage'
import { RegisterPage } from './pages/RegisterPage'
import { OperationsPage } from './pages/OperationsPage'
import { BloodGroupAnalysisPage } from './pages/BloodGroupAnalysisPage'
import { HospitalDirectoryPage } from './pages/HospitalDirectoryPage'
import { StaffTaskInboxPage } from './pages/StaffTaskInboxPage'

function Protected({ page }: { page: ReactNode }) {
  const { session } = useAuth()
  return session ? page : <Navigate to="/login" replace />
}

export function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="doctors" element={<DoctorDirectoryPage />} />
        <Route path="hospitals" element={<HospitalDirectoryPage />} />
        <Route path="booking" element={<QueueBookingPage />} />
        <Route path="dashboard" element={<Protected page={<DashboardPage />} />} />
        <Route path="queue/:appointmentId" element={<Protected page={<LiveQueuePage />} />} />
        <Route path="notifications" element={<Protected page={<NotificationsPage />} />} />
        <Route path="records" element={<Protected page={<MedicalRecordsPage />} />} />
        <Route path="assistant" element={<Protected page={<CareAssistantPage />} />} />
        <Route path="diagnostics" element={<Protected page={<DiagnosticsPage />} />} />
        <Route path="blood-support" element={<Protected page={<BloodSupportPage />} />} />
        <Route path="ambulance" element={<Protected page={<AmbulancePage />} />} />
        <Route path="operations" element={<Protected page={<OperationsPage />} />} />
        <Route path="staff/tasks" element={<Protected page={<StaffTaskInboxPage />} />} />
        <Route path="blood-group-analysis" element={<Protected page={<BloodGroupAnalysisPage />} />} />
        <Route path="navigate" element={<NavigationPage />} />
        <Route path="navigate/:checkpointCode" element={<NavigationPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
