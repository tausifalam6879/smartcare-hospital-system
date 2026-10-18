import { useEffect, useState } from 'react'
import { useLanguage } from '../context/LanguageContext'
import { accountTypes } from './AccountTypeSelect'
import { api, messageFromError } from '../services/api'

export type StaffRequest = { id: string; name: string; mobileNumber: string; email?: string; accountType: string; hospitalId: string; message: string; createdAt: string }
export function StaffRequestInbox({ hospitalId, refresh, onReview }: { hospitalId: string; refresh: number; onReview: (request: StaffRequest) => void }) {
  const { text } = useLanguage()
  const [requests, setRequests] = useState<StaffRequest[]>([]), [page, setPage] = useState(0), [pages, setPages] = useState(0)
  const [loading, setLoading] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [reload, setReload] = useState(0)
  useEffect(() => { setPage(0) }, [hospitalId])
  useEffect(() => {
    let active = true
    setRequests([]); setError(''); setLoading(false)
    if (!hospitalId) return
    setLoading(true)
    api.get<{ content: StaffRequest[]; totalPages: number }>('/api/v1/auth/staff-access-requests', { params: { hospitalId, page } })
      .then(({ data }) => { if (active) { setRequests(data.content); setPages(data.totalPages); if (page > 0 && page >= data.totalPages) setPage(Math.max(0, data.totalPages - 1)) } })
      .catch(e => { if (active) setError(messageFromError(e)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [hospitalId, page, refresh, reload])
  async function reject(request: StaffRequest) {
    setBusy(true); setError('')
    try { await api.post(`/api/v1/auth/staff-access-requests/${request.id}/reject`); setReload(r => r + 1) }
    catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  return <section className="my-6 border-y border-blue-100 py-5">
    <h2 className="text-xl font-bold">{text('Staff access requests', 'स्टाफ एक्सेस अनुरोध')}</h2>
    <p className="my-2 text-sm text-slate-600">{text('Choose a hospital to review requests. Verify the person independently; a request is not proof of employment.', 'अनुरोध देखने के लिए अस्पताल चुनें। पहचान स्वतंत्र रूप से सत्यापित करें; अनुरोध नौकरी का प्रमाण नहीं है।')}</p>
    <button type="button" disabled={!hospitalId || loading || busy} onClick={() => setReload(r => r + 1)} className="mb-3 font-bold text-blue-700">{text('Refresh requests', 'अनुरोध फिर लोड करें')}</button>
    {loading && <p role="status">{text('Loading requests…', 'अनुरोध लोड हो रहे हैं…')}</p>}
    {error && <p role="alert">{error}</p>}
    {!loading && !error && hospitalId && !requests.length && <p>{text('No pending requests for this hospital.', 'इस अस्पताल के लिए कोई लंबित अनुरोध नहीं है।')}</p>}
    <div className="space-y-3">{requests.map(request => <article key={request.id} className="rounded-xl border border-blue-100 p-4">
      <p className="font-bold">{request.name} · {text(accountTypes.find(t => t[0] === request.accountType)?.[1] ?? request.accountType, accountTypes.find(t => t[0] === request.accountType)?.[2] ?? request.accountType)}</p>
      <p className="break-words text-sm">{request.mobileNumber}{request.email && ` · ${request.email}`}</p>
      <p className="my-2 whitespace-pre-wrap break-words text-sm">{request.message}</p>
      <div className="flex gap-3"><button type="button" disabled={busy} onClick={() => onReview(request)} className="rounded-lg bg-blue-600 px-3 py-2 text-sm font-bold text-white">{text('Review for invitation', 'invitation के लिए समीक्षा करें')}</button><button type="button" disabled={busy} onClick={() => void reject(request)} className="rounded-lg border border-rose-200 px-3 py-2 text-sm text-rose-800">{text('Reject request', 'अनुरोध अस्वीकार करें')}</button></div>
    </article>)}</div>
    {pages > 1 && <div className="mt-3 flex items-center gap-4"><button type="button" disabled={loading || page === 0} onClick={() => setPage(page - 1)}>{text('Previous requests', 'पिछले अनुरोध')}</button><span>{page + 1} / {pages}</span><button type="button" disabled={loading || page + 1 >= pages} onClick={() => setPage(page + 1)}>{text('Next requests', 'अगले अनुरोध')}</button></div>}
  </section>
}
