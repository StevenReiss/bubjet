/****************************************************************************************/
/*											*/
/*		BubjetApplicationService.java						*/
/*											*/
/*	Application Service for Bubbles 						*/
/*											*/
/****************************************************************************************/


package edu.brown.cs.bubbles.bubjet;

import java.util.HashMap;
import java.util.Map;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

@Service
public final class BubjetApplicationService {




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<Project,BubjetMonitor>   bubjet_monitors;
private BubjetProjectManager project_manager;
private BubjetSearchManager search_manager;
private Map<Project,BubjetLaunchManager> launch_managers;
private Map<Project,BubjetBreakpointManager> break_managers;
private Map<Project,BubjetEditManager> edit_managers;
private Map<Project,BubjetExecutionManager> execution_managers;
private Map<Project,BubjetEvaluationManager> evaluation_managers;
private Map<Project,BubjetPreferenceManager> preference_managers;



/****************************************************************************************/
/*											*/
/*	Constructors									*/
/*											*/
/****************************************************************************************/

public BubjetApplicationService()
{
   BubjetLog.setup();
   
   BubjetLog.logD("Application service started");
   
   project_manager = new BubjetProjectManager(this);
   search_manager = new BubjetSearchManager(this);
   
   break_managers = new HashMap<>();
   launch_managers = new HashMap<>();
   edit_managers = new HashMap<>();
   execution_managers = new HashMap<>();
   evaluation_managers = new HashMap<>();
   bubjet_monitors = new HashMap<>();
   preference_managers = new HashMap<>();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public synchronized BubjetMonitor getMonitor(Project p)
{
   BubjetMonitor bm = bubjet_monitors.get(p);
   if (bm == null) {
      bm = new BubjetMonitor(this,p);
      bubjet_monitors.put(p,bm);
    }
   return bm;
}


BubjetProjectManager getProjectManager()
{
   return project_manager;
}


BubjetSearchManager getSearchManager()
{
   return search_manager;
}

BubjetLaunchManager getLaunchManager(Project p)
{
   BubjetLaunchManager lm = launch_managers.get(p);
   if (lm == null) {
      if (p == null) return null;
      lm = new BubjetLaunchManager(this,p);
      launch_managers.put(p,lm);
    }
   return lm;
}


BubjetExecutionManager getExecutionManager(Project p)
{
   BubjetExecutionManager em = execution_managers.get(p);
   if (em == null) {
      if (p == null) return null;
      em = new BubjetExecutionManager(this,p);
      execution_managers.put(p,em);
    }
   return em;
}


BubjetEvaluationManager getEvaluationManager(Project p)
{
   BubjetEvaluationManager em = evaluation_managers.get(p);
   if (em == null) {
      if (p == null) return null;
      em = new BubjetEvaluationManager(this,p);
      evaluation_managers.put(p,em);
    }
   return em;
}


synchronized BubjetBreakpointManager getBreakpointManager(Project p)
{
   BubjetBreakpointManager bm = break_managers.get(p);
   if (bm == null) {
      bm = new BubjetBreakpointManager(this,p);
      break_managers.put(p,bm);
    }
   return bm;
}


synchronized BubjetEditManager getEditManager(Project p)
{
   BubjetEditManager em = edit_managers.get(p);
   if (em == null) {
      em = new BubjetEditManager(this,p);
      edit_managers.put(p,em);
    }
   return em;
}



synchronized BubjetPreferenceManager getPreferenceManager(Project p)
{
   BubjetPreferenceManager pm = preference_managers.get(p);
   if (pm == null) {
      pm = new BubjetPreferenceManager(this,p);
      preference_managers.put(p,pm);
    }
   return pm;
}




/********************************************************************************/
/*                                                                              */
/*      Project methods                                                         */
/*                                                                              */
/********************************************************************************/

void addProject(Project project)
{
   project_manager.addProject(project);
   getBreakpointManager(project);
   getLaunchManager(project);
   getExecutionManager(project);
}


void removeProject(Project project)
{
   project_manager.removeProject(project);
}


/********************************************************************************/
/*                                                                              */
/*      Handle exit                                                             */
/*                                                                              */
/********************************************************************************/

void exit()
{
   ExitAction eact = new ExitAction();
   eact.start();
}


private class ExitAction extends BubjetAction.SimpleCommand {
   
   ExitAction() {
      super(null,null,null);
    }
   
   @Override void process() {
      Application app = ApplicationManager.getApplication();
      BubjetLog.logD("EXIT REQUEST " + app.isActive() + " " + app.isCommandLine() + " " +
            app.isHeadlessEnvironment() + " " + app.isInternal() + " " + app.isEAP());
      
      app.saveAll();
      app.saveSettings();
      if (app.isCommandLine() || app.isHeadlessEnvironment()) app.exit(false,false,false);
      if (app instanceof ApplicationEx) {
         ApplicationEx aex = (ApplicationEx) app;
         aex.exit(true,false);
       }
      else {
         app.exit(false,false,false);
       }
    }
}       // end of inner class ExitAction




}	// end of class BubjetApplicationService





/* end of BubjectApplicationService.java */
