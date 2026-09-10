package com.smartcare.bloodgroupai.port;

import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodgroupai.domain.ModelInferenceStatus;

import java.math.BigDecimal;

public interface BloodSlideAnalyzer {
    AnalysisAttempt analyze(byte[] image, String contentType);

    record AnalysisAttempt(ModelInferenceStatus status, BloodGroup suggestedGroup, BigDecimal confidence) {
        public static AnalysisAttempt notConfigured() {
            return new AnalysisAttempt(ModelInferenceStatus.NOT_CONFIGURED, null, null);
        }
    }
}
