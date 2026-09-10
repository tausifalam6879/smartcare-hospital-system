export function BrandMark({ compact = false, inverted = false }: { compact?: boolean; inverted?: boolean }) {
  return (
    <div className="flex items-center gap-3" aria-label="SmartCare">
      <span className="relative grid size-11 place-items-center overflow-hidden rounded-[.9rem] bg-ink-950 text-white shadow-md shadow-blue-950/20" aria-hidden="true">
        <span className="absolute -right-2 -top-2 size-7 rounded-full bg-cyan-400/35" />
        <svg viewBox="0 0 28 28" className="relative size-7" fill="none">
          <path d="M7 7h5.5a4.5 4.5 0 0 1 0 9H10a3 3 0 0 0-3 3v2" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" />
          <circle cx="7" cy="7" r="2.25" fill="#2dd4bf" />
          <path d="M20.5 5.5v6M17.5 8.5h6" stroke="#60a5fa" strokeWidth="2.4" strokeLinecap="round" />
          <path d="m5 21 2 2 2-2" stroke="#2dd4bf" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </span>
      {!compact && (
        <span className="leading-none">
          <span className={`block text-xl font-black tracking-[-0.045em] ${inverted ? 'text-white' : 'text-ink-950'}`}>Smart<span className="text-cyan-500">Care</span></span>
          <span className={`mt-1.5 block text-[8px] font-extrabold uppercase tracking-[0.16em] ${inverted ? 'text-blue-300' : 'text-care-700'}`}>OPD · Navigate · Queue · Emergency</span>
        </span>
      )}
    </div>
  )
}
