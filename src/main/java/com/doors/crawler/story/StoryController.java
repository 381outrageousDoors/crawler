package com.doors.crawler.story;

import com.doors.crawler.scan.ScanService;
import com.doors.crawler.usage.UsageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class StoryController {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final ScanService scanService;
    private final StoryRepository stories;
    private final UsageService usage;

    StoryController(ScanService scanService, StoryRepository stories, UsageService usage) {
        this.scanService = scanService;
        this.stories = stories;
        this.usage = usage;
    }

    @GetMapping("/")
    public String index(@RequestHeader(name = "User-Agent", required = false) String userAgent,
                        Model model) {
        populate(FilterSpec.fromParams(null, null, null, null, null, null), "index", null, userAgent, model);
        return "index";
    }

    /** htmx target: run a debounced scan, then swap in the fresh view (default ordering). */
    @PostMapping("/scan")
    public String scan(@RequestHeader(name = "User-Agent", required = false) String userAgent,
                       Model model) {
        scanService.requestScan();
        populate(FilterSpec.fromParams(null, null, null, null, null, null), "scan", null, userAgent, model);
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
            @RequestHeader(name = "User-Agent", required = false) String userAgent,
            Model model) {
        populate(FilterSpec.fromParams(field, op, value, sort, dir, preset), "stories", preset, userAgent, model);
        return "fragment :: view";
    }

    private void populate(FilterSpec spec, String endpoint, String preset, String userAgent, Model model) {
        long t0 = System.nanoTime();
        List<Story> filtered = spec.apply(stories.findAll());
        model.addAttribute("stories", filtered);
        model.addAttribute("field", spec.fieldParam());
        model.addAttribute("op", spec.opParam());
        model.addAttribute("value", spec.value());
        model.addAttribute("sort", spec.sortParam());
        model.addAttribute("dir", spec.desc() ? "desc" : "asc");
        model.addAttribute("storiesUrl", spec.pollUrl());
        model.addAttribute("statusText", statusText(spec));
        usage.record(endpoint, spec, preset, userAgent, filtered.size(), (System.nanoTime() - t0) / 1_000_000);
    }

    private String statusText(FilterSpec spec) {
        Instant last = scanService.lastScanAt();
        String base = last == null ? "Not scanned yet" : "Last scanned: " + FMT.format(last) + " UTC";
        String desc = spec.describe();
        return desc == null ? base : base + " · " + desc;
    }
}
