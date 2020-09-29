/*
 * Copyright 1994-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package java.lang;

import java.io.*;
import java.util.*;
import java.lang.isolate.*;
import javax.microedition.io.*;
import com.sun.squawk.util.*;

/**
 * A <i>thread</i> is a thread of execution in a program. The Java
 * Virtual Machine allows an application to have multiple threads of
 * execution running concurrently.
 * <p>
 * Every thread has a priority. Threads with higher priority are
 * executed in preference to threads with lower priority.
 * <p>
 * There are two ways to create a new thread of execution. One is to
 * declare a class to be a subclass of <code>Thread</code>. This
 * subclass should override the <code>run</code> method of class
 * <code>Thread</code>. An instance of the subclass can then be
 * allocated and started. For example, a thread that computes primes
 * larger than a stated value could be written as follows:
 * <p><hr><blockquote><pre>
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <p><blockquote><pre>
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * </pre></blockquote>
 * <p>
 * The other way to create a thread is to declare a class that
 * implements the <code>Runnable</code> interface. That class then
 * implements the <code>run</code> method. An instance of the class can
 * then be allocated, passed as an argument when creating
 * <code>Thread</code>, and started. The same example in this other
 * style looks like the following:
 * <p><hr><blockquote><pre>
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 * <p>
 * The following code would then create a thread and start it running:
 * <p><blockquote><pre>
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * </pre></blockquote>
 * <p>
 *
 *
 * @author  unascribed
 * @see     java.lang.Runnable
 * @see     java.lang.Runtime#exit(int)
 * @see     java.lang.Thread#run()
 * @since   JDK1.0
 */

/*
 * Implementation note.
 *
 * Thread schedulting is disabled by calling rechedule(). If the current
 * thread should not be disabled then "scheduler.runnableThreads.add(scheduler.currentThread);"
 * should be executed before the call to rechedule() (see yield()).
 */

public class Thread implements Runnable, NativeOpcodes {

    /**
     * The minimum priority that a thread can have.
     */
    public final static int MIN_PRIORITY = 1;

   /**
     * The default priority that is assigned to a thread.
     */
    public final static int NORM_PRIORITY = 5;

    /**
     * The maximum priority that a thread can have.
     */
    public final static int MAX_PRIORITY = 10;

    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return  the currently executing thread.
     */
    public static Thread currentThread() {
        if (scheduler == null) {
            return null;
        }
        return scheduler.currentThread;
    }

    public void setDaemon(boolean value) {
        throw new RuntimeException("Thread::setDaemon");
    }


    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds. The thread
     * does not lose ownership of any monitors.
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     * @see        java.lang.Object#notify()
     */
    public static void sleep(long delta) throws InterruptedException {
        if (scheduler.trace) {
            System.out.println("Thread sleep("+delta+") "+scheduler.currentThread.threadNumber);
        }
        if (delta < 0) {
            throw new IllegalArgumentException("negitive sleep time");
        }
        if (delta > 0) {
            scheduler.timerQueue.add(scheduler.currentThread, delta);
            reschedule();
        }
    }

   /**
     * Allocates a new <code>Thread</code> object.
     * <p>
     * Threads created this way must have overridden their
     * <code>run()</code> method to actually do anything.
     *
     * @see     java.lang.Runnable
     */
    public Thread() {
        this(null);
    }

    /**
     * Allocates a new <code>Thread</code> object with a
     * specific target object whose <code>run</code> method
     * is called.
     *
     * @param   target   the object whose <code>run</code> method is called.
     */
    public Thread(Runnable target) {
        this.threadNumber = scheduler.nextThreadNumber++;
        this.target       = target;
        this.state        = NEW;
        this.isolate      = Isolate.getCurrentIsolate();
        if (scheduler.currentThread != null) {
            priority = scheduler.currentThread.getPriority();
        } else {
            priority = NORM_PRIORITY;
        }
    }

    /**
     * Constructor only called when creating the master isolate.
     */
    Thread(int threadNumber) {
        this.threadNumber = threadNumber;
        this.target       = target;
        this.state        = NEW;
        this.isolate      = null;
        this.priority     = NORM_PRIORITY;
    }

    /**
     * setIsolate
     */
    void setIsolate(Isolate isolate) {
        this.isolate = isolate;
    }

   /**
    * Causes the currently executing thread object to temporarily pause
    * and allow other threads to execute.
    */
    public static void yield() {
        if (scheduler == null) {
            return;
        }
        if (scheduler.trace) {
            System.out.println("Thread yield() "+scheduler.currentThread.threadNumber);
        }
        scheduler.runnableThreads.add(scheduler.currentThread);
        reschedule();
    }

    /**
     * Causes this thread to begin execution; the Java Virtual Machine
     * calls the <code>run</code> method of this thread.
     * <p>
     * The result is that two threads are running concurrently: the
     * current thread (which returns from the call to the
     * <code>start</code> method) and the other thread (which executes its
     * <code>run</code> method).
     *
     * @exception  IllegalThreadStateException  if the thread was already
     *               started.
     * @see        java.lang.Thread#run()
     */

    public void start() {
        scheduler.runnableThreads.add(scheduler.currentThread);
        startPrim();
    }


   /*
    * startPrim
    *
    * Start the thread without rescheduling the current thread
    */
    void startPrim() {

       /*
        * This local variable is used to differentiate between the calling
        * thread and the one created here. In the calling thread the value
        * will be zero. The created thread will have its value changed to one.
        */
        int flag = 0xBABECAFE;

       /*
        * Check that the new thread is only started once, and set its state.
        */
        if (state != NEW) {
            throw new IllegalThreadStateException();
        }
        state = ALIVE;

       /*
        * Duplicate the current activation record
        */
        int[] ar = Native.getActivation();
        int[] newAR = new int[ar.length];
        Native.arraycopy(ar, 0, newAR, 0, ar.length);

       /*
        * Assign the new activation record to the new thread
        */
        this.activation = newAR;

       /*
        * Change the type of the new activation record from int[] to an activation record.
        */
        Native.setHeader(newAR, ar.getClass()); // Change from being an int array to be an activation array;

       /*
        * Zero the reference to the previous stack frames.
        */
        newAR[AR_previousAR] = 0;
//        newAR[AR_locals+0] = 0;

       /*
        * Change the value of 'flag' in the new activation record to one and to zero in this one.
        */
        for (int i = 0; i != newAR.length; i++) {
            if (newAR[i] == 0xBABECAFE) {
                newAR[i] = 1;
                break;
            }
        }
        flag = 0;

       /*
        * Use Native.setActivation() to write the correct IP value into the calling activation record
        * This is then copied into the new activation record so that the new thread will start at this
        * point.
        */
        Native.setActivation(ar);
        newAR[AR_ip] = ar[AR_ip];

       /*
        * This is the point where the two threads will fork.
        */
        if (flag == 0) {
           /*
            * This is the path of the original thread. If the scheduler state has not been
            * created, then this is the bootstrap thread and the state must now be created.
            */
            if (scheduler == null) {
                scheduler = SchedulerState.create();
            }
            scheduler.aliveThreads++;

           /* Add the new thread to the list of
            * runnable threads and reshedule the VM. If the current thread should also be
            * allowed to run then this will have been done prior to this routine in the start()
            * routine.
            */
            scheduler.runnableThreads.add(this);
            reschedule();
        } else {
           /*
            * This is the path of the new thread. Call the run() method and kill the VM
            * if a return is made.
            */
            callRun();
            Native.fatalVMError();
        }
    }

    /**
     * If this thread was constructed using a separate
     * <code>Runnable</code> run object, then that
     * <code>Runnable</code> object's <code>run</code> method is called;
     * otherwise, this method does nothing and returns.
     * <p>
     * Subclasses of <code>Thread</code> should override this method.
     *
     * @see     java.lang.Thread#start()
     * @see     java.lang.Runnable#run()
     */
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    /**
     * Tests if this thread is alive. A thread is alive if it has
     * been started and has not yet died.
     *
     * @return  <code>true</code> if this thread is alive;
     *          <code>false</code> otherwise.
     */
    public final boolean isAlive() {
        return state == ALIVE;
    }

    /**
     * isDead
     */
    protected boolean isDead() {
        return state == DEAD;
    }

    /**
     * Changes the priority of this thread.
     *
     * @param newPriority priority to set this thread to
     * @exception  IllegalArgumentException  If the priority is not in the
     *             range <code>MIN_PRIORITY</code> to
     *             <code>MAX_PRIORITY</code>.
     * @see        #getPriority
     * @see        java.lang.Thread#getPriority()
     * @see        java.lang.Thread#MAX_PRIORITY
     * @see        java.lang.Thread#MIN_PRIORITY
     */
    public final void setPriority(int newPriority) {
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }
        priority = newPriority;
    }

    /**
     * Returns this thread's priority.
     *
     * @return  this thread's name.
     * @see     #setPriority
     * @see     java.lang.Thread#setPriority(int)
     */
    public final int getPriority() {
        return priority;
    }

    /**
     * Returns the current number of active threads in the VM.
     *
     * @return the current number of active threads
     */
    public static int activeCount() {
        return scheduler.runnableThreads.size() + 1;
    }

    /**
     * Waits for this thread to die.
     *
     * @exception  InterruptedException if another thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     */
    public final void join() throws InterruptedException {
        if (this == scheduler.currentThread) {
            return;
        }
        while (isAlive()) {
            yield();
        }
    }

    /**
     * Returns a string representation of this thread, including a unique number
     * that identifies the thread and the thread's priority.
     *
     * @return  a string representation of this thread.
     */
    public String toString() {
        // Can't use StringBuffer as threading may have not been initialized.
        return "Thread[".concat(String.valueOf(threadNumber)).concat(" (pri=").concat(String.valueOf(getPriority())).concat(")]");
    }


   /* ------------------------------------------------------------------------ *\
    *                              Private stuff                               *
   \* ------------------------------------------------------------------------ */

   /**
    * callRun - This is called by the VM when a new thread is started.
    * The call sequence is that Thread.start() calls Thread.reschedule()
    * calls Thread.switchAndEnablePreemption() which calls this function.
    */
    private void callRun() {

        isolate.addThread(this);

            if (scheduler.trace) {
                Native.println("Thread start ".concat(String.valueOf(threadNumber)));
            }

            try {
                run();
            } catch (ExitVMError ex) {
                // Print nothing because this is the result of System.exit();
            } catch (Throwable ex) {
                Native.print("Uncaught exception ");
                Native.println(ex);
                ex.printStackTrace();
            }
            if (scheduler.trace) {
                Native.println("Thread exit ".concat(String.valueOf(threadNumber)));
            }
            state = DEAD;

        isolate.removeThread(this);

        scheduler.aliveThreads--;
        reschedule();
        Native.fatalVMError();  // Catch VM errors here.
    }

   /**
    * Context switch to another thread
    */
    private static void reschedule() {

        Thread thread;
        Thread oldThread = scheduler.currentThread;
        scheduler.currentThread = null; // safety

       /*
        * Loop until there is something to do
        */
        for (;;) {

           /*
            * Add any threads that are ready to be restarted.
            */
            int event;
            while ((event = Native.getEvent()) != 0) {
                signalEvent(event);
            }

           /*
            * Add any threads waiting for a certain time that
            * are now due.
            */
            while ((thread = scheduler.timerQueue.next()) != null) {
                Monitor monitor = thread.monitor;
                if (monitor != null) {
                    monitor.removeCondvarWait(thread);
                }
                scheduler.runnableThreads.add(thread);
            }

           /*
            * Break if there is something to do
            */
            if ((thread = scheduler.runnableThreads.next()) != null) {
                break;
            }

           /*
            * Stop if there are no runnable threads
            */
            if (scheduler.aliveThreads == 0) {
                Native.fatalVMError();
            }

           /*
            * Wait for an event or until timeout
            */
            Native.waitForEvent(scheduler.timerQueue.nextTime());
        }

       /*
        * Set the current thread
        */
        scheduler.currentThread = thread;

       /*
        * Set up the correct isolate context
        */
        thread.isolate.makeCurrent();

       /*
        * The following will either return to a previous context
        * or cause callRun() to be entered if currentThread
        * is a new thread.
        */
        if (oldThread != null) {
            oldThread.activation = Native.getActivation();
        }
        Native.setActivation(thread.activation);

    }



   /**
    * Block a thread
    */
    static void waitForEvent(int event) {
        scheduler.events.put(event, scheduler.currentThread);
        reschedule();
    }

   /**
    * Restart a blocked thread
    */

    private static void signalEvent(int event) {
        Thread thread = (Thread)scheduler.events.remove(event);
        scheduler.runnableThreads.add(thread);
    }

   /**
    * addMonitorWait
    */
    private static void addMonitorWait(Monitor monitor, Thread thread) {
       /*
        * Check the nesting depth
        */
        Native.assume(thread.monitorDepth > 0);
       /*
        * Add to the wait queue
        */
        monitor.addMonitorWait(thread);
       /*
        * If the wait queue has no owner then try and start a
        * waiting thread.
        */
        if (monitor.owner == null) {
            removeMonitorWait(monitor);
        }
    }

   /**
    * removeMonitorWait
    */
    private static void removeMonitorWait(Monitor monitor) {
       /*
        * Try and remove a thread from the wait queue
        */
        Thread waiter = monitor.removeMonitorWait();
        if (waiter != null) {
           /*
            * Set the monitor's ownership and nesting depth
            */
            monitor.owner = waiter;
            monitor.depth = waiter.monitorDepth;
            Native.assume(waiter.monitorDepth > 0);
           /*
            * Restart execution of the thread
            */
            scheduler.runnableThreads.add(waiter);
        } else {
           /*
            * No thread is waiting for this monitor,
            * so mark it as unused
            */
            monitor.owner = null;
            monitor.depth = 0;
        }
    }


   /**
    * monitiorEnter
    */
    static void monitorEnter(Object object) {
        Native.assume(scheduler.currentThread != null);
        Monitor monitor = getMonitor(object);
        if (monitor.owner == null) {
           /*
            * Unowned monitor, make the current thread the owner
            */
            monitor.owner = scheduler.currentThread;
            monitor.depth = 1;
        } else if (monitor.owner == scheduler.currentThread) {
           /*
            * Thread already owns the monitor, increment depth
            */
            monitor.depth++;
        } else {
           /*
            * Add to the wait queue and set the depth for when thread
            * is restarted
            */
            scheduler.currentThread.monitorDepth = 1;
            addMonitorWait(monitor, scheduler.currentThread);
            reschedule();
           /*
            * Safety...
            */
            Native.assume(monitor.owner == scheduler.currentThread);
            scheduler.currentThread.monitor = null;
            scheduler.currentThread.monitorDepth = 0;
        }
    }

   /**
    * monitiorExit
    */
    static void monitorExit(Object object) {
        Native.assume(scheduler.currentThread != null);
        Monitor monitor = getMonitor(object);

       /*
        * Throw an exception if things look bad
        */
        if (monitor.owner != scheduler.currentThread) {
            throw new IllegalMonitorStateException();
        }
       /*
        * Try and restart a thread if the nesting depth is zero
        */
        if (--monitor.depth == 0) {
            removeMonitorWait(monitor);
        }

       /*
        * We can free off the monitor if it has no owner
        */
        if (monitor.owner == null) {
            freeMonitor(object, monitor);
        }
    }



   /**
    * monitiorWait
    */
    static void monitorWait(Object object, long delta) throws InterruptedException {
        Monitor monitor = getMonitor(object);

       /*
        * Throw an exception if things look bad
        */
        if (monitor.owner != scheduler.currentThread) {
            throw new IllegalMonitorStateException();
        }
       /*
        * Add to timer queue if time is > 0
        */
        if (delta > 0) {
            scheduler.timerQueue.add(scheduler.currentThread, delta);
        }
       /*
        * Save the nesting depth so it can be restored
        * when it regains the monitor
        */
        scheduler.currentThread.monitorDepth = monitor.depth;
       /*
        * Add to the wait queue
        */
        monitor.addCondvarWait(scheduler.currentThread);
       /*
        * Having relinquishing the monitor
        * get the next thread off the wait queue
        */
        removeMonitorWait(monitor);
       /*
        * Wait for a notify or timeout
        */
        Native.assume(scheduler.currentThread.monitor == monitor);
        reschedule();
       /*
        * At this point the thread has been restarted. This could have
        * been because of a call to notify() or a timeout.
        *
        * Must get the monitor again
        */
        addMonitorWait(monitor, scheduler.currentThread);
        reschedule();
       /*
        * Safety...
        */
        scheduler.currentThread.monitor = null;
        scheduler.currentThread.monitorDepth = 0;
    }


   /**
    * monitorNotify
    */
    static void monitorNotify(Object object, boolean notifyAll) {
        Monitor monitor = getMonitor(object);
       /*
        * Throw an exception if things look bad
        */
        if (monitor.owner != scheduler.currentThread) {
            throw new IllegalMonitorStateException();
        }
       /*
        * Try and restart a thread
        */
        do {
            Thread waiter = monitor.removeCondvarWait();
            if (waiter == null) {
                break;
            }
           /*
            * Remove timeout is there was one and restart
            */
            scheduler.timerQueue.remove(waiter);
            scheduler.runnableThreads.add(waiter);
       /*
        * Loop here if it is a notifyAll
        */
        } while (notifyAll);
    }

   /**
    * getMonitor
    */
    private static Monitor getMonitor(Object object) {
        Monitor monitor = null;
       /*
        * Get the class or monitor pointer
        */
        Object something = Native.getHeader(object);
        if (something instanceof Monitor) {
            monitor = (Monitor)something;
           /*
            * If this is a proxy monitor then look for the real one in the isloate hash table
            */
            if (monitor.isProxy != 0) {
               /*
                * Look for the monitor, and create one if there is none.
                */
                monitor = (Monitor)currentThread().isolate.hashedMonitors.get(object);
                if (monitor == null) {
                    monitor = new Monitor(null);
                    currentThread().isolate.hashedMonitors.put(object, monitor);
                }
            }
        } else {
            Native.assume(something instanceof Class);
           /*
            * Create a monitor and place it between the object and it's class
            */
            monitor = new Monitor(Native.asClass(something));
            Native.setHeader(object, monitor);
        }
        return monitor;
    }

   /**
    * freeMonitor
    */
    private static void freeMonitor(Object object, Monitor monitor) {
        Native.assume(monitor.owner == null);
        Native.assume(monitor.monitorQueue == null);
        Native.assume(monitor.condvarQueue == null);
        Native.assume(monitor.depth == 0);
        if (monitor.isProxy != 0) {
            currentThread().isolate.hashedMonitors.remove(object);
        }
    }

   /**
    * getHashCode
    */
    static int getHashCode(Object object) {
       /*
        * If the header points to a real monitor or a proxy monitor then use the
        * hashcode in both cases. Only very poor hashcodes can be found in a proxy
        * monitor, but as these are only used for String and Class objects this
        * does not matter because the method is overridden in both cases.
        */
        Object something = Native.getHeader(object);
        if (!(something instanceof Monitor)) {
            something = getMonitor(object);
        }

        Monitor monitor = (Monitor)something;

        while (monitor.hashCode == 0) {
            Native.assume(monitor.isProxy == 0);
            monitor.hashCode = scheduler.nextHashCode++;
        }
        return monitor.hashCode;
    }


   /**
    * setInQueue
    */
    protected void setInQueue() {
        Native.assume(!inQueue);
        inQueue = true;
    }

   /**
    * setNotInQueue
    */
    protected void setNotInQueue() {
        Native.assume(inQueue);
        inQueue = false;
    }

   /**
    * setInTimerQueue
    */
    protected void setInTimerQueue() {
        Native.assume(!inTimerQueue);
        inTimerQueue = true;
    }

   /**
    * setNotInTimerQueue
    */
    protected void setNotInTimerQueue() {
        Native.assume(inTimerQueue);
        inTimerQueue = false;
    }

   /* ------------------------------------------------------------------------ *\
    *                               Global state                               *
   \* ------------------------------------------------------------------------ */

    /**
     * This is the pointer to the object holding all the global threading state
     * that must be shared across all Isolates. It's value is initialized in the
     * Thread.startPrim method.
     */
    static SchedulerState scheduler;

    /**
     * Return whether or the threading system has been initialized. This is
     * a pre-requisite for a number of actions, the use of StringBuffer (which
     * has synchronized methods) being a good example.
     */
    static boolean threadingInitialized() { return scheduler != null; }

    static boolean tracing() { return scheduler != null && scheduler.trace; }

   /* ------------------------------------------------------------------------ *\
    *                              Instance state                              *
   \* ------------------------------------------------------------------------ */

    private final static int NEW   = 0;
    private final static int ALIVE = 1;
    private final static int DEAD  = 2;

    private   Runnable target;          /* Target to run (if run() is not overridden)                        */
    int       priority;                 /* Execution priority                                                */
    private   int state;                /* Aliveness                                                         */
    private   Object ar;                /* Used internally by the VM to point to the activation record       */
    private   int ip;                   /* Used internally by the VM to record the instruction pointer       */
    boolean   inQueue;                  /* Flag to show if thread is in a queue                              */
    Thread    nextThread;               /* For enqueueing in the ready, monitor wait, or condvar wait queues */
    boolean   inTimerQueue;             /* Flag to show if thread is in a queue                              */
    Thread    nextTimerThread;          /* For enqueueing in the timer queue                                 */
    long      time;                     /* Time to emerge from the timer queue                               */
    int       monitorDepth;             /* Saved monitor nesting depth                                       */
    Monitor   monitor;                  /* Monitor when thread is in the condvar queue                       */
    final int threadNumber;             /* The 'name' of the thread                                          */
    private   int[] activation;         /* Activation record                                                 */
    Isolate   isolate;                  /* The Isolate under which the thread is running                     */

}



/* ======================================================================== *\
 *                                 Monitor                                  *
\* ======================================================================== */

/*
 * Note - All the code in the following class is run with preemption disabled
 */

class Monitor {
    protected Class   realType;       /* Used internally by the VM to point to the Type          */
    protected Thread  owner;          /* Owening thread of the monitor                           */
    protected Thread  monitorQueue;   /* Queue of threads waiting to claim the monitor           */
    protected Thread  condvarQueue;   /* Queue of threads waiting to claim the object            */
    protected int     hashCode;       /* Object's hashcode                                       */
    protected int     depth;          /* Nesting depth                                           */
    protected int     isProxy = 0;    /* Flag to say if the monitor is in the isolate hash table */

    public Monitor(Class cls) {
        realType = cls;
    }

   /**
    * addMonitorWait
    */
    void addMonitorWait(Thread thread) {
        thread.setInQueue();
        Native.assume(thread.nextThread == null);
        Thread next = monitorQueue;
        if (next == null) {
            monitorQueue = thread;
        } else {
            while (next.nextThread != null) {
                next = next.nextThread;
            }
            next.nextThread = thread;
        }
    }

   /**
    * removeMonitorWait
    */
    Thread removeMonitorWait() {
        Thread thread = monitorQueue;
        if (thread != null) {
            monitorQueue = thread.nextThread;
            thread.setNotInQueue();
            thread.nextThread = null;
        }
        return thread;
    }

   /**
    * addCondvarWait
    */
    void addCondvarWait(Thread thread) {
        thread.setInQueue();
        thread.monitor = this;
        Native.assume(thread.nextThread == null);
        Thread next = condvarQueue;
        if (next == null) {
            condvarQueue = thread;
        } else {
            while (next.nextThread != null) {
                next = next.nextThread;
            }
            next.nextThread = thread;
        }
    }

   /**
    * removeCondvarWait
    */
    Thread removeCondvarWait() {
        Thread thread = condvarQueue;
        if (thread != null) {
            condvarQueue = thread.nextThread;
            thread.setNotInQueue();
            thread.monitor = null;
            thread.nextThread = null;
        }
        return thread;
    }

   /**
    * removeCondvarWait
    */
    void removeCondvarWait(Thread thread) {
        if (thread.inQueue) {
            thread.setNotInQueue();
            thread.monitor = null;
            Thread next = condvarQueue;
            if (next != null) {
                if (next == thread) {
                    condvarQueue = thread.nextThread;
                    thread.nextThread = null;
                    return;
                }
                while (next.nextThread != thread) {
                    next = next.nextThread;
                }
                if (next.nextThread == thread) {
                    next.nextThread = thread.nextThread;
                }
                thread.nextThread = null;
                return;
            }
        }
    }

}


/* ======================================================================== *\
 *                               ThreadQueue                                *
\* ======================================================================== */

/*
 * Note - All the code in the following class is run with preemption disabled
 */

class ThreadQueue {

    Thread first;
    int count;

   /**
    * add
    */
    void add(Thread thread) {
        if (thread != null) {
            thread.setInQueue();
            if (first == null) {
                first = thread;
            } else {
                if (first.priority < thread.priority) {
                    thread.nextThread = first;
                    first = thread;
                } else {
                    Thread last = first;
                    while (last.nextThread != null && last.nextThread.priority >= thread.priority) {
                        last = last.nextThread;
                    }
                    thread.nextThread = last.nextThread;
                    last.nextThread = thread;
                }
            }
            count++;
        }
    }

   /**
    * size
    */
    int size() {
        return count;
    }

   /**
    * next
    */
    Thread next() {
        Thread thread = first;
        if (thread != null) {
            thread.setNotInQueue();
            first = thread.nextThread;
            thread.nextThread = null;
            count--;
        }
        return thread;
    }

}


/* ======================================================================== *\
 *                                TimerQueue                                *
\* ======================================================================== */

/*
 * Note - All the code in the following class is run with preemption disabled
 */

class TimerQueue {

    Thread first;

   /**
    * add
    */
    void add(Thread thread, long delta) {
        Native.assume(thread.nextTimerThread == null);
        thread.setInTimerQueue();
        thread.time = System.currentTimeMillis() + delta;
        if (thread.time < 0) {
           /*
            * If delta is so huge that the time went negative then just make
            * it a very large value. The universe will end before the error
            * is detected!
            */
            thread.time = Long.MAX_VALUE;
        }
        if (first == null) {
            first = thread;
        } else {
            if (first.time > thread.time) {
                thread.nextTimerThread = first;
                first = thread;
            } else {
                Thread last = first;
                while (last.nextTimerThread != null && last.nextTimerThread.time < thread.time) {
                    last = last.nextTimerThread;
                }
                thread.nextTimerThread = last.nextTimerThread;
                last.nextTimerThread = thread;
            }
        }
    }

   /**
    * next
    */
    Thread next() {
        Thread thread = first;
        if (thread == null || thread.time > System.currentTimeMillis()) {
            return null;
        }
        first = first.nextTimerThread;
        thread.setNotInTimerQueue();
        thread.nextTimerThread = null;
        Native.assume(thread.time != 0);
        thread.time = 0;
        return thread;
    }

   /**
    * remove
    */
    void remove(Thread thread) {
        if (first == null || thread.time == 0) {
            Native.assume(!thread.inQueue);
            return;
        }
        thread.setNotInTimerQueue();
        if (thread == first) {
            first = thread.nextTimerThread;
            thread.nextTimerThread = null;
            return;
        }
        Thread p = first;
        while (p.nextTimerThread != null) {
            if (p.nextTimerThread == thread) {
                p.nextTimerThread = thread.nextTimerThread;
                thread.nextTimerThread = null;
                return;
            }
            p = p.nextTimerThread;
        }
        Native.fatalVMError();
    }

   /**
    * nextTime
    */
    long nextTime() {
        if (first != null) {
            return first.time;
        } else {
            return Long.MAX_VALUE;
        }
    }

}

/* ======================================================================== *\
 *                         SchedulerState                                   *
\* ======================================================================== */

/**
 * This class contains all the threading state that must be global across all Isolates.
 */
class SchedulerState {
    /*private*/ Thread currentThread;                /* The current thread                           */
    /*private*/ int aliveThreads;                    /* Number of alive threads                      */
    /*private*/ ThreadQueue runnableThreads;         /* Queue of runnable threads                    */
    /*private*/ TimerQueue timerQueue;               /* Queue of timed waiting threads               */
    /*private*/ int nextThreadNumber;                /* The 'name' of the next thread                */
    /*private*/ boolean trace;                       /* Trace flag                                   */
    /*private*/ IntHashtable events;                 /* Hashtable of pending threads                 */
    /*private*/ int nextHashCode;                    /* The next hashcode to use                     */

    private SchedulerState() {
        nextThreadNumber    = 1; // Thread 0 is the bootstrap thread
        aliveThreads        = 0;
        nextHashCode        = 0;
        trace               = false;
        runnableThreads     = new ThreadQueue();
        timerQueue          = new TimerQueue();
        events              = new IntHashtable();
    }

    static SchedulerState create() {
        return new SchedulerState();
    }
}
