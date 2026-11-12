package com.doors.crawler.usage;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class RequestLogController {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final RequestLogRepository requests;

    RequestLogController(RequestLogRepository requests) { this.requests = requests; }

    @GetMapping("/requests")
    public String page(Model model) {
        model.addAttribute("rows", rows());
        return "requests";
    }

    /** htmx polling target: table fragment only. */
    @GetMapping("/requests/data")
    public String data(Model model) {
        model.addAttribute("rows", rows());
        return "requests :: view";
    }

    private List<RequestRow> rows() {
        return requests.findTop100ByOrderByRequestedAtDesc().stream()
                .map(r -> new RequestRow(
                        FMT.format(r.getRequestedAt()),
                        r.getPreset() != null ? r.getPreset()
                                : r.getFilter() != null ? r.getFilter() : "—",
                        r.getResultCount(), r.getDurationMs(), r.getEndpoint(), r.getBrowser()))
                .toList();
    }

    record RequestRow(String time, String presetOrFilter, Integer resultCount,
                      Long durationMs, String endpoint, String browser) {}
}
