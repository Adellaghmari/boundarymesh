const portfolioDateFormat = new Intl.DateTimeFormat('en-GB', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

export function formatPortfolioDate(now: Date): string {
  return portfolioDateFormat.format(now)
}

export function millisecondsUntilNextLocalDate(now: Date): number {
  const next = new Date(now.getTime())
  next.setHours(24, 0, 0, 0)
  const delay = next.getTime() - now.getTime()
  return delay > 0 ? delay : 24 * 60 * 60 * 1000
}
