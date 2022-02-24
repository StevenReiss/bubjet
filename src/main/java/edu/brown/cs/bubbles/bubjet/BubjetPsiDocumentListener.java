/********************************************************************************/
/*                                                                              */
/*              BubjetPsiDocumentListener.java                                  */
/*                                                                              */
/*      Program Structure Interface Document listener                           */
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

import com.intellij.psi.PsiDocumentListener;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.AnyPsiChangeListener;
import com.intellij.psi.impl.PsiDocumentTransactionListener;
import com.intellij.openapi.project.Project;


public class BubjetPsiDocumentListener implements PsiDocumentListener, AnyPsiChangeListener,
        PsiDocumentTransactionListener
{

/********************************************************************************/
/*                                                                              */
/*      PSI Document Listener methods                                           */
/*                                                                              */
/********************************************************************************/

@Override public void documentCreated(Document doc,PsiFile psifile,Project proj)
{
   BubjetLog.logD("PSI documentCreated " + psifile + " " + this);
}



@Override public void fileCreated(PsiFile psifile,Document doc)
{
   BubjetLog.logD("PSI fileCreated " + psifile);
}



/********************************************************************************/
/*                                                                              */
/*      AnyPsiChangeListener                                                    */
/*                                                                              */
/********************************************************************************/

@Override public void beforePsiChanged(boolean phys)
{
   BubjetLog.logD("beforePsiChanged " + phys);
}


@Override public void afterPsiChanged(boolean phys)
{
   BubjetLog.logD("afterPsiChanged " + phys);
}


/********************************************************************************/
/*                                                                              */
/*      PsiDocumentTransactionListener methods                                  */
/*                                                                              */
/********************************************************************************/

@Override public void transactionStarted(Document doc,PsiFile psifile)
{
   BubjetLog.logD("PSI transactionStarted " + psifile);
}



@Override public void transactionCompleted(Document doc,PsiFile psifile)
{
   BubjetLog.logD("PSI transactionCompleted " + psifile);
}


}       // end of class BubjetPsiDocumentListener




/* end of BubjetPsiDocumentListener.java */

