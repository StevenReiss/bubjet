/********************************************************************************/
/*                                                                              */
/*              BubjetExecutionManager.java                                     */
/*                                                                              */
/*      Handle debugging via IDEA                                               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.bubbles.bubjet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.ExecutionTargetManager;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.module.Module;
import com.intellij.util.messages.MessageBus;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerManagerListener;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.StepRequest;

import edu.brown.cs.bubbles.bubjet.BubjetDebug.BStackFrame;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BThread;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BubjetExecutionManager implements BubjetConstants, ExecutionListener, XDebuggerManagerListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService        app_service;
private Project                         for_project;
private List<BubjetDebugProcess>        debug_processes;
private Set<ProcessHandler>             start_waits;
private Set<ProcessHandler>             started_already;
private Map<BubjetDebugProcess,DebugListener> debug_listeners;
private Map<Project,ConsoleThread>      console_threads;
private Map<BubjetDebugProcess,ConsoleData> console_map;





/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetExecutionManager(BubjetApplicationService app,Project p)
{
   app_service = app;
   for_project = p;
   debug_processes = new ArrayList<>();
   MessageBus mbus = p.getMessageBus();
   start_waits = new HashSet<>();
   started_already = new HashSet<>();
   debug_listeners = new HashMap<>();
   console_threads = new HashMap<>();
   console_map = new HashMap<>();
   mbus.connect().subscribe(ExecutionManager.EXECUTION_TOPIC,this);
   mbus.connect().subscribe(XDebuggerManager.TOPIC,this);
}




/********************************************************************************/
/*                                                                              */
/*      Process management methods                                              */
/*                                                                              */
/********************************************************************************/

private synchronized BubjetDebugProcess findProcess(String name,ProcessHandler ph,ExecutionEnvironment ev)
{
   for (BubjetDebugProcess dp : debug_processes) {
      if (dp.getHandler() == ph) {
         dp.setEnvironment(name,ev);
         return dp;
       }
    }
   if (ev == null) return null;
   
   BubjetDebugProcess bdp = new BubjetDebugProcess(for_project,name,ph,ev);
   debug_processes.add(bdp);
   return bdp;
}


private synchronized BubjetDebugProcess findProcess(DebugProcess dp)
{
   for (BubjetDebugProcess bdp : debug_processes) {
      if (bdp.getHandler() == dp.getProcessHandler()) {
         bdp.setProcess(dp);
         return bdp;
       }
    }
   BubjetDebugProcess bdp = new BubjetDebugProcess(for_project,dp);
   debug_processes.add(bdp);
   return bdp;
}



private synchronized BubjetDebugProcess findProcess(XDebugProcess xdp)
{
   for (BubjetDebugProcess dp : debug_processes) {
      if (dp.getHandler() == xdp.getProcessHandler()) {
         dp.setXDebugProcess(xdp);
         return dp;
       }
    }
   
   BubjetDebugProcess bdp = new BubjetDebugProcess(for_project,xdp);
   debug_processes.add(bdp);
   return bdp;
}



static boolean matchProcess(String name,BubjetDebugProcess p)
{
   if (name == null) return true;
   if (name.equals(p.getId())) return true;
   if (name.equals(p.getLaunchId())) return true;
   if (name.equals(p.getLaunchName())) return true;
      
   return false;   
}


static boolean matchLaunch(String name,BubjetDebugProcess p)
{
   if (name == null) return true;
   if (name.equals(p.getLaunchId())) return true;
   if (name.equals(p.getLaunchConfiguration().getConfiguration().getId())) return true;
   if (name.equals(p.getLaunchName())) return true;
   
   return false;
}


static boolean matchTarget(String name,BubjetDebugProcess p)
{
   if (name == null) return true;
   return true;
}


static boolean matchThread(String name,ThreadReference thr)
{
   if (name == null) return true;
   String tid = String.valueOf(thr.uniqueID());
   if (name.equals(tid)) return true;
   if (name.equals(thr.name())) return true;
      
   return false;
}





private synchronized void removeProcess(BubjetDebugProcess proc)
{
   if (debug_processes.remove(proc)) {
      BubjetDebug.removeProcess(proc);
    }
}



Collection<BubjetDebugProcess> getProcesses()
{
   return debug_processes;
}


Collection<BubjetDebugProcess> getProcesses(String match)
{
   List<BubjetDebugProcess> rslt = new ArrayList<>();
   for (BubjetDebugProcess bdp : debug_processes) {
      if (matchProcess(match,bdp)) rslt.add(bdp);
    }
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Evaluation calls                                                        */
/*                                                                              */
/********************************************************************************/

void startEvaluation(BThread thrd)
{
   if (thrd == null) return;
   
   generateThreadEvent("RESUME",thrd.getProcess(),thrd,true,false,true);
}


void endEvaluation(BThread thrd)
{
   if (thrd == null) return;
   generateThreadEvent("SUSPEND",thrd.getProcess(),thrd,false,false,true);
} 



/********************************************************************************/
/*                                                                              */
/*      START command                                                           */
/*                                                                              */
/********************************************************************************/

void handleStart(Project p,Module m,RunnerAndConfigurationSettings cfg,String mode,
      boolean build,boolean register,String vmarg,String id,IvyXmlWriter xw)
        throws BubjetException
{
   if (cfg == null) return;
   
   RunConfiguration rcfg = cfg.getConfiguration();
   if (vmarg != null) {
      if (rcfg instanceof CommonJavaRunConfigurationParameters) {
         CommonJavaRunConfigurationParameters jrcp = (CommonJavaRunConfigurationParameters) rcfg;
         String vmp = jrcp.getVMParameters();
         if (vmp == null || vmp.length() == 0) vmp = vmarg;
         else if (!vmp.contains(vmarg)) vmp = vmp + " " + vmarg;
         else vmp = null;
         if (vmp != null) jrcp.setVMParameters(vmp);
       }
    }
   
   ExecutionTargetManager etm = ExecutionTargetManager.getInstance(p);
   List<ExecutionTarget> tgts =  etm.getTargetsFor(cfg.getConfiguration());
   for (ExecutionTarget tgt : tgts) {
      BubjetLog.logD("CHECK TARGET " + tgt + " " + ExecutionTargetManager.canRun(rcfg,tgt));
    }
   ExecutionTarget dtgt = DefaultExecutionTarget.INSTANCE;
   BubjetLog.logD("DEFAULT TARGET " + dtgt);
   
   Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
   ProgramRunner<RunnerSettings> runner = ProgramRunner.getRunner(executor.getId(),rcfg);
   ExecutionEnvironment ev = new ExecutionEnvironment(executor,runner,cfg,p);
   StartedAction started = new StartedAction();
   ev.setCallback(started);
   ExecutionManager em = ExecutionManager.getInstance(p);
   StartAction startact = new StartAction(runner,ev,started);
   if (build) {
      em.compileAndRun(startact,ev,null);
    }
   else {
      startact.run();
    }
   RunContentDescriptor rcd = started.getProcess();
   if (rcd == null) throw new BubjetException("Launch Failed");
   BubjetLog.logD("MODE " + executor.getActionName() + " " + executor.getDescription() + " " +
         executor.getId() + " " + executor.getStartActionText());
   BubjetLog.logD("RCD " + rcd.getDisplayName() + ": " + rcd.getExecutionId() + " " +
         rcd.getExecutionConsole() + " " + rcd.getProcessHandler());
   BubjetLog.logD("RCD1 " + rcd.getProcessHandler().getUserDataString());
  
   waitForStarted(rcd.getProcessHandler());
   
   xw.begin("LAUNCH");
   xw.field("MODE","debug");
   xw.field("ID",rcd.getExecutionId());
   xw.field("CID",rcfg.getUniqueID());
   xw.field("PROCESS",rcd.getProcessHandler().hashCode());
   xw.end("LAUNCH");
}



private class StartAction implements Runnable, ProgramRunner.Callback {

   private ProgramRunner<?>     program_runner;
   private ExecutionEnvironment exec_environment;
   private StartedAction        exec_status;
   
   StartAction(ProgramRunner<?> runner,ExecutionEnvironment ee,StartedAction status) {
      program_runner = runner;
      exec_environment = ee;
      exec_status = status;
      ee.setCallback(status);
      BubjetLog.logD("START RUN");
    }
   
   @Override public void run() {
      BubjetLog.logD("START RUN with runner " + program_runner + " " + exec_environment);
      try {
         program_runner.execute(exec_environment);
       }
      catch (ExecutionException e) {
         exec_status.failed();
       }
      BubjetLog.logD("RUN STARTED");
    }
   
   @Override public void processStarted(RunContentDescriptor d) {
      BubjetLog.logD("PROCESS1 STARTED " + d);
      
    }
}


private class DebuggerStarter extends XDebugProcessStarter {
   
   public XDebugProcess start(XDebugSession sess) {
      return JavaDebugProcess.create(sess,null);
    }
   
}       // end of inner class Debugger Starter





private class StartedAction implements ProgramRunner.Callback {
   
   private RunContentDescriptor run_content;
   private boolean is_done;
   
   StartedAction() {
      run_content = null;
      is_done = false;
    }
   
   @Override public synchronized void processStarted(RunContentDescriptor d) {
      BubjetLog.logD("PROCESS STARTED " + d);
      run_content = d;
      is_done = true;
      noteProcessReady(d.getProcessHandler());
    }
   
   synchronized void failed() {
      is_done = true;
      notifyAll();
    }
   
   synchronized RunContentDescriptor getProcess() {
      while (!is_done) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
      return run_content;
    }
   
}       // end of inner class StartedAction



private synchronized void noteProcessReady(ProcessHandler ph)
{
   if (started_already.remove(ph)) return;
   else start_waits.add(ph);
}


private synchronized void noteProcessStarted(ProcessHandler ph) 
{
   if (start_waits.remove(ph)) {
      notifyAll();
    }
   else started_already.add(ph);
}


private synchronized void waitForStarted(ProcessHandler ph)
{
   if (started_already.contains(ph)) return;
   while (start_waits.contains(ph)) {
      try {
         wait(1000);
       }
      catch (InterruptedException e) { }
    }
}


/********************************************************************************/
/*                                                                              */
/*      DEBUGACTION command                                                     */
/*                                                                              */
/********************************************************************************/

void handleDebugAction(Project p,Module m,String launch,
      String target,String process,String thread,String frame,
      BubjetDebugAction action,IvyXmlWriter xw)
{
   DebugAction act = new DebugAction(launch,target,process,thread,frame,action,xw);
   act.start();
}



private class DebugAction extends BubjetAction.ReadDispatch {
   
   private String launch_name;
   private String target_name;
   private String process_name;
   private String thread_name;
   private String frame_name;
   private BubjetDebugAction the_action;
   private IvyXmlWriter xml_writer;
   
   DebugAction(String lname,String tgtname,String procname,String tname,
         String fname,BubjetDebugAction action,IvyXmlWriter xw) {
      launch_name = lname;
      target_name = tgtname;
      process_name = procname;
      thread_name = tname;
      frame_name = fname;
      the_action = action;
      xml_writer = xw;
    }
   
   @Override void process() {
      for (BubjetDebugProcess bdp : debug_processes) {
         if (!matchLaunch(launch_name,bdp)) continue;
         if (!matchTarget(target_name,bdp)) continue;
         if (!matchProcess(process_name,bdp)) continue;
         if (thread_name == null) {
            if (doAction(bdp)) {
               xml_writer.textElement("LAUNCH",the_action.toString());
             }
          }
         for (BThread thr : bdp.getThreads(thread_name)) {
            BStackFrame frm = null;
            if (frame_name != null) {
               frm = thr.getFrame(frame_name);
             }
            if (!thr.isSuspended() && the_action != BubjetDebugAction.SUSPEND) continue;
            if (doAction(bdp,thr,frm)) {
               xml_writer.textElement("THREAD",the_action.toString());
             }
          }
       }
    }
   
   private boolean doAction(BubjetDebugProcess bdp) {
      switch (the_action) {
         case NONE :
            break;
         case TERMINATE :
            if (bdp.getSession().isStopped()) return false;
            bdp.getSession().stop();
            break;
         default :
            return false;
       }
      
      return true;
    }
   
   private boolean doAction(BubjetDebugProcess bdp,BThread thr,BStackFrame frm) {
      try {
         switch (the_action) {
            case NONE :
               return false;
            case TERMINATE :
               return false;
            case RESUME :
               if (!thr.isSuspended()) return false;
               resumeThread(bdp,thr);
               thr.resume();
               break;
            case SUSPEND :
               if (thr.isSuspended()) return false;
               thr.suspend();
               break;
            case STEP_INTO :
               if (!thr.isSuspended()) return false;
               resumeThread(bdp,thr);
               bdp.getSession().stepInto();
               break;
            case STEP_OVER :
               if (!thr.isSuspended()) return false;
               resumeThread(bdp,thr);
               bdp.getSession().stepOver(false);
               break;
            case STEP_RETURN :
               if (!thr.isSuspended()) return false;
               resumeThread(bdp,thr);
               bdp.getSession().stepOut();
               break;
            case DROP_TO_FRAME :
               if (!thr.isSuspended()) return false;
               if (frm == null) {
                  frm = thr.getTopFrame();
                }
               if (frm == null) return false;
               thr.popFrames(frm);
               break;
          }
       }
      catch (Throwable t) {
         BubjetLog.logE("Problem doing debugger action",t);
         return false;
       }
      
      return true;
    }
   
   private void resumeThread(BubjetDebugProcess bdp,BThread thr) {
      thr.freeFrames();
      BubjetLog.logD("RESUME THREAD " + thr.getUniqueId() + " " + bdp.getId());
      generateThreadEvent("RESUME",bdp,thr,true,false,false);
   }
}



/********************************************************************************/
/*                                                                              */
/*      CONSOLEINPUT command                                                    */
/*                                                                              */
/********************************************************************************/

void handleConsoleInput(Project p,Module m,String launch,String input)
{
   ConsoleInputAction act = new ConsoleInputAction(launch,input);
   act.start();
}


private class ConsoleInputAction extends BubjetAction.Read {

   private String process_id;
   private String text_input;
   
   ConsoleInputAction(String lid,String text) {
      process_id = lid;
      text_input = text;
    }
   
   @Override void process() {
      for (BubjetDebugProcess bdp : getProcesses(process_id)) {
         bdp.getProcess().printToConsole(text_input);
       }
    }
   
}       // end of inner class ConsoleInputAction




/********************************************************************************/
/*                                                                              */
/*      Debugger event interface                                                */
/*                                                                              */
/********************************************************************************/

private void generateProcessEvent(String what,BubjetDebugProcess bdp)
{
   BubjetMonitor mon = app_service.getMonitor(for_project);
   IvyXmlWriter xw = mon.beginMessage("RUNEVENT");
   xw.field("TIME",System.currentTimeMillis());
   xw.begin("RUNEVENT");
   xw.field("TYPE","PROCESS");
   xw.field("KIND",what);
   BubjetUtil.outputProcess(bdp,xw,false);
   xw.end("RUNEVENT");
   BubjetLog.logD("RUNEVENT: " + xw.toString());
   
   if (what.equals("TERMINATE")) {
      // queueConsole(ph,null,false,true);
    }
   
   mon.finishMessageWait(xw);
}


private void generateThreadEvent(String what,BubjetDebugProcess bdp,BThread thrd,
      boolean forcerun,boolean forcedead,boolean eval)
{
   if (thrd == null) return;
   
   try {
      BubjetMonitor mon = app_service.getMonitor(for_project);
      IvyXmlWriter xw = mon.beginMessage("RUNEVENT");
      xw.field("TIME",System.currentTimeMillis());
      xw.begin("RUNEVENT");
      xw.field("TYPE","THREAD");
      xw.field("KIND",what);
      xw.field("DETAIL","NONE");
      xw.field("EVAL",eval);
      BubjetUtil.outputThread(bdp,thrd,null,forcerun,forcedead,xw);
      xw.end("RUNEVENT");
      BubjetLog.logD("RUNEVENT: " + xw.toString());
      mon.finishMessageWait(xw);
    }
   catch (Throwable t) {
      BubjetLog.logE("Problem processing thread event",t);
    }
}


private void generateContextEvent(String what,BubjetDebugProcess bpd,SuspendContext ctx)
{
   if (ctx == null) return ;
   
   SuspendContextImpl cimp = (SuspendContextImpl) ctx;
   
   // need to generate process event if this is for process rather than thread
   // might need to generate multiple RUNEVENTS
   
   BThread thrd = BubjetDebug.getThread(bpd,ctx.getThread());
   if (thrd == null) return;
   String detail = null;
   for (Event e : cimp.getEventSet()) {
      BubjetLog.logD("FOUND EVENT " + e);
      if (e instanceof BreakpointEvent) detail = "BREAKPOINT";
      else if (e instanceof StepEvent) {
         StepRequest sreq = (StepRequest) e.request();
         BubjetLog.logD("STEP " + sreq.depth() + " " + sreq.size());
         if (cimp.isResumed()) {
            if (sreq.depth() == StepRequest.STEP_INTO) detail = "STEP_INTO";
            else if (sreq.depth() == StepRequest.STEP_LINE) detail = "STEP_INTO";
            else if (sreq.depth() == StepRequest.STEP_OUT) detail = "STEP_RETURN";
            else if (sreq.depth() == StepRequest.STEP_OVER) detail = "STEP_OVER"; 
            else  detail = "STEP_INTO";
          }        
         else detail = "STEP_END";
       }
    }
   if (detail == null) {
      if (!cimp.isResumed() && cimp.isEvaluating()) detail = "EVALUATION";
      else if (thrd.isAtBreakpoint()) detail = "BREAKPOINT";
      else detail = "NONE";
    }
   
   if (ctx.getSuspendPolicy() == EventRequest.SUSPEND_ALL) {
      generateProcessEvent("CHANGE",bpd);
    }
   else {
      BubjetMonitor mon = app_service.getMonitor(for_project);
      IvyXmlWriter xw = mon.beginMessage("RUNEVENT");
      xw.field("TIME",System.currentTimeMillis());
      xw.begin("RUNEVENT");
      xw.field("TYPE","THREAD");
      xw.field("KIND",what);
      xw.field("DETAIL",detail);
      if (!cimp.isResumed() && cimp.isEvaluating()) xw.field("EVAL",true);
      BubjetUtil.outputThread(bpd,thrd,cimp,false,false,xw);
      xw.end("RUNEVENT");
      BubjetLog.logD("RUNEVENT: " + xw.toString());
      mon.finishMessageWait(xw);
    }
}




/********************************************************************************/
/*                                                                              */
/*      ExecutionListener interface                                             */
/*                                                                              */
/********************************************************************************/

@Override public void processStartScheduled(String id,ExecutionEnvironment env)
{
   BubjetLog.logD("mgr processStartScheduled " + id + " " + env);
}


@Override public void processStarting(String id,ExecutionEnvironment env)
{
   BubjetLog.logD("mgr processStarting " + id + " " + env);
}


@Override public void processNotStarted(String id,ExecutionEnvironment env)
{
   BubjetLog.logD("mgr processNotStarted " + id + " " + env);
}


public void processNotStarted(String id,ExecutionEnvironment env,Throwable cause)
{
   BubjetLog.logD("mgr processNotStarted " + id + " " + env + " " + cause);
}


@Override public void processStarting(String id,ExecutionEnvironment env,ProcessHandler hdlr)
{
   BubjetLog.logD("mgr processStarting1 " + id + " " + env + " " + hdlr.hashCode());
   BubjetDebugProcess dp = findProcess(id, hdlr,env);
   DebugListener dl = new DebugListener(dp);
   debug_listeners.put(dp,dl);
   DebuggerManager.getInstance(for_project).addDebugProcessListener(hdlr,dl);
   hdlr.addProcessListener(dl);
}


@Override public void processStarted(String id,ExecutionEnvironment env,ProcessHandler hdlr)
{
   BubjetLog.logD("mgr processStarted " + id + " " + hdlr.hashCode() + " " + hdlr.getUserDataString() + " " +
        env.getExecutionId() + " " + env.getRunnerAndConfigurationSettings().getConfiguration().getUniqueID() + " " +
        env.getRunProfile());
}


@Override public void processTerminating(String id,ExecutionEnvironment env,ProcessHandler hdlr)
{
   BubjetLog.logD("mgr processTerminating " + id + " " + env + " " + hdlr.hashCode());
   BubjetDebugProcess proc = findProcess(id,hdlr,env);
   proc.setTerminated();
}



@Override public void processTerminated(String id,ExecutionEnvironment env,ProcessHandler hdlr,int exitcode)
{
   BubjetDebugProcess proc = findProcess(id,hdlr,env);
   if (proc == null) return;
   
   DebugListener dl = debug_listeners.get(proc);
   if (dl != null) {
      DebuggerManager.getInstance(for_project).removeDebugProcessListener(hdlr,dl);
      if (proc.getProcess() != null) {
         proc.getProcess().removeDebugProcessListener(dl);
       }
    }
   
   BubjetLog.logD("mgr processTerminated " + id + " " + env + " " + hdlr.hashCode() + " " + exitcode);
   removeProcess(proc);
}



/********************************************************************************/
/*                                                                              */
/*      Java Debug Listener                                                     */
/*                                                                              */
/********************************************************************************/

private class DebugListener implements DebugProcessListener, ProcessListener {
   
   DebugListener(BubjetDebugProcess dp) { }
   
   @Override public void connectorIsReady() {
      BubjetLog.logD("mgr connectorIsReady");
    }
   
   @Override public void paused(SuspendContext ctx) {
      BubjetDebugProcess dp = findProcess(ctx.getDebugProcess());
      generateContextEvent("SUSPEND",dp,ctx);
      BubjetLog.logD("mgr paused " + ctx + " " + ctx.getDebugProcess() + " " +
            ctx.getThread() + " " + ctx.getFrameProxy());
    }
   
   
   @Override public void resumed(SuspendContext ctx) {
      BubjetDebugProcess dp = findProcess(ctx.getDebugProcess());
      generateContextEvent("RESUME",dp,ctx);
      BubjetLog.logD("mgr resumed " + ctx);
    }
   
   @Override public void processDetached(DebugProcess proc,boolean closedbyuser) {
      BubjetLog.logD("processDetached " + proc + " " + closedbyuser);
      BubjetDebugProcess dp = findProcess(proc);
      generateProcessEvent("CHANGE",dp);
    }
   
   @Override public void processAttached(DebugProcess proc) {
      BubjetLog.logD("mgr processAttached " + proc + " " + proc.getVirtualMachineProxy());
      
      BubjetDebugProcess dp = findProcess(proc);
      generateProcessEvent("CREATE",dp);
      noteProcessStarted(proc.getProcessHandler());
      dp.setStarted();
      
      VirtualMachineProxyImpl vmp = (VirtualMachineProxyImpl) proc.getVirtualMachineProxy();
      for (ThreadReference thr : vmp.getVirtualMachine().allThreads()) {
         threadStarted(proc,thr);
       }
      
      BubjetLog.logD("mgr end processAttached " + proc);
    }
   
   @Override public void attachException(RunProfileState state,ExecutionException exc,RemoteConnection conn) {
      BubjetLog.logD("mgr attachException " + state + " " + exc + " " + conn);
    }
   
   @Override public void threadStarted(DebugProcess proc,ThreadReference thrdref) { 
      BubjetDebugProcess dp = findProcess(proc);
      BThread thrd = BubjetDebug.getThread(dp,thrdref);
      BubjetLog.logD("mgr threadStarted " + proc + " " + thrd);
      thrd.freeFrames();
      if (dp.addThread(thrd)) {
         generateThreadEvent("CREATE",dp,thrd,true,false,false);
       }
      else {
         BubjetLog.logD("RESUME EVENT FOUND");
         generateThreadEvent("RESUME",dp,thrd,false,false,false);
       }
    }
   
   @Override public void threadStopped(DebugProcess proc,ThreadReference thrdref) {
      BubjetDebugProcess dp = findProcess(proc);
      BThread thrd = BubjetDebug.getThread(dp,thrdref);
      BubjetLog.logD("mgr threadStopped " + proc + " " + thrd);
      if (dp.removeThread(thrd)) {
         generateThreadEvent("TERMINATE",dp,thrd,false,true,false);
       }
    }
   
   @Override public void startNotified(ProcessEvent evt) {
      BubjetLog.logD("mgr startNotified " + evt);
    }
   
   @Override public void processTerminated(ProcessEvent evt) {
      BubjetLog.logD("mgr processTerminated p " + evt);
    }
   
   @Override public void processWillTerminate(ProcessEvent evt,boolean destroy) {
      BubjetLog.logD("mgr processsWillTerminate p " + evt + " " + destroy);
      ProcessHandler hdlr = evt.getProcessHandler();
      BubjetDebugProcess proc = findProcess(null,hdlr,null);
      if (proc != null) {
         try {
            for (BThread thrd : proc.getThreads()) {
               if (proc.removeThread(thrd)) {
                  generateThreadEvent("TERMINATE",proc,thrd,false,true,false);
                }
             }
          }
         catch (Throwable t) {
            BubjetLog.logE("Problem stopping threads",t);
          }
         generateProcessEvent("TERMINATE",proc);
       }
    }
   
   @SuppressWarnings("rawtypes")
   @Override public void onTextAvailable(ProcessEvent evt,Key outputtype) {
      BubjetLog.logD("mgr onTextAvailable " + evt.getSource() + " " + outputtype);
      BubjetLog.logD("TEXT: " + evt.getText());
      BubjetDebugProcess proc = findProcess(null,evt.getProcessHandler(),null);
      BubjetOutputType bot = BubjetOutputType.STDOUT;
      if (outputtype == ProcessOutputType.STDERR) bot = BubjetOutputType.STDERR;
      else if (outputtype == ProcessOutputType.SYSTEM) bot = BubjetOutputType.SYSTEM;
      
      if (proc != null) {
         queueConsole(proc,evt.getText(),bot);
       }
    }

}      // end of inner class DebugListener

  
   
/********************************************************************************/
/*                                                                              */
/*      Debugger manager listener                                               */
/*                                                                              */
/********************************************************************************/
   
@Override public void processStarted(XDebugProcess dp) 
{
   BubjetLog.logD("mgr XD processStarted");
   BubjetDebugProcess bdp = findProcess(dp);
   BubjetLog.logD("FOUND PROCESS " + bdp);
}

@Override public void processStopped(XDebugProcess dp) 
{
   BubjetLog.logD("mgr XD processStopped");
   BubjetDebugProcess bdp = findProcess(dp);
   BubjetLog.logD("FOUND PROCESS " + bdp);
}


@Override public void currentSessionChanged(XDebugSession prev,XDebugSession cur) 
{
   BubjetLog.logD("mgr XD currentSessionChanged");
}




/********************************************************************************/
/*                                                                              */
/*      Console management                                                      */
/*                                                                              */
/********************************************************************************/

private void queueConsole(BubjetDebugProcess pid,String txt,BubjetOutputType typ)
{
   synchronized (console_map) {
      ConsoleThread ct = console_threads.get(pid.getProject());
      if (ct == null) {
	 ct = new ConsoleThread(pid.getProject());
         console_threads.put(pid.getProject(),ct);
	 ct.start();
       }
      
      boolean eof = (txt == null);
      
      ConsoleData cd = console_map.get(pid);
      if (cd != null) {
	 BubjetLog.logD("Console append " + pid.getId() + " " + txt.length());
	 cd.addWrite(txt,typ,eof);
       }
      else {
	 BubjetLog.logD("Console newapp " + pid.getId() + " " + (txt == null ? 0 : txt.length()));
	 cd = new ConsoleData();
	 cd.addWrite(txt,typ,eof);
	 console_map.put(pid,cd);
	 console_map.notifyAll();
       }
    }
}



private class ConsoleData {

   private List<ConsoleWrite> pending_writes;
   
   ConsoleData() {
      pending_writes = new ArrayList<ConsoleWrite>();
    }
   
   synchronized void addWrite(String txt,BubjetOutputType bot,boolean eof) {
      pending_writes.add(new ConsoleWrite(txt,bot,eof));
    }
   
   List<ConsoleWrite> getWrites()		{ return pending_writes; }
   
}	// end of inner class ConsoleData


private static class ConsoleWrite {
   
   private String write_text;
   private BubjetOutputType output_type;
   private boolean is_eof;
   
   ConsoleWrite(String txt,BubjetOutputType bot,boolean eof) {
      write_text = txt;
      output_type = bot;
      is_eof = eof;
    }
   
   String getText()			{ return write_text; }
   BubjetOutputType getOutputType()     { return output_type; }
   boolean isEof()			{ return is_eof; }

}	// end of inner class ConsoleWrite


private class ConsoleThread extends Thread {
   
   private BubjetMonitor our_monitor;
   
   ConsoleThread(Project p) {
      super("BubjetConsoleMonitor");
      our_monitor = app_service.getMonitor(p);
    }
   
   @Override public void run() {
      for ( ; ; ) {
         try {
            ConsoleData cd = null;
            BubjetDebugProcess pid = null;
            synchronized (console_map) {
               while (console_map.isEmpty()) {
                  try {
                     console_map.wait(10000);
                   }
                  catch (InterruptedException e) { }
                }
               for (Iterator<Map.Entry<BubjetDebugProcess,ConsoleData>> it = console_map.entrySet().iterator(); 
                     it.hasNext(); ) {
                  Map.Entry<BubjetDebugProcess,ConsoleData> ent = it.next();
                  pid = ent.getKey();
                  if (!pid.isStarted()) continue;
                  cd = ent.getValue();
                  BubjetLog.logD("Console thread data " + pid + " " + cd);
                  it.remove();
                  if (cd != null) break;
                }
             }
            if (cd != null) processConsoleData(pid,cd);
            else if (!console_map.isEmpty()) {
               synchronized (console_map) {
                  try {
                     console_map.wait(100);
                   }
                  catch (InterruptedException e) { }
                }
             }
          }
         catch (Throwable t) {
            BubjetLog.logE("Problem with console thread: " + t,t);
          }
       }
    }
   
   private void processConsoleData(BubjetDebugProcess pid,ConsoleData cd) {
      StringBuffer buf = null;
      BubjetOutputType bot = BubjetOutputType.STDOUT;
      for (ConsoleWrite cw : cd.getWrites()) {
         if (cw.isEof()) {
            if (buf != null) flushConsole(pid,buf,bot);
            buf = null;
            eofConsole(pid);
            continue;
          }
         if (buf == null) {
            if (cw.getText() != null) {
               buf = new StringBuffer();
               bot = cw.getOutputType();
               buf.append(cw.getText());
             }
          }
         else if (bot == cw.getOutputType()) {
            if (cw.getText() != null) buf.append(cw.getText());
          }
         else {
            flushConsole(pid,buf,bot);
            buf = null;
            if (cw.getText() != null) {
               buf = new StringBuffer();
               bot = cw.getOutputType();
               buf.append(cw.getText());
             }
          }
         if (buf != null && buf.length() > 32768) {
            flushConsole(pid,buf,bot);
            buf = null;
          }
       }
      if (buf != null) flushConsole(pid,buf,bot);
    }
   
   private void flushConsole(BubjetDebugProcess pid,StringBuffer buf,BubjetOutputType bot) {
      IvyXmlWriter xw = our_monitor.beginMessage("CONSOLE");
      xw.field("PID",pid.getId());
      if (bot == BubjetOutputType.STDERR) xw.field("STDERR",true);
      else if (bot == BubjetOutputType.SYSTEM) xw.field("SYSTEM",true);
      //TODO: fix this correctly
      String txt = buf.toString();
      // txt = txt.replace("]]>","] ]>");
      txt = txt.replace("\010"," ");
      if (txt.length() == 0) return;
      xw.cdataElement("TEXT",txt);
      our_monitor.finishMessageWait(xw);
      BubjetLog.logD("Console write " + txt.length());
    }
   
   private void eofConsole(BubjetDebugProcess pid) {
      IvyXmlWriter xw = our_monitor.beginMessage("CONSOLE");
      xw.field("PID",pid.getId());
      xw.field("EOF",true);
      our_monitor.finishMessageWait(xw);
    }
   
}	// end of innerclass ConsoleThread


}       // end of class BubjetExecutionManager




/* end of BubjetExecutionManager.java */

