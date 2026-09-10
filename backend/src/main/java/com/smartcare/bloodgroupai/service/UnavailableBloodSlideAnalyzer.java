package com.smartcare.bloodgroupai.service;

import com.smartcare.bloodgroupai.port.BloodSlideAnalyzer;
import org.springframework.stereotype.Component;

@Component
public class UnavailableBloodSlideAnalyzer implements BloodSlideAnalyzer {
    @Override
    public AnalysisAttempt analyze(byte[] image, String contentType) {
        return AnalysisAttempt.notConfigured();
    }
}
