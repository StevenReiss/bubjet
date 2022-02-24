/****************************************************************************************/
/*											*/
/*		BubjetProjectService.java						*/
/*											*/
/*	Project service for BUBJET							*/
/*											*/
/****************************************************************************************/


package edu.brown.cs.bubbles.bubjet;

import com.intellij.openapi.project.Project;
import com.intellij.problems.ProblemListener;
import com.intellij.task.ProjectTaskListener;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.impl.ProjectTaskManagerImpl;
import com.intellij.tasks.TaskManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.ProjectTopics;
import com.intellij.analysis.problemsView.ProblemsListener;
import com.intellij.build.BuildViewManager;
import com.intellij.execution.ExecutionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.components.Service;


@Service
public final class BubjetProjectService {


/****************************************************************************************/
/*											*/
/*	Private Storage 								*/
/*											*/
/****************************************************************************************/

private Project 	for_project;




/****************************************************************************************/
/*											*/
/*	Constructors									*/
/*											*/
/****************************************************************************************/

public BubjetProjectService()
{
   BubjetLog.logD("Project service initialized without a project");
}



public BubjetProjectService(Project project)
{
   for_project = project;
   BubjetApplicationService bas = BubjetBundle.getAppService();
   bas.getMonitor(project);

   BubjetLog.logD("Project service initialized for " + for_project.getName());
   
   BubjetProjectTaskListener taskl = new BubjetProjectTaskListener(project);
   BubjetProblemListener probs = new BubjetProblemListener(project);
   BubjetModuleListener ml = new BubjetModuleListener(project);
   BubjetCompilationStatusListener compl = new BubjetCompilationStatusListener(project);
   
   Application app = ApplicationManager.getApplication();
   MessageBus mbus = app.getMessageBus();
   mbus.connect().subscribe(ProblemsListener.TOPIC,probs);
   mbus.connect().subscribe(ProblemListener.TOPIC,probs);
   mbus.connect().subscribe(XDebuggerManager.TOPIC,new BubjetDebuggerListener(project));
   mbus.connect().subscribe(ProjectTaskListener.TOPIC,new BubjetProjectTaskListener(project));
   mbus.connect().subscribe(ProjectTaskListener.TOPIC,taskl);
   mbus.connect().subscribe(CompilerTopics.COMPILATION_STATUS,compl);
   
   mbus = project.getMessageBus();
   mbus.connect().subscribe(ProjectTopics.MODULES,ml);
   mbus.connect().subscribe(ProjectTopics.PROJECT_ROOTS,ml);
   mbus.connect().subscribe(ExecutionManager.EXECUTION_TOPIC,new BubjetExecutionListener(project));
   mbus.connect().subscribe(ProblemsListener.TOPIC,probs);
   mbus.connect().subscribe(ProblemListener.TOPIC,probs);
   mbus.connect().subscribe(CompilerTopics.COMPILATION_STATUS,compl);
   mbus.connect().subscribe(ProjectTaskListener.TOPIC,taskl);
   ProjectTaskManagerImpl ptm = (ProjectTaskManagerImpl) ProjectTaskManager.getInstance(project);
   ptm.addListener(taskl);
   TaskManager tm = TaskManager.getManager(project);
   tm.addTaskListener(taskl,project);
  
   
// XBreakpointManager xmgr = XDebuggerManager.getInstance(project).getBreakpointManager();
// xmgr.addBreakpointListener(new BubjetBreakpointListener(project));
   
   DebuggerManagerEx dbg = DebuggerManagerEx.getInstanceEx(project);
   BreakpointManager bpt = dbg.getBreakpointManager();
   bpt.addListeners(mbus.connect()); 
   
   BuildViewManager mgr2 = project.getService(BuildViewManager.class);
// Object o1 = BuildViewManager.createBuildProgress(project);
// BubjetLog.logD("BUILD PROGRESS " + o1);
   mgr2.addListener(compl,project);
}


public void noteOpen(Project p)
{
   if (for_project == null) for_project = p;
   
   BubjetLog.logD("Project Note Open " + p + " " + for_project);
// 
// BubjetApplicationService bas = BubjetBundle.getAppService();
// bas.getProjectManager().noteOpen(p);
}


public void noteClosed(Project p)
{
   BubjetLog.logD("Project Note Closed " + p + " " + for_project);
// 
// BubjetApplicationService bas = BubjetBundle.getAppService();
// bas.getProjectManager().noteClosed(p);
}







}	// end of class BubjetProjectService


/* end of BubjetProjectService.java */



