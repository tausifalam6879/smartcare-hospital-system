import { useEffect, useState, type FormEvent } from 'react'
import { Camera, LockKeyhole, ShieldCheck, UserRound } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
import { api, messageFromError } from '../services/api'
import { IdentityAvatar } from '../components/IdentityAvatar'
import { isStaticDemo } from '../config/runtime'

type Profile = { displayName: string; gender: string | null; photo: string | null; hospitalIds: string[] }
const field = 'mt-2 w-full rounded-xl border border-blue-200 bg-blue-50/40 px-4 py-3 text-sm text-blue-950 focus:outline-blue-500'
const button = 'rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white disabled:opacity-50'
export function AccountPage() {
  const { session, logout, refreshProfile } = useAuth()
  const { text } = useLanguage()
  const navigate = useNavigate()
  const [profile, setProfile] = useState<Profile | null>(null)
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  useEffect(() => {
    let active = true
    if (isStaticDemo) { setError(text('Account changes require the hospital backend.', 'खाता बदलने के लिए अस्पताल बैकएंड ज़रूरी है।')); return }
    api.get<Profile>('/api/v1/account').then(({ data }) => { if (active) setProfile(data) }).catch(e => { if (active) setError(messageFromError(e)) })
    return () => { active = false }
  }, [session?.user.id])
  async function run(action: () => Promise<void>) {
    setBusy(true); setError(''); setNotice('')
    try { await action() } catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  async function save(event: FormEvent) {
    event.preventDefault()
    if (!profile) return
    await run(async () => {
      const { data } = await api.put<Profile>('/api/v1/account', { displayName: profile.displayName, gender: profile.gender })
      setProfile(data); await refreshProfile(); setNotice(text('Profile saved.', 'प्रोफ़ाइल सहेजी गई।'))
    })
  }
  async function upload(file?: File) {
    if (!file) return
    if (!['image/png', 'image/jpeg'].includes(file.type) || file.size > 1_048_576) { setError(text('Choose a JPEG or PNG under 1 MB.', '1 MB से कम JPEG या PNG चुनें।')); return }
    await run(async () => {
      const body = new FormData(); body.append('file', file)
      const { data } = await api.post<Profile>('/api/v1/account/photo', body, { headers: { 'Content-Type': 'multipart/form-data' } })
      setProfile(data); await refreshProfile(); setNotice(text('Photo updated privately.', 'फ़ोटो निजी रूप से अपडेट हुई।'))
    })
  }
  async function password(event: FormEvent) {
    event.preventDefault()
    if (next !== confirm) { setError(text('New passwords do not match.', 'नए पासवर्ड मेल नहीं खाते।')); return }
    await run(async () => {
      await api.post('/api/v1/account/password', { currentPassword: current, newPassword: next })
      logout(); navigate('/login', { replace: true })
    })
  }
  return <main className="mx-auto max-w-6xl space-y-6 p-6">
    <header className="rounded-3xl border border-blue-100 bg-gradient-to-r from-blue-100 via-white to-indigo-50 p-8"><p className="text-sm font-semibold text-blue-600">SmartCare · {text('Your account', 'आपका खाता')}</p><h1 className="mt-2 text-3xl font-bold text-blue-950">{text('Profile & security', 'प्रोफ़ाइल और सुरक्षा')}</h1><p className="mt-3 text-slate-600">{text('Your identity, photo and sign-in protection in one place.', 'आपकी पहचान, फ़ोटो और लॉगिन सुरक्षा एक जगह।')}</p></header>
    {error && <p role="alert" className="rounded-xl bg-rose-50 p-4 text-rose-800">{error}</p>}
    {notice && <p role="status" className="rounded-xl bg-blue-50 p-4 text-blue-800">{notice}</p>}
    {!profile && !error && <p role="status">{text('Loading account…', 'खाता लोड हो रहा है…')}</p>}
    {profile && <div className="grid grid-cols-2 gap-6">
      <form onSubmit={save} className="rounded-3xl border border-blue-100 bg-white p-7 shadow-sm"><h2 className="flex items-center gap-2 text-lg font-bold text-blue-950"><UserRound className="size-5 text-blue-600" />{text('Your identity', 'आपकी पहचान')}</h2><fieldset disabled={busy} className="mt-6 space-y-5">
        <div className="flex items-center gap-5"><IdentityAvatar name={profile.displayName} gender={profile.gender} imageUrl={profile.photo ?? undefined} size="lg" /><div><label className="inline-flex cursor-pointer items-center gap-2 text-sm font-semibold text-blue-700"><Camera className="size-4" />{text('Upload photo', 'फ़ोटो अपलोड करें')}<input aria-label={text('Profile photo', 'प्रोफ़ाइल फ़ोटो')} type="file" accept="image/png,image/jpeg" className="sr-only" onChange={e => { void upload(e.target.files?.[0]); e.target.value = '' }} /></label><p className="mt-1 text-xs text-slate-500">JPEG / PNG · {text('under 1 MB', '1 MB से कम')}</p>{profile.photo && <button type="button" className="mt-2 text-xs text-rose-600" onClick={() => void run(async () => { await api.delete('/api/v1/account/photo'); setProfile({ ...profile, photo: null }); await refreshProfile() })}>{text('Remove photo', 'फ़ोटो हटाएँ')}</button>}</div></div>
        <label className="block text-sm font-medium">{text('Full name', 'पूरा नाम')}<input required maxLength={120} className={field} value={profile.displayName} onChange={e => setProfile({ ...profile, displayName: e.target.value })} /></label>
        {session?.user.roles.includes('PATIENT') && <label className="block text-sm font-medium">{text('Gender (optional)', 'लिंग (वैकल्पिक)')}<select className={field} value={profile.gender ?? 'UNDISCLOSED'} onChange={e => setProfile({ ...profile, gender: e.target.value })}><option value="UNDISCLOSED">{text('Prefer not to say', 'नहीं बताना चाहते')}</option><option value="MALE">{text('Male', 'पुरुष')}</option><option value="FEMALE">{text('Female', 'महिला')}</option><option value="OTHER">{text('Other', 'अन्य')}</option></select></label>}
        <p className="rounded-xl bg-blue-50 p-3 text-xs leading-5 text-slate-600">{text('Your photo is private. Only you and your authorized treating doctor can see it. Missing photos use a fallback; gender is never guessed from a name.', 'आपकी फ़ोटो निजी है। केवल आप और आपके अधिकृत इलाज करने वाले डॉक्टर इसे देख सकते हैं। फ़ोटो न होने पर वैकल्पिक अवतार दिखता है; नाम से लिंग नहीं तय किया जाता।')}</p>
        <button className={button}>{text('Save profile', 'प्रोफ़ाइल सहेजें')}</button>
      </fieldset></form>
      <section className="rounded-3xl border border-blue-100 bg-white p-7 shadow-sm"><h2 className="flex items-center gap-2 text-lg font-bold text-blue-950"><LockKeyhole className="size-5 text-blue-600" />{text('Sign-in security', 'लॉगिन सुरक्षा')}</h2><form onSubmit={password}><fieldset disabled={busy} className="mt-6 space-y-4"><label className="block text-sm font-medium">{text('Current password', 'वर्तमान पासवर्ड')}<input type="password" autoComplete="current-password" required maxLength={72} className={field} value={current} onChange={e => setCurrent(e.target.value)} /></label><label className="block text-sm font-medium">{text('New password', 'नया पासवर्ड')}<input type="password" autoComplete="new-password" required minLength={12} maxLength={72} className={field} value={next} onChange={e => setNext(e.target.value)} /></label><label className="block text-sm font-medium">{text('Confirm new password', 'नया पासवर्ड दोहराएँ')}<input type="password" autoComplete="new-password" required minLength={12} maxLength={72} className={field} value={confirm} onChange={e => setConfirm(e.target.value)} /></label><p className="text-xs text-slate-500">{text('12–72 characters. Changing your password signs out all devices.', '12–72 अक्षर। पासवर्ड बदलने पर सभी डिवाइस लॉगआउट होंगे।')}</p><button className={button}>{text('Change password', 'पासवर्ड बदलें')}</button></fieldset></form><div className="mt-7 border-t border-blue-100 pt-5"><button disabled={busy} type="button" className="flex items-center gap-2 text-sm font-semibold text-blue-700" onClick={() => void run(async () => { await api.post('/api/v1/account/logout-all'); logout(); navigate('/login', { replace: true }) })}><ShieldCheck className="size-4" />{text('Sign out all devices', 'सभी डिवाइस से लॉगआउट करें')}</button><p className="mt-2 text-xs text-slate-500">{text('Staff invitations authorize enrollment; they are not a second factor at login.', 'स्टाफ आमंत्रण पंजीकरण की अनुमति देते हैं; वे लॉगिन का दूसरा सुरक्षा चरण नहीं हैं।')}</p></div></section>
    </div>}
  </main>
}
