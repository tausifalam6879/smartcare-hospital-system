import {
  AlertTriangle, ArrowRight, Bot, BrainCircuit, ChevronRight, CircleStop, FileCheck2,
  FileText, History, LoaderCircle, LockKeyhole, MessageSquarePlus, Send, ShieldCheck,
  Sparkles, Stethoscope, UserRound,
} from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { messageFromError } from '../services/api'
import {
  askAssistant, createConversation, getAssistantStatus, getConversationMessages, getConversations,
  type AssistantMessage, type AssistantStatus, type Conversation,
} from '../services/assistant'

const prompts = [
  'What medicines were prescribed during my previous visit?',
  'When did my doctor ask me to follow up?',
  'What did my latest lab report say?',
  'Show my clinician-recorded allergies.',
]

const safetyStyle: Record<AssistantMessage['safetyClass'], string> = {
  NORMAL: 'border-slate-200 bg-white',
  EVIDENCE_UNAVAILABLE: 'border-amber-200 bg-amber-50',
  CLINICAL_BOUNDARY: 'border-violet-200 bg-violet-50',
  EMERGENCY_ESCALATION: 'border-rose-300 bg-rose-50',
}

function shortTime(value: string) {
  return new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })
    .format(new Date(value))
}

export function CareAssistantPage() {
  const [conversations, setConversations] = useState<Conversation[]>([])
  const [currentId, setCurrentId] = useState('')
  const [messages, setMessages] = useState<AssistantMessage[]>([])
  const [status, setStatus] = useState<AssistantStatus | null>(null)
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    Promise.all([getConversations(), getAssistantStatus()])
      .then(async ([items, assistantStatus]) => {
        setConversations(items)
        setStatus(assistantStatus)
        if (items[0]) {
          setCurrentId(items[0].id)
          setMessages(await getConversationMessages(items[0].id))
        }
      })
      .catch((requestError) => setError(messageFromError(requestError)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { endRef.current?.scrollIntoView?.({ behavior: 'smooth' }) }, [messages, sending])

  async function selectConversation(id: string) {
    setCurrentId(id)
    setError('')
    setLoading(true)
    try { setMessages(await getConversationMessages(id)) }
    catch (requestError) { setError(messageFromError(requestError)) }
    finally { setLoading(false) }
  }

  async function newConversation() {
    setError('')
    try {
      const created = await createConversation()
      setConversations((items) => [created, ...items])
      setCurrentId(created.id)
      setMessages([])
      setQuestion('')
    } catch (requestError) { setError(messageFromError(requestError)) }
  }

  async function send(event?: FormEvent, suggestedQuestion?: string) {
    event?.preventDefault()
    const text = (suggestedQuestion ?? question).trim()
    if (text.length < 3 || sending) return
    setSending(true)
    setError('')
    try {
      let conversationId = currentId
      if (!conversationId) {
        const created = await createConversation()
        conversationId = created.id
        setCurrentId(created.id)
        setConversations((items) => [created, ...items])
      }
      const optimistic: AssistantMessage = {
        id: `local-${Date.now()}`, role: 'USER', content: text, safetyClass: 'NORMAL', grounded: false,
        createdAt: new Date().toISOString(), citations: [],
      }
      setMessages((items) => [...items, optimistic])
      setQuestion('')
      const result = await askAssistant(conversationId, text)
      setMessages((items) => [...items, result.message])
      setConversations(await getConversations())
    } catch (requestError) {
      setMessages((items) => items.filter((item) => !item.id.startsWith('local-')))
      setError(messageFromError(requestError))
    } finally { setSending(false) }
  }

  return (
    <div className="min-h-screen bg-[#f3f7fb]">
      <section className="overflow-hidden bg-[linear-gradient(115deg,#06172e_0%,#0a315a_60%,#08716b_130%)] text-white">
        <div className="mx-auto grid max-w-7xl gap-7 px-4 py-9 sm:px-6 lg:grid-cols-[1fr_.7fr] lg:px-8 lg:py-12">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-teal-300/20 bg-teal-300/10 px-3 py-2 text-xs font-black uppercase tracking-[.18em] text-teal-200"><LockKeyhole className="size-4" />Patient-isolated evidence</div>
            <h1 className="mt-5 text-4xl font-black tracking-tight sm:text-5xl">Ask your record. See the source.</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-blue-100/75 sm:text-base">SmartCare searches only your authorized visits and reports, then shows exactly where each answer came from. Unsupported information stays unavailable.</p>
          </div>
          <div className="grid grid-cols-3 gap-3 self-end text-center">
            {[[ShieldCheck, 'Patient filtered'], [FileCheck2, 'Cited answers'], [CircleStop, 'No diagnosis']].map(([Icon, label]) => { const ItemIcon = Icon as typeof ShieldCheck; return <div key={label as string} className="rounded-2xl border border-white/10 bg-white/8 p-3 backdrop-blur"><ItemIcon className="mx-auto size-5 text-teal-200" /><p className="mt-2 text-[11px] font-bold text-blue-100/70">{label as string}</p></div> })}
          </div>
        </div>
      </section>

      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        {error && <div role="alert" className="mb-5 flex gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-800"><AlertTriangle className="size-5 shrink-0" />{error}</div>}
        <div className="grid min-h-[680px] overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-[0_24px_70px_-45px_rgba(9,45,87,.45)] lg:grid-cols-[280px_1fr]">
          <aside className="border-b border-slate-200 bg-[#081d38] p-4 text-white lg:border-b-0 lg:border-r">
            <button onClick={() => void newConversation()} className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-teal-500 px-4 text-sm font-black text-white transition hover:bg-teal-400"><MessageSquarePlus className="size-4" />New conversation</button>
            <div className="mt-5 flex items-center gap-2 text-[11px] font-black uppercase tracking-[.16em] text-blue-200/60"><History className="size-4" />Your history</div>
            <div className="mt-3 flex gap-2 overflow-x-auto pb-1 lg:block lg:space-y-2 lg:overflow-visible">
              {conversations.length === 0 ? <p className="rounded-xl border border-dashed border-white/15 p-4 text-xs leading-5 text-blue-100/50">Your private conversations will appear here.</p> : conversations.map((conversation) => <button key={conversation.id} onClick={() => void selectConversation(conversation.id)} className={`min-w-56 rounded-xl p-3 text-left transition lg:min-w-0 lg:w-full ${currentId === conversation.id ? 'bg-white/12 text-white' : 'text-blue-100/65 hover:bg-white/7'}`}><p className="truncate text-xs font-extrabold">{conversation.title}</p><p className="mt-1 text-[10px] opacity-55">{shortTime(conversation.lastActivityAt)}</p></button>)}
            </div>
            <div className="mt-6 hidden rounded-2xl border border-teal-300/15 bg-teal-300/8 p-4 lg:block"><ShieldCheck className="size-5 text-teal-300" /><p className="mt-3 text-xs font-extrabold">Private by default</p><p className="mt-1 text-[11px] leading-5 text-blue-100/55">{status?.externalDataSharing === false ? 'No record text leaves this local grounded mode.' : 'Patient authorization applies before retrieval.'}</p></div>
          </aside>

          <main className="flex min-w-0 flex-col">
            <div className="border-b border-slate-100 px-5 py-4 sm:px-6"><div className="flex items-center justify-between gap-4"><div className="flex items-center gap-3"><span className="grid size-10 place-items-center rounded-xl bg-care-50 text-care-700"><BrainCircuit className="size-5" /></span><div><h2 className="font-black text-ink-950">SmartCare Care Assistant</h2><p className="text-[11px] font-bold text-emerald-700">Grounded mode · citations on</p></div></div><Link to="/records" className="hidden items-center gap-2 text-xs font-black text-care-700 hover:underline sm:flex">Open health record <ChevronRight className="size-4" /></Link></div></div>

            <div className="flex-1 space-y-5 overflow-y-auto p-5 sm:p-6 lg:max-h-[650px]">
              {loading ? <div className="grid min-h-72 place-items-center"><span className="flex items-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin text-care-600" />Opening your private assistant…</span></div> : messages.length === 0 ? <div className="mx-auto max-w-2xl py-8 text-center"><span className="mx-auto grid size-16 place-items-center rounded-3xl bg-care-50 text-care-700"><Sparkles className="size-8" /></span><h2 className="mt-5 text-2xl font-black text-ink-950">Start with a question about your care</h2><p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-slate-500">The assistant can locate existing facts. It cannot create a diagnosis, prescription or emergency clearance.</p><div className="mt-7 grid gap-3 text-left sm:grid-cols-2">{prompts.map((prompt) => <button key={prompt} onClick={() => void send(undefined, prompt)} className="group rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm font-bold leading-5 text-slate-700 transition hover:border-care-300 hover:bg-care-50"><span>{prompt}</span><ArrowRight className="mt-3 size-4 text-care-600 transition group-hover:translate-x-1" /></button>)}</div></div> : messages.map((message) => <article key={message.id} className={`flex gap-3 ${message.role === 'USER' ? 'justify-end' : 'justify-start'}`}>
                {message.role === 'ASSISTANT' && <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-care-600 text-white"><Bot className="size-5" /></span>}
                <div className={`max-w-3xl ${message.role === 'USER' ? 'rounded-2xl rounded-tr-sm bg-ink-950 px-4 py-3 text-white' : `rounded-2xl rounded-tl-sm border p-4 text-slate-700 ${safetyStyle[message.safetyClass]}`}`}>
                  <p className="whitespace-pre-line text-sm leading-6">{message.content}</p>
                  {message.citations.length > 0 && <div className="mt-4 space-y-2 border-t border-slate-200/70 pt-3"><p className="text-[10px] font-black uppercase tracking-[.15em] text-slate-400">Authorized sources</p>{message.citations.map((citation) => <Link key={citation.chunkId} to={citation.sourcePath} className="block rounded-xl border border-slate-200 bg-white/80 p-3 transition hover:border-care-300"><div className="flex items-start gap-3"><span className="grid size-6 shrink-0 place-items-center rounded-lg bg-care-600 text-[10px] font-black text-white">{citation.number}</span><div className="min-w-0"><p className="truncate text-xs font-black text-ink-950">{citation.sourceLabel}</p><p className="mt-1 text-[10px] font-bold text-care-700">{citation.provenance}{citation.pageNumber ? ` · Page ${citation.pageNumber}` : ''}</p><p className="mt-1 line-clamp-2 text-[11px] leading-4 text-slate-500">{citation.excerpt}</p></div></div></Link>)}</div>}
                </div>
                {message.role === 'USER' && <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-slate-200 text-slate-600"><UserRound className="size-5" /></span>}
              </article>)}
              {sending && <div className="flex items-center gap-3"><span className="grid size-9 place-items-center rounded-xl bg-care-600 text-white"><Bot className="size-5" /></span><span className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-xs font-bold text-slate-500"><LoaderCircle className="size-4 animate-spin" />Filtering and checking sources…</span></div>}
              <div ref={endRef} />
            </div>

            <form onSubmit={(event) => void send(event)} className="border-t border-slate-200 bg-slate-50 p-4 sm:p-5"><div className="flex gap-2"><label className="sr-only" htmlFor="care-question">Ask about your care record</label><textarea id="care-question" rows={2} maxLength={600} value={question} onChange={(event) => setQuestion(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); void send() } }} placeholder="Ask about a report, medicine, allergy or follow-up…" className="min-h-14 flex-1 resize-none rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm leading-5 outline-none focus:border-care-500 focus:ring-4 focus:ring-care-100" /><button disabled={question.trim().length < 3 || sending} className="grid size-14 shrink-0 place-items-center rounded-2xl bg-care-600 text-white shadow-md shadow-blue-700/15 disabled:bg-slate-300" aria-label="Send question">{sending ? <LoaderCircle className="size-5 animate-spin" /> : <Send className="size-5" />}</button></div><div className="mt-3 flex flex-col justify-between gap-2 text-[10px] text-slate-500 sm:flex-row"><p className="flex items-center gap-1.5"><ShieldCheck className="size-3.5 text-emerald-600" />Patient authorization happens before retrieval.</p><p className="flex items-center gap-1.5"><Stethoscope className="size-3.5 text-violet-600" />Medical decisions remain with qualified staff.</p></div></form>
          </main>
        </div>

        <div className="mt-5 flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs leading-5 text-amber-950"><AlertTriangle className="mt-0.5 size-4 shrink-0" /><p><strong>Emergency:</strong> contact your hospital emergency desk or local emergency services. Do not wait for an assistant response. Text PDFs are supported; scanned images need approved OCR before their contents can be summarized.</p></div>
      </div>
    </div>
  )
}
