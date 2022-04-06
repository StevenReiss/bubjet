/********************************************************************************/
/*                                                                              */
/*              BubjetDebugProcess.java                                         */
/*                                                                              */
/*      description of class                                                    */
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaExecutionStack;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.SuspendManager;
import com.intellij.debugger.engine.managerThread.DebuggerCommand;
import com.intellij.debugger.engine.managerThread.DebuggerManagerThread;
import com.intellij.debugger.engine.managerThread.SuspendContextCommand;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.frame.XValue;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

import edu.brown.cs.bubbles.bubjet.BubjetDebug.BStackFrame;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BValue;

import static edu.brown.cs.bubbles.bubjet.BubjetDebug.BThread;

class BubjetDebugProcess implements BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private ProcessHandler  process_handler;
private DebugProcess    debug_process;
private XDebugProcess   xdebug_process;
private ExecutionEnvironment execution_environment;
private String          process_name;
private Set<BThread>    all_threads;
private Project         for_project;
private boolean         is_started;
private boolean         is_terminated;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetDebugProcess(Project p,String id,ProcessHandler ph,ExecutionEnvironment ev)
{
   this(p);
   process_handler = ph;
   execution_environment = ev;
   process_name = id;
}


BubjetDebugProcess(Project p,DebugProcess dp)
{
   this(p);
   process_handler = dp.getProcessHandler();
   debug_process = dp;
}


BubjetDebugProcess(Project p,XDebugProcess dp)
{
   this(p);
   process_handler = dp.getProcessHandler();
   xdebug_process = dp;
}


private BubjetDebugProcess(Project p)
{
   process_handler = null;
   execution_environment = null;
   debug_process = null;
   xdebug_process = null;
   all_threads = new HashSet<>();
   for_project = p;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

ProcessHandler getHandler()             { return process_handler; }
ExecutionEnvironment getEnvironment()   { return execution_environment; }
DebugProcess getProcess()               { return debug_process; }
boolean isStarted()                     { return is_started; }

boolean isTerminated() {
   if (!is_terminated) is_terminated = process_handler.isProcessTerminated();
   return is_terminated;
}

void setStarted()                       { is_started = true; }
void setTerminated()                    { is_terminated = true; }

XDebugProcess getXDebugProcess()        { return xdebug_process; }


XSuspendContext getSuspendContext()
{
   if (xdebug_process.getSession() == null) return null;
   return xdebug_process.getSession().getSuspendContext();
}


VirtualMachine getVirtualMachine()
{
   VirtualMachineProxyImpl vmpi = (VirtualMachineProxyImpl) debug_process.getVirtualMachineProxy();
   return vmpi.getVirtualMachine();
}


DebuggerContextImpl getDebuggerContext()
{
   DebugProcessImpl dpi = (DebugProcessImpl) debug_process;
   return dpi.getDebuggerContext();
}


SuspendManager getSuspendManager()
{
   DebugProcessImpl dpi = (DebugProcessImpl) debug_process;
   return dpi.getSuspendManager();
}

XDebugSession getSession()
{
   return xdebug_process.getSession();
}


String getId()                          
{ 
   return String.valueOf(process_handler.hashCode());
}

String getLaunchId()
{
   return String.valueOf(execution_environment.getExecutionId());
}

String getLaunchName()
{
   return execution_environment.getExecutionTarget().getDisplayName();
}

RunnerAndConfigurationSettings getLaunchConfiguration()
{
   return execution_environment.getRunnerAndConfigurationSettings();
}

Project getProject()                                    { return for_project; }

void setEnvironment(String id,ExecutionEnvironment ev) 
{
   if (process_name == null) process_name = id;
   if (execution_environment == null) execution_environment = ev;
}

void setProcess(DebugProcess dp)
{
   if (debug_process == null) debug_process = dp;
}

void setXDebugProcess(XDebugProcess dp)
{
   if (xdebug_process == null) xdebug_process = dp;
}


boolean addThread(BThread thrd)
{
   return all_threads.add(thrd);
}

boolean removeThread(BThread thrd)
{
   return all_threads.remove(thrd);
}


List<BThread> getThreads()              { return getThreads(null); }

List<BThread> getThreads(String match) 
{
   List<BThread> rslt = new ArrayList<>();
   for (BThread tr : all_threads) {
      if (tr.match(match)) rslt.add(tr);
    }
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Evaluation methods                                                      */
/*                                                                              */
/********************************************************************************/

boolean invoke(DebuggerCommand cmd,BThread evalthread,boolean ext) throws BubjetException
{
   BubjetApplicationService app = BubjetBundle.getAppService();
   BubjetExecutionManager mgr = app.getExecutionManager(for_project);
   if (evalthread != null && ext) mgr.startEvaluation(evalthread);
   else if (evalthread != null) evalthread.saveFrames();
   
   BubjetLog.logD("DEBUGGER COMMAND " + cmd + " " + evalthread);
// debug_process.getManagerThread().invokeCommand(cmd);
   BDebugCommand lclcmd = null;
   XSuspendContext xsc = getSuspendContext();
   if (xsc != null && xsc instanceof SuspendContext || cmd instanceof SuspendContextCommand) 
      lclcmd = new BDebugCtxCommand(cmd);
   else 
      lclcmd = new BDebugCommand(cmd);
   DebuggerManagerThread dt = debug_process.getManagerThread();
   dt.invokeCommand(lclcmd);
   boolean rslt = lclcmd.waitForResult();
   
   BubjetLog.logD("DONE DEBUGGER COMMAND");
   
   if (evalthread != null && ext) mgr.endEvaluation(evalthread);
   
   return rslt;
}


private class BDebugCommand implements DebuggerCommand {
   
   protected DebuggerCommand for_command;
   private Boolean is_done;
   private BubjetException command_error;
   
   BDebugCommand(DebuggerCommand cmd) {
      for_command = cmd;
      is_done = null;
      command_error = null;
    }
   
   synchronized boolean waitForResult() throws BubjetException {
      while (is_done == null) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
      if (command_error != null) throw command_error;
      return is_done;
    }
   
   @Override public void action() {
      try {
         for_command.action();
       }
      catch (InvalidStackFrameException e) {
         is_done = false;
       }
      catch (Throwable t) {
         if (t instanceof BubjetException) {
            command_error = (BubjetException) t;
          }
         
       }
      synchronized (this) {
         if (is_done == null) is_done = true;
         notifyAll();
       }
    }
   
   @Override public void commandCancelled() {
      for_command.commandCancelled(); 
      synchronized (this) {
         is_done = false;
         notifyAll();
       }
    }
   
}       // end of BDebugCommand

private class BDebugCtxCommand extends BDebugCommand implements SuspendContextCommand {

   BDebugCtxCommand(DebuggerCommand cmd) {
      super(cmd);
    }
   
   @Override public SuspendContext getSuspendContext() {
      if (for_command instanceof SuspendContextCommand) {
         return ((SuspendContextCommand)for_command).getSuspendContext();
       }
      else {
         XSuspendContext ctx = BubjetDebugProcess.this.getSuspendContext();
         if (ctx instanceof SuspendContext) return (SuspendContext) ctx;
       }
      return null;
    }
   
}       // end of BDebugCtxCommand




boolean evaluate(String expr,BThread thrd,BStackFrame frm,EvalCallback cbk) throws BubjetException
{
   EvaluateCommand ecmd = new EvaluateCommand(expr,thrd,frm,cbk);
   return invoke(ecmd,thrd,true);
}


private class EvaluateCommand implements DebuggerCommand {
   
   private String eval_expression;
   private BThread use_thread;
   private BStackFrame use_frame;
   private EvalCallback eval_callback;
   
   EvaluateCommand(String expr,BThread thrd,BStackFrame frm,EvalCallback cbk) {
      eval_expression = expr;
      use_thread = thrd;
      use_frame = frm;
      eval_callback = cbk;
    }
   
   @Override public void commandCancelled() {
      BubjetLog.logD("EVALUATE CANCELLED");
    }
   
   @Override public void action() {
      BubjetLog.logD("START EVALUATE");
      XEvalCallback cb = new XEvalCallback(eval_callback);
      XExecutionStack actstk = getExecutionStack(use_thread);
      XStackFrame actfrm = getExecFrame(actstk,use_frame);
      XDebuggerEvaluator eval = actfrm.getEvaluator();
      eval.evaluate(eval_expression,cb,null);
      BubjetLog.logD("END EVALUATE");
    }
   
}       // end of inner class EvaluateCommand



private XExecutionStack getExecutionStack(BThread thrd) 
{
   XSuspendContext ctx = xdebug_process.getSession().getSuspendContext();
   XExecutionStack stk = ctx.getActiveExecutionStack();
   if (isStackForThread(stk,thrd)) return stk;
   XAllStacks allstk = new XAllStacks();
   ctx.computeExecutionStacks(allstk);
   List<XExecutionStack> stks = allstk.getStacks();
   if (stks != null) {
      for (XExecutionStack stk1 : stks) {
         if (isStackForThread(stk1,thrd)) return stk1;
       }
    }
   else {
      BubjetLog.logD("Problem getting stack: " + allstk.getError());
    }
   return null;
}


private boolean isStackForThread(XExecutionStack stk,BThread thrd)
{
   try {
      if (stk instanceof JavaExecutionStack) {
         JavaStackFrame frm =  (JavaStackFrame) stk.getTopFrame();
         BStackFrame frm0 = BubjetDebug.getStackFrame(this,frm.getStackFrameProxy().getStackFrame());
         if (frm0 == thrd.getTopFrame()) return true;
       }
    }
   catch (Throwable t) { 
      BubjetLog.logE("Problem checking stack for thread",t);
    }
   
   return false;
}


private XStackFrame getExecFrame(XExecutionStack stk,BStackFrame frm) 
{
   try {
      XStackFrame xsf = stk.getTopFrame();
      if (isProperExecFrame(xsf,frm)) return xsf;
      XAllFrames allfrm = new XAllFrames();
      stk.computeStackFrames(0,allfrm);
      List<XStackFrame> frms = allfrm.getFrames();
      if (frms != null) {
         for (XStackFrame xsf1 : frms) {
            if (isProperExecFrame(xsf1,frm)) return xsf1;
          }
       }
      else {
         BubjetLog.logD("Problem getting frame: " + allfrm.getError());
       }
    }
   catch (Throwable t) { 
      BubjetLog.logE("Problem getting execution frame",t);
    }
   return null;
}



private boolean isProperExecFrame(XStackFrame frm,BStackFrame bfrm)
{
   try {
      if (frm instanceof JavaStackFrame) {
         JavaStackFrame jsf = (JavaStackFrame) frm;
         StackFrame sf = jsf.getStackFrameProxy().getStackFrame();
         BStackFrame bsf = BubjetDebug.getStackFrame(this,sf);
         return bfrm == null || bfrm == bsf;
       }
    }
   catch (Throwable t) { }
   return false;
}

private class XAllStacks implements XSuspendContext.XExecutionStackContainer {
   
   private List<XExecutionStack> result_stacks;
   private boolean is_done;
   private String stack_error;
   
   XAllStacks() {
      result_stacks = new ArrayList<>();
      is_done = false;
      stack_error = null;
    }
   
   synchronized List<XExecutionStack> getStacks() {
      while (!is_done) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
      return result_stacks;
    }
   
   String getError()                            { return stack_error; }
   
   @Override public synchronized void addExecutionStack(List<? extends XExecutionStack> stks,
         boolean last) {
      if (result_stacks != null) result_stacks.addAll(stks);
      if (last) {
         is_done = true;
         notifyAll();
       }
    }
   
   @Override public synchronized void errorOccurred(String msg) {
      stack_error = msg;
      result_stacks = null;
      is_done = true;
      notifyAll();
    }
   
}       // end of inner class XAllStacks


private class XAllFrames implements XExecutionStack.XStackFrameContainer {
   
   private List<XStackFrame> result_frames;
   private boolean is_done;
   private String frame_error;
   
   XAllFrames() {
      result_frames = new ArrayList<>();
      is_done = false;
      frame_error = null;
    }
   
   synchronized List<XStackFrame> getFrames() {
      while (!is_done) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
      return result_frames;
    }
   
   String getError()                            { return frame_error; }
   
   @Override public synchronized void addStackFrames(List<? extends XStackFrame> frms,
         boolean last) { 
      if (result_frames != null) result_frames.addAll(frms);
      if (last) {
         is_done = true;
         notifyAll();
       }
    }
   
   @Override public void errorOccurred(String msg) { 
      frame_error = msg;
      result_frames = null;
      is_done = true;
      notifyAll();
    }
   
}       // end of inner class XAllFrames

private class XEvalCallback implements XDebuggerEvaluator.XEvaluationCallback
{
   private EvalCallback bubjet_callback;
   
   XEvalCallback(EvalCallback cb) {
      bubjet_callback = cb;
    }
   
   @Override public void evaluated(XValue rslt) { 
       BubjetLog.logD("EVALUTION RESULT " + rslt);
       if (rslt instanceof JavaValue) {
          JavaValue jv = (JavaValue) rslt;
          Value v = jv.getDescriptor().getValue();
          BValue bv = BubjetDebug.getValue(BubjetDebugProcess.this,v);
          bubjet_callback.evaluated(bv);
        }
       else { 
          bubjet_callback.evaluated(null);
        }
    }
   
   @Override public void errorOccurred(String msg) { 
      BubjetLog.logD("EVALUATION ERROR " + msg);
      bubjet_callback.evaluationError(msg);
    }
   
}







}       // end of class BubjetDebugProcess




/* end of BubjetDebugProcess.java */

