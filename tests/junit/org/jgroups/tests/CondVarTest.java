// $Id: CondVarTest.java,v 1.3 2005/07/26 18:36:00 belaban Exp $

package org.jgroups.tests;


import EDU.oswego.cs.dl.util.concurrent.FutureResult;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;
import junit.framework.TestCase;
import org.jgroups.util.Util;
import org.jgroups.util.CondVar;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


/**
 * Various test cases for CondVar
 * @author Bela Ban
 */
public class CondVarTest extends TestCase {
    CondVar cond=new CondVar("blocking", Boolean.FALSE);


    public CondVarTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }


    public void testConditionTrue() {
        try {
            cond.waitUntilWithTimeout(Boolean.FALSE, 500);
        }
        catch(org.jgroups.TimeoutException e) {
            fail("received TimeoutException");
        }
    }

    public void testConditionTrueWaitForever() {
        cond.waitUntil(Boolean.FALSE);
    }


    public void testWithTimeoutException() {
        try {
            cond.waitUntilWithTimeout(Boolean.TRUE, 500);
            fail("expected timeout exception");
        }
        catch(org.jgroups.TimeoutException e) {
        }
    }


    public void testWithResultSetter() throws org.jgroups.TimeoutException {
        new ResultSetter(cond, 500).start();
        cond.waitUntilWithTimeout(Boolean.TRUE,  2000);
    }

    public void testWithResultSetter_ResultSetBeforeAccess() throws org.jgroups.TimeoutException {
        new ResultSetter(cond, 10).start();
        Util.sleep(100);
        cond.waitUntilWithTimeout(Boolean.TRUE,  2000);
    }

    public void testDoubleLocking() throws org.jgroups.TimeoutException {
        final Map m=new HashMap();
        final CondVar c=new CondVar("bla", Boolean.FALSE, m);

        new Thread() {
            public void run() {
                Util.sleep(1000);
                _setValue(m, c);
            }
        }.start();

        _enterMonitor(m, c);
    }

    private void _setValue(Map m, CondVar c) {
        log("acquiring m");
        synchronized(m) {
            log("acquired m. setting c");
            c.set(Boolean.TRUE);
            log("set c. released c");
        }
        log("released m");
    }

    private void _enterMonitor(final Map m, final CondVar c) throws org.jgroups.TimeoutException {
        log("acquiring m");
        synchronized(m) {
            log("acquired m. acquiring and waiting on c");
            c.waitUntilWithTimeout(Boolean.TRUE, 10000);
            log("released c");
        }
        log("released m");
    }


    private void log(String msg) {
        System.out.println(System.currentTimeMillis() + " " + Thread.currentThread() + " - " + msg);
    }

    public void testStressOnGet() {
        long start, stop;
        long NUM=1000000L;
        start=System.currentTimeMillis();
        for(int i=0; i < NUM; i++) {
            if(cond.get().equals(Boolean.TRUE))
                ;
        }
        stop=System.currentTimeMillis();
        long diff=stop-start;
        diff*=1000; // microsecs
        double microsecs_per_get=diff / (double)NUM;
        System.out.println("took " + microsecs_per_get + " microsecs/get for " + NUM + " gets (" + diff + " microsecs)");
    }


    class ResultSetter extends Thread {
        long wait_time=2000;
        CondVar target=null;

        ResultSetter(CondVar target, long wait_time) {
            this.target=target;
            this.wait_time=wait_time;
        }

        public void run() {
            Util.sleep(wait_time);
            System.out.println("-- [ResultSetter] set result to true");
            target.set(Boolean.TRUE);
            System.out.println("-- [ResultSetter] set result to true -- DONE");
        }
    }



    public static void main(String[] args) {
        String[] testCaseName={CondVarTest.class.getName()};
        junit.textui.TestRunner.main(testCaseName);
    }

}