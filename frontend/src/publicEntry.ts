export type PublicEntry = 'product' | 'redirect-home'

export function selectPublicEntry(pathname: string): PublicEntry {
  const path = pathname.replace(/\/+$/, '') || '/'
  if (path === '/explore' || path.startsWith('/explore/')) return 'redirect-home'
  return 'product'
}
