import unittest
from io import BytesIO

from PIL import Image

from app import (
    WaitTimeRequest,
    health,
    interpret_abo_panel,
    predict_agglutination,
    predict_wait,
)


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

    def test_blood_reaction_inference_returns_probability_and_safety_boundary(self):
        image = Image.new("RGB", (128, 128), color=(150, 10, 10))
        buffer = BytesIO()
        image.save(buffer, format="PNG")

        response = predict_agglutination(buffer.getvalue())

        self.assertGreaterEqual(response.agglutinationProbability, 0)
        self.assertLessEqual(response.agglutinationProbability, 1)
        self.assertGreaterEqual(response.confidence, 0.5)
        self.assertIn("qualified laboratory", response.safetyNotice.lower())

    def test_rule_engine_interprets_three_clear_reactions(self):
        panel = interpret_abo_panel(
            self.reaction(True),
            self.reaction(False),
            self.reaction(True),
        )

        self.assertEqual("A+", panel.bloodGroup)
        self.assertIn("clinician verification", panel.interpretationStatus)

    def test_rule_engine_rejects_low_confidence_reaction(self):
        panel = interpret_abo_panel(
            self.reaction(True),
            self.reaction(False, manual_review=True),
            self.reaction(True),
        )

        self.assertIsNone(panel.bloodGroup)
        self.assertIn("Manual verification", panel.interpretationStatus)

    @staticmethod
    def reaction(detected, manual_review=False):
        from app import AgglutinationResponse

        probability = 0.99 if detected else 0.01
        return AgglutinationResponse(
            agglutinationDetected=detected,
            agglutinationProbability=probability,
            confidence=0.54 if manual_review else 0.99,
            manualReviewRequired=manual_review,
            modelName="test model",
            modelVersion="test",
            safetyNotice="test safety notice",
        )


if __name__ == "__main__":
    unittest.main()
