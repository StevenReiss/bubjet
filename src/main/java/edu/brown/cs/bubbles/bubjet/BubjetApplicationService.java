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
   bubjet_monitors = new HashMap<>();
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
      lm = new BubjetLaunchManager(this,p);
      launch_managers.put(p,lm);
    }
   return lm;
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


private class ExitAction extends BubjetAction.Command {
   
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
    }
}       // end of inner class ExitAction




}	// end of class BubjetApplicationService





/* end of BubjectApplicationService.java */
