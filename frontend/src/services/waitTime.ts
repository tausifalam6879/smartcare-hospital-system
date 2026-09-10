export type WaitTimeRequest = {
  department: string
  triageCategory:
    | 'Immediate'
    | 'Emergency'
    | 'Urgent'
    | 'Semi-urgent'
    | 'Non-urgent'
  patientsAhead: number
  activeDoctors: number
  averageConsultationMinutes: number
  currentDoctorDelayMinutes: number
  occupancyRate: number
}

export type WaitTimePrediction = {
  estimatedWaitMinutes: number
  estimatedRangeMinutes: {
    minimum: number
    maximum: number
  }
  mlBaseWaitMinutes: number
  liveQueueWaitMinutes: number
  departmentSupported: boolean
  baseEstimateSource:
    | 'department-specific'
    | 'generic-department-average'
  modelVersion: string
  modelStatus: string
  urgentNotice: string | null
}

const mlApiUrl = (
  import.meta.env.VITE_ML_API_URL
  ?? 'http://127.0.0.1:8001'
).replace(/\/$/, '')

export async function predictWaitTime(
  request: WaitTimeRequest,
): Promise<WaitTimePrediction> {
  const response = await fetch(`${mlApiUrl}/predict-wait`, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    throw new Error(
      `Wait-time service returned status ${response.status}`,
    )
  }

  return response.json() as Promise<WaitTimePrediction>
}