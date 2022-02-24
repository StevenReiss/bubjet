/********************************************************************************/
/*                                                                              */
/*              BubjetJavaDebugListener.java                                    */
/*                                                                              */
/*      Listener for java debugging                                             */
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

import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfileState;
import com.sun.jdi.ThreadReference;
import com.intellij.debugger.engine.DebugProcess;


public class BubjetJavaDebugListener implements DebugProcessListener
{


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void connectorIsReady()
{
   BubjetLog.logD("connectorIsReady");
}



@Override public void paused(SuspendContext ctx)
{
   BubjetLog.logD("paused " + ctx);
}


@Override public void resumed(SuspendContext ctx)
{
   BubjetLog.logD("resumed " + ctx);
}



@Override public void processDetached(DebugProcess proc,boolean closedbyuser)
{
   BubjetLog.logD("processDetached " + proc + " " + closedbyuser);
}


@Override public void processAttached(DebugProcess proc)
{
   BubjetLog.logD("processAttached " + proc);
}



@Override public void attachException(RunProfileState state,ExecutionException exc,RemoteConnection conn)
{
   BubjetLog.logD("attachException");
}



@Override public void threadStarted(DebugProcess proc,ThreadReference thrd)
{  
   BubjetLog.logD("threadStarted " + thrd);
}



@Override public void threadStopped(DebugProcess proc,ThreadReference thrd)
{
   BubjetLog.logD("threadStopped " + thrd);
}






}       // end of class BubjetJavaDebugListener




/* end of BubjetJavaDebugListener.java */

