import { describe, expect, it } from 'vitest'
import { recommendDepartment } from './departmentRecommendation'

describe('recommendDepartment', () => {
  it('explains a specialist department match', () => {
    expect(recommendDepartment('I have knee pain and joint stiffness')).toEqual(expect.objectContaining({
      department: 'Orthopaedics', matchedKeywords: ['joint', 'knee pain'], emergency: false,
    }))
  })

  it('falls back to General Medicine instead of inventing a diagnosis', () => {
    expect(recommendDepartment('I am not feeling well')).toEqual(expect.objectContaining({
      department: 'General Medicine', matchedKeywords: [],
    }))
  })

  it('flags emergency language for immediate escalation', () => {
    expect(recommendDepartment('severe chest pain and difficulty breathing')).toEqual(expect.objectContaining({
      department: 'Cardiology', emergency: true,
    }))
  })
})
