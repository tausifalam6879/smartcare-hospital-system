import { describe, expect, it } from 'vitest'
import { staticDemoRoute } from './staticNavigationDemo'

describe('staticNavigationDemo', () => {
  it('builds the lowest-cost step-free route to the cardiology OPD', () => {
    const route = staticDemoRoute('RMQ-DEMO-ENTRANCE', 'OPD_307', 'en', true)

    expect(route.available).toBe(true)
    expect(route.pathCodes).toEqual([
      'MAIN_ENTRANCE', 'MAIN_REGISTRATION', 'LIFT_B_G',
      'LIFT_B_3', 'CARDIO_RECEPTION', 'OPD_307',
    ])
    expect(route.totalDistanceMeters).toBe(142)
    expect(route.steps).toHaveLength(5)
    expect(route.steps.every((step) => step.stepFree)).toBe(true)
  })

  it('returns hospital-approved-style Hindi demo instructions', () => {
    const route = staticDemoRoute('RMQ-DEMO-ENTRANCE', 'OPD_214', 'hi', true)

    expect(route.available).toBe(true)
    expect(route.message).toContain('सबसे कम लागत')
    expect(route.steps[0].instruction).toContain('पंजीकरण')
  })
})
