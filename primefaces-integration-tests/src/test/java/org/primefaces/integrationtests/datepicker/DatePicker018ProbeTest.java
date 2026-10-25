/*
 * The MIT License
 *
 * Copyright (c) 2009-2026 PrimeFaces
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.primefaces.integrationtests.datepicker;

import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.DatePicker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

// fork-only probe, not for upstream
class DatePicker018ProbeTest extends AbstractPrimePageTest {

    private static final String HOOK = "var i=PrimeFaces.getWidgetById('form:datepicker').jq.data('primeDatePicker');"
            + "window.__entered=null;window.__clickAt=null;window.__dpc=null;"
            + "var o=i.bindDocumentClickListener;"
            + "i.bindDocumentClickListener=function(){window.__entered=performance.now();return o.apply(this,arguments);};"
            + "document.addEventListener('click',function(e){if(e.target.id==='outside'){"
            + "window.__clickAt=performance.now();window.__dpc=i.datepickerClick;}},true);";

    @Test
    void probe() {
        int n = Integer.getInteger("probe.n", 40);
        for (String arm : new String[] {"base", "fixed"}) {
            int fail = 0;
            int dpcTrue = 0;
            int dpcTrueFail = 0;
            int dpcFalseFail = 0;
            List<String> rows = new ArrayList<>();
            int errors = 0;
            for (int r = 0; r < n; r++) {
                String stage = "goTo";
                try {
                DatePicker018Test.Page page = PrimeSelenium.goTo(DatePicker018Test.Page.class);
                PrimeSelenium.executeScript(HOOK);
                DatePicker datePicker = page.datepicker;
                // same WebDriver call sequence as DatePicker018Test.timeInputCommit
                stage = "showPanel";
                WebElement panel = datePicker.showPanel();
                WebElement minuteInput = panel.findElement(By.cssSelector(".ui-minute-picker input"));
                minuteInput.getAttribute("value");
                page.dateSelectCount.getText();
                stage = "type";
                minuteInput.sendKeys(Keys.CONTROL, "a", Keys.BACK_SPACE);
                minuteInput.sendKeys("05");
                stage = "count0";
                page.dateSelectCount.getText();
                stage = "guardWait";
                if ("fixed".equals(arm)) {
                    PrimeSelenium.waitGui().until(d -> Boolean.FALSE.equals(PrimeSelenium.executeScript(
                            "return " + datePicker.getWidgetByIdScript() + ".jq.data('primeDatePicker').datepickerClick;")));
                }
                stage = "click";
                PrimeSelenium.executeScript("return 1;"); // stands in for the guardAjax script round-trip
                page.outside.click();
                boolean committed;
                try {
                    new WebDriverWait(PrimeSelenium.getWebDriver(), Duration.ofSeconds(3))
                            .until(d -> "1".equals(d.findElement(By.id("form:dateSelectCount")).getText()));
                    committed = true;
                }
                catch (TimeoutException e) {
                    committed = false;
                }
                Object dpc = PrimeSelenium.executeScript("return window.__dpc;");
                Object dt = PrimeSelenium.executeScript(
                        "return window.__entered==null||window.__clickAt==null?null:Math.round(window.__clickAt-window.__entered);");
                boolean t = Boolean.TRUE.equals(dpc);
                if (!committed) {
                    fail++;
                }
                if (t) {
                    dpcTrue++;
                    if (!committed) {
                        dpcTrueFail++;
                    }
                }
                else if (!committed) {
                    dpcFalseFail++;
                }
                rows.add(dt + (t ? "T" : "F") + (committed ? "+" : "-"));
                }
                catch (RuntimeException e) {
                    errors++;
                    StackTraceElement at = null;
                    for (StackTraceElement el : e.getStackTrace()) {
                        if (el.getClassName().startsWith("org.primefaces")) {
                            at = el;
                            break;
                        }
                    }
                    System.out.println("PROBE err arm=" + arm + " r=" + r + " stage=" + stage + " " + e.getClass().getSimpleName() + " at " + at);
                }
            }
            System.out.println("PROBE arm=" + arm + " n=" + n + " errors=" + errors + " fail=" + fail + " dpcTrue=" + dpcTrue
                    + " dpcTrueFail=" + dpcTrueFail + " dpcFalseFail=" + dpcFalseFail + " rows(dtMs,dpc,committed)=" + rows);
        }
    }
}
