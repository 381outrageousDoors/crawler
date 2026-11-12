package com.doors.crawler.usage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsageServiceTest {

    @Test
    void edgeUaIsEdgeNotChromeOrSafari() {
        assertThat(UsageService.browserFrom("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.2903.86")).isEqualTo("Edge");
    }

    @Test
    void chromeUaIsChromeNotSafari() {
        assertThat(UsageService.browserFrom("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")).isEqualTo("Chrome");
    }

    @Test
    void firefoxUa() {
        assertThat(UsageService.browserFrom("Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0"))
                .isEqualTo("Firefox");
    }

    @Test
    void macosSafariUa() {
        assertThat(UsageService.browserFrom("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15")).isEqualTo("Safari");
    }

    @Test
    void operaUaIsOperaNotChrome() {
        assertThat(UsageService.browserFrom("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 OPR/116.0.0.0")).isEqualTo("Opera");
    }

    @Test
    void curlIsOtherAndMissingUaIsUnknown() {
        assertThat(UsageService.browserFrom("curl/8.0")).isEqualTo("Other");
        assertThat(UsageService.browserFrom(null)).isEqualTo("Unknown");
        assertThat(UsageService.browserFrom("   ")).isEqualTo("Unknown");
    }
}
