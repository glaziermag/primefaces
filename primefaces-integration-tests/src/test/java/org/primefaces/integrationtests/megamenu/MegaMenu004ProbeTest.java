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
package org.primefaces.integrationtests.megamenu;

import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeSelenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

// fork-only probe, not for upstream
class MegaMenu004ProbeTest extends AbstractPrimePageTest {

    private static final String HOOK = "window.__ev={enter:0,over:0,move:0};"
            + "var a=document.querySelector('#form\\\\:menu > ul > li.ui-menuitem > a');"
            + "a.addEventListener('mouseenter',function(){window.__ev.enter++;});"
            + "a.addEventListener('mouseover',function(){window.__ev.over++;});"
            + "document.addEventListener('mousemove',function(){window.__ev.move++;},true);";

    @Test
    void probe() {
        WebDriver d = PrimeSelenium.getWebDriver();
        int[] fails = new int[2];
        for (int r = 0; r < 20; r++) {
            for (int k = 0; k < 2; k++) {
                boolean fixed = ((r + k) % 2) == 1;
                // prime: leave the Actions pointer resting on root 0, as invalidIndex[1] does
                goTo("megamenu/megaMenu004.xhtml?index=-1");
                WebElement link0 = d.findElement(By.cssSelector("#form\\:menu > ul > li.ui-menuitem > a"));
                new Actions(d).moveToElement(d.findElement(By.id("form:outside"))).moveToElement(link0).perform();
                String primed = visible(d) ? "open" : "CLOSED";
                d.findElement(By.id("form:outside")).click();
                // act: same sequence as invalidIndex[2]
                goTo("megamenu/megaMenu004.xhtml?index=2");
                PrimeSelenium.executeScript(HOOK);
                link0 = d.findElement(By.cssSelector("#form\\:menu > ul > li.ui-menuitem > a"));
                Actions act = new Actions(d);
                if (fixed) {
                    act.moveToElement(d.findElement(By.id("form:outside")));
                }
                act.moveToElement(link0).perform();
                boolean ok = visible(d);
                if (!ok) {
                    fails[fixed ? 1 : 0]++;
                }
                Object ev = PrimeSelenium.executeScript("return JSON.stringify(window.__ev);");
                System.out.println("PROBE r=" + r + " arm=" + (fixed ? "fixed" : "base") + " primed=" + primed
                        + " open=" + (ok ? "ok" : "FAIL") + " ev=" + ev);
            }
        }
        System.out.println("PROBE TOTAL base_fail=" + fails[0] + "/20 fixed_fail=" + fails[1] + "/20");
    }

    private boolean visible(WebDriver d) {
        try {
            PrimeSelenium.waitGui().until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("#form\\:menu > ul > li.ui-menuitem:first-child > ul.ui-menu-child")));
            return true;
        }
        catch (TimeoutException e) {
            return false;
        }
    }
}
