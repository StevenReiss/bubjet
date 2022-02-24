/********************************************************************************/
/*                                                                              */
/*              BubjetToolWindowListener.java                                   */
/*                                                                              */
/*      Tool window listener for bubjet                                         */
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

import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindow;

import java.util.List;

public class BubjetToolWindowListener implements ToolWindowManagerListener
{


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void toolWindowsRegistered(List<String> ids,ToolWindowManager wm)
{
   BubjetLog.logD("toolWindowsRegistered " + ids);
}


@Override public void toolWindowUnregistered(String id,ToolWindow win)
{
   BubjetLog.logD("toolWindowUnregistgered " + id);
}


@Override public void stateChanged(ToolWindowManager wm)
{
   BubjetLog.logD("stateChanged window mangager");
}


@Override public void toolWindowShown(ToolWindow win)
{
   BubjetLog.logD("toolWindowShown " + win.getTitle());
}


}       // end of class BubjetToolWindowListener




/* end of BubjetToolWindowListener.java */

