package com.doors.crawler.story;

import com.doors.crawler.scan.ScanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class StoryController {

    private final ScanService scanService;
    private final StoryRepository stories;

    StoryController(ScanService scanService, StoryRepository stories) {
        this.scanService = scanService;
        this.stories = stories;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stories", stories.findAllByOrderByRankAsc());
        return "index";
    }

    /** htmx target: run a debounced scan, then swap in the fresh rows. */
    @PostMapping("/scan")
    public String scan(Model model) {
        scanService.requestScan();
        model.addAttribute("stories", stories.findAllByOrderByRankAsc());
        return "fragment :: stories";
    }

    /** htmx polling target: current front page as bare rows. */
    @GetMapping("/stories")
    public String stories(Model model) {
        model.addAttribute("stories", stories.findAllByOrderByRankAsc());
        return "fragment :: stories";
    }
}
