import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { OperationsWorklist } from './OperationsWorklist'
import type { OperationsQueueRow } from '../services/operations'
import { LanguageProvider } from '../context/LanguageContext'

const rows: OperationsQueueRow[] = [
  { appointmentId: '1', patientNumber: 'PAT-ONE', doctorId: 'a', doctorName: 'Doctor A', queuePosition: 1, status: 'CASH_PENDING', paymentMethod: 'CASH' },
  { appointmentId: '2', patientNumber: 'PAT-TWO', doctorId: 'b', doctorName: 'Doctor B', queuePosition: 2, status: 'CONFIRMED', paymentMethod: 'ONLINE' },
]
const props = { rows, date: '2026-09-25', today: '2026-09-24', saving: false, canManage: true, canConfirmCash: true, onAction: vi.fn(), onNoShow: vi.fn() }
afterEach(() => { cleanup(); localStorage.removeItem('smartcare-language') })
describe('OperationsWorklist', () => {
  it('shows Hindi filters and actions without changing API values', () => {
    localStorage.setItem('smartcare-language', 'hi')
    render(<LanguageProvider><OperationsWorklist {...props} /></LanguageProvider>)
    expect(screen.getByRole('heading', { name: 'मरीज़ों की कतार' })).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('भुगतान का तरीका'), { target: { value: 'CASH' } })
    expect(screen.queryByText('PAT-TWO')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'नकद भुगतान की पुष्टि करें' }))
    expect(props.onAction).toHaveBeenCalledWith('cash', '1')
  })
  it('intersects filters and restores the whole queue on clear', () => {
    render(<OperationsWorklist {...props} />)
    fireEvent.change(screen.getByLabelText('Payment method'), { target: { value: 'CASH' } })
    expect(screen.getByText('PAT-ONE')).toBeInTheDocument()
    expect(screen.queryByText('PAT-TWO')).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Filter doctor'), { target: { value: 'b' } })
    expect(screen.getByText(/No matching appointments/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))
    fireEvent.change(screen.getByLabelText('Search patient / OPD'), { target: { value: '2' } })
    expect(screen.getByText('PAT-TWO')).toBeInTheDocument()
    expect(screen.queryByText('PAT-ONE')).not.toBeInTheDocument()
  })
  it('explains future check-in and respects cashier permissions', () => {
    render(<OperationsWorklist {...props} canConfirmCash={false} />)
    expect(screen.getByText('Check-in opens on 2026-09-25')).toBeInTheDocument()
    expect(screen.getByText('Awaiting cashier confirmation')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirm cash' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Check in' })).not.toBeInTheDocument()
  })
  it('sends the selected appointment ID for same-day check-in', () => {
    render(<OperationsWorklist {...props} date={props.today} />)
    fireEvent.click(screen.getByRole('button', { name: 'Check in' }))
    expect(props.onAction).toHaveBeenCalledWith('check-in', '2')
  })
})
