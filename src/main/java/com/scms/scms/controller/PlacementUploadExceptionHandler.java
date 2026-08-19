package com.scms.scms.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

@ControllerAdvice
public class PlacementUploadExceptionHandler {

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public String handlePlacementUploadFailure(Exception ex, HttpServletRequest request) {
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        String referer = request.getHeader("Referer");
        String fallbackTarget = (referer != null && !referer.isBlank()) ? referer : "/";
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        if (uri.startsWith("/placements/apply/")) {
            flashMap.put("placementError", "Resume upload failed. Keep the file within 10 MB and use PDF, DOC, or DOCX.");
            return "redirect:/placements";
        }
        flashMap.put("uploadError", "Upload failed. Please try again with a smaller file.");
        return "redirect:" + fallbackTarget;
    }
}
