/********************************************************************************/
/*                                                                              */
/*              BubjetDebugSessionListener.java                                 */
/*                                                                              */
/*      Debug session interface                                                 */
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

import com.intellij.xdebugger.XDebugSessionListener;

public class BubjetDebugSessionListener implements XDebugSessionListener
{

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BubjetDebugSessionListener()
{
   BubjetLog.logD("Bubjet debug session listener created");
}




/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
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


}       // end of class BubjetDebugSessionListener




/* end of BubjetDebugSessionListener.java */

