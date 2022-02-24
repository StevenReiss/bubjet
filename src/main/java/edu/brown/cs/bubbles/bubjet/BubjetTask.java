/********************************************************************************/
/*                                                                              */
/*              BubjetTask.java                                                 */
/*                                                                              */
/*      Runnable that works with intellij                                       */
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

abstract class BubjetTask implements BubjetConstants, Runnable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Project                 for_project;
private String                  progress_name;
private boolean                 need_read;
private boolean                 need_write;
private boolean                 need_dispatch;
private boolean                 run_background;

private BubjetProgressIndicator progress_indicator;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetTask(Project p,boolean read,boolean write,String prog,boolean dispatch,boolean bkg)
{
   for_project = p;
   progress_name = prog;
   need_read = read;
   need_write = write;
   need_dispatch = dispatch;
   run_background = bkg;
}



/********************************************************************************/
/*                                                                              */
/*      Start the task                                                          */
/*                                                                              */
/********************************************************************************/

void start()
{
   if (need_read) {
      ApplicationManager.getApplication().runReadAction(this);
    }
   else if (need_write) {
      ApplicationManager.getApplication().runWriteAction(this);
    }
   else if (run_background) {
      ApplicationManager.getApplication().invokeLater(this);
    }
   else run();
}



/********************************************************************************/
/*                                                                              */
/*      Task controller                                                         */
/*                                                                              */
/********************************************************************************/

@Override public final void run()
{
   if (progress_name != null && progress_indicator == null) {
      progress_indicator = new BubjetProgressIndicator(progress_name,for_project);
      BubjetLog.logD("CREATE PROGRESS INDICATOR for " + for_project.getName());
      ProgressManager.getInstance().runProcess(this,progress_indicator);
      return;
    }
   if (need_dispatch && !ApplicationManager.getApplication().isDispatchThread()) {
      if (run_background) {
         ApplicationManager.getApplication().invokeLater(this);
       }
      else {
         ApplicationManager.getApplication().invokeAndWait(this);
       }
      return;
    }
   
   process();
}



abstract void process();



}       // end of class BubjetTask




/* end of BubjetTask.java */

