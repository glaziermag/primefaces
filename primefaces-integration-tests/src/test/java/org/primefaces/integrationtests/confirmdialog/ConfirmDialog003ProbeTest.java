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
package org.primefaces.integrationtests.confirmdialog;

import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;

import java.time.Duration;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// fork-only probe, not for upstream
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfirmDialog003ProbeTest extends AbstractPrimePageTest {

    private static final String SPY = "window.__pfl=[];"
            + "var L=function(s){window.__pfl.push(Math.round(performance.now())+' '+s)};"
            + "document.addEventListener('focusin',function(e){L('focus '+(e.target.id||e.target.tagName))},true);"
            + "document.addEventListener('click',function(e){L('click '+(e.target.id||e.target.className||e.target.tagName))},true);"
            + "document.addEventListener('keydown',function(e){L('keydown '+e.key+'@'+(e.target.id||e.target.tagName))},true);"
            + "document.addEventListener('change',function(e){L('change '+(e.target.id||e.target.tagName))},true);";

    @Test
    @Order(1)
    void mechanism() throws Exception {
        for (boolean popup : new boolean[] {false, true}) {
            goTo("confirmdialog/confirmDialog003.xhtml?popup=" + popup);
            js().executeScript(SPY);
            element("form:select:0").sendKeys(Keys.ARROW_RIGHT);
            answer(popup, false);
            String rightAfter = activeDesc();
            Thread.sleep(700);
            String later = activeDesc();
            // a keyboard user presses the arrow key wherever focus is now
            new Actions(driver()).sendKeys(Keys.ARROW_RIGHT).perform();
            boolean shown = shown(popup, 2);
            System.out.println("PROBE mechanism popup=" + popup + " activeRightAfterNo=" + rightAfter + " activeAfter700ms=" + later
                    + " arrowAtFocusOpensConfirm=" + shown + " values=" + element("form:values").getText()
                    + " checked0=" + element("form:select:0").isSelected()
                    + " log=" + js().executeScript("return window.__pfl.join(' | ')"));
        }
    }

    @Test
    @Order(2)
    void realistic() throws Exception {
        for (boolean popup : new boolean[] {false, true}) {
            for (String arm : new String[] {"base", "fixed"}) {
                int fail = 0;
                int n = 60;
                for (int r = 0; r < n; r++) {
                    goTo("confirmdialog/confirmDialog003.xhtml?popup=" + popup);
                    element("form:select:0").sendKeys(Keys.ARROW_RIGHT);
                    answer(popup, false);
                    element("form:select:0").isSelected();
                    element("form:select:1").isSelected();
                    element("form:values").getText();
                    if ("fixed".equals(arm)) {
                        awaitFocusReturn();
                    }
                    element("form:select:0").sendKeys(Keys.ARROW_RIGHT);
                    if (!shown(popup, 5)) {
                        fail++;
                    }
                }
                System.out.println("PROBE realistic popup=" + popup + " arm=" + arm + " failures=" + fail + "/" + n);
            }
        }
    }

    @Test
    @Order(3)
    void sweep() throws Exception {
        for (boolean popup : new boolean[] {false, true}) {
            for (String arm : new String[] {"base", "fixed"}) {
                StringBuilder at = new StringBuilder();
                int fail = 0;
                int n = 0;
                for (int d = 0; d <= 240; d += 4) {
                    int f = 0;
                    for (int r = 0; r < 2; r++) {
                        n++;
                        goTo("confirmdialog/confirmDialog003.xhtml?popup=" + popup);
                        element("form:select:0").sendKeys(Keys.ARROW_RIGHT);
                        answer(popup, false);
                        if (d > 0) {
                            Thread.sleep(d);
                        }
                        if ("fixed".equals(arm)) {
                            awaitFocusReturn();
                        }
                        element("form:select:0").sendKeys(Keys.ARROW_RIGHT);
                        if (!shown(popup, 2)) {
                            f++;
                        }
                    }
                    fail += f;
                    if (f > 0) {
                        at.append(d).append("ms:").append(f).append(' ');
                    }
                }
                System.out.println("PROBE sweep popup=" + popup + " arm=" + arm + " failures=" + fail + "/" + n + " delays=" + at);
            }
        }
    }

    private void awaitFocusReturn() {
        new WebDriverWait(driver(), Duration.ofSeconds(5))
                .until(d -> "form:select".equals(d.switchTo().activeElement().getDomAttribute("name")));
    }

    private boolean shown(boolean popup, int secs) {
        String prefix = popup ? "ui-confirm-popup-" : "ui-confirmdialog-";
        WebElement button = driver().findElement(By.className(prefix + "yes"));
        try {
            new WebDriverWait(driver(), Duration.ofSeconds(secs)).until(ExpectedConditions.visibilityOf(button));
            return true;
        }
        catch (TimeoutException e) {
            return false;
        }
    }

    private void answer(boolean popup, boolean yes) {
        String prefix = popup ? "ui-confirm-popup-" : "ui-confirmdialog-";
        WebElement button = driver().findElement(By.className(prefix + (yes ? "yes" : "no")));
        new WebDriverWait(driver(), Duration.ofSeconds(5)).until(ExpectedConditions.visibilityOf(button));
        button.click();
    }

    private String activeDesc() {
        return (String) js().executeScript("var a=document.activeElement; return a.tagName+'#'+a.id+'[name='+a.getAttribute('name')+',checked='+a.checked+']'");
    }

    private WebDriver driver() {
        return PrimeSelenium.getWebDriver();
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver();
    }

    private WebElement element(String id) {
        return driver().findElement(By.id(id));
    }
}
