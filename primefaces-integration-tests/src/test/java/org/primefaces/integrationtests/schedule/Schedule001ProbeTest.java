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
package org.primefaces.integrationtests.schedule;

import org.primefaces.selenium.AbstractPrimePageTest;
import org.primefaces.selenium.PrimeExpectedConditions;
import org.primefaces.selenium.PrimeSelenium;
import org.primefaces.selenium.component.Schedule;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

// fork-only probe, not for upstream
class Schedule001ProbeTest extends AbstractPrimePageTest {

    private static final String HOOK = "if(!window.__hooked){window.__hooked=1;window.__log=[];"
            + "var L=function(k,v){window.__log.push([Math.round(performance.now()),k,v]);};"
            + "var P=PrimeFaces.widget.Schedule.prototype;"
            + "var hb=P.hasBehavior;P.hasBehavior=function(n){if(n.indexOf('event')===0||n.indexOf('date')===0){L('hb',n);}return hb.apply(this,arguments);};"
            + "var seh=P.setupEventHandlers;P.setupEventHandlers=function(){seh.apply(this,arguments);var w=this,o=this.cfg.options.eventClick;"
            + "L('init',window.__w===w?'same':'new');window.__w=w;"
            + "this.cfg.options.eventClick=function(i){L('ec',w.doubleClick===i.event.id?'SAME':(w.doubleClick==null?'null':'other'));return o.apply(this,arguments);};};"
            + "var H=PrimeFaces.ajax.Request.handle;PrimeFaces.ajax.Request.handle=function(c){"
            + "var s=c.event||(c.params&&c.params.length&&c.params[0].name)||'';L('req',String(s).replace(/^form:schedule_/,''));return H.apply(this,arguments);};"
            + "var R=PrimeFaces.ajax.Response.handle;PrimeFaces.ajax.Response.handle=function(){L('resp','');return R.apply(this,arguments);};"
            + "['mousedown','mouseup','click'].forEach(function(t){document.addEventListener(t,function(e){"
            + "if(e.target.closest&&e.target.closest('.fc-daygrid-event')){L(t,'');}},true);});"
            + "var M=function(n){return n.nodeType===1&&(n.matches('.fc-daygrid-event,.fc-daygrid-event-harness')||n.querySelector('.fc-daygrid-event'));};"
            + "new MutationObserver(function(ms){var a=0,r=0;ms.forEach(function(m){m.addedNodes.forEach(function(n){if(M(n))a++;});"
            + "m.removedNodes.forEach(function(n){if(M(n))r++;});});if(a)L('ins',a);if(r)L('rm',r);})"
            + ".observe(document.body,{childList:true,subtree:true});}";

    // timeline relative to the last mousedown on an event (the second select), window [-arguments[0], +1500] ms
    private static final String SIG = "var W=arguments[0],l=window.__log,md=-1,n=0;for(var i=l.length-1;i>=0;i--){if(l[i][1]==='mousedown'){n++;if(md<0)md=i;}}"
            + "if(md<0)return 'nomd '+JSON.stringify(l.slice(-20));var t=l[md][0],f=null;"
            + "for(var j=0;j<l.length;j++){if(l[j][1]==='mousedown'){f=l[j][0];break;}}"
            + "return 'md='+n+' sinceFirst='+(t-f)+' | '+l.filter(function(e){return e[0]>=t-W&&e[0]<=t+1500;})"
            + ".map(function(e){return (e[0]-t)+e[1]+(e[2]===''?'':'='+e[2]);}).join(' ');";

    @Test
    void probe() {
        int n = Integer.getInteger("probe.n", 30);
        int fail = 0;
        int errors = 0;
        for (int r = 0; r < n; r++) {
            String stage = "goTo";
            try {
                Schedule001Test.Page page = PrimeSelenium.goTo(Schedule001Test.Page.class);
                PrimeSelenium.executeScript(HOOK);
                Schedule schedule = page.schedule;

                // same WebDriver call sequence as Schedule001Test.eventSelect up to line 186
                stage = "first";
                waitForScheduledRenderedAndClickable();
                schedule.select("fc-daygrid-event");
                stage = "assert1";
                page.messages.getMessage(0);
                page.selectedEventTitle.getValue();
                page.selectedEventStartDate.getValue();
                page.selectedEventEndDate.getValue();
                stage = "tz";
                page.timeZone.select(Schedule001Test.ALTERNATIV_SERVER_TIMEZONE);
                waitForScheduledRenderedAndClickable();
                page.clientTimeZone.select(Schedule001Test.ALTERNATIV_CLIENT_TIMEZONE);
                waitForScheduledRenderedAndClickable();
                stage = "second";
                waitForScheduledRenderedAndClickable();
                boolean ok = true;
                try {
                    schedule.select("fc-daygrid-event");
                }
                catch (TimeoutException e) {
                    ok = false;
                    fail++;
                }
                Object sig = PrimeSelenium.executeScript("return (function(){" + SIG + "}).apply(null, arguments);", ok ? 300 : 4000);
                System.out.println("PROBE r=" + r + (ok ? " ok " : " FAIL ") + sig);
            }
            catch (RuntimeException e) {
                errors++;
                System.out.println("PROBE err r=" + r + " stage=" + stage + " " + e.getClass().getSimpleName() + " " + String.valueOf(e.getMessage()).split("\n")[0]);
            }
        }
        System.out.println("PROBE summary n=" + n + " fail=" + fail + " errors=" + errors);
    }

    private void waitForScheduledRenderedAndClickable() {
        PrimeSelenium.waitGui().until(PrimeExpectedConditions.ajaxQueueEmpty());
        PrimeSelenium.waitGui().until(ExpectedConditions.elementToBeClickable(By.cssSelector(".fc-daygrid-event")));
    }
}
