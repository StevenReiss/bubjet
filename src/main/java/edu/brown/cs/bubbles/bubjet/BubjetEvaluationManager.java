/********************************************************************************/
/*                                                                              */
/*              BubjetEvaluationManager.java                                    */
/*                                                                              */
/*      Handle evaluation requests of various types                             */
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.intellij.debugger.engine.managerThread.DebuggerCommand;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.sun.jdi.InvalidStackFrameException;

import edu.brown.cs.bubbles.bubjet.BubjetDebug.BArray;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BObject;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BStackFrame;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BThread;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BType;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BValue;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BVariable;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BubjetEvaluationManager implements BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService        app_service;
private BubjetExecutionManager          exec_manager;
private Project                         for_project;
private Map<BStackFrame,Map<String,BValue>> outside_variables;

private static Set<BThread>     variable_threads;
private static Map<String,CallFormatter> format_map;


static {
   variable_threads = new HashSet<>();
   format_map = new HashMap<>();
   CallFormatter xmlfmt = new CallFormatter("edu.brown.cs.ivy.xml.IvyXml.convertXmlToString",
	 "(Lorg/w3c/dom/Node;)Ljava/lang/String;",null);
   format_map.put("org.apache.xerces.dom.DeferredElementImpl",xmlfmt);
   format_map.put("org.apache.xerces.dom.DeferredElementNSImpl",xmlfmt);
   format_map.put("org.apache.xerces.dom.ElementImpl",xmlfmt);
   format_map.put("org.apache.xerces.dom.ElementNSImpl",xmlfmt);
   format_map.put("com.sun.apache.xerces.internal.dom.DeferredElementImpl",xmlfmt);
   format_map.put("com.sun.apache.xerces.internal.dom.DeferredElementNSImpl",xmlfmt);
   format_map.put("com.sun.org.apache.xerces.internal.dom.DeferredElementImpl",xmlfmt);
   format_map.put("com.sun.org.apache.xerces.internal.dom.DeferredElementNSImpl",xmlfmt);
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetEvaluationManager(BubjetApplicationService app,Project p) 
{
   app_service = app;
   exec_manager = app_service.getExecutionManager(p);
   for_project = p;
   outside_variables = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      GETSTACKFRAMES command                                                  */
/*                                                                              */
/********************************************************************************/

void handleGetStackFrames(Project p,Module m,String lname,String tname,
      int count,int depth,int array,IvyXmlWriter xw)
{
   GetStackFramesAction act = new GetStackFramesAction(lname,tname,count,depth,array,xw);
   act.start();
}


private class GetStackFramesAction extends BubjetAction.Read {

private String launch_name;
private String thread_name;
private int dump_count;
private int var_depth;
private int array_size;
private IvyXmlWriter xml_writer;

GetStackFramesAction(String lname,String tname,int cnt,int depth,int array,IvyXmlWriter xw) {
   launch_name = lname;
   thread_name = tname;
   dump_count = cnt;
   var_depth = depth;
   array_size = array;
   xml_writer = xw;
}

@Override void process() {
   xml_writer.begin("STACKFRAMES");
   
   try {
      for (BubjetDebugProcess proc : exec_manager.getProcesses(launch_name)) {
         if (proc.isTerminated()) continue;
         try {
            BubjetLog.logD("GET FRAMES FOR PROCESS " + proc.getId());
            for (BThread thr : proc.getThreads(thread_name)) {
               BubjetLog.logD("WORK ON THREAD " + thr.hashCode() + " " + thr.getUniqueId() + " " +
                     thr.getProcess().getId());
               if (thr.isSuspended()) {
                  dumpFrames(proc,thr,dump_count,
                        var_depth,array_size,xml_writer); 
                }
               else {
                  BubjetLog.logD("THREAD NOT SUSPENDED");
                }
             }
          }
         catch (Throwable t) {
            BubjetLog.logD("Problem dumping frames",t);
          }
       }
    }
   catch (Throwable e) { 
       BubjetLog.logE("Problem getting stack frames",e);
    }
   
   xml_writer.end("STACKFRAMES");
}

}       // end of inner class GetStackFramesAction





private void dumpFrames(BubjetDebugProcess proc,BThread thrd,
      int count,int vdepth,int arraysz,IvyXmlWriter xw)
{
   xw.begin("THREAD");
   xw.field("ID",thrd.getUniqueId());
   xw.field("NAME",thrd.getName());
   // TAG???
   int ctr = 0;
   try {
      thrd.saveFrames();
      for (BStackFrame frm : thrd.getFrames()) {
         BubjetLog.logD("Outputing stack frame " + frm);
         BubjetUtil.outputStackFrame(proc,frm,ctr,vdepth,arraysz,xw);
         if (count > 0 && ctr > count) break;
         ++ctr;
       }
      if (thrd.isSuspended()) {
         List<BObject> ors = thrd.getOwnedMonitors();
         if (ors != null && ors.size() > 0) {
            xw.begin("OWNS");
            for (BObject or : ors) {
               BubjetUtil.outputValue(proc,or,null,null,0,arraysz,xw);
             }
            xw.end("OWNS");
          }
       }
    }
   catch (Throwable t) {
      BubjetLog.logE("Problem dumping frames",t);
    }
   
   xw.end("THREAD");
}


/********************************************************************************/
/*                                                                              */
/*      VARVAL command                                                          */
/*                                                                              */
/********************************************************************************/

void handleVarVal(Project p,Module m,String thread,String frame,String var,
      int depth,int array,IvyXmlWriter xw) throws BubjetException
{
   VarValAction act = new VarValAction(thread,frame);
   act.start();
   BubjetDebugProcess proc = act.getDebugProcess();
   if (proc == null) return;
   
   VarValCommand vvc = new VarValCommand(proc,act.getThread(),act.getStackFrame(),
         var,depth,array,xw);
   BubjetLog.logD("BEGIN VARVAL COMMAND " + act.getThread());
   proc.invoke(vvc,act.getThread(),false);
   vvc.checkError();
}



private class VarValAction extends BubjetAction.Read {

   private String thread_name;
   private String frame_name;
   private BubjetDebugProcess use_process;
   private BThread use_thread;
   private BStackFrame use_frame;
   
   VarValAction(String thr,String frm) {
      thread_name = thr;
      frame_name = frm;
      use_thread = null;
      use_frame = null;
      use_process = null;
    }
   
   BubjetDebugProcess getDebugProcess()         { return use_process; }
   BThread getThread()                          { return use_thread; }
   BStackFrame getStackFrame()                  { return use_frame; }
   
   @Override void process() throws BubjetException {
      BubjetLog.logD("START VARVAL");
      
      for (BubjetDebugProcess proc : exec_manager.getProcesses()) {
         for (BThread thr : proc.getThreads()) {
            if (thr.match(thread_name)) {
               if (thr.isSuspended()) {
                  use_thread = thr;
                  break;
                }
               else if (thread_name != null) {
                  BubjetLog.logI("Thread " + thread_name + " not suspended");
                }
             }
          }
         if (use_thread == null) continue;
         for (BStackFrame frm : use_thread.getFrames(frame_name)) {
            use_frame = frm;
            break;
          }
         if (use_frame == null) {
            BubjetLog.logI("Stack frame " + frame_name + " not accessible");
            continue;
          }
         
         use_process = proc;
         break;
       }
    }
   
   
}       // end of inner class VarValAction




private class VarValCommand implements DebuggerCommand {
   
   private int value_depth;
   private int array_size;
   private BubjetDebugProcess use_process;
   private BThread use_thread;
   private BStackFrame use_frame;
   private String var_name;
   private IvyXmlWriter xml_writer;
   private String error_text;
   
   
   VarValCommand(BubjetDebugProcess proc,BThread thread,BStackFrame frame,String var,
         int depth,int array,IvyXmlWriter xw) {
      var_name = var;
      use_process = proc;
      use_thread = thread;
      use_frame = frame;
      value_depth = depth;
      array_size = array;
      xml_writer = xw;
      error_text = null;
    }
   
   void checkError() throws BubjetException {
      if (error_text != null) throw new BubjetException(error_text);
    }
   
   private void setError(String msg) {
      error_text = msg;
      BubjetLog.logD("VARVAL ERROR " + msg);
    }
   
   @Override public void commandCancelled() {
      BubjetLog.logD("VARVAL COMMAND cancelled");
    }
   
   @Override public void action() {
      BubjetLog.logD("VARVAL COMMAND " + var_name);
      StringTokenizer tok = new StringTokenizer(var_name,"?");
      if (!tok.hasMoreTokens()) {
         setError("No variable specified");
         return;
       }
      
      try {
         String vhead = tok.nextToken();
         BValue val = null;
         BVariable var = null;
         if (vhead.startsWith("*")) {
            synchronized (outside_variables) {
               Map<String,BValue> vals = outside_variables.get(use_frame);
               BubjetLog.logD("VAR FIND " + vhead + " " + vals);
               if (vals != null) {
                  val = vals.get(vhead);
                  if (val == null) val = vals.get(vhead.substring(1));
                }
             }
            if (val == null) {
               BubjetLog.logD("VAR FAIL " + vhead + " " + use_frame.hashCode() + " " + 
                     use_thread.hashCode() + " " + var_name);
               setError("Save variable " + vhead + " not found");
               return;
             }
          }
         else {
            var = use_frame.getStackVariable(vhead);
            val = use_frame.getVariableValue(var);
          }
         
         while (tok.hasMoreTokens()) {
            boolean found = false;
            var = null;
            String next = tok.nextToken();
            if (next.startsWith("@")) {
               if (next.equals("@hashCode")) {
                  List<BValue> args = new ArrayList<>();
                  args.add(val);
                  startThreadEval(use_thread);
                  try {
                     val = use_thread.invokeStaticMethod("java.lang.System",
                           "identityHashCode",
                           "(Ljava/lang/Object;)I",args);
                   }
                  catch (Throwable t) {
                     BubjetLog.logE("Problem getting system hash code",t);
                     setError("Problem getting hash code");
                     return;
                   }
                  finally {
                     endThreadEval(use_thread);
                   }
                  
                }
               continue;
             }
            
            if (val instanceof BObject) {
               BObject obj = (BObject) val;
               val = obj.getFieldValue(next);
               if (val != null) {
                  found = true;
                  BubjetLog.logD("VAR FOUND " + next + " " + val);
                }
             }
            if (!found && val instanceof BArray) {
               BArray arr = (BArray) val;
               int idx0 = next.indexOf("[");
               if (idx0 >= 0) next = next.substring(idx0+1);
               idx0 = next.indexOf("]");
               if (idx0 >= 0) next = next.substring(0,idx0);
               try {
                  int sub = Integer.parseInt(next);
                  val = arr.getIndexValue(sub);
                  found = true;
                }
               catch (NumberFormatException e) {
                  setError("Index expected");
                  return;
                }
             }
            
            if (!found) {
               val = null;
               break;
             }
          }
         
         if (val == null || tok.hasMoreTokens()) {
            setError("Variable doesn't exists: " + var_name);
            return;
          }
         
         outputValue(val,var);
       }
      catch (InvalidStackFrameException e) {
         
       }
    }
   
   private void outputValue(BValue val,BVariable var) {
      BubjetLog.logD("OUTPUT VALUE " + val + " " + var);
      if (value_depth < 0) {
         boolean fnd = false;
         if (val instanceof BArray) {
            try {
               startThreadEval(use_thread);
               BArray avl = (BArray) val;
               String tsg = val.getValueType().getSignature();
               if (tsg.startsWith("[[") || tsg.contains(";")) tsg = "[Ljava/lang/Object;";
               else tsg = "(" + tsg + ")Ljava/lang/String;";
               List<BValue> args = new ArrayList<>();
               args.add(avl);
               val = use_thread.invokeStaticMethod("java.util.Arrays","toString",tsg,args);
               fnd = true;
             }
            catch (Throwable t) {
               BubjetLog.logE("Problem getting arrays.toString",t);
             }
            finally {
               endThreadEval(use_thread);
             }
          }
         if (!fnd) {
            val = handleSpecialCases(use_process,val,use_thread);
          }
       }
      
      BubjetUtil.outputValue(use_process,val,var,var_name,value_depth,array_size,xml_writer);
    }
   
}       // end of inner class VarValCommand




/********************************************************************************/
/*                                                                              */
/*      EVALUATE command                                                        */
/*                                                                              */
/********************************************************************************/

void handleEvaluate(Project p,Module m,String bid,String expr,String thread,String frame,
      boolean implicit,boolean allowbreak,String eid,int level,int array,
      String saveid,boolean allframes,IvyXmlWriter xw)
{
   EvaluateAction act = new EvaluateAction(expr,thread,frame,implicit,
         allowbreak,allframes,xw);
   act.start();
  
   BubjetDebugProcess proc = act.getDebugProcess();
   if (proc == null) return;
   
   DoEvaluate doev = new DoEvaluate(bid,expr,proc,act.getThread(),act.getStackFrame(),
         eid,saveid,level,array);
   doev.start();
}



private class EvaluateAction extends BubjetAction.Read {

   private BubjetDebugProcess use_process;
   private String thread_name;
   private BThread use_thread;
   private String frame_name;
   private BStackFrame use_frame;
   
   EvaluateAction(String expr,String thread,String frame,
         boolean implicit,boolean allowbreak,
         boolean allframes,IvyXmlWriter xw) {
      use_process = null;
      thread_name = thread;
      use_thread = null;
      frame_name = frame;
      use_frame = null;
    }
   
   BubjetDebugProcess getDebugProcess()                 { return use_process; }
   BThread getThread()                                  { return use_thread; }
   BStackFrame getStackFrame()                          { return use_frame; }
   
   @Override void process() throws BubjetException {
      use_thread = null;
      use_frame = null;
      
      for (BubjetDebugProcess proc :  exec_manager.getProcesses()) {
         for (BThread thr : proc.getThreads(thread_name)) {
            if (thr.isSuspended()) {
               use_thread = thr;
               break;
             }
            else if (thread_name != null) {
               throw new BubjetException("Thread " + thread_name + " not suspended");
             }
          }
         if (use_thread == null) continue;
         for (BStackFrame frm : use_thread.getFrames(frame_name)) {
            // if allframes, check that expression compiles in this frame
            use_frame = frm;
            break;
          }
         if (use_frame == null) {
            BubjetLog.logD("NO FRAME " + use_thread.getUniqueId() + " " + frame_name);
            for (BStackFrame frm : use_thread.getFrames()) {
               BubjetLog.logD("FOUND FRAME " + frm.getUniqueId());
             }
            throw new BubjetException("Stack frame " + frame_name + " doesn't exist");
          }
         use_process = proc;
         
         break;
       }
    }
}       // end of inner class EvaluateAction


private class DoEvaluate extends BubjetAction.BackgroundRead implements EvalCallback {
   
   private String bubbles_id;
   private String eval_expression;
   private BubjetDebugProcess use_process;
   private BThread use_thread;
   private BStackFrame use_frame;
   private String expr_id;
   private String save_id;
   private int output_level;
   private int array_size;
   
   DoEvaluate(String bid,String expr,BubjetDebugProcess proc,BThread bt,BStackFrame bsf,
         String eid,String sid,int lvls,int asz) {
      super(proc.getProject(),null);
      bubbles_id = bid;
      eval_expression = expr;
      use_process = proc;
      use_thread = bt;
      use_frame = bsf;
      expr_id = eid;
      save_id = sid;
      output_level = lvls;
      array_size = asz;
    }
   
   @Override void process() {
      BubjetLog.logD("START EVALUATION OF " + eval_expression + " " + bubbles_id + " " +
            expr_id + " " + save_id + " " + use_thread.getUniqueId() + " " +
               use_frame.getUniqueId());
      
      try {
         boolean rslt = use_process.evaluate(eval_expression,use_thread,use_frame,this);
         if (!rslt) evaluationError("Evaluation canceled");
       }
      catch (BubjetException e) {
         evaluationError(e.getMessage());
       }
    }
   
   @Override public void evaluated(BValue result) { 
      BubjetLog.logD("Evaluation okay for " + bubbles_id + " " + expr_id + " " + save_id + " " +
            output_level + ": " + result);
      if (save_id != null) {
         synchronized (outside_variables) {
            Map<String,BValue> vals = outside_variables.get(use_frame);
            if (vals == null) {
               vals = new HashMap<>();
               outside_variables.put(use_frame,vals);
             }
            vals.put(save_id,result);
          }
       }
      BubjetLog.logD("START EVAL MESSAGE OUT");
      IvyXmlWriter xw = app_service.getMonitor(for_project).beginMessage("EVALUATION",bubbles_id);
      xw.field("ID",expr_id);
      if (save_id != null) xw.field("SAVEID",save_id);
      BubjetUtil.outputEvaluation(use_process,result,null,
            eval_expression,output_level,array_size,xw);
      BubjetLog.logD("EVAL: " + xw.toString());
      app_service.getMonitor(for_project).finishMessage(xw);
    }
   
   @Override public void evaluationError(String msg) {
      BubjetLog.logD("Evaluation error for " + bubbles_id + " " + expr_id + " " + save_id + " " +
            output_level + ": " + msg);
      BubjetLog.logD("START EVAL MESSAGE OUT");
      IvyXmlWriter xw = app_service.getMonitor(for_project).beginMessage("EVALUATION",bubbles_id);
      xw.field("ID",expr_id);
      if (save_id != null) xw.field("SAVEID",save_id);
      BubjetUtil.outputEvaluation(use_process,null,msg,eval_expression,output_level,array_size,xw);
      BubjetLog.logD("EVALERROR: " + xw.toString());
      app_service.getMonitor(for_project).finishMessage(xw);
    }
   
}



/********************************************************************************/
/*                                                                              */
/*      Evaluation methods                                                      */
/*                                                                              */
/********************************************************************************/

private static void startThreadEval(BThread th)
{
   if (th == null) return;
   synchronized (variable_threads) {
      variable_threads.add(th);
    }
}


private static void endThreadEval(BThread th) 
{
   if (th == null) return;
   synchronized (variable_threads) {
      variable_threads.remove(th);
    }
}


private BValue handleSpecialCases(BubjetDebugProcess proc,BValue val,BThread thrd)
{
   if (!(val instanceof BObject)) return val;
   BObject ovl = (BObject) val;
   try {
      startThreadEval(thrd);
      BType jt = ovl.getValueType();
      BValue xvl = null;
      CallFormatter cfmt = format_map.get(jt.getName());
      if (cfmt != null) {
         xvl = cfmt.convertValue(proc,ovl,thrd);
       }
      if (xvl == null) {
         if (jt.getName().equals("org.apache.xerces.dom.DeferredElementImpl")) {
            xvl = convertXml(proc,thrd,ovl);
          }
         else if (jt.getName().equals("com.sun.apache.xerces.internal.dom.DeferredElementImpl")) {
            xvl = convertXml(proc,thrd,ovl);
          }
       }
      if (xvl == null) {
         val = thrd.invokeMethod("toString","()Ljava/lang/String;",ovl,null);
       }
      else val = xvl;
    }
   catch (Throwable t) { }
   finally {
      endThreadEval(thrd);
    }
   
   return val;
}



BValue convertXml(BubjetDebugProcess proc,BThread thrd,BObject xml)
{
   try {
      List<BValue> args = new ArrayList<>();
      args.add(xml);
      BValue rslt = thrd.invokeStaticMethod("edu.brown.cs.ivy.xml.IvyXml",
            "convertXmlToString","(Lorg/w3c/dom/Node;)Ljava/lang/String;",
	    args);
      return rslt;
    }
   catch (Throwable t) {
      BubjetLog.logE("Problem converting XML",t);
    }
   
   return null;
}




private static class CallFormatter {

   private String static_class;
   private String method_name;
   private String method_signature;
   private List<Object> arg_values;
   
   CallFormatter(String method,String sign,Iterable<Object> args) {
      int idx = method.lastIndexOf(".");
      if (idx > 0) {
         static_class = method.substring(0,idx);
         method_name = method.substring(idx+1);
       }
      else {
         static_class = null;
         method_name = method;
       }
      method_signature = sign;
      arg_values = null;
      if (args != null) {
         for (Object o : args) {
            arg_values.add(o);
          }
       }
    }
   
   BValue convertValue(BubjetDebugProcess proc,BObject v,BThread thrd) {
      BValue rslt = null;
      try {
         startThreadEval(thrd);
         if (static_class == null) {
            // method call on v
            List<BValue> args = setupArgs(proc,null);
            rslt = thrd.invokeMethod(method_name,method_signature,v,args);
          }
         else {
            List<BValue> args = setupArgs(proc,v);
            rslt = v.getValueType().invokeStaticMethod(method_name,method_signature,args,thrd);
          }
       }
      catch (Throwable t) {
         BubjetLog.logE("Problem handling value conversion",t);
       }
      finally {
         endThreadEval(thrd);
       }
      
      return rslt;
    }
   
   List<BValue> setupArgs(BubjetDebugProcess proc,BValue arg0) {
      List<BValue> args = new ArrayList<>();
      if (arg0 != null) args.add(arg0);
      if (arg_values != null) {
         for (Object o : arg_values) {
            BValue v = BubjetDebug.getPrimitiveValue(proc,o);
            args.add(v);
          }
       }
      
      return args;
    }
   
}	// end of inner class CallFormatter


}       // end of class BubjetEvaluationManager




/* end of BubjetEvaluationManager.java */

