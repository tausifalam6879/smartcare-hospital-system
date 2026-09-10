package com.smartcare.ai.service;

import com.smartcare.ai.domain.AiSafetyClass;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class MedicalAiSafetyPolicy {
    private static final Set<String> EMERGENCY_SIGNALS = Set.of(
            "chest pain", "difficulty breathing", "cannot breathe", "severe bleeding", "unconscious",
            "not waking", "stroke", "face drooping", "suicide", "kill myself", "overdose", "seizure"
    );
    private static final Set<String> CLINICAL_REQUESTS = Set.of(
            "diagnose me", "what disease do i have", "what should i take", "prescribe", "change my medicine",
            "change my dose", "stop taking", "should i stop", "is it safe to wait", "can i ignore"
    );

    public PolicyResult evaluate(String question) {
        String normalized = question.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (EMERGENCY_SIGNALS.stream().anyMatch(normalized::contains)) {
            return new PolicyResult(AiSafetyClass.EMERGENCY_ESCALATION,
                    "This may need urgent medical attention. Contact your hospital emergency desk or local emergency services now. Do not use this assistant to decide whether it is safe to wait. If possible, stay with the patient and follow qualified emergency staff instructions.");
        }
        if (CLINICAL_REQUESTS.stream().anyMatch(normalized::contains)) {
            return new PolicyResult(AiSafetyClass.CLINICAL_BOUNDARY,
                    "I can locate and summarize existing SmartCare records, but I cannot diagnose, prescribe, change a dose, or tell you to stop treatment. Please ask your qualified doctor or care team for that decision.");
        }
        return new PolicyResult(AiSafetyClass.NORMAL, null);
    }

    public record PolicyResult(AiSafetyClass safetyClass, String response) {
    }
}
