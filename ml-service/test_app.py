import unittest

from app import WaitTimeRequest, health, predict_wait


def request(**changes):
    values = {
        "department": "Cardiology",
        "triageCategory": "Non-urgent",
        "patientsAhead": 5,
        "activeDoctors": 2,
        "averageConsultationMinutes": 12,
        "currentDoctorDelayMinutes": 10,
        "occupancyRate": 0.75,
    }
    values.update(changes)
    return WaitTimeRequest(**values)


class WaitTimeServiceTest(unittest.TestCase):
    def test_health_reports_loaded_model(self):
        response = health()

        self.assertEqual("ok", response["status"])
        self.assertEqual("1.0.0", response["version"])
        self.assertEqual(
            "prototype-not-clinically-validated",
            response["modelStatus"],
        )

    def test_known_department_returns_versioned_estimate(self):
        response = predict_wait(request())

        self.assertTrue(response.departmentSupported)
        self.assertEqual("department-specific", response.baseEstimateSource)
        self.assertEqual("1.0.0", response.modelVersion)
        self.assertGreater(response.estimatedWaitMinutes, 0)
        self.assertLessEqual(
            response.estimatedRangeMinutes.minimum,
            response.estimatedWaitMinutes,
        )
        self.assertGreaterEqual(
            response.estimatedRangeMinutes.maximum,
            response.estimatedWaitMinutes,
        )

    def test_unknown_department_uses_explicit_generic_fallback(self):
        response = predict_wait(request(department="ENT"))

        self.assertFalse(response.departmentSupported)
        self.assertEqual(
            "generic-department-average",
            response.baseEstimateSource,
        )

    def test_high_load_never_estimates_less_than_low_load(self):
        low_load = predict_wait(
            request(
                patientsAhead=3,
                activeDoctors=5,
                currentDoctorDelayMinutes=0,
                occupancyRate=0.30,
            )
        )
        high_load = predict_wait(
            request(
                patientsAhead=15,
                activeDoctors=2,
                currentDoctorDelayMinutes=15,
                occupancyRate=0.90,
            )
        )

        self.assertGreater(
            high_load.estimatedWaitMinutes,
            low_load.estimatedWaitMinutes,
        )

    def test_immediate_cases_receive_escalation_notice(self):
        response = predict_wait(request(triageCategory="Immediate"))

        self.assertIsNotNone(response.urgentNotice)
        self.assertIn("immediately", response.urgentNotice.lower())


if __name__ == "__main__":
    unittest.main()
