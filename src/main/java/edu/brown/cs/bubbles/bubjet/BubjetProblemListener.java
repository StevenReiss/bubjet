/********************************************************************************/
/*                                                                              */
/*              BubjetProblemsListener.java                                     */
/*                                                                              */
/*      Problem listener                                             */
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

import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.ProblemsListener;
import com.intellij.analysis.problemsView.toolWindow.HighlightingProblem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.ProblemListener;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;



public class BubjetProblemListener implements ProblemListener, ProblemsListener
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

public BubjetProblemListener(Project p)
{
   for_project = p;
   BubjetLog.logD("Problem listener created for " + p);  
}



/********************************************************************************/
/*                                                                              */
/*      Listener methods for ProblemListener                                    */
/*                                                                              */
/********************************************************************************/

@Override public void problemsAppeared(VirtualFile f)
{
   BubjetLog.logD("problemsAppeared for " + f);
   PsiFile pf = BubjetUtil.getPsiFile(for_project,f);
  
   for (PsiErrorElement err : PsiTreeUtil.collectElementsOfType(pf,PsiErrorElement.class)) {
      BubjetLog.logD("FOUND ERROR ELEMENT: " + err + " " + err.getContainingFile() + " " +
            err.getTextRange() + " " + err.getParent() + " " + err.getParent().getTextRange() + " " +
               err.getChildren().length + " " + err.getErrorDescription());
    }
}


@Override public void problemsChanged(VirtualFile f)
{
   BubjetLog.logD("problemsChanged for " + f);
   PsiFile pf = BubjetUtil.getPsiFile(for_project,f);
   for (PsiErrorElement err : PsiTreeUtil.collectElementsOfType(pf,PsiErrorElement.class)) {
      BubjetLog.logD("FOUND ERROR ELEMENT: " + err + " " + err.getContainingFile() + " " +
            err.getTextRange() + " " + err.getParent() + " " + err.getParent().getTextRange() + " " +
               err.getChildren().length + " " + err.getErrorDescription());
    }
}


@Override public void problemsDisappeared(VirtualFile f)
{
   BubjetLog.logD("problemsDisappeared for " + f);
   PsiFile pf = BubjetUtil.getPsiFile(for_project,f);
   for (PsiErrorElement err : PsiTreeUtil.collectElementsOfType(pf,PsiErrorElement.class)) {
      BubjetLog.logD("FOUND ERROR ELEMENT: " + err + " " + err.getContainingFile() + " " +
            err.getTextRange() + " " + err.getParent() + " " + err.getParent().getTextRange() + " " +
               err.getChildren().length + " " + err.getErrorDescription());
    }
}


/********************************************************************************/
/*                                                                              */
/*      Listener methods for Problems Listener                                  */
/*                                                                              */
/********************************************************************************/

@Override public void problemAppeared(Problem p)
{
   BubjetLog.logD("problemAppeared " + p + " " + p.getDescription() + " " +
      p.getText() + " " + p.getClass() + " " + p.getProvider());
   if (p instanceof HighlightingProblem) {
      HighlightingProblem hp = (HighlightingProblem) p;
      BubjetLog.logD("HPROB " + hp.getFile() + " " + hp.getLine() + " " +
            hp.getColumn() + " " + hp.getGroup() + " " + hp.getSeverity() + " " +
            hp.getHighlighter().getStartOffset() + " " + hp.getHighlighter().getEndOffset());
      PsiFile pf = BubjetUtil.getPsiFile(for_project,hp.getFile());
      for (PsiErrorElement err : PsiTreeUtil.collectElementsOfType(pf,PsiErrorElement.class)) {
         BubjetLog.logD("FOUND ERROR ELEMENT: " + err + " " + err.getContainingFile() + " " + 
               err.getTextRange() + " " + err.getParent() + " " + err.getParent().getTextRange() + " " +
               err.getChildren().length + " " + err.getErrorDescription());
       }
    }
  
}

@Override public void problemDisappeared(Problem p)
{
   BubjetLog.logD("problemDisappeared " + p);
}


@Override public void problemUpdated(Problem p)
{
   BubjetLog.logD("problemAppeared " + p);
}




}       // end of class BubjetProblemsListener




/* end of BubjetProblemsListener.java */

