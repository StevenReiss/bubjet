/********************************************************************************/
/*                                                                              */
/*              BubjetErrorPass.java                                            */
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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ReflectionUtil;

import java.util.ArrayList;
import java.util.List;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BubjetErrorPass implements BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetFileData          file_data;
private String                  bubbles_id;
private int                     edit_id;

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetErrorPass(BubjetFileData fd,String bid,int id)
{
   file_data = fd;
   bubbles_id = bid;
   edit_id = id;
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process()
{
   PsiFile pf = file_data.getPsiFile();
   String what = (file_data.isPrivateId(bubbles_id) ? "PRIVATEERROR" : "FILEERROR");
   IvyXmlWriter xw = file_data.getMonitor().beginMessage(what);
   xw.field("FILE",BubjetUtil.outputFileName(file_data.getVirutalFile()));
   xw.field("PROJECT",file_data.getModule().getName());
   xw.begin("MESSAGES");
   for (PsiErrorElement err : PsiTreeUtil.collectElementsOfType(pf,PsiErrorElement.class)) {
      BubjetLog.logD("FOUND ERROR ELEMENT: " + err + " " + err.getContainingFile() + " " +
            err.getTextRange() + " " + err.getParent() + " " + err.getParent().getTextRange() + " " +
               err.getChildren().length + " " + err.getErrorDescription());
//    BubjetUtil.outputProblem(file_data.getProject(),file_data.getDocument(),err,xw);
    }
   scanForErrors(xw);
   xw.end("MESSAGES");
   if (checkCurrent()) {
      file_data.getMonitor().finishMessage(xw);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Error scanner                                                           */
/*                                                                              */
/********************************************************************************/

private void scanForErrors(IvyXmlWriter xw)
{
   ErrorScanner ev = new ErrorScanner(xw);
   BubjetLog.logD("START SCAN");
   boolean fg = ev.scan();
   BubjetLog.logD("FINISH SCAN " + fg);
}





private boolean checkCurrent()
{
   boolean fg = file_data.isCurrent(bubbles_id,edit_id);
   if (!fg) BubjetLog.logD("ERROR PASS NOT CURRENT");
   
   return fg;
}



/********************************************************************************/
/*                                                                              */
/*      Psi Tree Visitor to check for errors                                    */
/*                                                                              */
/********************************************************************************/

private class ErrorScanner {
   
   private ErrorHolder  our_holder;
   private List<Annotator> our_annotators;
   private List<HighlightVisitor> our_visitors;
   
   ErrorScanner(IvyXmlWriter xw) { 
      our_holder = new ErrorHolder(xw);
      our_annotators = new ArrayList<>();
      Language lang = LanguageUtil.getLanguageForPsi(file_data.getProject(),file_data.getVirutalFile());
      BubjetLog.logD("USE LANGUAGE " + lang);
      for (Annotator a : LanguageAnnotators.INSTANCE.allForLanguageOrAny(lang)) {
         try {
            Annotator a1 = ReflectionUtil.newInstance(a.getClass());
            BubjetLog.logD("USE ANNOTATOR " + a1);
            our_annotators.add(a1);
          }
         catch (Exception e) { }
       }
      our_visitors = new ArrayList<>();
      for (HighlightVisitor hv : HighlightVisitor.EP_HIGHLIGHT_VISITOR.getExtensions(file_data.getProject())) {
         HighlightVisitor hv1 = hv.clone();
         BubjetLog.logD("CHECK VISITOR " + hv1);
         String nm = hv.getClass().getName();
         int idx = nm.lastIndexOf(".");
         if (idx > 0) nm = nm.substring(idx+1);
         switch (nm) {
            case "DefaultHighlightVisitor" :
            case "HighlightVisitorImpl" :
               our_visitors.add(hv1);
               break;
          }
       }
    }
   
   boolean scan() {
      boolean rslt = true;
      for (HighlightVisitor hv : our_visitors) {
         BubjetLog.logD("RUN VISITOR " + hv + " " + hv.suitableForFile(file_data.getPsiFile()));
         if (hv.suitableForFile(file_data.getPsiFile())) {
            boolean fg = hv.analyze(file_data.getPsiFile(),true,our_holder,new ErrorRunner(hv));
            rslt &= fg;
          }
       }
      return rslt;
    }
   
}       // end of inner class ErrorVisitor



private class ErrorRunner extends PsiRecursiveElementWalkingVisitor implements Runnable {
   
   private HighlightVisitor high_visitor;
   
   ErrorRunner(HighlightVisitor hv) {
      high_visitor = hv;
    }
   
   @Override public void run() {
      file_data.getPsiFile().accept(this);
    }
   
   @Override public void visitElement(PsiElement e) {
      high_visitor.visit(e);
      super.visitElement(e);
    }
   
}       // end of error runner




private class ErrorHolder extends HighlightInfoHolder {
   
   private IvyXmlWriter xml_writer;
   
   ErrorHolder(IvyXmlWriter xw) {
      super(file_data.getPsiFile());
      xml_writer = xw;
    }
   
   @Override public boolean add(HighlightInfo info) {
      if (info == null || info.getDescription() == null) return false;
      if (info.getSeverity() == HighlightSeverity.INFORMATION) return false;
      if (info.getSeverity() == HighlightInfoType.SYMBOL_TYPE_SEVERITY) return false;
      
      BubjetLog.logD("ADD HIGHLIGHT " + info.getStartOffset() + " " + info.getActualStartOffset() + " " +
            info.getEndOffset() + " " + info.getActualEndOffset() + " " +
            info.getDescription() + " " + info.getSeverity() + " " +
            info.getProblemGroup());
      
      BubjetUtil.outputProblem(file_data,info,xml_writer);
      
      return super.add(info);
    }
   
}       // end of inner class ErrorHolder



}       // end of class BubjetErrorPass




/* end of BubjetErrorPass.java */

