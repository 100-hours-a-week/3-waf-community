package com.ktb.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * View Controller
 * Thymeleaf 템플릿 페이지 라우팅
 */
@Controller
public class ViewController {

    /**
     * 이용약관 페이지
     *
     * @return terms.html 템플릿
     */
    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    /**
     * 개인정보처리방침 페이지
     *
     * @return privacy.html 템플릿
     */
    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
}
