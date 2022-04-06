/********************************************************************************/
/*                                                                              */
/*              BubjetBreakpointManager.java                                    */
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

import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.java.debugger.breakpoints.properties.JavaExceptionBreakpointProperties;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import com.intellij.debugger.ui.breakpoints.JavaExceptionBreakpointType;
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.SuspendPolicy;
import com.intellij.xdebugger.breakpoints.XBreakpoint;


class BubjetBreakpointManager implements BubjetConstants, XBreakpointListener<XBreakpoint<?>>
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService app_service;
private Project for_project;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetBreakpointManager(BubjetApplicationService app,Project p)
{
   app_service = app;
   for_project = p;
   p.getMessageBus().connect().subscribe(XBreakpointListener.TOPIC,this);
}



/********************************************************************************/
/*                                                                              */
/*      Handle GETALLBREAKPOINTS                                                */
/*                                                                              */
/********************************************************************************/

void handleGetAllBreakpoints(Project p,Module m,IvyXmlWriter xw)
{
   GetAllBreakpointsAction act = new GetAllBreakpointsAction(p,m,xw);
   act.start();
}


private class GetAllBreakpointsAction extends BubjetAction.Read {
   
   private Project for_project;
   private IvyXmlWriter xml_writer;
   
   GetAllBreakpointsAction(Project p,Module m,IvyXmlWriter xw) {
      for_project = p;
      xml_writer = xw;
    }
   
   @Override void process() {
      XDebuggerManager dmgr = XDebuggerManager.getInstance(for_project);
      XBreakpointManager bmgr = dmgr.getBreakpointManager();
      
      xml_writer.begin("BREAKPOINTS");
      xml_writer.field("REASON","LIST");
      for (XBreakpoint<?> bpt : bmgr.getAllBreakpoints()) {
         BubjetUtil.outputBreakpoint(for_project,bpt,xml_writer);
       }
      xml_writer.end("BREAKPOINTS");
    }
   
}       // end of inner class GetAllBreakpointsAction



/********************************************************************************/
/*                                                                              */
/*      Handle ADDLINEBREAKPOINT                                                */
/*                                                                              */
/********************************************************************************/

void handleAddLineBreakpoint(Project p,Module m,String bid,String file,String clsnm,
      int line,boolean suspvm,boolean trace)
{
   AddLineBreakpointAction act = new AddLineBreakpointAction(p,m,bid,file,clsnm,line,suspvm,trace);
   act.start();
}


private class AddLineBreakpointAction extends BubjetAction.WriteDispatch {

   private Project for_project;
   private VirtualFile for_file;
   private int line_number;
   private SuspendPolicy suspend_policy;
   
   AddLineBreakpointAction(Project p,Module m,String bid,String file,String clsnm,
         int line,boolean suspvm,boolean trace) {
      for_project = p;
      for_file = BubjetUtil.getVirtualFile(file);
      line_number = line;
      suspend_policy = (suspvm ? SuspendPolicy.ALL : SuspendPolicy.THREAD);
      if (trace) suspend_policy = SuspendPolicy.NONE;
    }
   
   @Override void process() {
      XDebuggerUtil util = XDebuggerUtil.getInstance();
      JavaLineBreakpointType type = util.findBreakpointType(JavaLineBreakpointType.class);
      XDebuggerManager xdm = XDebuggerManager.getInstance(for_project);
      XBreakpointManager xbm = xdm.getBreakpointManager();
      int lno = line_number-1;
      
      JavaLineBreakpointProperties props = type.createBreakpointProperties(for_file,lno);
      
      XLineBreakpoint<?> bp = xbm.addLineBreakpoint(type,for_file.getUrl(),lno,props,false);
      bp.setSuspendPolicy(suspend_policy);
      if (suspend_policy == SuspendPolicy.NONE) {
         bp.setLogMessage(true);
       }
    }
   
}       // end of inner class AddLineBreakpointAction



/********************************************************************************/
/*                                                                              */
/*      Handle ADDEXCEPTIONBREAKPOINT                                           */
/*                                                                              */
/********************************************************************************/

void handleAddExceptionBreakpoint(Project p,Module m,String clsnm,
      boolean caught,boolean uncaught,boolean checked,boolean suspvm,
      boolean subclss)
{
   AddExceptionBreakpointAction act = new AddExceptionBreakpointAction(p,m,clsnm,
         caught,uncaught,checked,suspvm,subclss);
   act.start();
}


private class AddExceptionBreakpointAction extends BubjetAction.WriteDispatch {

   private Project for_project;
   private SuspendPolicy suspend_policy;
   private JavaExceptionBreakpointProperties use_props;
   
   AddExceptionBreakpointAction(Project p,Module m,String clsnm,
         boolean caught,boolean uncaught,boolean checked,boolean suspvm,
         boolean subclss) {
      for_project = p;
      suspend_policy = (suspvm ? SuspendPolicy.ALL : SuspendPolicy.THREAD);
      GlobalSearchScope scp = BubjetUtil.getSearchScope(p,m);
      use_props = new JavaExceptionBreakpointProperties();
      use_props.NOTIFY_CAUGHT = caught;
      use_props.NOTIFY_UNCAUGHT = uncaught;
      if (clsnm != null) {
         PsiManager pm = PsiManager.getInstance(p);
         PsiClass pc = ClassUtil.findPsiClass(pm,clsnm,null,true,scp);
         if (pc == null) pc = ClassUtil.findPsiClass(pm,clsnm,null,true,BubjetUtil.getSearchScope(p));
         if (pc != null) {
            use_props.myQualifiedName = pc.getQualifiedName();
            use_props.myPackageName = PsiUtil.getPackageName(pc);
          }
       }
      if (!subclss) {
         ClassFilter [] cfs = new ClassFilter[] { new ClassFilter(clsnm) };
         use_props.setCatchClassFilters(cfs);
       }
    }
   
   @Override void process() {
      XDebuggerUtil util = XDebuggerUtil.getInstance();
      JavaExceptionBreakpointType type = util.findBreakpointType(JavaExceptionBreakpointType.class);
      XDebuggerManager xdm = XDebuggerManager.getInstance(for_project);
      XBreakpointManager xbm = xdm.getBreakpointManager();
      
      XBreakpoint<?> bp = xbm.addBreakpoint(type,use_props);
      bp.setSuspendPolicy(suspend_policy);
    }
   
}       // end of inner class AddExceptionBreakpointAction





/********************************************************************************/
/*                                                                              */
/*      Handle EDITBREAKPOINT                                                   */
/*                                                                              */
/********************************************************************************/

void handleEditBreakpoint(Project p,Module m,int id,String ... propvals)
{
   
}



/********************************************************************************/
/*                                                                              */
/*      Handle CLEARALLLINEBREAKPOINTS and CLEARALINEBREAKPOINT                 */
/*                                                                              */
/********************************************************************************/

void handleClearLineBreakpoint(Project p,Module m,String file,String clsnm,int line)
{
    ClearLineBreakpointAction act = new ClearLineBreakpointAction(p,m,file,clsnm,line);
    act.start();
}


private class ClearLineBreakpointAction extends BubjetAction.WriteDispatch {
   
   private Project for_project;
   private VirtualFile for_file;
   private String class_name;
   private int line_number;
   
   ClearLineBreakpointAction(Project p,Module m,String file,String clsnm,int line) {
      for_project = p;
      for_file = BubjetUtil.getVirtualFile(file);
      class_name = clsnm;
      line_number = line;
    }
   
   @Override void process() {
      XDebuggerManager xdm = XDebuggerManager.getInstance(for_project);
      XBreakpointManager xbm = xdm.getBreakpointManager();
      List<XBreakpoint<?>> rem = new ArrayList<>();
      for (XBreakpoint<?> bp : xbm.getAllBreakpoints()) {
         if (bp.getProperties() instanceof JavaLineBreakpointProperties) {
            XSourcePosition srcpos = bp.getSourcePosition();
            if (for_file != null && !for_file.equals(srcpos.getFile())) continue;
            if (line_number > 0 && srcpos.getLine() != line_number) continue;
            if (class_name != null) {
               PsiFile pf = BubjetUtil.getPsiFile(for_project,for_file);
               PsiElement pe = pf.findElementAt(srcpos.getOffset());
               PsiClass pc = BubjetUtil.getClass(pe);
               if (pc != null && !pc.getQualifiedName().equals(class_name)) continue;
             }
            rem.add(bp);
          }
       }
      for (XBreakpoint<?> bp : rem) {
         xbm.removeBreakpoint(bp);
       }
    }
   
}       // end of inner class ClearLineBreakpointAction



/********************************************************************************/
/*                                                                              */
/*      Notify on breakpoint events                                             */
/*                                                                              */
/********************************************************************************/

private void sendBreakpointEvent(String what,XBreakpoint<?> bpt)
{
   BreakEventSendAction act = new BreakEventSendAction(what,bpt);
   act.start();
}


private class BreakEventSendAction extends BubjetAction.Read {
   
   private String event_type;
   private XBreakpoint<?> break_point;
   
   BreakEventSendAction(String what,XBreakpoint<?> bp) {
      event_type = what;
      break_point = bp;
    }
   
   @Override public void process() {
      BubjetMonitor bm = app_service.getMonitor(for_project);
      IvyXmlWriter xw = bm.beginMessage("BREAKEVENT");
      xw.begin("BREAKPOINTS");
      xw.field("REASON",event_type);
      BubjetUtil.outputBreakpoint(for_project,break_point,xw);
      xw.end("BREAKPOINTS");
      bm.finishMessage(xw);
    }
   
}       // end of inner class BreakEventSendAction



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void breakpointAdded(XBreakpoint<?> bpt)
{
   BubjetLog.logD("breakpointAdded " + bpt);
   sendBreakpointEvent("ADD",bpt);
}




@Override public void breakpointRemoved(XBreakpoint<?> bpt)
{
   BubjetLog.logD("breakpointRemoved " + bpt);
   sendBreakpointEvent("REMOVE",bpt);
}




@Override public void breakpointChanged(XBreakpoint<?> bpt)
{
   BubjetLog.logD("breakpointChanged " + bpt);
   sendBreakpointEvent("CHANGE",bpt);
}



@Override public void breakpointPresentationUpdated(XBreakpoint<?> bpt,XDebugSession sess)
{
   BubjetLog.logD("breakpointPresentationUpdated " + bpt);
   sendBreakpointEvent("CHANGE",bpt);
}








}       // end of class BubjetBreakpointManager




/* end of BubjetBreakpointManager.java */

