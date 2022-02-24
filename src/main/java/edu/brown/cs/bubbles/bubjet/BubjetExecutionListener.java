/********************************************************************************/
/*                                                                              */
/*              BubjetExecutionListener.java                                    */
/*                                                                              */
/*      Listen for start/stop execution events                                  */
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

import com.intellij.debugger.DebuggerManager;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;



public class BubjetExecutionListener implements ExecutionListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Project for_project;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BubjetExecutionListener(Project proj)
{
   for_project = proj;
   
   BubjetLog.logD("Executionlistener created for " + proj);
}


public BubjetExecutionListener()
{
   BubjetLog.logD("ExecutionListener created");
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void processStartScheduled(String id,ExecutionEnvironment env)
{
   BubjetLog.logD("processStartScheduled " + id + " " + env);
}


@Override public void processStarting(String id,ExecutionEnvironment env)
{
   BubjetLog.logD("processStarting " + id + " " + env);
}


@Override public void processNotStarted(String id,ExecutionEnvironment env)
{
   BubjetLog.logD("processNotStarted " + id);
}


public void processNotStarted(String id,ExecutionEnvironment env,Throwable cause)
{
   BubjetLog.logD("processNotStarted " + id + " " + cause);
}


@Override public void processStarting(String id,ExecutionEnvironment env,ProcessHandler hdlr)
{
   BubjetLog.logD("processStarting " + id);
}


@Override public void processStarted(String id,ExecutionEnvironment env,ProcessHandler hdlr)
{
   BubjetLog.logD("processStarted " + id);
   
   DebuggerManager.getInstance(for_project).addDebugProcessListener(hdlr,new BubjetJavaDebugListener());
}


@Override public void processTerminating(String id,ExecutionEnvironment env,ProcessHandler hdlr)
{
   BubjetLog.logD("processTerminating " + id);
}



@Override public void processTerminated(String id,ExecutionEnvironment env,ProcessHandler hdlr,int exitcode)
{
   BubjetLog.logD("processTerminated " + id + " " + exitcode);
}



}       // end of class BubjetExecutionListener




/* end of BubjetExecutionListener.java */

