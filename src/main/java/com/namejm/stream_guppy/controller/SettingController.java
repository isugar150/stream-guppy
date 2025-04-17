package com.namejm.stream_guppy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SettingController {

    @GetMapping("/")
    public String settingHome(Model model) {
        model.addAttribute("message", "aaaaa");
        return "settings";
    }

}
