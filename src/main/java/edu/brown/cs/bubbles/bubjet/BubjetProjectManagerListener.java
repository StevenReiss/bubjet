/****************************************************************************************/
/*											*/
/*		BubjetProjectManagerListener.java					*/
/*											*/
/*	Listen for project manager requests						*/
/*											*/
/****************************************************************************************/

package edu.brown.cs.bubbles.bubjet;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;


public class BubjetProjectManagerListener implements ProjectManagerListener {


/****************************************************************************************/
/*											*/
/*	Callback methods								*/
/*											*/
/****************************************************************************************/

@Override public void projectOpened(Project project)
{
   BubjetLog.logD("projectOpened " + project.getName());
   BubjetProjectService serv = project.getService(BubjetProjectService.class);
   if (serv != null && project != null) serv.noteOpen(project);
   
   BubjetApplicationService appser = BubjetBundle.getAppService();
   appser.addProject(project);
}



@Override public void projectClosed(Project project)
{ 
   BubjetLog.logD("projectClosed " + project.getName());
 
   BubjetProjectService serv = ApplicationManager.getApplication().getService(BubjetProjectService.class);
   if (serv != null && project != null) {
      serv.noteClosed(project);
    }
   
   BubjetApplicationService appser = BubjetBundle.getAppService();
   appser.removeProject(project);
}


@Override public void projectClosing(Project project)
{ 
   BubjetLog.logD("projectClosing " + project.getName());
}


@Override public void projectClosingBeforeSave(Project project)
{ 
   BubjetLog.logD("projectClosingBeforeSave " + project.getName());
}



}	// end of BubjectProjectManagerListener



/* end of BubjetProjectManagerListener.java */
