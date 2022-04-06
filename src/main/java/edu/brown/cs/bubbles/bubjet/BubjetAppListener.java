/********************************************************************************/
/*                                                                              */
/*              BubjetAppListener.java                                          */
/*                                                                              */
/*      Handle application state changes                                        */
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

import com.intellij.ide.AppLifecycleListener;

import java.util.List;


public class BubjetAppListener implements AppLifecycleListener
{

/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void appFrameCreated(List<String> args)
{
   BubjetBundle bndl = BubjetBundle.INSTANCE;
   BubjetLog.logD("Created bundle on appFrameCreated " + bndl);
   BubjetLog.logD("APP appFrameCreated " + args);
}


@Override public void welcomeScreenDisplayed()
{
   BubjetBundle bndl = BubjetBundle.INSTANCE;
   BubjetLog.logD("APP welcomeScreenDisplayed " + bndl);
}


@Override public void projectFrameClosed()
{
   BubjetLog.logD("APP projectFrameClosed");
}


@Override public void projectOpenFailed()
{
   BubjetBundle bndl = BubjetBundle.INSTANCE;
   BubjetLog.logD("APP projectOpenFailed " + bndl);
}


@Override public void appClosing()
{
   BubjetLog.logD("APP appClosing");
}


@Override public void appWillBeClosed(boolean restart)
{
   BubjetLog.logD("APP appWillBeClosed " + restart);
}



}       // end of class BubjetAppListener




/* end of BubjetAppListener.java */

