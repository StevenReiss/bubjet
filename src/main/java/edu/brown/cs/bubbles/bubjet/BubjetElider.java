/********************************************************************************/
/*                                                                              */
/*              BubjetElider.java                                               */
/*                                                                              */
/*      Handle elision output                                                   */
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiCall;
import com.intellij.psi.PsiCallExpression;
import com.intellij.psi.PsiCatchSection;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSwitchBlock;
import com.intellij.psi.PsiSwitchLabelStatementBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.ClassUtil;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BubjetElider implements BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private List<ElidePriority> elide_pdata;
private List<ElideRegion> elide_rdata;

//TODO: replace up_map, scaleUp, and merge with Prioritizer methods

private static Prioritizer down_priority;

private static final double	UP_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_SCALE = 0.8;
private static final double	DOWN_DEFAULT_COUNT = 0.95;
private static final double	DOWN_DEFAULT_ITEM  = 0.99;
private static final double	SWITCH_BLOCK_SCALE = 0.90;



static {
   down_priority = new Prioritizer(DOWN_DEFAULT_SCALE,DOWN_DEFAULT_COUNT,DOWN_DEFAULT_ITEM);
}




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetElider()
{ 
   elide_pdata = new ArrayList<>();
   elide_rdata = new ArrayList<>();
}




/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void noteEdit(EditData ed) 
{ 
   int soff = ed.getOffset();
   int len = ed.getLength();
   int rlen = 0;
   if (ed.getText() != null) rlen = ed.getText().length();
   
   for (Iterator<ElidePriority> it = elide_pdata.iterator(); it.hasNext(); ) {
      ElidePriority ep = it.next();
      if (!ep.noteEdit(soff,len,rlen)) it.remove();
    }

}


void clearElideData()
{
   elide_pdata.clear();
   elide_rdata.clear();
}


void addElidePriority(int soff,int eoff,double pri)
{
   ElidePriority ep = new ElidePriority(soff,eoff,pri);
   elide_pdata.add(ep);
}


void addElideRegion(int soff,int eoff)
{
   ElideRegion er = new ElideRegion(soff,eoff);
   elide_rdata.add(er);
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean computeElision(PsiFile pf,IvyXmlWriter xw)
{
   if (pf == null || elide_rdata.isEmpty()) return false;
   
   ElidePass1 ep1 = null;
   if (elide_pdata.isEmpty()) {
      ep1 = new ElidePass1();
      pf.accept(ep1);
    }
   
   ElidePass2 ep2 = new ElidePass2(ep1,xw);
   pf.accept(ep2);
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Main priority method                                                    */
/*                                                                              */
/********************************************************************************/

private double computePriority(double parprior,PsiElement base,double pass1prior)
{
   double p = down_priority.getPriority(parprior,base);
   
   if (pass1prior > p) p = pass1prior;
   
   return p;
}




/********************************************************************************/
/*										*/
/*	Access methods for elision information					*/
/*										*/
/********************************************************************************/

private double getElidePriority(PsiElement n)
{
   for (ElidePriority ep : elide_pdata) {
      if (ep.useForPriority(n)) return ep.getPriority();
    }
   
   return 0;
}



private boolean isActiveRegion(TextRange rng)
{
   for (ElideRegion er : elide_rdata) {
      if (er.overlaps(rng)) return true;
    }
   
   return false;
}


private boolean isActiveRegion(PsiElement e) 
{
   boolean fg = isActiveRegion(e.getTextRange());
// BubjetLog.logD("ELIDE ACTIVE " + e + " " + e.getTextRange() + " " + fg);
   return fg;
}


private boolean isRootRegion(TextRange rng)
{
   for (ElideRegion er : elide_rdata) {
      if (er.contains(rng)) return true;
    }
   
   return false;
}



private double scaleUp(PsiElement n)
{
   return UP_DEFAULT_SCALE;
}



/********************************************************************************/
/*                                                                              */
/*      Formatting type information methods                                     */
/*                                                                              */
/********************************************************************************/

private String getFormatType(PsiElement e)
{
   if (!(e instanceof PsiIdentifier)) return null;
   String typ = null;
   // handle Defifinitions
   PsiElement last = null;
   for (PsiElement p = e; p != null && typ == null; p = p.getParent()) {
//    BubjetLog.logD("FORMAT TYPE " + p );
      if (p instanceof PsiMethod) {
         PsiMethod pm = (PsiMethod) p;
         if (pm.getNameIdentifier() == e) typ = "METHODDECL" + getMethodType(pm);
       }
      else if (p instanceof PsiCatchSection) {
         PsiCatchSection c = (PsiCatchSection) p;
         if (c.getParameter() == last) typ = "EXCEPTIONDECL";
       }
      else if (p instanceof PsiLocalVariable) {
         typ = "LOCALDECL";
       }
      else if (p instanceof PsiParameter) {
         typ = "PARAMDECL";
       }
      else if (p instanceof PsiClass) {
         typ = "CLASSDECL" + getClassType((PsiClass) p);
       }
      else if (p instanceof PsiField) {
         typ = "FIELDDECL";
       }
      else if (p instanceof PsiStatement) break;
      else if (p instanceof PsiReferenceExpression) break;
      else if (p instanceof PsiReference) break;
      last = p;
    }

   if (typ == null) {
      PsiElement def = BubjetUtil.getDefinitionElement(e);
      if (def == null) typ = "UNDEF";
      if (def instanceof PsiEnumConstant) typ = "ENUMC";
      else if (def instanceof PsiField) typ = "FIELD" + getVariableType((PsiField) def);
      else if (def instanceof PsiMethod) typ = "CALL" + getMethodType((PsiMethod) def);
      else if (def instanceof PsiAnnotation) typ = "ANNOT";
      else if (def instanceof PsiClass) {
         PsiClass pc = (PsiClass) def;
         if (pc.isAnnotationType()) typ = "ANNOT";
         else typ = "TYPE";
       }
      else if (def instanceof PsiType) typ = "TYPE";
    }
   
   BubjetLog.logT("Identifier " + e + " -> " + typ);
   
   return typ;
}

private String getMethodType(PsiMethod mb)
{
   if (mb == null) return "U";
   
   String typ = "";
   
   if (mb.isDeprecated()) typ = "D";
   
   PsiModifierList ml = mb.getModifierList();
   if (ml.hasModifierProperty(PsiModifier.ABSTRACT)) typ = "A";
   else if (ml.hasModifierProperty(PsiModifier.STATIC)) typ = "S";
   
   return typ;
}


private String getVariableType(PsiVariable v)
{
   if (v == null) return "U";
   
   String typ = "";
   
   PsiModifierList ml = v.getModifierList();
   if (ml.hasModifierProperty(PsiModifier.STATIC)) {
      if (ml.hasModifierProperty(PsiModifier.FINAL)) typ = "C";
      else typ = "S";
    }
   
   return typ;
}


private String getClassType(PsiClass c)
{
   if (c == null) return "";
   
   String typ = "";
   if (c.getContainingClass() != null) typ = "M";
   if (c.getScope() instanceof PsiMethod) typ = "L";

   return typ;
}



private String getNodeType(PsiElement e)
{
   String typ = null;
   
   if (e instanceof PsiBlockStatement) {
      typ = "BLOCK";
    }
   else if (e instanceof PsiCodeBlock) {
      typ = "BLOCK";
    }
   else if (e instanceof PsiSwitchLabelStatementBase) {
      typ = "CASE";
    }
   else if (e instanceof PsiStatement) {
      typ = "STMT";
    }
   else if (e instanceof PsiCatchSection) {
      typ = "CATCH";
    }
   else if (e instanceof PsiClass) {
      PsiClass pc = (PsiClass) e;
      if (pc.isAnnotationType()) typ = "ATYPE";
      else if (pc.isEnum()) typ = "ENUM";
      else if (pc.getScope() instanceof PsiMethod) typ = "ACLASS";
      else typ = "CLASS";
    }
   else if (e instanceof PsiEnumConstant) {
      typ = "ENUMC";
    }
   else if (e instanceof PsiField) {
      typ = "FIELD";
    }
   else if (e instanceof PsiClassInitializer) {
      typ = "INITIALIZER";
    }
   else if (e instanceof PsiMethod) {
      typ = "METHOD";
    }
   else if (e instanceof PsiAnnotation) {
      typ = "ANNOT";
    }
   else if (e instanceof PsiFile) {
      typ = "COMPUNIT";
    }
   else if (e instanceof PsiImportStatement) {
      typ = "IMPORT";
    }
   else if (e instanceof PsiCallExpression) {
      typ = "CALL";
      PsiElement p = e.getParent();
      if (p instanceof PsiExpression) ;
      else if (p instanceof PsiType) ;
      else typ = "CALLEXPR";   
    }
   else if (e instanceof PsiExpression) {
      PsiElement p = e.getParent();
      if (p instanceof PsiExpression) ;
      else if (p instanceof PsiType) {
         BubjetLog.logD("SKIP TYPE ");
       }
      else typ = "EXPR";
    }
   
   return typ;
}



/********************************************************************************/
/*                                                                              */
/*      Output information for hints                                            */
/*                                                                              */
/********************************************************************************/

private void outputHintData(PsiElement n,IvyXmlWriter xw)
{
   PsiMethod mthd = null;
   if (n instanceof PsiCall) {
      mthd = ((PsiCall) n).resolveMethod();
    }
   
   if (mthd != null) {
      xw.begin("HINT");
      xw.field("KIND","METHOD");
      if (mthd.isConstructor()) xw.field("CONSTRUCTOR",true);
      if (mthd.getReturnType() != null) {
         String rettyp = ClassUtil.getBinaryPresentation(mthd.getReturnType());
         xw.field("RETURNS",rettyp);
       }
      xw.field("NUMPARAM",mthd.getParameters().length);
      for (JvmParameter pp : mthd.getParameters()) {
         xw.begin("PARAMETER");
         xw.field("NAME",pp.getName());
         BubjetLog.logD("PARAMETER " + pp.getName() + " " + pp.getType());
         xw.end("PARAMETER");
       }
      xw.end("HINT");
    }
}




/********************************************************************************/
/*                                                                              */
/*      Tree walk for setting initial priorities                                */
/*                                                                              */
/********************************************************************************/

private class ElidePass1 extends JavaRecursiveElementVisitor {
   
   private Map<PsiElement,Double> result_value;
   private int inside_count;
   
   ElidePass1() {
      result_value = new HashMap<>();
      inside_count = 0;
    }
   
   double getPriority(PsiElement e) {
      Double dv = result_value.get(e);
      if (dv != null) return dv.doubleValue();
      return 0.0;
    }
   
   @Override public void visitElement(PsiElement e) {
      if (inside_count > 0) {
         ++inside_count;
       }
      else {
         double p = getElidePriority(e);
         if (p != 0) {
            result_value.put(e,p);
            ++inside_count;
          }
       }
      super.visitElement(e);
      if (inside_count > 0) {
         --inside_count;
       }
      else {
         double p = 0;
         for (PsiElement ce : e.getChildren()) {
            p = merge(p,ce);
          }
         if (p > 0) {
            p *= scaleUp(e);
            result_value.put(e,p);
          }
       }
    }
   
   @Override public void visitMethod(PsiMethod e) {
      if (isActiveRegion(e)) {
         super.visitMethod(e);
       }
    }
   
   @Override public void visitClass(PsiClass e) {
      if (isActiveRegion(e)) {
         super.visitClass(e);
       }
    }
   
   @Override public void visitClassInitializer(PsiClassInitializer e) {
      if (isActiveRegion(e)) {
         super.visitClassInitializer(e);
       }
    }
   
   private double merge(double p,PsiElement e) {
      Double dv = result_value.get(e);
      if (dv == null) return p;
      double q = 1 - (1-p)*(1-dv.doubleValue());
      return q;
    }
   
}       // end of inner class ElidePass1



/********************************************************************************/
/*                                                                              */
/*      Tree walk for setting final priorities                                  */
/*                                                                              */
/********************************************************************************/

private class ElidePass2 extends JavaRecursiveElementVisitor {
   
   private ElidePass1 up_values;
   private IvyXmlWriter xml_writer;
   private Map<PsiElement,Double> result_value;
   private PsiElement active_node;
   private boolean last_case;
   private Stack<PsiElement> switch_stack;
   
   ElidePass2(ElidePass1 up,IvyXmlWriter xw) {
      up_values = up;
      xml_writer = xw;
      result_value = new HashMap<>();
      active_node = null;
      last_case = false;
      switch_stack = new Stack<>();
    }
   
   @Override public void visitElement(PsiElement e) {
      visitElement(e,true);
    }
   
  private void visitElement(PsiElement e,boolean chld) {
//    BubjetLog.logD("ELIDE VISIT: " + e + " " + e.getTextRange());
      boolean finish = false;
      if (active_node == null) {
         if (isRootRegion(e.getTextRange())) {
            active_node = e;
            result_value.put(e,1.0);
            finish = outputXmlStart(e);
          }
       }
      else {
         double v = getPriority(e.getParent());
         double v0 = 0;
         if (up_values != null) v0 = up_values.getPriority(e);
         double p = computePriority(v,e,v0);
         if (p != 0) {
            result_value.put(e,p);
            checkSwitchBlock(e);
            finish = outputXmlStart(e);
          }
         else {
            BubjetLog.logD("Zero priority " + v + " " + e + " " + v0);
          }
       }
      if (chld) super.visitElement(e);
      if (active_node == e) active_node = null;
      if (finish) {
         outputHintData(e,xml_writer);
         xml_writer.end("ELIDE");
       }
      checkEndSwitchBlock(e);
    }
   
   @Override public void visitPackageStatement(PsiPackageStatement n) {
      if (isActiveRegion(n)) super.visitPackageStatement(n);
    }
   @Override public void visitImportList(PsiImportList n) {
      if (isActiveRegion(n)) super.visitElement(n);
    }
   @Override public void visitClass(PsiClass n) {
      if (isActiveRegion(n)) super.visitClass(n);
    }
   @Override public void visitMethod(PsiMethod n) {
      if (isActiveRegion(n)) super.visitMethod(n);
    }
   @Override public void visitClassInitializer(PsiClassInitializer n) {
      if (isActiveRegion(n)) super.visitClassInitializer(n);
    }
   @Override public void visitComment(PsiComment n) {    
      if (n.getTokenType() != JavaTokenType.END_OF_LINE_COMMENT) super.visitComment(n);
    }
   @Override public void visitDocComment(PsiDocComment n) {
      visitElement(n,false);
    }
   
   @Override public void visitWhiteSpace(PsiWhiteSpace n)       { }
   @Override public void visitKeyword(PsiKeyword n)             { }
   @Override public void visitJavaToken(PsiJavaToken n) {
      if (n instanceof PsiIdentifier) super.visitJavaToken(n);
    }
   @Override public void visitModifierList(PsiModifierList n) {
      super.visitElement(n);
    }
   @Override public void visitParameterList(PsiParameterList n) {
      super.visitElement(n);
    }
   @Override public void visitTypeParameterList(PsiTypeParameterList n) {
      super.visitElement(n);
    }
   @Override public void visitReferenceList(PsiReferenceList n) {
      super.visitElement(n);
    }
   @Override public void visitExpressionList(PsiExpressionList n) {
      super.visitElement(n);
    }
   @Override public void visitReferenceExpression(PsiReferenceExpression n) {
      super.visitElement(n);
    }
   @Override public void visitReferenceParameterList(PsiReferenceParameterList n) {
      super.visitElement(n);
    }
   @Override public void visitAnnotationParameterList(PsiAnnotationParameterList n) {
      super.visitElement(n);
    }
   @Override public void visitBlockStatement(PsiBlockStatement n) {
      super.visitElement(n);
    }
   
   double getPriority(PsiElement e) {
      Double dv = result_value.get(e);
      if (dv != null) return dv.doubleValue();
      else if (e.getParent() != null) return getPriority(e.getParent());
      return 0;
    }
   
   private boolean outputXmlStart(PsiElement e) {
      if (xml_writer == null) return false;
      TextRange tr0 = e.getTextRange();
      TextRange tr1 = BubjetUtil.getExtendedRange(e);
      int sp = tr0.getStartOffset();
      int esp = tr1.getStartOffset();
      int ln = tr0.getLength();
      int eln = tr1.getLength();
      if (ln == 0 && eln == 0) {
         BubjetLog.logD("Zero length element: " + e + " " + tr0 + " " + tr1);
         return false;
       }  
      xml_writer.begin("ELIDE");
      xml_writer.field("START",sp);
      if (esp != sp) xml_writer.field("ESTART",esp);
      xml_writer.field("LENGTH",ln);
      if (eln != ln) xml_writer.field("ELENGTH",eln);
      double p = result_value.get(e);
      for (int i = 0; i < switch_stack.size(); ++i) p *= SWITCH_BLOCK_SCALE;
      xml_writer.field("PRIORITY",p);
      String typ = getFormatType(e);
      if (typ != null) {
         xml_writer.field("TYPE",typ);
         if (typ.startsWith("METHODDECL") || typ.startsWith("FIELDDECL") ||
               typ.startsWith("CLASSDECL")) {
            outputDeclInfo(e);
          }
       }
      String ttyp = getNodeType(e);
      if (ttyp != null) xml_writer.field("NODE",ttyp);
      if (e instanceof PsiErrorElement) xml_writer.field("ERROR",true);
      return true;
    } 
   
   private void outputDeclInfo(PsiElement e) {
      PsiElement def = BubjetUtil.getDefinitionElement(e);
      if (def == null) return;
      
      if (def instanceof PsiClass) {
         PsiClass pc = (PsiClass) def;
         xml_writer.field("FULLNAME",pc.getQualifiedName());
         xml_writer.field("MEMBER",pc.getContainingClass() != null);
         xml_writer.field("LOCAL",pc.getScope() instanceof PsiMethod);
       }
      else if (def instanceof PsiMethod) {
         PsiMethod pm = (PsiMethod) def;
         StringBuffer buf = new StringBuffer();
         buf.append(pm.getContainingClass().getQualifiedName());
         buf.append(".");
         buf.append(pm.getName());
         buf.append("(");
         int ct = 0;
         for (PsiParameter pp : pm.getParameterList().getParameters()) {
            if (ct++ > 0) buf.append(",");
            buf.append(pp.getType().getCanonicalText());
          }
         buf.append(")");
         xml_writer.field("FULLNAME",buf.toString());
       }
      else if (def instanceof PsiField) {
         PsiField pf = (PsiField) def;
         String nm = pf.getContainingClass().getQualifiedName();
         nm += "." + pf.getName();
         xml_writer.field("FULLNAME",nm);
       }
    }
   
   private void checkSwitchBlock(PsiElement e) {
      if (!last_case || xml_writer == null) return;
      // BedrockPlugin.logD("SWITCH BLOCK CHECK " + result_value.get(n) + " " + n);
      last_case = false;
      if (result_value.get(e) == null) return;
      if (e instanceof PsiSwitchLabelStatementBase) return;
      PsiElement last = null;
      if (e instanceof PsiStatement) {
         PsiElement pn = e.getParent();
         if (!(pn instanceof PsiSwitchBlock)) return;
         PsiSwitchBlock sb = (PsiSwitchBlock) pn;
         PsiStatement [] body = sb.getBody().getStatements();
         int fnd = -1;
         int lidx;
         for (lidx = 0; lidx < body.length; ++lidx) {
            if (body[lidx] == e) fnd = lidx;
            if (fnd >= 0 && lidx+1 < body.length) {
               if (body[lidx+1] instanceof PsiSwitchLabelStatementBase) break;
               else if (!(body[lidx+1] instanceof PsiStatement)) return;
             }
          }
         if (lidx - fnd >= 2) last = body[lidx];
       }
      if (last == null) return;
      xml_writer.begin("ELIDE");
      TextRange tr0 = e.getTextRange();
      TextRange tr1 = BubjetUtil.getExtendedRange(e);
      int sp = tr0.getStartOffset();
      int esp = tr1.getStartOffset();
      int ln = tr0.getLength();
      int eln = tr1.getLength();
      xml_writer.field("START",sp);
      if (esp != sp) xml_writer.field("ESTART",esp);
      xml_writer.field("LENGTH",ln);
      if (eln != ln) xml_writer.field("ELENGTH",eln);
      double p = result_value.get(e);
      for (int i = 0; i < switch_stack.size(); ++i) p *= SWITCH_BLOCK_SCALE;
      xml_writer.field("PRIORITY",p);
      xml_writer.field("NODE","SWBLOCK");
      switch_stack.push(last);
    }
   
   private void checkEndSwitchBlock(PsiElement e) {
      while (!switch_stack.isEmpty() && e == switch_stack.peek()) {
         switch_stack.pop();
         xml_writer.end("ELIDE");
       }
      last_case = (e instanceof PsiSwitchLabelStatementBase);
    }
   
}       // end of inner class ElidePass2




/********************************************************************************/
/*										*/
/*	Classes for elision region and priorities				*/
/*										*/
/********************************************************************************/

private abstract class ElideData {

   private int start_offset;
   private int end_offset;
   
   ElideData(int soff,int eoff) {
      start_offset = soff;
      end_offset = eoff;
      BubjetLog.logD("SET ELIDE DATA " + soff + " " + eoff);
    }
   
   boolean contains(TextRange rng) { 							
      return (start_offset <= rng.getStartOffset() && end_offset >= rng.getEndOffset());
    }
   
   boolean useForPriority(PsiElement n) {
      if (start_offset != end_offset) return contains(n.getTextRange());
      if (!overlaps(n.getTextRange())) return false;
      for (PsiElement celt : n.getChildren()) {
         if (isListElement(celt)) {
            for (PsiElement ccelt : celt.getChildren()) {
              if (overlaps(ccelt.getTextRange())) return false; 
             }
          }
         if (overlaps(celt.getTextRange())) return false;
       }
      return true;
    }
   
   boolean overlaps(TextRange rng) {
      if (start_offset >= rng.getEndOffset()) return false;
      if (end_offset <= rng.getStartOffset()) return false;
      return true;
    }
   
   boolean noteEdit(int soff,int len,int rlen) {
      if (end_offset <= soff) ; 			// before the change
      else if (start_offset > soff + len - 1) { 	// after the change
         start_offset += rlen - len;
         end_offset += rlen - len;
       }
      else if (start_offset <= soff && end_offset >= soff+len-1) {	// containing the change
         end_offset += rlen -len;
       }
      else return false;				     // in the edit -- remove it
      return true;
    }
   
}	// end of inner abstract class ElideData


private class ElideRegion extends ElideData {
   
   ElideRegion(int soff,int eoff) {
      super(soff,eoff);
    }
   
}	// end of innerclass ElideData




private class ElidePriority extends ElideData {
   
   private double elide_priority;
   
   ElidePriority(int soff,int eoff,double pri) {
      super(soff,eoff);
      elide_priority = pri;
    }
   
   double getPriority() 			{ return elide_priority; }
   
}	// end of innerclass ElideData



/********************************************************************************/
/*										*/
/*	Priority computation classes						*/
/*										*/
/********************************************************************************/

private static class Prioritizer {

   private double base_value;
   private double count_scale;
   private double item_scale;
   
   Prioritizer(double v,double scl,double iscl) {
      base_value = v;
      count_scale = scl;
      item_scale = iscl;
    }
   
   double getPriority(double ppar,PsiElement base) {
      double dv = base_value;
      if (base.getParent() == null) return ppar * dv;
      
      double p0 = handleSpecialCases(ppar,base);
      if (p0 > 0) return p0;
      
      PsiElement chld = base;
      PsiElement par = base.getParent();
      if (isListElement(par)) {
         chld = par;
         par = par.getParent();
       }
      if (item_scale != 1) {
         for (PsiElement pe : par.getChildren()) {
            if (pe == chld) break;
            dv *= item_scale;
          }
       }
      if (count_scale != 1 && chld != base) {
         boolean fnd = false;
         for (PsiElement sib : chld.getChildren()) {
            if (sib == base) fnd = true;
            dv *= count_scale;
            if (!fnd) dv *= item_scale;
          }
       }
      
      return ppar * dv;
    }
   
   private double handleSpecialCases(double ppar,PsiElement base) {
      if (base instanceof PsiIfStatement && 
            base.getParent() instanceof PsiIfStatement) {
         PsiIfStatement pstmt = (PsiIfStatement) base.getParent();
         if (pstmt.getElseBranch() == base) return ppar;
       }
       return -1;    
    }
         
}	// end of innerclass DefaultPrioritizer




private static boolean isListElement(PsiElement elt)
{
   String nm = elt.getClass().getName();
   
   if (nm.endsWith("ListImpl")) return true;
   
   return false;
}


}       // end of class BubjetElider




/* end of BubjetElider.java */

