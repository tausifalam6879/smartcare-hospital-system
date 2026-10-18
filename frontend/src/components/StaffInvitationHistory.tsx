import { useEffect, useState } from 'react'
import { useLanguage } from '../context/LanguageContext'
import { accountTypes } from './AccountTypeSelect'
import { api, messageFromError } from '../services/api'

type Invitation = { id: string; mobileNumber: string; accountType: string; expiresAt: string; status: string }
export function StaffInvitationHistory({ hospitalId, refresh, onRevoked }: { hospitalId: string; refresh: number; onRevoked: (id: string) => void }) {
  const { text } = useLanguage()
  const [items, setItems] = useState<Invitation[]>([]), [page, setPage] = useState(0), [pages, setPages] = useState(0)
  const [loading, setLoading] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [reload, setReload] = useState(0)
  useEffect(() => { setPage(0) }, [hospitalId])
  useEffect(() => {
    let active = true
    setItems([]); setError(''); setLoading(false)
    if (!hospitalId) return
    setLoading(true)
    api.get<{ content: Invitation[]; totalPages: number }>('/api/v1/auth/staff-invitations', { params: { hospitalId, page } })
      .then(({ data }) => { if (active) { setItems(data.content); setPages(data.totalPages) } })
      .catch(e => { if (active) setError(messageFromError(e)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [hospitalId, page, refresh, reload])
  async function revoke(id: string) {
    setBusy(true); setError('')
    try { await api.delete(`/api/v1/auth/staff-invitations/${id}`); onRevoked(id); setReload(r => r + 1) }
    catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  const status = (value: string) => ({ ACTIVE: text('Active', 'सक्रिय'), EXPIRED: text('Expired', 'समाप्त'), ACCEPTED: text('Accepted', 'स्वीकार किया'), REVOKED: text('Revoked', 'रद्द') })[value] ?? value
  return <section className="mt-8 border-t border-blue-100 pt-5">
    <div className="flex items-center justify-between gap-4"><h2 className="text-xl font-bold">{text('Invitation history', 'invitation इतिहास')}</h2><button type="button" disabled={!hospitalId || loading || busy} onClick={() => setReload(r => r + 1)} className="font-bold text-blue-700">{text('Refresh invitations', 'invitation फिर लोड करें')}</button></div>
    <p className="my-3 text-sm text-slate-600">{text('Codes cannot be recovered after closing the page. Revoke a lost active invitation here, then issue a replacement.', 'पेज बंद होने के बाद कोड वापस नहीं देखा जा सकता। खोया सक्रिय invitation यहाँ रद्द करके नया बनाएँ।')}</p>
    {!hospitalId && <p>{text('Choose a hospital to view invitations.', 'invitation देखने के लिए अस्पताल चुनें।')}</p>}
    {loading && <p role="status">{text('Loading invitations…', 'invitation लोड हो रहे हैं…')}</p>}
    {error && <p role="alert">{error}</p>}
    {!loading && !error && hospitalId && !items.length && <p>{text('No invitations for this hospital yet.', 'इस अस्पताल के लिए अभी invitation नहीं है।')}</p>}
    <div className="space-y-3">{items.map(item => <article key={item.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-blue-100 p-4">
      <div><p className="font-semibold">{item.mobileNumber} · {text(accountTypes.find(t => t[0] === item.accountType)?.[1] ?? item.accountType, accountTypes.find(t => t[0] === item.accountType)?.[2] ?? item.accountType)}</p><p className="text-sm">{status(item.status)} · {text('Expires', 'समाप्ति')} {new Date(item.expiresAt).toLocaleString()}</p></div>
      {item.status === 'ACTIVE' && <button type="button" disabled={busy} onClick={() => void revoke(item.id)} className="rounded-lg border border-rose-200 px-3 py-2 font-semibold text-rose-800">{text('Revoke invitation for', 'invitation रद्द करें')} {item.mobileNumber}</button>}
    </article>)}</div>
    {pages > 1 && <div className="mt-3 flex items-center gap-4"><button type="button" disabled={loading || page === 0} onClick={() => setPage(page - 1)}>{text('Previous invitations', 'पिछले invitation')}</button><span>{page + 1} / {pages}</span><button type="button" disabled={loading || page + 1 >= pages} onClick={() => setPage(page + 1)}>{text('Next invitations', 'अगले invitation')}</button></div>}
  </section>
}
