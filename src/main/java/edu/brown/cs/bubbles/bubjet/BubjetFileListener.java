/********************************************************************************/
/*                                                                              */
/*              BubjetFileListener.java                                         */
/*                                                                              */
/*      Bulk file listener (not async)                                          */
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

import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;

import java.util.List;

public class BubjetFileListener implements BulkFileListener
{


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void before(List<? extends VFileEvent> evts)
{
   BubjetLog.logD("File before " + evts);
}


@Override public void after(List<? extends VFileEvent> evts)
{
   BubjetLog.logD("File after " + evts);
}



}       // end of class BubjetFileListener




/* end of BubjetFileListener.java */

