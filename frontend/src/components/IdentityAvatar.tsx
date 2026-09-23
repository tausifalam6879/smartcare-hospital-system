import { UserRound } from 'lucide-react'

type VerifiedGender = 'MALE' | 'FEMALE' | 'OTHER' | 'UNDISCLOSED'

type IdentityAvatarProps = {
  name?: string
  imageUrl?: string
  gender?: VerifiedGender
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const sizes = {
  sm: 'size-9 text-xs',
  md: 'size-12 text-sm',
  lg: 'size-16 text-lg',
}

const genderTones: Record<VerifiedGender, string> = {
  MALE: 'from-blue-100 to-indigo-100 text-blue-800',
  FEMALE: 'from-violet-100 to-fuchsia-100 text-violet-800',
  OTHER: 'from-cyan-100 to-violet-100 text-indigo-800',
  UNDISCLOSED: 'from-slate-100 to-blue-100 text-slate-700',
}

function initials(name?: string) {
  const clean = name?.replace(/^(Dr|Mr|Mrs|Ms)\.\s*/i, '').trim()
  if (!clean) return ''
  return clean.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()
}

/** Uses gender styling only when a verified value is supplied; names are never used to infer gender. */
export function IdentityAvatar({ name, imageUrl, gender = 'UNDISCLOSED', size = 'md', className = '' }: IdentityAvatarProps) {
  const fallback = initials(name)
  const shared = `${sizes[size]} shrink-0 overflow-hidden rounded-full ring-2 ring-white shadow-sm ${className}`
  if (imageUrl) return <img src={imageUrl} alt={name ? `${name} profile` : 'Profile'} className={`${shared} object-cover`} />
  return (
    <span aria-label={name ? `${name} avatar` : 'Private profile avatar'} className={`${shared} grid place-items-center bg-gradient-to-br font-black ${genderTones[gender]}`}>
      {fallback || <UserRound className="size-1/2" />}
    </span>
  )
}
