import { ShieldCheck } from 'lucide-react'
import type { ReactNode } from 'react'
import { BrandMark } from './BrandMark'
import { useLanguage } from '../context/LanguageContext'

export function AuthShell({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  const { text } = useLanguage()
  return (
    <div className="mx-auto grid min-h-[calc(100vh-4.5rem)] max-w-6xl items-center gap-10 px-4 py-10 sm:px-6 lg:grid-cols-2 lg:px-8">
      <div className="hidden rounded-[2rem] bg-ink-950 p-10 text-white lg:block">
        <BrandMark />
        <h2 className="mt-16 text-4xl font-black leading-tight tracking-tight">{text('Your account. Your authorized workspace.', 'आपका अकाउंट। आपका अधिकृत कार्यक्षेत्र।')}</h2>
        <p className="mt-5 leading-7 text-slate-300">{text('Patients can register directly. Doctors and hospital staff need an administrator-issued invitation to activate a new staff account.', 'मरीज़ सीधे रजिस्टर कर सकते हैं। डॉक्टर और अस्पताल स्टाफ को नया स्टाफ अकाउंट चालू करने के लिए एडमिन का invitation चाहिए।')}</p>
        <div className="mt-12 flex items-start gap-3 rounded-2xl bg-white/10 p-4"><ShieldCheck className="mt-0.5 size-5 shrink-0 text-care-300" /><p className="text-sm leading-6 text-slate-200">{text('Passwords are securely hashed. Sign-in is stored in this browser until expiry or sign-out. Always sign out on shared computers.', 'पासवर्ड सुरक्षित हैश के रूप में रखे जाते हैं। लॉगिन इस ब्राउज़र में अवधि समाप्त होने या साइन आउट तक रहता है। साझा कंप्यूटर पर हमेशा साइन आउट करें।')}</p></div>
      </div>
      <div className="mx-auto w-full max-w-md">
        <p className="text-xs font-extrabold uppercase tracking-[.2em] text-care-700">{text('SmartCare account access', 'SmartCare अकाउंट प्रवेश')}</p>
        <h1 className="mt-2 text-3xl font-black tracking-tight text-ink-950">{title}</h1>
        <p className="mt-2 text-sm leading-6 text-slate-600">{subtitle}</p>
        <div className="mt-7">{children}</div>
      </div>
    </div>
  )
}
