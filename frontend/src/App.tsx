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
import { CareFollowUpsPage } from './pages/CareFollowUpsPage'
import { DoctorConsultationPage } from './pages/DoctorConsultationPage'

function Protected({ page }: { page: ReactNode }) {
  const { session } = useAuth()
  return session ? page : <Navigate to="/login" replace />
}

function PatientOrGuest({ page }: { page: ReactNode }) {
  const { session } = useAuth()
  return session && !session.user.roles.includes('PATIENT') ? <Navigate to="/dashboard" replace /> : page
}

function RoleProtected({ page, roles }: { page: ReactNode; roles: string[] }) {
  const { session } = useAuth()
  if (!session) return <Navigate to="/login" replace />
  return session.user.roles.some((role) => roles.includes(role)) ? page : <Navigate to="/dashboard" replace />
}

function RoleHome() {
  const { session } = useAuth()
  if (!session) return <Navigate to="/login" replace />
  const roles = session.user.roles
  if (roles.includes('DOCTOR')) return <Navigate to="/doctor/consultations" replace />
  if (roles.some((role) => ['AMBULANCE_DISPATCHER', 'BLOOD_BANK_STAFF', 'LAB_TECHNICIAN', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role))) {
    return <Navigate to="/staff/tasks" replace />
  }
  return <DashboardPage />
}

export function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="doctors" element={<DoctorDirectoryPage />} />
        <Route path="hospitals" element={<HospitalDirectoryPage />} />
        <Route path="booking" element={<PatientOrGuest page={<QueueBookingPage />} />} />
        <Route path="dashboard" element={<Protected page={<RoleHome />} />} />
        <Route path="queue/:appointmentId" element={<RoleProtected roles={['PATIENT']} page={<LiveQueuePage />} />} />
        <Route path="notifications" element={<Protected page={<NotificationsPage />} />} />
        <Route path="records" element={<RoleProtected roles={['PATIENT']} page={<MedicalRecordsPage />} />} />
        <Route path="follow-ups" element={<RoleProtected roles={['PATIENT']} page={<CareFollowUpsPage />} />} />
        <Route path="doctor/consultations" element={<RoleProtected roles={['DOCTOR']} page={<DoctorConsultationPage />} />} />
        <Route path="assistant" element={<RoleProtected roles={['PATIENT']} page={<CareAssistantPage />} />} />
        <Route path="diagnostics" element={<RoleProtected roles={['PATIENT']} page={<DiagnosticsPage />} />} />
        <Route path="blood-support" element={<RoleProtected roles={['PATIENT']} page={<BloodSupportPage />} />} />
        <Route path="ambulance" element={<RoleProtected roles={['PATIENT', 'AMBULANCE_DISPATCHER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']} page={<AmbulancePage />} />} />
        <Route path="operations" element={<RoleProtected roles={['PATIENT', 'DOCTOR', 'RECEPTIONIST', 'CASHIER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']} page={<OperationsPage />} />} />
        <Route path="staff/tasks" element={<RoleProtected roles={['AMBULANCE_DISPATCHER', 'BLOOD_BANK_STAFF', 'LAB_TECHNICIAN', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']} page={<StaffTaskInboxPage />} />} />
        <Route path="blood-group-analysis" element={<RoleProtected roles={['PATIENT', 'LAB_TECHNICIAN', 'BLOOD_BANK_STAFF', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']} page={<BloodGroupAnalysisPage />} />} />
        <Route path="navigate" element={<NavigationPage />} />
        <Route path="navigate/:checkpointCode" element={<NavigationPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
