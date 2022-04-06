/********************************************************************************/
/*                                                                              */
/*              BubjetEditorHintListener.java                                   */
/*                                                                              */
/*      Listen for editor hints                                                 */
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

import com.intellij.codeInsight.hint.EditorHintListener;
import com.intellij.openapi.project.Project;
import com.intellij.ui.LightweightHint;


public class BubjetEditorHintListener implements EditorHintListener
{

/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void hintShown(Project project,LightweightHint hint,int flags)
{
   BubjetLog.logD("hintShown " + hint + " " + flags);
}



}       // end of class BubjetEditorHintListener




/* end of BubjetEditorHintListener.java */

