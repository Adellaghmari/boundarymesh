export type PublicEntry = 'landing' | 'explore' | 'missing'

export function selectPublicEntry(pathname: string): PublicEntry {
  const path = pathname.replace(/\/+$/, '') || '/'
  if (path === '/') return 'landing'
  if (path === '/explore' || path.startsWith('/explore/')) return 'explore'
  return 'missing'
}
