package com.scms.scms.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExamAutomationScheduler {

    private final ExamAutomationService examAutomationService;

    public ExamAutomationScheduler(ExamAutomationService examAutomationService) {
        this.examAutomationService = examAutomationService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void finalizeOverdueExams() {
        examAutomationService.finalizeOverdueExams();
    }
}
