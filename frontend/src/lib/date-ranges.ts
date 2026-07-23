export const validateHistoricalRange = (fromValue: string, toValue: string, now: number) => {
  const from = fromValue ? new Date(fromValue) : null
  const to = toValue ? new Date(toValue) : null
  if ((from && from.getTime() > now) || (to && to.getTime() > now)) return 'Audit dates cannot be in the future.'
  if (from && to && from > to) return 'From must be earlier than or equal to To.'
  return null
}
