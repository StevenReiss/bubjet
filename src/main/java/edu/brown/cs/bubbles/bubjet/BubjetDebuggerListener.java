/********************************************************************************/
/*                                                                              */
/*              BubjetDebuggerListener.java                                     */
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

import com.intellij.xdebugger.XDebuggerManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSessionListener;

public class BubjetDebuggerListener implements XDebuggerManagerListener, XDebugSessionListener
{



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BubjetDebuggerListener()
{
   BubjetLog.logD("Bubjet debugger listener created");
}


public BubjetDebuggerListener(Project proj)
{
   BubjetLog.logD("Bubjet debugger listener created for " + proj);
}




/********************************************************************************/
/*                                                                              */
/*      XDebuggerManager interface                                              */
/*                                                                              */
/********************************************************************************/

@Override public void processStarted(com.intellij.xdebugger.XDebugProcess proc)
{
   BubjetLog.logD("Xdebug processStarted " + proc);
   proc.getSession().addSessionListener(this);
}


@Override public void processStopped(com.intellij.xdebugger.XDebugProcess proc)
{
  BubjetLog.logD("processStopped " + proc);
}


@Override public void currentSessionChanged(com.intellij.xdebugger.XDebugSession sess0,com.intellij.xdebugger.XDebugSession sess1)
{
   BubjetLog.logD("currentSessionChanged " + sess0 + " " + sess1);
}


/********************************************************************************/
/*                                                                              */
/*      Debug Session interface                                                 */
/*                                                                              */
/********************************************************************************/

@Override public void sessionPaused()
{
   BubjetLog.logD("sessionPaused");
}



@Override public void sessionResumed()
{
   BubjetLog.logD("sessionResumed");
}


@Override public void sessionStopped()
{
   BubjetLog.logD("sessionStopped");
}


@Override public void stackFrameChanged()
{
   BubjetLog.logD("stackFrameChanged");
}


@Override public void beforeSessionResume()
{
   BubjetLog.logD("beforeSessionResume");
}


@Override public void settingsChanged()
{
   BubjetLog.logD("settingsChanged");
}


public void breakpointsMuted(boolean fg)
{
   BubjetLog.logD("breakpointsMuted " + fg);
}



}       // end of class BubjetDebuggerListener




/* end of BubjetDebuggerListener.java */

