package com.namejm.stream_guppy.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingController {

    @Value("${stream_guppy.frontUrl}")
    private String frontUrl;

    @GetMapping("/settings")
    public String settingHome(Model model) {
        model.addAttribute("frontUrl", frontUrl);
        return "settings";
    }

}
