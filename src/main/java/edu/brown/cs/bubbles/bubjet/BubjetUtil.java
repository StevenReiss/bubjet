/********************************************************************************/
/*                                                                              */
/*              BubjetUtil.java                                                 */
/*                                                                              */
/*      Utility (mostly output) methods for Bubjet to Bubbles                   */
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

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties;
import org.jetbrains.java.debugger.breakpoints.properties.JavaExceptionBreakpointProperties;
import org.jetbrains.java.debugger.breakpoints.properties.JavaFieldBreakpointProperties;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;
import org.jetbrains.java.debugger.breakpoints.properties.JavaMethodBreakpointProperties;

import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionSorter;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.compiler.CompilerMessageImpl;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterListOwner;
import com.intellij.psi.PsiQualifiedNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.impl.search.JavaFilesSearchScope;
import com.intellij.psi.impl.source.javadoc.PsiDocParamRef;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.SuspendPolicy;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.ThreadDeathEvent;

import edu.brown.cs.bubbles.bubjet.BubjetDebug.BArray;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BClass;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BField;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BLocal;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BMethod;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BObject;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BPrimitive;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BStackFrame;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BString;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BThis;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BThread;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BValue;
import edu.brown.cs.bubbles.bubjet.BubjetDebug.BVariable;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BubjetUtil implements BubjetConstants
{



/********************************************************************************/
/*                                                                              */
/*      Get valid name for virtual file                                         */
/*                                                                              */
/********************************************************************************/

static String outputFileName(PsiFile pf)
{
   return outputFileName(pf.getVirtualFile());
}



static String outputFileName(VirtualFile vf)
{
   if (vf ==  null) return null;
   
   String vfn = vf.getPath();
   if (vfn.endsWith("!")) {
      vfn = vfn.substring(0,vfn.length()-1);
    }
   
   return vfn;
}


/********************************************************************************/
/*                                                                              */
/*      Output Java Element                                                     */
/*                                                                              */
/********************************************************************************/

static void outputJavaProject(Module m,IvyXmlWriter xw)
{
   xw.begin("ITEM");
   
   xw.field("TYPE","Project");
   xw.field("NAME",m.getName());
   xw.field("SOURCE","USERSOURCE");
   xw.field("PROJECT",m.getName()); 
   xw.field("PATH",outputFileName(m.getModuleFile().getParent()));
   xw.field("KEY",m.getName() + "!");
   xw.field("KNOWN",true);
   
   xw.end("ITEM");
}



static void outputJavaPackage(PsiDirectory ps,String nm,Module m,IvyXmlWriter xw)
{
   xw.begin("ITEM");
   
   xw.field("TYPE","Package");
   xw.field("NAME",nm);
   xw.field("SOURCE","USERSOURCE");
   xw.field("PROJECT",m.getName()); 
   xw.field("PATH",outputFileName(ps.getVirtualFile()));
   xw.field("KEY",m.getName() + "!" + nm);
   xw.field("KNOWN",true);
   
   xw.end("ITEM");
}


static int outputJavaElement(Module m,PsiElement elt,boolean children,
      IvyXmlWriter xw) 
{
   if (elt == null) return 0;
   JavaElementOutput out = new JavaElementOutput(m,children,xw);
   elt.accept(out);
   return out.getCount();
}




private static class JavaElementOutput extends JavaElementVisitor {
   
   private final Module for_module;
   private final boolean do_children;
   private final IvyXmlWriter xml_writer;
   private int output_count;
   
   JavaElementOutput(Module m,boolean child,IvyXmlWriter xw) {
      for_module = m;
      do_children = child;
      xml_writer = xw;
      output_count = 0;
    }
   
   int getCount()                                               { return output_count; }
   
   @Override public void visitDirectory(PsiDirectory n) {
      for (PsiElement pe : n.getChildren()) {
         if (pe instanceof PsiJavaFile) {
            PsiJavaFile pjf = (PsiJavaFile) pe;
            String nm = pjf.getPackageName();
            if (nm != null && nm.length() > 0) {
              outputJavaPackage(n,nm,for_module,xml_writer);
             }
          }
       }
      if (do_children) {
         for (PsiElement pe : n.getChildren()) {
            if (pe instanceof PsiDirectory || pe instanceof PsiJavaFile) {
               pe.accept(this);
             }
          }
       }
    }
   
   @Override public void visitFile(PsiFile n)                   { }
   
   @Override public void visitJavaFile(PsiJavaFile n) {
      if (do_children) {
         accept(n.getPackageStatement());
         for (PsiClass c : n.getClasses()) {
            c.accept(this);
          }
       }
    }
   
   @Override public void visitPackageStatement(PsiPackageStatement n) {
      ++output_count;
      outputNameInformation(n,"PackageDecl",for_module,xml_writer);
    }
   
   @Override public void visitClass(PsiClass n) {
      ++output_count;
      String tnm = "Class";
      if (n.isInterface()) tnm = "Interface";
      else if (n.isEnum()) tnm = "Enum";
      else if (isThrowable(n)) tnm = "Throwable";
      outputNameInformation(n,tnm,for_module,xml_writer);
      if (do_children) {
         for (PsiField pf : n.getFields()) {
            accept(pf);
          }
         for (PsiMethod pm : n.getMethods()) {
            accept(pm);
          }
         for (PsiClass pc : n.getInnerClasses()) {
            accept(pc);
          }
         for (PsiClassInitializer pci : n.getInitializers()) {
            accept(pci);
          }
       }
    }
   
   @Override public void visitField(PsiField pf) {
      ++output_count;
      String tnm = "Field";
      if (pf instanceof PsiEnumConstant) tnm = "EnumConstant";
      outputNameInformation(pf,tnm,for_module,xml_writer);
    }
   
   @Override public void visitMethod(PsiMethod pm) {
      ++output_count;
      String tnm = "Function";
      if (pm.isConstructor()) tnm = "Constructor";
      outputNameInformation(pm,tnm,for_module,xml_writer);
//      if (do_children) {
//       for (PsiParameter pp : pm.getParameterList().getParameters()) {
//          accept(pp);
//        }
//       for (PsiStatement ps : pm.getBody().getStatements()) {
//          accept(ps);
//        }
//       }
    }
   
   @Override public void visitClassInitializer(PsiClassInitializer pi) {
      ++output_count;
      outputNameInformation(pi,"StaticInitializer",for_module,xml_writer);
    }
   
   @Override public void visitVariable(PsiVariable pv) {
      ++output_count;
      outputNameInformation(pv,"Local",for_module,xml_writer);
    }
   
   @Override public void visitIdentifier(PsiIdentifier pi) {
    }
   
   @Override public void visitElement(PsiElement elt) {
      BubjetLog.logD("UNKNOWN VISIT " + elt);
      super.visitElement(elt);
    }
   
   private void accept(PsiElement e) {
      if (e != null) e.accept(this);
    }
   
}       // end of inner class JavaElementOutput



/********************************************************************************/
/*                                                                              */
/*      Output Class/Interface/Enum Information                                 */
/*                                                                              */
/********************************************************************************/

private static boolean isThrowable(PsiClass c)
{
   for (PsiClassType pt : c.getSuperTypes()) {
      if (isThrowable(pt)) return true;
    }
   return false;
}


private static boolean isThrowable(PsiClassType pt)
{
   switch (pt.getCanonicalText()) {
      case "java.lang.Object" :
         return false;
      case "java.lang.Throwable" :
      case "java.lang.Exception" :
      case "java.lang.Error" :
         return true;
    }
   
   for (PsiType pt1 : pt.getSuperTypes()) {
      if (pt1 instanceof PsiClassType) {
         if (isThrowable((PsiClassType) pt1)) return true;
       }
    }
   
   return false;
}



static int getModifierFlags(PsiModifierList ml)
{
   int flags = 0;

   if (ml == null) return flags;
   if (ml.hasModifierProperty(PsiModifier.PUBLIC)) flags |= Modifier.PUBLIC;
   if (ml.hasModifierProperty(PsiModifier.PROTECTED)) flags |= Modifier.PROTECTED;
   if (ml.hasModifierProperty(PsiModifier.PRIVATE)) flags |= Modifier.PRIVATE;
// if (ml.hasModifierProperty(PsiModifier.PACKAGE_LOCAL)) flags |= Modifier.PACKAGE_LOCAL;
   if (ml.hasModifierProperty(PsiModifier.STATIC)) flags |= Modifier.STATIC;
   if (ml.hasModifierProperty(PsiModifier.ABSTRACT)) flags |= Modifier.ABSTRACT;
   if (ml.hasModifierProperty(PsiModifier.FINAL)) flags |= Modifier.FINAL;
   if (ml.hasModifierProperty(PsiModifier.NATIVE)) flags |= Modifier.NATIVE;
   if (ml.hasModifierProperty(PsiModifier.SYNCHRONIZED)) flags |= Modifier.SYNCHRONIZED;
   if (ml.hasModifierProperty(PsiModifier.STRICTFP)) flags |= Modifier.STRICT;
   if (ml.hasModifierProperty(PsiModifier.TRANSIENT)) flags |= Modifier.TRANSIENT;
   if (ml.hasModifierProperty(PsiModifier.VOLATILE)) flags |= Modifier.VOLATILE;
// if (ml.hasModifierProperty(PsiModifier.DEFAULT)) flags |= Modifier.DEFAULT;
// if (ml.hasModifierProperty(PsiModifier.OPEN)) flags |= Modifier.OPEN;
// if (ml.hasModifierProperty(PsiModifier.TRANSITIVE)) flags |= Modifier.TRANSITIVE; 
// if (ml.hasModifierProperty(PsiModifier.SEALED)) flags |= Modifier.SEALED; 
// if (ml.hasModifierProperty(PsiModifier.NON_SEALED)) flags |= Modifier.NON_SEALED; 
   
   return flags;
}


private static void outputNameInformation(PsiElement e,String tnm,Module m,IvyXmlWriter xw)
{
   xw.begin("ITEM");
   
   if (tnm != null) xw.field("TYPE",tnm);
   
   if (e instanceof PsiQualifiedNamedElement) {
      xw.field("NAME",((PsiQualifiedNamedElement) e).getQualifiedName());
    }
   else if (e instanceof PsiNameIdentifierOwner) {
      String nm = ((PsiNameIdentifierOwner) e).getName();
      xw.field("NAME",nm);
      if (e instanceof PsiMember) {
         PsiClass pc = ((PsiMember) e).getContainingClass();
         xw.field("QNAME",pc.getQualifiedName() + "."  + nm);
       }
    }
   else if (e instanceof PsiClassInitializer) {
      xw.field("NAME","<clinit>");
      PsiClass pc = ((PsiMember) e).getContainingClass();
      xw.field("QNAME",pc.getQualifiedName() + ".<clinit>");
    }
   else if (e instanceof PsiPackageStatement) {
      PsiPackageStatement ppd = (PsiPackageStatement) e;
      xw.field("NAME",ppd.getPackageName());
    }
   else {
      xw.field("NAME","???");
    }
   
   TextRange rng = e.getTextRange();
   xw.field("STARTOFFSET",rng.getStartOffset());
   xw.field("ENDOFFSET",rng.getEndOffset());
   xw.field("LENGTH",rng.getLength());
   
   xw.field("SOURCE","USERSOURCE");
   
   if (m == null) {
      PsiFile pf = e.getContainingFile();
      m = BubjetUtil.findModuleForFile(pf);
    }
   if (m != null) xw.field("PROJECT",m.getName()); 
   
   if (getVarArgs(e)) xw.field("VARARGS",true);
   if (e instanceof PsiMethod) {
      String sgn = ClassUtil.getAsmMethodSignature((PsiMethod) e);
      int idx = sgn.lastIndexOf(")");
      String ptyp = sgn.substring(idx+1);
      String prms = sgn.substring(0,idx+1);
      xw.field("PARAMETERS",prms);
      xw.field("RETURNTYPE",ptyp);
    }
   else if (e instanceof PsiVariable) {
      PsiType pt = ((PsiVariable) e).getType();
      String rt = ClassUtil.getBinaryPresentation(pt);
      xw.field("RETURNTYPE",rt);
    }
   
   if (e instanceof PsiModifierListOwner) {
      xw.field("FLAGS",getModifierFlags(((PsiModifierListOwner) e).getModifierList()));
    }
   
   PsiFile pf = e.getContainingFile();
   if (pf != null) {
      xw.field("PATH",outputFileName(pf.getVirtualFile()));
    }
   
   xw.field("KEY",getKey(e,m));
   
   outputAnnotations(e,xw);
   
   xw.end("ITEM");
}



private static void outputAnnotations(PsiElement e,IvyXmlWriter xw)
{
   PsiAnnotation [] annots = null;
   if (e instanceof PsiModifierListOwner) {
      annots = ((PsiModifierListOwner) e).getAnnotations();
    }
   else if (e instanceof PsiAnnotationOwner) {
      annots = ((PsiAnnotationOwner) e).getAnnotations();
    }
   else return;
   
   for (PsiAnnotation pa : annots) {
      xw.begin("ANNOTATION");
      xw.field("NAME",pa.getQualifiedName());
      xw.field("COUNT",1);
      for (PsiNameValuePair nvp : pa.getParameterList().getAttributes()) {
         xw.begin("VALUE");
         if (nvp.getName() == null) xw.field("NAME","value");
         else xw.field("NAME",nvp.getName());
         if (nvp.getValue() != null) {
            xw.field("VALUE",nvp.getLiteralValue());
          }
//       xw.field("KIND","???");
         xw.end("VALUE");
       }
      xw.end("ANNOTATION");
    } 
}



static String getKey(PsiElement e)
{
   return getKey(e,null);
}


static String getKey(PsiElement e,Module m)
{
   if (e == null) return null;
   
   if (m == null) {
      PsiFile pf = e.getContainingFile();
      if (pf != null && pf.getFileType().getName().equals("CLASS")) return null;
      if (pf != null) m = BubjetUtil.findModuleForFile(pf);
      if (m == null) {
         BubjetLog.logD("Can't find module for " +
                  e + " " + e.getClass() + " " + pf + " " +
            pf.getFileType());

       }
    }
   
   StringBuffer buf = new StringBuffer();
   if (m == null) buf.append("???");
   else buf.append(m.getName());
   buf.append("!");
   
   String typ = "???";
   String key = "???";
   if (e instanceof PsiClass) {
      typ = "C";
      key = ((PsiClass) e).getQualifiedName();
    }
   else if (e instanceof PsiMember) {
      if (e instanceof PsiField) typ = "F";
      else if (e instanceof PsiMethod) typ = "M";
      else if (e instanceof PsiClassInitializer) typ = "I";
      key = getMemberKeyPart((PsiMember) e);
    }
   else if (e instanceof PsiLocalVariable) {
      typ = "V";
      PsiElement cont = findContainter(e);
      if (cont instanceof PsiMethod) {
         PsiMethod pm = (PsiMethod) cont;
         key = getMemberKeyPart(pm);
         key += "!" + ((PsiLocalVariable) e).getName();
       }
    }
   else if (e instanceof PsiNameIdentifierOwner) {
      BubjetLog.logD("GET KEY FOR UNKNOWN " + e + " " + e.getClass());
    }  
   
   buf.append(typ);
   buf.append("!");
   buf.append(key);
   
// BubjetLog.logD("KEY FOR " + e + " = " + buf.toString());
   
   return buf.toString();
}


private static String getMemberKeyPart(PsiMember pm)
{
   String key = pm.getContainingClass().getQualifiedName();
   if (pm instanceof PsiClassInitializer) {
      key += "!<clinit>";
    }
   else {
      key += "!" + pm.getName();
    }
   if (pm instanceof PsiMethod) {
      key += "!" + ClassUtil.getAsmMethodSignature((PsiMethod) pm);
    }
   
   return key;
}



private static boolean getVarArgs(PsiElement e)
{
   if (e instanceof PsiParameterListOwner) {
      PsiParameterListOwner pplo = (PsiParameterListOwner) e;
      for (PsiParameter pp : pplo.getParameterList().getParameters()) {
         if (pp.isVarArgs()) return true;
       }
    }
   
   return false;
}



/********************************************************************************/
/*                                                                              */
/*      Output type hierarchy                                                   */
/*                                                                              */
/********************************************************************************/

static void outputTypeHierarchy(Set<PsiClass> clss,IvyXmlWriter xw)
{
   xw.begin("HIERARCHY");
   for (PsiClass pc : clss) {
      outputTypeHierarchyElement(clss,pc,xw);
    }
   xw.end("HIERARCHY");
}


private static void outputTypeHierarchyElement(Set<PsiClass> clss,PsiClass pc,IvyXmlWriter xw)
{
   PsiFile pf = pc.getContainingFile();
   Module m = BubjetUtil.findModuleForFile(pf);
   GlobalSearchScope scp = getSearchScope(m);
   xw.begin("TYPE");
   xw.field("NAME",pc.getQualifiedName());
   xw.field("QNAME",pc.getQualifiedName());
   xw.field("PNAME",pc.getQualifiedName());  // parameterized?
   if (pc.isEnum()) xw.field("KIND","ENUM");
   else if (pc.isInterface()) xw.field("KIND","INTERFACE");
   else if (pc.isAnnotationType()) xw.field("KIND","ANNOTATION");
   else xw.field("KIND","CLASS");
   xw.field("LOCAL",pc.getContainingClass() != null);
   xw.field("MEMBER",pc.getContainingClass() != null);
   String key = getKey(pc,m);
   if (key != null) xw.field("KEY",getKey(pc,m));
   for (PsiClass ps : pc.getSupers()) {
      if (ps == pc.getSuperClass()) outputTypeSubElement("SUPERCLASS",clss,ps,xw);
      else if (ps.isInterface()) outputTypeSubElement("EXTENDIFACE",clss,ps,xw);
      else if (pc.isInterface()) outputTypeSubElement("IMPLEMENTOR",clss,ps,xw);
      else outputTypeSubElement("SUPERTYPE",clss,ps,xw);
    }
   if (scp != null) {
      Query<PsiClass> q = ClassInheritorsSearch.search(pc,scp,true,true,false);
      q.forEach(new TypeHierProc(clss,xw));
    }
   xw.end("TYPE");
}


private static void outputTypeSubElement(String typ,Set<PsiClass> clss,PsiClass pc,IvyXmlWriter xw)
{
   if (pc == null || !clss.contains(pc)) return;
   xw.begin(typ);
   xw.field("NAME",pc.getQualifiedName());
   xw.field("KEY",getKey(pc));
   xw.end(typ);
}


private static class TypeHierProc implements Processor<PsiClass> {
 
   private final Set<PsiClass> class_set;
   private final IvyXmlWriter xml_writer;
   
   TypeHierProc(Set<PsiClass> clss,IvyXmlWriter xw) {
      class_set = clss;
      xml_writer = xw;
    }
   
   @Override public boolean process(PsiClass pc) {
      String what = "IMPLEMENTOR";
      if (pc.isInterface()) what = "EXTENDIFACE";
      outputTypeSubElement(what,class_set,pc,xml_writer);
      return true;
    }
   
}       // end of inner class TypeHierProc




/********************************************************************************/
/*                                                                              */
/*      Output Launch Configuration Data                                        */
/*                                                                              */
/********************************************************************************/

static void outputLaunchConfiguration(RunnerAndConfigurationSettings rcs,IvyXmlWriter xw)
{
   if (rcs == null) return;

   RunConfiguration rc = rcs.getConfiguration();

   xw.begin("CONFIGURATION");
   xw.field("ID",rc.getUniqueID());
   xw.field("NAME",rc.getName());
   if (rcs.isTemporary()) {
      xw.field("WORKING",true);
    }
      
   if (rc instanceof ApplicationConfiguration) {
      xw.field("TYPE","JAVA_APP");
      ApplicationConfiguration ac = (ApplicationConfiguration) rc;
      outputLaunchAttr("MAIN_TYPE",ac.getMainClassName(),xw);
      outputLaunchAttr("PROGRAM_ARGUMENTS",ac.getProgramParameters(),xw);
      outputLaunchAttr("STOP_IN_MAIN",false,xw);
      String vmarg = ac.getVMParameters();
      outputLaunchAttr("VM_ARGUMENTS",vmarg,xw);
      if (vmarg != null && vmarg.contains("-ea")) outputLaunchAttr("CONTRACTS",true,xw);
      outputLaunchAttr("WORKING_DIRECTORY",ac.getWorkingDirectory(),xw);
    }     
   else if (rc instanceof RemoteConfiguration) {
      RemoteConfiguration rec = (RemoteConfiguration) rc;
      xw.field("TYPE","REMOTE_JAVA");
      xw.field("HOST",rec.HOST);
      xw.field("PORT",rec.PORT);
    }
   else if (rc instanceof JUnitConfiguration) {
      xw.field("TYPE","JUNIT_TEST");
      JUnitConfiguration juc = (JUnitConfiguration) rc;
      JUnitConfiguration.Data d = juc.getPersistentData();
      String test = d.getPackageName() + "." + d.getMainClassName() + "." + 
         d.getMethodName();
      outputLaunchAttr("TESTNAME",test,xw);
    }
   
   String orig = null;
   Boolean ign = null;   
   if (rc instanceof RunConfigurationBase) {
      RunConfigurationBase<?> rcb = (RunConfigurationBase<?>) rc;
      ign = rcb.getUserData(BubjetLaunchManager.BUBJET_LAUNCH_IGNORE_KEY);
      if (ign == Boolean.TRUE) xw.field("IGNORE",true);
      orig = rcb.getUserData(BubjetLaunchManager.BUBJET_LAUNCH_ORIG_ID);
      if (orig != null) xw.field("ORIGID",orig);
      outputLaunchAttr("CAPTURE_IN_FILE",rcb.getOutputFilePath(),xw);
    }
   
   if (rc instanceof ModuleBasedConfiguration) {
      ModuleBasedConfiguration<?,?> mbc = (ModuleBasedConfiguration<?,?>) rc;
      Module m = mbc.getDefaultModule();
      if (m != null) outputLaunchAttr("PROJECT_ATTR",m.getName(),xw);
    }
   
   xw.end("CONFIGURATION");
}



private static void outputLaunchAttr(Object key,Object val,IvyXmlWriter xw)
{
   if (val == null || key == null) return;
   
   xw.begin("ATTRIBUTE");
   xw.field("NAME",key.toString());
   xw.field("TYPE",val.getClass());
   xw.cdata(val.toString());
   xw.end("ATTRIBUTE");
}



/********************************************************************************/
/*                                                                              */
/*      Message output                                                          */
/*                                                                              */
/********************************************************************************/

static void outputMessage(Project p,CompilerMessage msg,IvyXmlWriter xw)
{
   BubjetLog.logD("MESSAGE " + msg.getCategory() + " " + msg.getClass() + " " +
         msg.getVirtualFile() + " " + msg.getNavigatable() + " >" +
         msg.getMessage() + "< " + msg.getExportTextPrefix() + " >" + "< >" +
         msg.getRenderTextPrefix() + "< " + msg.hashCode());
   
   CompilerMessageImpl cmi = null;
   if (msg instanceof CompilerMessageImpl) {
      cmi = (CompilerMessageImpl) msg;
    }
   
   int soff = -1;
   int eoff = -1;
   if (msg.getNavigatable() != null && msg.getNavigatable() instanceof OpenFileDescriptor) {
      OpenFileDescriptor ofd = (OpenFileDescriptor) msg.getNavigatable();
      if (ofd.getRangeMarker() != null) {
         soff = ofd.getRangeMarker().getStartOffset();
         eoff = ofd.getRangeMarker().getEndOffset();
      }
    }
   if (msg.getNavigatable() != null && msg.getNavigatable() instanceof PsiElement) {
      PsiElement psi = (PsiElement) msg.getNavigatable();
      soff = psi.getTextRange().getStartOffset();
      eoff = psi.getTextRange().getEndOffset();
    }
   
   VirtualFile vf = msg.getVirtualFile();
   int pos = -1;
   
   xw.begin("PROBLEM");
   xw.field("TYPE","org.eclipse.jdt.core.problem");
   xw.field("ID",msg.hashCode());
   xw.field("SEVERITY",msg.getCategory());
   if (cmi != null && vf != null) {
      xw.field("LINE",cmi.getLine());
      xw.field("COL",cmi.getColumn());
      if (soff >= 0) {
         xw.field("START",soff);
         if (eoff >= 0) xw.field("END",eoff);
       }
      else {
         Document d = FileDocumentManager.getInstance().getDocument(vf);
         int lstart = d.getLineStartOffset(cmi.getLine());
         pos = lstart + cmi.getColumn();
         xw.field("START",pos);
       }
    }
   
   if (cmi != null) {
      PsiFile pf = BubjetUtil.getPsiFile(p,vf);
      if (pf != null) {
         PsiElement pe = PsiUtil.getElementAtOffset(pf,pos);
         BubjetLog.logD("Found element for error " + msg + " " + pe);
       }
    }
   
   // MSGID
   // FLAGS
   String msgtxt = msg.getMessage();
   msgtxt = IvyXml.xmlSanitize(msgtxt,false);
   xw.textElement("MESSAGE",msgtxt);
   if (vf != null) xw.textElement("FILE",BubjetUtil.outputFileName(vf));
   xw.end("PROBLEM");
}



static void outputProblem(Project p,CompilerMessage msg,IvyXmlWriter xw)
{
   BubjetLog.logD("PROBLEM MESSAGE " + msg.getCategory() + " " + msg.getClass() + " " +
         msg.getVirtualFile() + " " + msg.getNavigatable() + " >" +
         msg.getMessage() + "< " + msg.getExportTextPrefix() + " >" + "< >" +
         msg.getRenderTextPrefix() + "< " + msg.hashCode());
   
   CompilerMessageImpl cmi = null;
   if (msg instanceof CompilerMessageImpl) {
      cmi = (CompilerMessageImpl) msg;
    }
   
   int soff = -1;
   int eoff = -1;
   if (msg.getNavigatable() != null && msg.getNavigatable() instanceof OpenFileDescriptor) {
      OpenFileDescriptor ofd = (OpenFileDescriptor) msg.getNavigatable();
      if (ofd.getRangeMarker() != null) {
         soff = ofd.getRangeMarker().getStartOffset();
         eoff = ofd.getRangeMarker().getEndOffset();
      }
    }
   if (msg.getNavigatable() != null && msg.getNavigatable() instanceof PsiElement) {
      PsiElement psi = (PsiElement) msg.getNavigatable();
      BubjetLog.logD("PSI FOUND: " + psi + " " + psi.getTextRange() + " " +
            psi.getParent());
      soff = psi.getTextRange().getStartOffset();
      eoff = psi.getTextRange().getEndOffset();
    }
   
   VirtualFile vf = msg.getVirtualFile();
   int pos = -1;
   
   xw.begin("PROBLEM");
   xw.field("ID",msg.hashCode());
   switch (msg.getCategory()) {
      case ERROR : 
         xw.field("ERROR",true);
         break;
      case WARNING :
         xw.field("WARNING",true);
         break;
    }
   
   if (vf != null) xw.field("FILE",outputFileName(vf));
   String mod = null;
   for (String s : msg.getModuleNames()) {
      if (mod == null) mod = s;
    }
   if (mod == null && vf != null) {
      PsiFile pf = PsiManager.getInstance(p).findFile(vf);
      mod = BubjetUtil.findModuleForFile(pf).getName();
    }
   
   if (cmi != null && vf != null) {
      xw.field("LINE",cmi.getLine());
      xw.field("COL",cmi.getColumn());
      if (soff >= 0) {
         xw.field("START",soff);
         if (eoff >= 0) xw.field("END",eoff);
       }
      else {
         Document d = FileDocumentManager.getInstance().getDocument(vf);
         int lstart = d.getLineStartOffset(cmi.getLine());
         pos = lstart + cmi.getColumn();
         xw.field("START",pos);
       }
    }
   String msgtxt = msg.getMessage();
   // probably need to fix up the message here
   msgtxt = IvyXml.xmlSanitize(msgtxt,true);
   xw.field("MESSAGE",msgtxt);
   
   // for undefined method, add NEW_METHOD fix
   
   xw.end("PROBLEM");
}



static void outputProblem(Project p,Document doc,PsiErrorElement err,IvyXmlWriter xw)
{
   BubjetLog.logD("PROBLEM ERROR " + err);
   
   int soff = err.getTextRange().getStartOffset();
   int eoff = err.getTextRange().getEndOffset();
   int lin = doc.getLineNumber(soff);
   
   PsiFile pf = err.getContainingFile();
   
   xw.begin("PROBLEM");
   xw.field("ERROR",true);

   Module m = BubjetUtil.findModuleForFile(pf);
   String mod = null;
   if (m != null) mod = m.getName();
   xw.field("PROJECT",mod);
   xw.field("FILE",outputFileName(pf));
   xw.field("LINE",lin);
   xw.field("START",soff);
   xw.field("END",eoff);
   String msgtxt = err.getErrorDescription();
   // probably need to fix up the message here
   msgtxt = IvyXml.xmlSanitize(msgtxt,true);
   xw.field("MESSAGE",msgtxt);
   
   // for undefined method, add NEW_METHOD fix
   
   xw.end("PROBLEM");
}


static void outputProblem(BubjetFileData fd,HighlightInfo err,IvyXmlWriter xw) 
{
   BubjetLog.logD("HIGHLIGHT ERROR " + err.getDescription());
   
   int soff = err.getStartOffset();
   int eoff = err.getEndOffset();
   int line = fd.getDocument().getLineNumber(soff);
   PsiFile pf = fd.getPsiFile();
   Module m = BubjetUtil.findModuleForFile(pf);
   String mod = null;
   if (m != null) mod = m.getName();
   String msgtxt = err.getDescription();
   // probably need to fix up the message here
   msgtxt = IvyXml.xmlSanitize(msgtxt,true);
   HighlightSeverity sev = err.getSeverity();
   String typ = null;
   switch (sev.getName()) {
      case "INFORMATION" :
      case "SERVER PROBLEM" :
         break;
      case "INFO" :
      case "WEAK WARNING" :
      case "WARNING" :
         typ = "WARNING";
         break;
      default :
      case "ERROR" :
         typ = "ERROR";
         break;
    }
   if (typ == null) return;
   
   xw.begin("PROBLEM");
   xw.field("PROJECT",mod);
   xw.field(typ,true);
   xw.field("FILE",outputFileName(pf));
   xw.field("LINE",line);
   xw.field("START",soff);
   xw.field("END",eoff);
   xw.field("MESSAGE",msgtxt);
   
   // for undefined method, add NEW_METHOD fix
   // add fixes computed by WOLF
   
   xw.end("PROBLEM");
}




/********************************************************************************/
/*                                                                              */
/*      Breakpoint output                                                       */
/*                                                                              */
/********************************************************************************/

static void outputBreakpoint(Project p,XBreakpoint<?> bpt,IvyXmlWriter xw)
{
   xw.begin("BREAKPOINT");
   xw.field("ID",bpt.hashCode());
   xw.field("ENABLED",bpt.isEnabled());
   if (bpt.getSuspendPolicy() == SuspendPolicy.THREAD) xw.field("SUSPEND","THREAD");
   else xw.field("SUSPEND","VM");
   if (bpt.isLogMessage()) xw.field("TRACEPOINT",true);
   
   XSourcePosition srcpos = bpt.getSourcePosition();
   XBreakpointProperties<?> props = bpt.getProperties();
   JavaBreakpointProperties<?> jprops = null;
   
   if (srcpos != null) {
      if (srcpos.getFile().isInLocalFileSystem()) {
         xw.field("FILE",outputFileName(srcpos.getFile()));
         xw.field("LINE",srcpos.getLine()+1);
         xw.field("STARTPOS",srcpos.getOffset());
         PsiFile pf = getPsiFile(p,srcpos.getFile());
         PsiElement pe = pf.findElementAt(srcpos.getOffset());
         PsiClass pc = getClass(pe);
         if (pc != null) xw.field("CLASS",pc.getQualifiedName());
       }
    }
   
   if (props instanceof JavaBreakpointProperties) {
      jprops = (JavaBreakpointProperties<?>) props;
      if (jprops.getCOUNT_FILTER() != 0) xw.field("HITCOUNT",jprops.getCOUNT_FILTER());
    }
   if (props instanceof JavaExceptionBreakpointProperties) {
      xw.field("TYPE","EXCEPTION");
      JavaExceptionBreakpointProperties ebp = (JavaExceptionBreakpointProperties) props;
      xw.field("ISCAUGHT",ebp.NOTIFY_CAUGHT);
      xw.field("ISUNCAUGHT",ebp.NOTIFY_UNCAUGHT);
      // ISSUBCLASSES
      xw.field("EXCEPTION",ebp.myQualifiedName);
      for (ClassFilter cf : ebp.getCatchClassFilters()) {
         xw.textElement("INCLUDE",cf.getPattern());
       }
      for (ClassFilter cf : ebp.getCatchClassExclusionFilters()) {
         xw.textElement("EXCLUDE",cf.getPattern());
       }
    }
   else if (props instanceof JavaLineBreakpointProperties) {
//    JavaLineBreakpointProperties lbp = (JavaLineBreakpointProperties) props;
      xw.field("TYPE","LINE");
    }
   else if (props instanceof JavaMethodBreakpointProperties) {
      JavaMethodBreakpointProperties mbp = (JavaMethodBreakpointProperties) props;
      xw.field("TYPE","METHOD");
      xw.field("ENTRY",mbp.WATCH_ENTRY);
      xw.field("EXIT",mbp.WATCH_EXIT);
      xw.field("METHOD",mbp.myMethodName);
      xw.field("CLASS",mbp.myClassPattern);
      // need to get CLASS,METHOD,SIGNATURE inside METHOD ELEMENT
    }
   else if (props instanceof JavaFieldBreakpointProperties) {
      JavaFieldBreakpointProperties fbp = (JavaFieldBreakpointProperties) props;
      xw.field("TYPE","WATCHPOINT");
      xw.field("FIELD",fbp.myClassName + "." + fbp.myFieldName);
    }
   
   if (bpt.getConditionExpression() != null) {
      XExpression ex = bpt.getConditionExpression();
      xw.begin("CONDITION");
      xw.field("ENABLED",true);
      xw.field("SUSPEND",true);
      xw.text(ex.getExpression());
      xw.end("CONDITION");
    }
   
// BubjetLog.logD("BREAKPOINT " + bpt.getCondition() + " " + bpt.getLogExpression() + " " +
//       bpt.getNavigatable() + " " + bpt.getSuspendPolicy() + " " +
//       bpt.getTimeStamp() + " " + bpt.isLogMessage() + " " + bpt.isLogStack() + " " + bpt.getClass());
// BubjetLog.logD("BREAKPROPS " + props);
// if (srcpos != null) {
//    BubjetLog.logD("BREAKSRC " + srcpos.getFile() + " " + srcpos.getLine() + " " + srcpos.getOffset());
//  }
// BubjetLog.logD("BREAKTYPE " + btyp.getId() + " " + btyp.getTitle() + " " + btyp);
  
   xw.end("BREAKPOINT");
}



/********************************************************************************/
/*                                                                              */
/*      Output Completions                                                      */
/*                                                                              */
/********************************************************************************/

static void outputCompletion(CompletionResult r,int ct,int off,IvyXmlWriter xw)
{
   LookupElement lookup = r.getLookupElement();
   PrefixMatcher pm = r.getPrefixMatcher();
   CompletionSorter sorter = r.getSorter();
   
   BubjetLog.logD("COMPLETION " + lookup.getAutoCompletionPolicy() + " " +
         lookup.getLookupString() + " " + 
         lookup.getPsiElement() + " " + 
         pm.getPrefix() + " " + pm.matchingDegree(lookup.getLookupString()) + " " +
         sorter);
      
   PsiElement pe = lookup.getPsiElement();
   int fgs = 0;
   if (pe instanceof PsiModifierListOwner) {
      fgs = getModifierFlags(((PsiModifierListOwner) pe).getModifierList());
    }
   String kind = "OTHER";
   if (pe instanceof PsiMethod) kind = "METHOD_REF";
   else if (pe instanceof PsiClass) kind = "TYPE_REF";
   else if (pe instanceof PsiField) kind = "FIELD_REF";
   else if (pe instanceof PsiLocalVariable) kind = "LOCAL_VARIABLE_REF";
   else {
      BubjetLog.logD("UNKNOWN KIND OF COMPLETION " + pe);
    }
   
   xw.begin("COMPLETION");
   xw.field("TEXT",lookup.getLookupString());
   xw.field("NAME",lookup.getLookupString());
   xw.field("FLAGS",fgs);
   xw.field("KIND",kind);
   if (pe instanceof PsiMethod) {
      PsiMethod pmthd = (PsiMethod) pe;
      if (pmthd.isConstructor()) xw.field("CONSTRUCTOR",true);
      String sgn = ClassUtil.getAsmMethodSignature(pmthd);
      xw.field("SIGNATURE",sgn);
      String s = ClassUtil.getJVMClassName(pmthd.getContainingClass());
      s = s.replace(".","/");
      s = "L" + s + ";";
      xw.field("DECLSIGN",s);
    }
   else if (pe instanceof PsiClass) {
      String s = ClassUtil.getJVMClassName((PsiClass) pe);
      s = s.replace(".","/");
      s = "L" + s + ";";
      xw.field("DECLSIGN",s);
    }
   else if (pe instanceof PsiVariable) {
      xw.field("DECLSIGN",ClassUtil.getBinaryPresentation(((PsiVariable) pe).getType()));
    }
   int eoff = off; 
   if (pm.getPrefix() != null) eoff += pm.getPrefix().length();
   xw.field("RELEVANCE",100-ct);
   xw.field("REPLACE_START",off);
   xw.field("REPLACE_END",eoff);
         
   xw.end("COMPLETION");
}




/********************************************************************************/
/*                                                                              */
/*      Execution output                                                        */
/*                                                                              */
/********************************************************ProcessHandle**********/

static void outputProcess(BubjetDebugProcess process,IvyXmlWriter xw,boolean showtarget)
{
   if (process == null) return;
   
   xw.begin("PROCESS");
   xw.field("PID",process.getId());
   xw.field("LABEL",process.getLaunchName());
   if (process.isTerminated()) {
      xw.field("TERMINATED",true);
    }  
   else {
      xw.field("CANTERM",true);
    }
   xw.field("NAME",process.getLaunchName());
   outputLaunch(process,xw);
//   if (showtarget) {
//    xw.begin("TARGET");
//    xw.field("ID",ph.hashCode());
//    if (ph.isProcessTerminated()) xw.field("TERMINATED",true);
//    DISCONNECTED, CANDISCONNET
//    PID
//    outputLaunchConfiguration(env.getRunnerAndConfigurationSettings(),xw);
//    xw.end("TARGET");
//   }
   xw.end("PROCESS");
}



static void outputLaunch(BubjetDebugProcess process,IvyXmlWriter xw)
{
   xw.begin("LAUNCH");
   xw.field("MODE","debug");
   xw.field("ID",process.getLaunchId());
   RunnerAndConfigurationSettings cfg = process.getLaunchConfiguration();
   xw.field("CID",cfg.getConfiguration().getUniqueID());
   xw.end("LAUNCH");
}


static void outputThread(BubjetDebugProcess proc,BThread thrd,
      SuspendContextImpl ctx,boolean forcerun,boolean forcedead,IvyXmlWriter xw) 
{
   if (thrd == null) return;
   
   boolean dead = forcedead;
   String exception = null;
   if (ctx != null && ctx.getEventSet() != null) {
      for (Event e : ctx.getEventSet()) {
         if (e instanceof ExceptionEvent) {
            ExceptionEvent ee = (ExceptionEvent) e;
            exception = ee.exception().getClass().getName();
          }
         else if (e instanceof ThreadDeathEvent) {
            dead = true;
          }
       }
    }
   
   xw.begin("THREAD");
   if (!dead) { 
      xw.field("NAME",thrd.getName());
      xw.field("GROUP",thrd.getGroupName());
    }
   xw.field("ID",thrd.getUniqueId());
   if (dead || forcerun) xw.field("STACK",false);
   else {
      int ct = thrd.getFrameCount(); 
      if (ct > 0) {
         xw.field("STACK",true);
         xw.field("FRAMES",ct);
       }
      else xw.field("STACK",false);
    }
   String tnm = thrd.getValueType().getName();
   if (tnm.contains("jdk.internal.")) xw.field("SYSTEM",true);
   try {
      if (!dead) {
         if (!forcerun) xw.field("ATBREAKPOINT",thrd.isAtBreakpoint());
         BValue bv = thrd.getFieldValue("daemon");
         if (bv instanceof BPrimitive) {
            BPrimitive bvp = (BPrimitive) bv;
            if (bvp.booleanValue()) xw.field("DAEMON",true);
          }
       }
    }
   catch (VMDisconnectedException e) {
      dead = true;
    }
   catch (Throwable t) {
      BubjetLog.logE("Problem getting daemon value",t);
    }
   
   if (!dead && thrd.isSuspended() && !forcerun) {
      xw.field("SUSPENDED",true);
      xw.field("CANRESUME",true);
      xw.field("CANSTEPIN",true);
      xw.field("CANSTEPOVER",true);
      xw.field("CANSTEPOUT",true);
    }
   else if (dead || thrd.isTerminated() || forcedead) {
      xw.field("TERMINATED",true);
    }
   else {
      xw.field("CANSUSPEND",true);
    }
   
   xw.field("PID",proc.getId());
   
   if (ctx != null && !forcerun) {
      if (exception != null) xw.field("EXCEPTION",exception);
      if (!ctx.isResumed()) {
         Location loc = ctx.getLocation();
         if (loc != null) {
            xw.field("LOCLINE",loc.lineNumber());
            xw.field("LOCMETHOD",loc.method());
            String f1 = findSourceFile(proc,loc,false);
            if (f1 != null) xw.field("LOCFILE",f1);
         }
       }
    }
   
   outputLaunch(proc,xw);
   
   xw.end("THREAD");
}



static void outputStackFrame(BubjetDebugProcess proc,BStackFrame frm,int lvl,
      int vdepth,int arraysz,IvyXmlWriter xw)
{
   xw.begin("STACKFRAME");
   xw.field("NAME",frm.getMethodName());
   xw.field("ID",frm.getUniqueId());
   xw.field("CLASS",frm.getClassName());
   xw.field("METHOD",frm.getClassName() + "." + frm.getMethodName());
   xw.field("LINENO",frm.getLineNumber());
   BMethod mthd = frm.getMethod();
   if (mthd.isConstructor()) xw.field("CONSTRUCTOR",true);
   if (mthd.isNative()) xw.field("NATIVE",true);
   if (mthd.isStaticInitializer()) xw.field("INITIALIZER",true);
   if (mthd.isVarArgs()) xw.field("VARARGS",true);
   if (mthd.isStatic()) xw.field("STATIC",true);
   if (mthd.isSynthetic()) xw.field("SYNTHETIC",true);
   xw.field("SIGNATURE",mthd.getSignature());
   if (lvl > 0) xw.field("LEVEL",lvl);  
   dumpStackFrameFile(proc,frm,xw);
   List<BVariable> vars = frm.getVariables();
   if (vars != null) {
      for (BVariable lv : vars) {
         BubjetLog.logD("VISIBLE VARIABLE " + lv);
         BValue v = frm.getVariableValue(lv);
         outputValue(proc,v,lv,null,vdepth,arraysz,xw);
       }
    }
   
   for (String atn : mthd.getArgumentTypeNames()) {
      xw.begin("ARG");
      xw.field("TYPE",atn);
      xw.end("ARG");
    }
   xw.end("STACKFRAME");
}


private static String findSourceFile(BubjetDebugProcess proc,Location loc,boolean all)
{
   if (loc == null) return null;
   
   try {
      String path = loc.sourcePath();
      BubjetApplicationService appser = BubjetBundle.getAppService();
      BubjetProjectManager pm = appser.getProjectManager();
      return pm.findSourceFile(proc.getProject(),path,all);
    }
   catch (AbsentInformationException e) { }
   
   return null;
}



private static void dumpStackFrameFile(BubjetDebugProcess proc,BStackFrame frm,IvyXmlWriter xw)
{
   File fpath = frm.getSourceFile(false);
   if (fpath == null) {
      xw.field("FILETYPE","CLASSFILE");
      String path = frm.getSourcePath();
      BubjetLog.logD("BINARY PATH " + frm.getFullName() + " " + frm.getLineNumber() + " " +
            path);
      xw.field("FILE",path);
      File p1 = frm.getSourceFile(true);
      if (p1 == null) {
         xw.field("FILEPATH",path);
       }
      else {
         xw.field("FILEPATH",p1.getPath());
       }
      GlobalSearchScope scp = GlobalSearchScope.allScope(proc.getProject());
      JavaPsiFacade fac = JavaPsiFacade.getInstance(proc.getProject());
      PsiClass [] pcls = fac.findClasses(frm.getClassName(),scp);
      if (pcls.length == 1) {
         PsiDocumentManager pdm = PsiDocumentManager.getInstance(proc.getProject());
         Document pdoc = pdm.getDocument(pcls[0].getContainingFile());
         BMethod m = frm.getMethod();
         int start = -1;
         int len = -1;
         for (PsiMethod pm : pcls[0].getAllMethods()) {
            if (!m.getName().equals(pm.getName())) continue;
            TextRange rng = pm.getTextRange();
            int l0 = pdoc.getLineNumber(rng.getStartOffset());
            int l1 = pdoc.getLineNumber(rng.getEndOffset());
            if (frm.getLineNumber() >= l0 && frm.getLineNumber() <= l1) {
               start = rng.getStartOffset();
               len = rng.getLength();
               break;
             }
          }
         if (start > 0) {
            xw.field("SOURCEOFF",start);
            xw.field("SOURCELEN",len);
            xw.bytesElement("SOURCE",pdoc.getText().getBytes());
          }
       }
    }
   else {
      xw.field("FILE",fpath.getPath());
      xw.field("FILETYPE","JAVAFILE");
    }
}






static void outputValue(BubjetDebugProcess proc,BValue v,BVariable var,
      String name,int lvls,int arraysz,IvyXmlWriter xw)
{
  outputValue(proc, v,var,name,lvls,arraysz,null,xw);
}


static void outputValue(BubjetDebugProcess proc,BValue val,BVariable var,
      String name,int lvls,int arraysz,Set<String> donestatics,IvyXmlWriter xw)
{
   if (donestatics == null) donestatics = new HashSet<>();
   if (val == null) return;
   
   BubjetLog.logD("OUTPUT VALUE " + val + " " + val.getClass() + " " + var + " " + lvls + " " + arraysz);
   
   if (name == null && var != null) {
      name = var.toString();
    }
   String dtyp = null;
   String typ = "null";
   if (val.getValueType() != null) typ = val.getValueType().getName();
   String txt = val.toString();   
   boolean lcl = false;
   boolean stat = false;
   
   if (var instanceof BField) {
      BField jfv = (BField) var;
      dtyp = jfv.getDeclaringType().getName();
      stat = jfv.isStatic();
      name = jfv.getName();
    }
   else if (var instanceof BLocal) {
      BLocal lclv = (BLocal) var;
      lcl = true;
      name = lclv.getName();
    }
   else if (var instanceof BThis) {
      name = "this";
      lcl = true;
    }
   if (dtyp != null && donestatics.contains(dtyp) && stat) return;
   
   xw.begin("VALUE");
   try {
      if (name != null) xw.field("NAME",name);
      if (dtyp != null) xw.field("DECLTYPE",dtyp);
      if (lcl) xw.field("LOCAL",true);
      if (stat) xw.field("STATIC",true);
      xw.field("TYPE",typ);
      
      if (val instanceof BArray) {
         BArray arr = (BArray) val;
         int len = arr.getLength();
         xw.field("KIND","ARRAY");
         xw.field("LENGTH",len);
         xw.field("HASVARS",len > 0);
         int sz = arraysz;
         if (sz == 0) sz = 100;
         if (sz < 0) sz = len+1;
         if (lvls > 0) {
            int ctr = Math.min(len,sz);
            for (int i = 0; i < ctr; ++i) {
               outputValue(proc,arr.getIndexValue(i),null,"[" + i + "]",lvls-1,arraysz,donestatics,xw);
             }
          }
       }
      else if (val instanceof BPrimitive) {
         xw.field("KIND","PRIMITIVE");
       }
      else if (val instanceof BClass) {
         BClass cor = (BClass) val;
         xw.field("KIND","CLASS");
         xw.field("TYPENAME",cor.getReflectedType().getName());
       }
      else if (val instanceof BString) {
         xw.field("KIND","STRING");
       }
      else if (val instanceof BObject) {
         xw.field("KIND","OBJECT");
         BObject obj = (BObject) val;
         List<BField> flds = obj.getValueType().getFields();
         xw.field("HASVARS",flds.size() > 0);
         if (lvls > 0) {
            Map<BField,BValue> fvals = obj.getFieldValues(flds);
            for (Map.Entry<BField,BValue> ent : fvals.entrySet()) {
               outputValue(proc,ent.getValue(),ent.getKey(),null,lvls-1,
                     arraysz,donestatics,xw);
             }
            if (dtyp != null) donestatics.add(dtyp);
          }
       }
      if (txt.length() >= MAX_VALUE_SIZE) {
         txt = txt.substring(0,MAX_VALUE_SIZE) + "...";
       }
      xw.cdataElement("DESCRIPTION",txt);
    }
   catch (Throwable t) {
      BubjetLog.logE("Problem outputing value",t);
    }
   xw.end("VALUE");
}



static void outputEvaluation(BubjetDebugProcess proc,BValue val,String err,String expr,
      int lvls,int arraysz,IvyXmlWriter xw) 
{
   xw.begin("EVAL");
   if (err != null) {
      xw.field("STATUS","ERROR");
      xw.textElement("ERROR",err);
    }
   else if (val != null) {
      xw.field("STATUS","OK");
      outputValue(proc,val,null,expr,lvls,arraysz,xw);
    }
   xw.cdataElement("EXPR",expr);
   xw.end("EVAL");
}


/********************************************************************************/
/*                                                                              */
/*      Handle source files                                                     */
/*                                                                              */
/********************************************************************************/

static Collection<VirtualFile> findSourceFiles(Module m,Collection<VirtualFile> rslt) 
{
   if (rslt == null) rslt = new LinkedHashSet<>();
   
   Set<VirtualFile> excl = new HashSet<>(getExcludeRoots(m));
   List<VirtualFile> cand = getSourceRoots(m);
   for (VirtualFile vf3 : cand) {
      BubjetLog.logD("SOURCE ROOT " + vf3);
      findSourceFiles(vf3,excl,rslt);
    }
   
   return rslt;
}



static Collection<VirtualFile> findSourceFiles(VirtualFile base,Set<VirtualFile> excl,Collection<VirtualFile> rslt)
{
   if (rslt == null) rslt = new HashSet<>();
   
   if (excl != null && excl.contains(base)) return rslt;
   
   if (!base.isValid()) ;
   else if (base.isDirectory()) {
      if (isValidDirectory(base)) {
         for (VirtualFile chld : base.getChildren()) {
            findSourceFiles(chld,excl,rslt);
          }
       }
    }
   else if (isValidFile(base)) {
      rslt.add(base);
    }
   
   return rslt;
}



static boolean isValidDirectory(VirtualFile vf)
{
   if (vf == null) return false;
   if (!vf.exists()) return false;
   if (vf.getChildren().length == 0) return false;
   if (vf.getName().equals("bBACKUP")) return false;
   if (vf.getName().startsWith(".")) return false;
   return true;
}


static boolean isValidFile(VirtualFile vf)
{
   if (vf == null) return false;
   if (!vf.isValid()) return false;
   if (!vf.exists()) return false;
   if (vf.getName().equals("bCONTROL")) return false;
   if (vf.getName().equals("bBUFFERS")) return false;
   if (vf.getName().startsWith(".")) return false;
   if (!vf.getExtension().equals("java")) return false;
   return true;
}

/********************************************************************************/
/*                                                                              */
/*      Other utilities                                                         */
/*                                                                              */
/********************************************************************************/

static GlobalSearchScope getSearchScope(Project p)
{
   return getSearchScope(p,null);
}


static GlobalSearchScope getSearchScope(Module m)
{
   if (m == null) return null;
   return getSearchScope(m.getProject(),m);
}

static GlobalSearchScope getSearchScope(Project p,Module m)
{
   GlobalSearchScope scp = new JavaFilesSearchScope(p);
   GlobalSearchScope scp1 = ProjectScope.getContentScope(p);
   scp = scp.intersectWith(scp1);
   
   if (m != null) {
      GlobalSearchScope mscp = null;
      for (Module mm : getAssociatedModules(m)) {
         GlobalSearchScope s1 = GlobalSearchScope.moduleScope(mm);
         if (mscp == null) mscp = s1;
         else mscp = mscp.union(s1);
       }
      scp = scp.intersectWith(mscp);
    }
   
   return scp;
}

static GlobalSearchScope getSourceSearchScope(Module m)
{
   Collection<VirtualFile> fils = findSourceFiles(m,null);
   BubjetLog.logD("Source files: " + fils);
   
   GlobalSearchScope scp0 = GlobalSearchScope.filesWithoutLibrariesScope(m.getProject(),fils);
   return scp0;
}

static String convertWildcardToRegex(String s)
{
   if (s == null) return null;
   
   StringBuffer nb = new StringBuffer(s.length()*8);
   int brct = 0;
   boolean qtfg = false;
   boolean bkfg = false;
   String star = "\\w*";
   
   nb.append('^');
   
   for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      if (bkfg) {
	 if (c == '\\') qtfg = true;
	 else if (!qtfg && c == ']') bkfg = false;
	 else { nb.append(c); qtfg = false; continue; }
       }
      if (c == '/' || c == '\\') {
	 if (File.separatorChar == '\\') nb.append("\\\\");
	 else nb.append(File.separatorChar);
       }
      else if (c == '@') nb.append(".*");
      else if (c == '*') nb.append(star);
      else if (c == '.') nb.append("\\.");
      else if (c == '{') { nb.append("("); ++brct; }
      else if (c == '}') { nb.append(")"); --brct; }
      else if (brct > 0 && c == ',') nb.append('|');
      else if (c == '?') nb.append(".");
      else if (c == '[') { nb.append(c); bkfg = true; }
      else nb.append(c);
    }
   
   nb.append('$');
   
   return nb.toString();
}

static PsiElement findContainter(PsiElement e) {
   for (PsiElement pe = e; pe != null; pe = pe.getParent()) {
      if (pe instanceof PsiMember) return pe;
      if (pe instanceof PsiFile) return null;
    }
   return null;
}

static VirtualFile getVirtualFile(String file)
{
   if (file == null) return null;
   LocalFileSystem lfs = LocalFileSystem.getInstance(); 
   VirtualFile vf = lfs.findFileByPath(file);
   return vf;
}


static PsiFile getPsiFile(Project p,VirtualFile vf)
{
   if (vf == null) return null;
   PsiManager pm = PsiManager.getInstance(p);
   return pm.findFile(vf);
}


static PsiJavaFile getPsiFile(Project p,String file)
{
   return getJavaFile(getPsiFile(p,getVirtualFile(file)));
}

static PsiJavaFile getJavaFile(PsiFile pf)
{
   if (pf == null) return null;
   
   if (pf instanceof PsiJavaFile) return (PsiJavaFile) pf;
   
   return null;
}



static PsiClass getClass(PsiElement e)
{
   for (PsiElement pe = e; pe != null; pe = pe.getParent()) {
      if (pe instanceof PsiClass) {
         return (PsiClass) pe;
       }
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Handle extended positions                                               */
/*                                                                              */
/********************************************************************************/

static TextRange getExtendedRange(PsiElement e)
{
   TextRange rng = e.getTextRange();
   if (e instanceof PsiMember) {
      PsiElement start = e;
      for (PsiElement pre = e.getPrevSibling(); pre != null; pre = pre.getPrevSibling()) {
         if (pre instanceof PsiComment) start = pre;
         else if (pre instanceof PsiWhiteSpace) ;
         else break;
       }
      PsiElement end = e;
      PsiElement last = e;
      boolean haveline = false;
      for (PsiElement post = e.getNextSibling(); post != null; post = post.getNextSibling()) {
         IElementType etyp = PsiUtilCore.getElementType(post);
         if (!haveline && etyp == JavaTokenType.END_OF_LINE_COMMENT) end = post;
         if (post instanceof PsiComment) last = post;
         else if (post instanceof PsiWhiteSpace) {
            if (post.getText().contains("\n")) haveline = true;
          }
         else {
            last = null;
            break;
          }
       }
      if (last != null) end = last;
      
      if (start != e || end != e) {
         int off = start.getTextRange().getStartOffset();
         int eoff = end.getTextRange().getEndOffset();
         rng = new TextRange(off,eoff);
       }
    }
   
   return rng;
}


static PsiElement getDefinitionElement(PsiElement e)
{
   PsiElement rslt = e;
   
   for (PsiElement e1 = e; e1 != null; e1 = e1.getParent()) {
      PsiReference pr = e1.getReference();
      if (pr != null) {
         PsiElement e0 = pr.resolve();
         if (e0 == null) {
            e0 = e1;
          }
         rslt = e0;
         break;
       }
      else if (e1 instanceof PsiMember || e1 instanceof PsiVariable) {
         rslt = e1;
         break;
       }
      else if (e1 instanceof PsiReferenceExpression) {
         if (e1.getParent() instanceof PsiMember) {
            rslt = e1.getParent();
            break;
          }
       }
      else if (e1 instanceof PsiDocParamRef) {
         rslt = null;
         break;
       }
    }
   
   if (rslt instanceof PsiJavaCodeReferenceElement) {
      rslt = ((PsiJavaCodeReferenceElement)rslt).getReferenceNameElement();
    }
   
   return rslt;
}


static PsiElement getDefinitionIdentifier(PsiElement e)
{
   if (e instanceof PsiNameIdentifierOwner) {
      return ((PsiNameIdentifierOwner) e).getNameIdentifier();
    }
   return e;
}


static PsiElement getContainingElement(PsiElement e) 
{
   for (PsiElement e1 = e; e1 != null; e1 = e1.getParent()) {
      if (e1 instanceof PsiMember || e1 instanceof PsiClass) {
         return e1;
       }
    }
   
   return e;
}



/********************************************************************************/
/*                                                                              */
/*      Module management                                                       */
/*                                                                              */
/********************************************************************************/

static List<Module> getAllModules(Project p)
{
   List<Module> rslt = new ArrayList<>();
   ModuleManager mm = ModuleManager.getInstance(p);
   for (Module sm : mm.getModules()) {
      Module pm = getParentModule(sm); 
      BubjetLog.logD("ALL MODULES " + sm + " " + pm);
      if (pm != null && pm == sm) rslt.add(sm);
    }
   return rslt;
}
   
static Module findModuleByName(Project p,String nm)
{
   ModuleManager mm = ModuleManager.getInstance(p);
   Module emod = mm.findModuleByName(nm);
   Module par = getParentModule(emod);
   return par;
}


static Module findModuleForFile(PsiFile pf)
{
   Module m = ModuleUtilCore.findModuleForFile(pf); 
   Module pm = getParentModule(m);   
   return pm;
}
  
static Module findModuleForFile(VirtualFile vf,Project p)
{
   Module m = ModuleUtilCore.findModuleForFile(vf,p);  
   if (m == null) {
      BubjetLog.logD("Module not found for file " + vf + " " + p + " " + p.isOpen() + " " + p.isInitialized());
    }
   Module pm = getParentModule(m);
   return pm;
}

static List<VirtualFile> getSourceRoots(Module m)
{
   List<VirtualFile> rslt = new ArrayList<>();
   for (Module mm : getAssociatedModules(m)) { 
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      for (VirtualFile vf : mrm.getSourceRoots()) {
         rslt.add(vf);
       }
    }
   
   return rslt;
}
   
static List<VirtualFile> getContentRoots(Module m)
{
   List<VirtualFile> rslt = new ArrayList<>();
   for (Module mm : getAssociatedModules(m)) { 
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      for (VirtualFile vf : mrm.getContentRoots()) {
         rslt.add(vf);
       }
    }
   
   return rslt;
}
   

static List<OrderEntry> getOrderEntries(Module m)
{
   List<OrderEntry> rslt = new ArrayList<>();
   for (Module mm : getAssociatedModules(m)) { 
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      for (OrderEntry vf : mrm.getOrderEntries()) {
         rslt.add(vf);
       }
    }
   
   return rslt;
}

static List<Module> getOrderModules(Module m)
{
   List<Module> rslt = new ArrayList<>();
   for (Module mm : getAssociatedModules(m)) {
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      List<Module> nmods = new ArrayList<>();
      mrm.orderEntries().forEachModule(mod -> nmods.add(mod));
      for (Module xm : nmods) {
         if (!rslt.contains(xm)) rslt.add(xm);
       }
    }
   return rslt;
}


static List<ContentEntry> getContentEntries(Module m)
{
   List<ContentEntry> rslt = new ArrayList<>();
   for (Module mm : getAssociatedModules(m)) {
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      for (ContentEntry ce : mrm.getContentEntries()) {
         rslt.add(ce);
       }
    }
   return rslt;
}

static Sdk getSdk(Module m)
{
   for (Module mm : getAssociatedModules(m)) {
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      Sdk sdk = mrm.getSdk();
      if (sdk != null) return sdk;
    }
   return null;
}



static List<Library> getLibraries(Module m)
{
   List<Library> rslt = new ArrayList<>();
   Set<Library> fnd = new HashSet<>();
   for (Module mm : getAssociatedModules(m)) {
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      List<Library> nrslt = new ArrayList<>();
      mrm.orderEntries().forEachLibrary(lib -> nrslt.add(lib));
      for (Library l : nrslt) {
         if (fnd.add(l)) {
            rslt.add(l);
          }
       }
    }
   return rslt;
}




static Collection<Module> getDependencies(Module m)
{
   Set<Module> rslt = new HashSet<>();
   for (Module mm : getAssociatedModules(m)) {
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      for (Module dm : mrm.getDependencies()) {
         Module pdm = getParentModule(dm);
         if (pdm != m) rslt.add(pdm);
       }
    }
   return rslt;
}


static Collection<Module> getUsedBy(Module m) 
{
   ModuleManager mman = ModuleManager.getInstance(m.getProject());
   Set<Module> rslt = new HashSet<>();
   for (Module mm : getAssociatedModules(m)) {
      for (Module dm : mman.getModuleDependentModules(mm)) {
         Module pdm = getParentModule(dm);
         if (pdm != m) rslt.add(pdm);
       }
    }
   return rslt;
}
static List<VirtualFile> getExcludeRoots(Module m)
{
   List<VirtualFile> rslt = new ArrayList<>();
   for (Module mm : getAssociatedModules(m)) { 
      ModuleRootManager mrm = ModuleRootManager.getInstance(mm);
      for (VirtualFile vf : mrm.getExcludeRoots()) {
         rslt.add(vf);
       }
    }
      
   return rslt;
}
   
   
private static List<Module> getAssociatedModules(Module m)
{
   if (m == null) return new ArrayList<>();
   List<Module> rslt = new ArrayList<>();
   rslt.add(m);
   String pfx = m.getName() + ".";
   ModuleManager mm = ModuleManager.getInstance(m.getProject());
   for (Module sm : mm.getModules()) {
      if (sm.getName().startsWith(pfx)) rslt.add(sm);
    }
   return rslt;
}

   
private static Module getParentModule(Module m)
{
   if (m == null) return null;
   String nm = m.getName();
   int idx = nm.indexOf(".");
   if (idx < 0) return m;
   nm = nm.substring(0,idx);
   ModuleManager mm = ModuleManager.getInstance(m.getProject());
   Module par = mm.findModuleByName(nm);  
   if (par == null) {
      BubjetLog.logD("Can't find module by name " + nm + " from " + m.getName());
    }
   return par;
}
}       // end of class BubjetUtil




/* end of BubjetUtil.java */

