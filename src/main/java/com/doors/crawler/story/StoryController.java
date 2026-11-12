package com.doors.crawler.story;

import com.doors.crawler.scan.ScanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Controller
public class StoryController {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final ScanService scanService;
    private final StoryRepository stories;

    StoryController(ScanService scanService, StoryRepository stories) {
        this.scanService = scanService;
        this.stories = stories;
    }

    @GetMapping("/")
    public String index(Model model) {
        populate(FilterSpec.fromParams(null, null, null, null, null, null), model);
        return "index";
    }

    /** htmx target: run a debounced scan, then swap in the fresh view (default ordering). */
    @PostMapping("/scan")
    public String scan(Model model) {
        scanService.requestScan();
        populate(FilterSpec.fromParams(null, null, null, null, null, null), model);
        return "fragment :: view";
    }

    /** htmx polling target: whole view (controls + status + table) for the current params. */
    @GetMapping("/stories")
    public String stories(
            @RequestParam(name = "field", defaultValue = "none") String field,
            @RequestParam(name = "op", defaultValue = "gt") String op,
            @RequestParam(name = "value", defaultValue = "5") String value,
            @RequestParam(name = "sort", defaultValue = "rank") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir,
            @RequestParam(name = "preset", required = false) String preset,
            Model model) {
        populate(FilterSpec.fromParams(field, op, value, sort, dir, preset), model);
        return "fragment :: view";
    }

    private void populate(FilterSpec spec, Model model) {
        model.addAttribute("stories", spec.apply(stories.findAll()));
        model.addAttribute("field", spec.fieldParam());
        model.addAttribute("op", spec.opParam());
        model.addAttribute("value", spec.value());
        model.addAttribute("sort", spec.sortParam());
        model.addAttribute("dir", spec.desc() ? "desc" : "asc");
        model.addAttribute("storiesUrl", spec.pollUrl());
        model.addAttribute("statusText", statusText(spec));
    }

    private String statusText(FilterSpec spec) {
        Instant last = scanService.lastScanAt();
        String base = last == null ? "Not scanned yet" : "Last scanned: " + FMT.format(last) + " UTC";
        String desc = spec.describe();
        return desc == null ? base : base + " · " + desc;
    }
}
