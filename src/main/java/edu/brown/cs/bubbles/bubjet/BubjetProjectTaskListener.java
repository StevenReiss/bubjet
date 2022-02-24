/********************************************************************************/
/*                                                                              */
/*              BubjetProjectTaskListener.java                                  */
/*                                                                              */
/*      Alternative for handling compilations                                   */
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

import com.intellij.task.ProjectTaskListener;


import com.intellij.openapi.project.Project;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.impl.ProjectTaskManagerListener;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.TaskListener;


public class BubjetProjectTaskListener implements ProjectTaskListener, ProjectTaskManagerListener, TaskListener
{


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BubjetProjectTaskListener()
{
   BubjetLog.logD("ProjectTaskListener created");
}



public BubjetProjectTaskListener(Project project)
{
   BubjetLog.logD("ProjectTaskListener created for " + project);
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void started(ProjectTaskContext task)
{
   BubjetLog.logD("ProjectTask started " + task);
}


@Override public void finished(ProjectTaskManager.Result rslt)
{
   BubjetLog.logD("ProjectTask finished " + rslt + " " + rslt.hasErrors() + rslt.isAborted() + " " + rslt.getContext());
}

@Override public void beforeRun(ProjectTaskContext ctx) 
{ 
   BubjetLog.logD("ProjectTask beforeRun " + ctx);
}


@Override public void afterRun(ProjectTaskManager.Result rslt) 
{
   BubjetLog.logD("ProjectTask afterRun " + rslt + " " + rslt.hasErrors() + rslt.isAborted() + " " + rslt.getContext());
}


@Override public void taskDeactivated(LocalTask task) 
{
   BubjetLog.logD("taskDeactivated " + task);
}

@Override public void taskActivated(LocalTask task) 
{
   BubjetLog.logD("taskActivated " + task);
}

@Override public void taskAdded(LocalTask task) 
{
   BubjetLog.logD("taskAdded " + task);
}

@Override public void taskRemoved(LocalTask task) 
{
   BubjetLog.logD("taskRemoved " + task);
}


}       // end of class BubjetProjectTaskListener




/* end of BubjetProjectTaskListener.java */

