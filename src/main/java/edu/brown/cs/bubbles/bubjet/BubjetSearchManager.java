/********************************************************************************/
/*                                                                              */
/*              BubjetSearchManager.java                                        */
/*                                                                              */
/*      Handle search requests                                                  */
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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.IndexPattern;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Processor;
import com.intellij.util.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.intellij.find.TextSearchService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;

import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.ivy.file.IvyFile;

class BubjetSearchManager implements BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService app_service;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetSearchManager(BubjetApplicationService app)
{
   app_service = app;
}



/********************************************************************************/
/*                                                                              */
/*      Handle FIND commands to work on a location                              */
/*                                                                              */
/********************************************************************************/

void handleFindAll(Project p,Module m,String file,int start,int end,
      boolean defs,boolean refs,
      boolean impls,boolean equiv,boolean exact,boolean system,boolean typeof,
      boolean ronly,boolean wonly,
      IvyXmlWriter xw)
{
   FindAllAction faa = new FindAllAction(p,m,file,start,end,defs,refs,impls,
         equiv,exact,system,typeof,ronly,wonly,xw);
   faa.start();
}


class FindAllAction extends BubjetAction.Read {
   
   private Project for_project;
   private Module for_module;
   private String for_file;
   private int start_location;
   private int end_location;
   private boolean do_defs;
   private boolean do_refs;
   private boolean do_implements;
   private boolean do_typeof;
   private boolean do_ronly;
   private boolean do_wonly;
   private IvyXmlWriter xml_writer;
   
   FindAllAction(Project p,Module m,String file,int start,int end,
         boolean defs,boolean refs,
         boolean impls,boolean equiv,boolean exact,boolean system,boolean typeof,
         boolean ronly,boolean wonly,
         IvyXmlWriter xw) {
      super(p,"Find All");
      for_project = p;
      for_module = m;
      for_file = file;
      start_location = start;
      end_location = end;
      do_defs = defs;
      do_refs = refs;
      do_implements = impls;
      do_typeof = typeof;
      do_ronly = ronly;
      do_wonly = wonly;
      xml_writer = xw;
    }
         
   @Override void process() {
      List<PsiElement> elts = findElementsInRange(for_project,for_file,
            start_location,end_location);
      BubjetLog.logD("PROCESS ELEMENTS " + elts.size());
      if (elts.size() == 1 && !do_typeof) {
         PsiElement e1 = elts.get(0);
         PsiElement e0 = BubjetUtil.getDefinitionElement(e1);
         BubjetLog.logD("SEARCH FOR " + e1 + " " + e0);
         xml_writer.begin("SEARCHFOR");
         if (e0 instanceof PsiField) {
            xml_writer.field("TYPE","Field");
          }
         else if (e0 instanceof PsiLocalVariable) {
            xml_writer.field("TYPE","Local");
          }
         else if (e0 instanceof PsiClass) {
            xml_writer.field("TYPE","Class");
          }
         else if (e0 instanceof PsiMethod) {
            xml_writer.field("TYPE","Function");
          }
         else {
            BubjetLog.logE("Unknown element type " + e0 + " " + e0.getClass());
          }
         xml_writer.text(e1.getText());
         xml_writer.end("SEARCHFOR");
       }
      
      GlobalSearchScope scp = BubjetUtil.getSearchScope(for_project,for_module);
   
      for (PsiElement elt : elts) {
         PsiElement e0 = BubjetUtil.getDefinitionElement(elt);
         outputRelevantElements(e0,for_module,scp,do_defs,do_refs,do_implements,
               do_ronly,do_wonly,xml_writer);
       }
    }
   
}       // end of inner class FindAllAction



private List<PsiElement> findElementsInRange(Project p,String file,int start,int end)
{
   List<PsiElement> rslt = new ArrayList<>();
   
   PsiFile pf = BubjetUtil.getPsiFile(p,file);
   
   if (pf == null) {
      BubjetLog.logD("Cant find file by path " + file);
      return rslt;
    }
   BubjetLog.logD("FILE " + pf + " " + pf.getOwnDeclarations().size() + " " +
         pf.getOwnReferences().size());
   
   for (PsiElement elt = pf.findElementAt(start); 
      elt.getTextRange().getStartOffset() <= end;
      elt = getNextLeaf(elt)) {
      BubjetLog.logD("CONSIDER ELEMENT " + elt + " " + elt.getClass() + " " +
            elt.getReference() + " " + elt.getReferences().length);
      if (isRelevantElement(elt)) rslt.add(elt);
    }
   
   return rslt;
}


private PsiElement getNextLeaf(PsiElement e)
{
   while (e != null) {
      PsiElement c1 = e.getNextSibling();
      if (c1 != null) {
         while (c1.getFirstChild() != null) c1 = c1.getFirstChild();
         return c1;
       }
      e = e.getParent();
    }
   return null;
}


private boolean isRelevantElement(PsiElement e)
{
   if (e instanceof PsiVariable) return true;
   if (e instanceof PsiParameter) return true;
   if (e instanceof PsiField) return true;
   if (e instanceof PsiMethod) return true;
   if (e instanceof PsiTypeElement) return true;
   if (e instanceof PsiIdentifier) return true;
   
   BubjetLog.logD("SKIP ELEMENT " + e + " " + e.getClass());
   
   return false;
}






private boolean isWriteInstance(PsiElement e)
{
   if (e instanceof PsiExpression) {
      return PsiUtil.isAccessedForWriting(((PsiExpression) e));
    }
   return false;
}






private void outputRelevantElements(PsiElement e0,Module m,GlobalSearchScope scp,
      boolean defs,boolean refs,boolean impls,boolean ronly,boolean wonly,
      IvyXmlWriter xw)
{
   if (e0 == null) return; 
   if (defs) {
      outputElementResult(m,e0,e0,xw);
    }
   if (refs) {
      Query<PsiReference> q = ReferencesSearch.search(e0,scp);
      QueryRefResult qr = new QueryRefResult(m,e0,ronly,wonly,xw); 
      q.forEach(qr);
    }
   if (impls) {
      QueryEltResult qe = new QueryEltResult(m,xw);
      if (e0 instanceof PsiMethod) {
         PsiMethod pmthd = (PsiMethod) e0;
         Query<PsiMethod> q = OverridingMethodsSearch.search(pmthd,scp,true);
         q.forEach(qe);
       }
      else if (e0 instanceof PsiClass) {
         PsiClass pc = (PsiClass) e0;
         Query<PsiClass> q = ClassInheritorsSearch.search(pc,scp,true,true,false);
         q.forEach(qe);
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Handle getFullyQualifiedName                                            */
/*                                                                              */
/********************************************************************************/

void handleGetFullyQualifiedName(Project p,Module m,String file,int start,int end,
      IvyXmlWriter xw)
{
   QualifiedNameAction qna = new QualifiedNameAction(p,m,file,start,end,xw);
   qna.start();
}



private class QualifiedNameAction extends BubjetAction.Read {
   
   private Project for_project;
   private Module for_module;
   private String for_file;
   private int start_location;
   private int end_location;
   private IvyXmlWriter xml_writer;
   
   QualifiedNameAction(Project p,Module m,String file,int start,int end,IvyXmlWriter xw) {
      for_project = p;
      for_module = m;
      for_file = file;
      start_location = start;
      end_location = end;
      xml_writer = xw;
    }
   
   @Override void process() {
      List<PsiElement> elts = findElementsInRange(for_project,for_file,start_location,end_location);
      if (elts.isEmpty()) return;
      PsiElement elt = elts.get(0);
      PsiElement def = BubjetUtil.getDefinitionElement(elt);
      System.err.println("FOUND " + def); 
      String rslt = null;
      String sgn = null;
      if (def instanceof PsiClass) {
         PsiClass pc = (PsiClass) def;
         rslt = pc.getQualifiedName();
       }
      else if (def instanceof PsiField) {
         PsiField pfld = (PsiField) def;
//    rslt = pfld.getContainingClass().getQualifiedName() + "." + pf.getName();
         rslt = PsiUtil.getMemberQualifiedName(pfld);
         sgn = pfld.getType().getCanonicalText();
       }
      else if (def instanceof PsiMethod) {
         PsiMethod pmthd = (PsiMethod) def;
         rslt = PsiUtil.getMemberQualifiedName(pmthd);
         sgn = ClassUtil.getAsmMethodSignature(pmthd);
       }
      else if (def instanceof PsiClassInitializer) {
         PsiClassInitializer ci = (PsiClassInitializer) def;
         rslt = ci.getContainingClass().getQualifiedName() + ".<clinit>";
       }
      else if (def instanceof PsiPackageStatement) {
         PsiPackageStatement pps = (PsiPackageStatement) def;
         rslt = pps.getPackageName();
       }
      else if (def instanceof PsiLocalVariable) {
         PsiLocalVariable lcl = (PsiLocalVariable) def;
         sgn = lcl.getType().getCanonicalText();
         for (PsiElement pe = def; pe != null; pe = pe.getParent()) {
            if (pe instanceof PsiMember) {
               rslt = PsiUtil.getMemberQualifiedName((PsiMember) pe) + "." + lcl.getName();
               break;
             }
          }
       }
      
      if (rslt == null) return;
      xml_writer.begin("FULLYQUALIFIEDNAME");
      xml_writer.field("NAME",rslt);
      xml_writer.field("KEY",BubjetUtil.getKey(def,for_module));
      if (sgn != null) xml_writer.field("TYPE",sgn);
      xml_writer.end("FULLYQUALIFIEDNAME");
    }
   
}       // end of inner class QualifiedNameAction





/********************************************************************************/
/*                                                                              */
/*      Handle pattern search                                                   */
/*                                                                              */
/********************************************************************************/

void handleJavaSearch(Project p,Module m,String bid,String pat,
      String typ,boolean defs,boolean refs,boolean impls,boolean equiv,
      boolean exact,boolean system,IvyXmlWriter xw)
{
   JavaSearchAction jsa = new JavaSearchAction(p,m,pat,typ,defs,refs,impls,equiv,exact,system,xw);
   jsa.start();
  
}


private class JavaSearchAction extends BubjetAction.Read 
   implements Condition<String>, Processor<PsiElement> {
   
   private Project for_project;
   private Module for_module;
   private GlobalSearchScope search_scope;
   private String search_pattern;
   private String element_type;
   private boolean do_defs;
   private boolean do_refs;
   private boolean do_impls;
   private IvyXmlWriter xml_writer;
   private Pattern name_pattern;
   private String class_name;
   private String short_name;
   private Set<PsiElement> all_defs;
   
   JavaSearchAction(Project p,Module m,String pat,String typ,boolean defs,boolean refs,
         boolean impls,boolean equiv,boolean exact,boolean syst,IvyXmlWriter xw) {
      super(p,"Pattern Search");
      for_project = p;
      for_module = m;
      search_scope = BubjetUtil.getSearchScope(for_project,for_module);
      search_pattern = pat;
      element_type = typ;
      do_defs = defs;
      do_refs = refs;
      do_impls = impls;
      xml_writer = xw;
      all_defs = new HashSet<>();
    }
   
   @Override void process() {
      switch (element_type) {
         case "CLASS" :
         case "INTERFACE" :
         case "ENUM" :
         case "ANNOTATION" :
         case "CLASS&ENUM" :
         case "CLASS&INTERFACE" :
         case "TYPE" :
            buildTypePattern(search_pattern);
            break;
         case "FIELD" :
            // only used to find all fields of a class
            buildFieldPattern();
            break;
         case "METHOD" :
            buildMethodPattern();
            break;
         case "CONSTRUCTOR" :
            buildConstructorPattern();
            break;
         case "PACKAGE" :
         case "FIELDWRITE" :
         case "FIELDREAD" :
            // not used
            return;
       }
      
      BubjetLog.logD("CLASS SEARCH " + short_name + " " + name_pattern + " " + class_name); 
      Query<PsiClass> qc = AllClassesSearch.search(search_scope,for_project,this);
      qc.forEach(this);
      
      BubjetLog.logD("FOUND " + all_defs.size() + " definitions");
    }
   
   @Override public boolean value(String s) {
      if (short_name != null) return s.contains(short_name);
      if (name_pattern != null) {
         Matcher m = name_pattern.matcher(s);
         return m.matches();
       }
      BubjetLog.logD("Check class name condition for " + s + " " + search_pattern);
      return true;
    }
   
   @Override public boolean process(PsiElement def) {
      BubjetLog.logD("Consider candidate " + def);
      if (def instanceof PsiClass) {
         PsiClass pc = (PsiClass) def;
         BubjetLog.logD("Full name " + pc.getQualifiedName());
         switch (element_type) {
            case "FIELD" :
               if (!isValidClassName(pc)) return true;
               for (PsiField pf : pc.getAllFields()) {
                  processField(pf);
                }
               break;
            case "METHOD" :
               if (!isValidClassName(pc)) return true;
               for (PsiMethod pm : pc.getAllMethods()) {
                  processMethod(pm);
                }
               break;
            case "CONSTRUCTOR" :
               if (!isValidClassName(pc)) return true;
               for (PsiMethod pm : pc.getConstructors()) {
                  processConstructor(pm);
                }
               break;
            case "CLASS" :
            case "INTERFACE" :
            case "ENUM" :
            case "ANNOTATION" :
            case "CLASS&ENUM" :
            case "CLASS&INTERFACE" :
            case "TYPE" : 
               if (!isValidClassType(pc)) return true;
               if (!isValidClassName(pc)) return true;
               addDefinition(pc);
               break;
          }
       }
      else {
         BubjetLog.logD("Process element " + def);
       }
      
      return true;
    }
   
   private void addDefinition(PsiElement cand) {
      if (all_defs.add(cand)) {
         outputRelevantElements(cand,for_module,search_scope,do_defs,do_refs,do_impls,
              true,true,xml_writer);
       }
      BubjetLog.logD("Found definition " + cand);
      all_defs.add(cand);
    }
   
   private void processField(PsiField pf) {
      int idx = search_pattern.lastIndexOf(".");
      if (idx < 0) return;
      String pat = search_pattern.substring(idx+1);
      if (!pat.equals("*")) {
         if (!Pattern.matches(pat,pf.getName())) return;
       }
      addDefinition(pf);
    }
   
   private void processMethod(PsiMethod pm) {
      if (pm.isConstructor()) return;
      String key = search_pattern;
      String args = null;
      int idx1 = search_pattern.indexOf("(");
      if (idx1 > 0) {
         args = key.substring(idx1);
         key = key.substring(0,idx1);
       }     
      int idx2 = key.lastIndexOf(".");
      if (idx2 > 0) key = key.substring(idx2+1);
      if (!key.equals("*")) {
         if (key.contains("*")) {
            if (!Pattern.matches(key,pm.getName())) return;
          }
         else if (!key.equals(pm.getName())) return;
       }
      if (args != null) {
         int idx = args.lastIndexOf(")");
         args = args.substring(0,idx+1);
       }
      if (!matchArgs(pm,args)) return;
      addDefinition(pm);
    }
   
   private void processConstructor(PsiMethod pm) {
      if (!pm.isConstructor()) return;
      String args = null;
      int idx1 = search_pattern.indexOf("(");
      if (idx1 > 0) {
         args = search_pattern.substring(idx1);
         int idx2 = args.lastIndexOf(")");
         args = args.substring(0,idx2+1);
       }
      if (!matchArgs(pm,args)) return;
      addDefinition(pm);
    }
   
   private boolean matchArgs(PsiMethod pm,String args) {
      if (args == null) return true;
      BubjetLog.logD("MATCH method args " + pm.getParameters() + " " + args);
      for (PsiParameter pp : pm.getParameterList().getParameters()) {
         PsiType ptyp = pp.getType();
         BubjetLog.logD("PARAMETER " + ptyp.getCanonicalText() + " " +
               ptyp.getPresentableText());
       }
      return true;
    }
   
   private boolean isValidClassType(PsiClass pc) {
      boolean iface = pc.isInterface();
      boolean annot = pc.isAnnotationType();
      boolean etyp = pc.isEnum();
      switch (element_type) {
         case "CLASS" :
            return !iface && !annot && !etyp;
         case "INTERFACE" :
            return iface;
         case "ENUM" :
            return etyp;
         case "ANNOTATION" :
            return annot;
         case "CLASS&ENUM" :
            return !iface && !annot;
         case "CLASS&INTERFACE" :
            return !annot && !etyp;
         case "TYPE" : 
            return true;
       }
      return false;
    }
   
   private boolean isValidClassName(PsiClass pc) {
      if (class_name != null) {
         String key = class_name;
         int idx = key.lastIndexOf(".");
         if (idx < 0) return true;
         String qnm = pc.getQualifiedName();
         if (key.contains("*") || key.contains("@")) {
            return Pattern.matches(BubjetUtil.convertWildcardToRegex(key),qnm);
          }
         else {
            BubjetLog.logD("COMPARE NAMES: " + qnm + " " + key);
            return qnm.equals(key);
          }
       }
      return true;
    }
   
   private void buildTypePattern(String key) {
      String last = key;
      int idx = last.lastIndexOf(".");
      if (idx > 0) last = last.substring(idx+1);
      name_pattern = null;
      short_name = null;
      if (last.contains("*") || last.contains("@")) {
         name_pattern = Pattern.compile(BubjetUtil.convertWildcardToRegex(last));
         if (idx > 0) class_name = key;
       }
      else {
         short_name = last;
         class_name = key;
       }
    }
   
   private void buildFieldPattern() {
      String key = search_pattern;
      int idx = key.lastIndexOf(".");
      String cls = key.substring(0,idx);
      buildTypePattern(cls);
    }
   
   private void buildMethodPattern() {
      String key = search_pattern;
      int idx1 = search_pattern.indexOf("(");
      if (idx1 > 0) key = key.substring(0,idx1);
      int idx2 = key.lastIndexOf(".");
      if (idx2 > 0) {
         String cls = key.substring(0,idx2);
         buildTypePattern(cls);
       }
    }
   
   private void buildConstructorPattern() {
      String key = search_pattern;
      int idx1 = search_pattern.indexOf("(");
      if (idx1 > 0) key = key.substring(0,idx1);
      buildTypePattern(key);
    }
   
}       // end of inner class JavaSearchAction








/********************************************************************************/
/*                                                                              */
/*      Handle text search                                                      */
/*                                                                              */
/********************************************************************************/

void handleTextSearch(Project p,Module m,int flags,String pat,int max,
      IvyXmlWriter xw)
{
   GlobalSearchScope scp = BubjetUtil.getSearchScope(p,m);
   TextSearchAction tsa = new TextSearchAction(p,flags,pat,max,scp,xw);
   tsa.start();
}



private class TextSearchAction extends BubjetAction.Read {
    
   private String for_pattern;
   private int pattern_flags;
   private Project for_project;
   private GlobalSearchScope search_scope;
   private TextSearchProcessor search_processor;
   
   TextSearchAction(Project p,int flags,String pat,int max,GlobalSearchScope scp,IvyXmlWriter xw) {
      super(p,"Text Search");
      for_pattern = pat;
      pattern_flags = flags;
      for_project = p;
      search_scope = scp;
      QueryTextResult qtr = new QueryTextResult(p,xw);
      search_processor = new TextSearchProcessor(flags,pat,qtr,max);
    }
   
   @Override void process() {
      // these methods all require a single word
      if (pattern_flags == Pattern.LITERAL) {
         TextSearchService tss = TextSearchService.getInstance();
         tss.processFilesWithText(for_pattern,search_processor,search_scope);
       }
      else {
         PsiSearchHelper help = PsiSearchHelper.getInstance(for_project);
         String p1 = for_pattern;
         if ((pattern_flags & Pattern.LITERAL) != 0) {
            p1 = "\\Q" + p1 + "\\E";
          }
         boolean usecase = (pattern_flags & Pattern.CASE_INSENSITIVE) == 0;
         IndexPattern ipat = new IndexPattern(p1,usecase);
         String txt = null;
         for (String s : ipat.getWordsToFindFirst()) {
            if (txt == null) txt = s;
            else txt += " " + s;
            BubjetLog.logD("WORK ON WORD " + s);
          }
         help.processCandidateFilesForText(search_scope,UsageSearchContext.ANY,
               usecase,txt,search_processor);
       }
    }
   
}       // end of inner class TextSearchAction




private class TextSearchProcessor implements Processor<VirtualFile> {
   
   private int pattern_flags;
   private String pattern_text;
   private QueryTextResult text_result;
   private int max_result;
   private int num_result;
   
   TextSearchProcessor(int flags,String pat,QueryTextResult qir,int max) {
      pattern_flags = flags;
      pattern_text = pat;
      text_result = qir;
      max_result = max;
      num_result = 0;
    }
   
   @Override public boolean process(VirtualFile vf) {
      BubjetLog.logD("Text search in file " + vf);
      
      String cnts = null;
      try {
         cnts = IvyFile.loadFile(vf.getInputStream());
       }
      catch (IOException e) { 
         return true;
       }
        
      boolean usecase = (pattern_flags & Pattern.CASE_INSENSITIVE) == 0;
      if ((pattern_flags & Pattern.LITERAL) != 0 && usecase) {
         int ln = pattern_text.length();
         int idx = -1;
         for ( ; ; ) {
            if (idx < 0) idx = cnts.indexOf(pattern_text);
            else idx = cnts.indexOf(pattern_text,idx);
            if (idx < 0) break;
            if (num_result++ > max_result) return false;
            text_result.output(vf,idx,idx+ln);
            idx += ln;
          }
       }
      else {
         Pattern pat = Pattern.compile(pattern_text,pattern_flags);
         Matcher mat = pat.matcher(cnts);
         while (mat.find()) {
            if (num_result++ > max_result) return false;
            text_result.output(vf,mat.regionStart(),mat.regionEnd());
          }
       }
      return true;
    }
   
}       // end of inner class SimpleSearchProcessor


/********************************************************************************/
/*                                                                              */
/*      Handle hierarchy search                                                 */
/*                                                                              */
/********************************************************************************/

void handleFindHierarchy(Project p,Module m,String pkg,String cls,
      IvyXmlWriter xw)
{
   FindHierarchyAction fha = new FindHierarchyAction(p,m,pkg,cls,xw);
   fha.start();
}



private class FindHierarchyAction extends BubjetAction.Read {
   
   private Project for_project;
   private Module for_module;
   private String for_package;
   private String for_class;
   private IvyXmlWriter xml_writer;
   
   FindHierarchyAction(Project p,Module m,String pkg,String cls,IvyXmlWriter xw) {
      super(p,"Find Hierarchy");
      for_project = p;
      for_module = m;
      for_package = pkg;
      for_class = cls;
      xml_writer = xw;
    }
   
   @Override void process() {
      
      GlobalSearchScope scp = BubjetUtil.getSearchScope(for_project,for_module);
      TypeHierarchy th = new TypeHierarchy(scp);
      if (for_class != null) {
         JavaPsiFacade jpf = JavaPsiFacade.getInstance(for_project);
         PsiClass pc = jpf.findClass(for_class,scp);
         if (pc != null) addAllClasses(pc,th);
         else BubjetLog.logD("Can't find class element for " + for_class);
       }
      else if (for_package != null) {
         JavaPsiFacade jpf = JavaPsiFacade.getInstance(for_project);
         PsiPackage ppkg = jpf.findPackage(for_package);
         if (ppkg != null) addAllClasses(ppkg,th);
         else BubjetLog.logD("Can't find package element for " + for_package);
       }
      else if (for_module != null) {
         ModuleRootManager mrm = ModuleRootManager.getInstance(for_module);
         PsiManager pm = PsiManager.getInstance(for_project);
         for (VirtualFile vf : mrm.getContentRoots()) {
            PsiFile pf = pm.findFile(vf);
            PsiDirectory pd = pm.findDirectory(vf);
            if (pd != null) addAllClasses(pd,th);
            else if (pf != null) addAllClasses(pf,th);
            else {
               BubjetLog.logD("Can't find PSIfile for " + vf);
             }
          }
       }
      else {
         ModuleManager mm = ModuleManager.getInstance(for_project);
         PsiManager pm = PsiManager.getInstance(for_project);
         for (Module mod : mm.getModules()) {
            ModuleRootManager mrm = ModuleRootManager.getInstance(mod);
            for (VirtualFile vf : mrm.getContentRoots()) {
               PsiFile pf = pm.findFile(vf);
               PsiDirectory pd = pm.findDirectory(vf);
               if (pd != null) addAllClasses(pd,th);
               else if (pf != null) addAllClasses(pf,th);
               else {
                  BubjetLog.logD("Can't find PSIfile for " + vf);
                }
             }
          }
       }
      
      BubjetLog.logD("TYPE HIERARCHY include " + th.getClasses().size() + " classes");
      BubjetUtil.outputTypeHierarchy(th.getClasses(),xml_writer);
    }
   
}       // end of inner class FindHierarchyAction



private void addAllClasses(PsiElement pe,TypeHierarchy th)
{
   if (pe == null) return;
   if (pe instanceof PsiFile) {
      if (!(pe instanceof PsiJavaFile)) return;
    }
   if (pe instanceof PsiComment) return;
   if (pe instanceof PsiWhiteSpace) return;
   if (pe instanceof PsiPackageStatement) return;
   if (pe instanceof PsiImportList) return;
   
   if (pe instanceof PsiClass) {
      BubjetLog.logD("Add hierarchy class " + pe);
      th.addClass((PsiClass) pe);
    }
   else {
      BubjetLog.logD("Handle Hierarchy Element " + pe);
      for (PsiElement ce : pe.getChildren()) {
         addAllClasses(ce,th);
       }
    }
}


private class TypeHierarchy implements Processor<PsiClass> {

   private Set<PsiClass> all_classes;
   private GlobalSearchScope search_scope;
   private int cur_level;
   
   TypeHierarchy(GlobalSearchScope scp) {
      all_classes = new HashSet<>();
      search_scope = scp;
      cur_level = 0;
    }
   
   Set<PsiClass> getClasses()                           { return all_classes; }
   
   void addClass(PsiClass pc) {
      if (pc == null) return;
      if (!all_classes.add(pc)) return;
      if (pc instanceof PsiAnonymousClass) return;
      BubjetLog.logD("Add " + pc + " to type hierarchy " + cur_level);
      ++cur_level;
      addClass(pc.getSuperClass());
      BubjetLog.logD("Handle interfaces");
      for (PsiClass ic : pc.getInterfaces()) {
         addClass(ic);
       }
      if (cur_level <= 1) {
         BubjetLog.logD("Handle implementations of " + pc);
         Query<PsiClass> q = ClassInheritorsSearch.search(pc,search_scope,true,true,false);
         q.forEach(this);
         BubjetLog.logD("Finished implementations of " + pc);
       }
      --cur_level;
    }
   
   @Override public boolean process(PsiClass pc) { 
      addClass(pc);
      return true;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle FINDBYKEY                                                        */
/*                                                                              */
/********************************************************************************/

void handleFindByKey(Project p,Module m,String bid,String key,String file,IvyXmlWriter xw)
{
   FindByKeyAction act = new FindByKeyAction(p,m,bid,key,file,xw);
   act.start();
}


private class FindByKeyAction extends BubjetAction.Read {
   
   private Project for_project;
   private Module for_module;
   private String for_key;
   private IvyXmlWriter xml_writer;
   
   FindByKeyAction(Project p,Module m,String bid,String key,String file,IvyXmlWriter xw) {
      for_project = p;
      for_module = m;
      for_key = key;
      xml_writer = xw;
    }
   
   @Override public void process() {
      GlobalSearchScope scp = BubjetUtil.getSearchScope(for_project,for_module);
      
      String [] comps = for_key.split("\\!");
      String modnm = comps[0];
      String type = comps[1];
      String cls = comps[2];
      
      ModuleManager mm = ModuleManager.getInstance(for_project);
      Module emod = mm.findModuleByName(modnm);
      
      if (for_module != null && emod != for_module) return;
      
      JavaPsiFacade jpf = JavaPsiFacade.getInstance(for_project);
      PsiClass pc = jpf.findClass(cls,scp);
      BubjetLog.logD("FIND CLASS " + pc + " " + cls);
      if (pc == null) return;
      
      PsiElement rslt = null;
      
      switch (type) {
         case "C" :
            rslt = pc;
            break;
         case "F" :
            String fnm = comps[3];
            rslt = pc.findFieldByName(fnm,false);
            break;
         case "M" :
            String mnm = comps[3];
            String prms = comps[4];
            BubjetLog.logD("KEY METHOD " + mnm + " " + prms);
            for (PsiMethod pm : pc.findMethodsByName(mnm,false)) {
               String msg = ClassUtil.getAsmMethodSignature(pm);
               BubjetLog.logD("COMPARE " + msg + " " + prms);
               if (prms == null || prms.equals(msg)) {
                  rslt = pm;
                  break;
                }
             }
            break;
         case "I" :
            // need to find initializer
            break;
         case "V" :
            break;
       }
      
      
      if (rslt != null) {
         BubjetUtil.outputJavaElement(emod,rslt,false,xml_writer);
       }
    }
   
}       // end of inner class FindByKeyAction



/********************************************************************************/
/*                                                                              */
/*      Handle FINDREGIONS command                                              */
/*                                                                              */
/********************************************************************************/

void handleFindRegions(Project p,Module m,String bid,String file,String cls,
      boolean pfx,boolean stat,boolean compunit,boolean imps,boolean pkg,
      boolean topdecls,boolean fields,boolean all,IvyXmlWriter xw)
{
   FindRegionsAction fra = new FindRegionsAction(p,m,bid,file,cls,pfx,stat,
         compunit,imps,pkg,topdecls,fields,all,xw);
   fra.start();
}


private class FindRegionsAction extends BubjetAction.Read {
   
   private Project for_project;
   private Module for_module;
   private String for_file;
   private String for_class;
   private boolean do_prefix;
   private boolean do_inits;
   private boolean do_compunit;
   private boolean do_imports;
   private boolean do_package;
   private boolean do_topdecls;
   private boolean do_fields;
   private boolean all_flag;
   private IvyXmlWriter xml_writer;
   
   FindRegionsAction(Project p,Module m,String bid,String file,String cls,
         boolean pfx,boolean stat,boolean compunit,boolean imps,boolean pkg,
      boolean topdecls,boolean fields,boolean all,IvyXmlWriter xw) {
      for_project = p;
      for_module = m;
      for_file = file;
      for_class = cls;
      do_prefix = pfx;
      do_inits = stat;
      do_compunit = compunit;
      do_imports = imps;
      do_package = pkg;
      do_topdecls = topdecls;
      do_fields = fields;
      all_flag = all;
      xml_writer = xw;   
    }
   
   @Override void process() throws BubjetException {
      PsiClass pc = null;
      PsiJavaFile pf = null;
      if (for_class != null) {
         GlobalSearchScope scp = BubjetUtil.getSearchScope(for_project,for_module);
         JavaPsiFacade jpf = JavaPsiFacade.getInstance(for_project);
         pc = jpf.findClass(for_class,scp);
         pf = (PsiJavaFile) pc.getContainingFile();
       }
      else {
         pf = BubjetUtil.getPsiFile(for_project,for_file);
         if (pf != null) {
            for (PsiClass pcc : pf.getClasses()) {
               pc = (PsiClass) pcc;
             }
          }
       }
      if (pf == null) {
         throw new BubjetException("Can find project file for " + for_file + " " + for_class);
       }
      if (for_module == null) for_module = ModuleUtilCore.findModuleForFile(pf);
      
      int start = 0;
      PsiClass [] pccs = pf.getClasses();
      if (pc != pccs[0]) {
         start = BubjetUtil.getExtendedRange(pc).getStartOffset();
       }
      
      
      if (do_compunit) {
         outputRange("COMPUNIT",pf);
       }
      if (pc != null && do_prefix) {
         TextRange xrng = BubjetUtil.getExtendedRange(pc);
         TextRange srng = pc.getTextRange();
         int epos = getFirstDeclarationOffset(pc);
         if (epos < 0) {
            outputRange("PREFIX",pc,start,xrng.getEndOffset());
          }
         else {
            outputRange("PREFIX",pc,start,epos);
            outputRange("POSTFIX",pc,srng.getEndOffset()-1,xrng.getEndOffset());
          }
       }
      if (do_package) {
         if (pf.getPackageStatement() != null) {
            outputExtendedRange("PACKAGE",pf.getPackageStatement());
          }
       }
      if (do_imports) {
         if (pf.getImportList() != null) {
            for (PsiImportStatement imp : pf.getImportList().getImportStatements()) {
               outputExtendedRange("IMPORT",imp);
             }
          }
       }
      if (do_topdecls) {
         if (all_flag) { 
            for (PsiClass pcc : pf.getClasses()) {
               BubjetUtil.outputJavaElement(for_module,pcc,false,xml_writer);
             }
          }
         else if (pc != null) {
            TextRange tr = pc.getTextRange();
            int epos = getFirstDeclarationOffset(pc);
            if (epos < 0) {
               outputRange("TOPDECLS",pc);
             }
            else {
               outputRange("TOPDECLS",pc,tr.getStartOffset(),epos);
             }
          }
       }
      
      if ((do_inits || all_flag) && pc != null) {
        for (PsiClassInitializer pci : pc.getInitializers()) {
           outputExtendedRange("INITIALIZER",pci);
         }
       }
      if (do_fields && pc != null) {
         for (PsiField pfld : pc.getFields()) {
            outputExtendedRange("FIELD",pfld);
          }
       }
      if (all_flag && pc != null) {
         for (PsiElement pe : pc.getChildren()) {
            if (pe instanceof PsiMember) {
               BubjetUtil.outputJavaElement(for_module,pe,false,xml_writer);
             }
          }
       }
    }
   
   private void outputRange(String what,PsiElement pe,int start,int end) {
      TextRange rng = new TextRange(start,end);
      outputRange(what,rng,pe);
    }
   private void outputRange(String what,PsiElement pe) {
      TextRange rng = pe.getTextRange();
      outputRange(what,rng,pe);
    }
   
   private void outputExtendedRange(String what,PsiElement pe) {
      TextRange rng = BubjetUtil.getExtendedRange(pe);
      outputRange(what,rng,pe);
    }
   
   private void outputRange(String what,TextRange rng,PsiElement pe) {
      PsiFile pf = pe.getContainingFile();
      xml_writer.begin("RANGE");
      xml_writer.field("TYPE",what);    // for debugging
      xml_writer.field("PATH",BubjetUtil.outputFileName(pf));
      xml_writer.field("START",rng.getStartOffset());
      xml_writer.field("END",rng.getEndOffset());
      xml_writer.end("RANGE");
    }
   
   private int getFirstDeclarationOffset(PsiClass pc) {
      int epos = -1;
      for (PsiElement mem : pc.getChildren()) {
         if (mem instanceof PsiMember) {
            TextRange mrng = BubjetUtil.getExtendedRange(mem);
            int apos = mrng.getStartOffset();
            if (epos < 0 || epos >= apos) epos = apos-1;
          }
       }
      return epos;
    }
}       // end of inner class FindRegionsAction




/********************************************************************************/
/*                                                                              */
/*      Search Result processing                                                */
/*                                                                              */
/********************************************************************************/

private void outputElementResult(Module m,PsiElement e,PsiElement def,IvyXmlWriter xw)
{
   xw.begin("MATCH");
   TextRange rng = e.getTextRange();
   xw.field("OFFSET",rng.getStartOffset());
   xw.field("LENGTH",rng.getLength());
   xw.field("STARTOFFSET",rng.getStartOffset());
   xw.field("ENDOFFSET",rng.getEndOffset());
   VirtualFile vf = PsiUtil.getVirtualFile(e);
   xw.field("FILE",BubjetUtil.outputFileName(vf));
   if (PsiUtil.isInsideJavadocComment(e)) xw.field("INDOCCMMT",true);
   
   if (def != null) {
      BubjetUtil.outputJavaElement(m,def,false,xw);
    }
   
   xw.end("MATCH");
}



private class QueryEltResult implements Processor<PsiElement> {
   
   private Module for_module;
   private IvyXmlWriter xml_writer;
   
   QueryEltResult(Module m,IvyXmlWriter xw) {
      for_module = m;
      xml_writer = xw;
    }
   
   @Override public boolean process(PsiElement pe) { 
      BubjetLog.logD("QUERY ERESULT " + pe + " " +
            pe.getTextRange() + " " + pe.getReference());
      outputElementResult(for_module,pe,pe,xml_writer);
      return true;
   }
   
}       // end of inner class QueryRefResult 


private class QueryRefResult implements Processor<PsiReference> {
  
   private Module for_module;
   private PsiElement def_element;
   private boolean do_ronly;
   private boolean do_wonly;
   private IvyXmlWriter xml_writer;
   
   QueryRefResult(Module m,PsiElement def,boolean ronly,boolean wonly,IvyXmlWriter xw) {
      for_module = m;
      def_element = def;
      do_ronly = ronly;
      do_wonly = wonly;
      xml_writer = xw;
    }
   
   @Override public boolean process(PsiReference pr) {
      BubjetLog.logD("QUERY RRESULT " + pr.getElement() + " " +
            pr.getElement().getTextRange() + " " + pr.resolve());
      if (filterResult(pr.getElement())) {
         outputElementResult(for_module,pr.getElement(),def_element,xml_writer);
       }
      return true;
    }
   
   private boolean filterResult(PsiElement pe) {
      if (do_wonly && do_ronly) return true;
      if (do_wonly) {
         if (!isWriteInstance(pe)) return false;
       }
      if (do_ronly) {
         if (isWriteInstance(pe)) return false;
       }
      return true;
    }
   
}       // end of inner class QueryRefResult


private class QueryTextResult {
   
   private IvyXmlWriter xml_writer;
   private PsiManager psi_manager;
   
   QueryTextResult(Project p,IvyXmlWriter xw) {
      xml_writer = xw;
      psi_manager = PsiManager.getInstance(p);
    }

   void output(VirtualFile vf,int start,int end) {
      BubjetLog.logD("Find text result " + start + " " + end + " " + vf);
      xml_writer.begin("MATCH");
      xml_writer.field("STARTOFFSET",start);
      xml_writer.field("LENGTH",end-start);
      xml_writer.textElement("FILE",BubjetUtil.outputFileName(vf));
      PsiFile pf = psi_manager.findFile(vf);
      if (pf != null) {
         Module m = ModuleUtilCore.findModuleForFile(pf);
         PsiElement pe = PsiUtil.getElementAtOffset(pf,start);
         BubjetLog.logD("Found element " + pe + " " + pe.getTextRange() + " " + m);
         if (pe != null && pe.getTextLength() >= end-start) {
            PsiElement def = BubjetUtil.findContainter(pe);
            BubjetUtil.outputJavaElement(m,def,false,xml_writer);
          }
       }
      xml_writer.end("MATCH");
    }
   
   

}       // end of inner class QueryRefResult



}       // end of class BubjetSearchManager




/* end of BubjetSearchManager.java */

