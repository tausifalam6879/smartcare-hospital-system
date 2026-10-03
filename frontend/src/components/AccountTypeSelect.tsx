import { useLanguage } from '../context/LanguageContext'

export const accountTypes = [
  ['PATIENT', 'Patient / User', 'मरीज़ / यूज़र'], ['DOCTOR', 'Doctor', 'डॉक्टर'],
  ['HOSPITAL_ADMIN', 'Hospital admin', 'अस्पताल एडमिन'], ['OFFICE_CLERK', 'Office clerk (desk & cash)', 'ऑफिस क्लर्क (डेस्क और कैश)'],
  ['RECEPTIONIST', 'Receptionist', 'रिसेप्शनिस्ट'], ['CASHIER', 'Cashier', 'कैशियर'],
  ['LAB_TECHNICIAN', 'Lab technician', 'लैब कर्मचारी'], ['BLOOD_BANK_STAFF', 'Blood bank staff', 'ब्लड बैंक कर्मचारी'],
  ['AMBULANCE_DISPATCHER', 'Ambulance dispatcher', 'एम्बुलेंस डिस्पैचर'],
] as const

export function AccountTypeSelect({ value, onChange, login = false, staffOnly = false }: { value: string; onChange: (value: string) => void; login?: boolean; staffOnly?: boolean }) {
  const { text } = useLanguage()
  return <label className="mb-4 block text-sm font-bold text-ink-950">{text('Account / workspace', 'अकाउंट / कार्यक्षेत्र')}
    <select value={value} onChange={event => onChange(event.target.value)} className="mt-2 h-13 w-full rounded-2xl border border-blue-200 bg-blue-50 px-4">
      {login && <option value="AUTO">{text('Detect my authorized workspace', 'मेरा अधिकृत कार्यक्षेत्र पहचानें')}</option>}
      {accountTypes.filter(([id]) => !staffOnly || id !== 'PATIENT').map(([id, en, hi]) => <option key={id} value={id}>{text(en, hi)}</option>)}
      {login && <option value="SUPER_ADMIN">{text('System administrator', 'सिस्टम एडमिन')}</option>}
    </select>
  </label>
}
