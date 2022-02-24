/********************************************************************************/
/*                                                                              */
/*              BubjetProjectManager.java                                       */
/*                                                                              */
/*      Handle project-related requests                                         */
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

import com.intellij.openapi.module.ModuleManager;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.CompilerPaths;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.LanguageLevelUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Query;
import com.intellij.openapi.fileTypes.FileType;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.DataConversionException;

/**
 *      IntelliJ Idea and Eclipse (and hence Bubbles) use different terminology
 *      for projects, workspaces, etc.  Bubbles has a workspace that includes 
 *      multiple projects.  Intellij has a project that includes multiple modules.
 *      For that reason, this code maps and Intellij Project to a Bubbles workspace
 *      and and Intellij Module to a Bubbles project.
 *
 *      Note that while Eclipse has to be run as separate instances when run on
 *      separate workspaces, Intellij seems to be find running muliple projects in
 *      the same process.  This could lead to confusion that should be dealt with
 *      at some point.
 **/


class BubjetProjectManager implements BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService app_service;
private Map<Project,Boolean> open_projects;
private Set<Module> known_modules;







/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetProjectManager(BubjetApplicationService app)
{
   app_service = app;
   open_projects = new LinkedHashMap<>();
   known_modules = new HashSet<>();
}



/********************************************************************************/
/*                                                                              */
/*      Handle updates                                                          */
/*                                                                              */
/********************************************************************************/

void addProject(Project project)
{
   if (open_projects.containsKey(project)) return;
   open_projects.put(project,Boolean.FALSE);
   initializeProject(project);
   synchronized (this) {
      open_projects.put(project,Boolean.TRUE);
      notifyAll();
    }
}


void removeProject(Project project)
{
   open_projects.remove(project);
   for (Iterator<Module> it = known_modules.iterator(); it.hasNext(); ) {
      Module m = it.next();
      if (m.getProject() == project) it.remove();
    }
}


synchronized void waitForProject(Project p)
{
   for ( ; ; ) {
      Boolean fg = open_projects.get(p);
      if (fg == Boolean.TRUE) break;
      try {
         wait(1000);
       }
      catch (InterruptedException e) { }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle initialization                                                   */
/*                                                                              */
/********************************************************************************/

private void initializeProject(Project p)
{
   if (!p.isInitialized()) {
      
    }
   
   ModuleManager mm = ModuleManager.getInstance(p);
   for (Module m : mm.getModules()) {
      known_modules.add(m);
    }
}


Collection<Project> getProjects()
{
   return new ArrayList<>(open_projects.keySet());
}



/********************************************************************************/
/*                                                                              */
/*      Handle PROJECTS command                                                 */
/*                                                                              */
/********************************************************************************/

void listProjects(String ws,IvyXmlWriter xw)
{
   Project p = findProject(ws);
   for (Module m : known_modules) {
      if (m.getProject() != p) continue;
      xw.begin("PROJECT");
      xw.field("NAME",m.getName());
      xw.field("OPEN",m.isLoaded());
      xw.field("WORKSPACE",p.getBasePath());
      ModuleManager mm = ModuleManager.getInstance(p);
      ModuleRootManager mrm = ModuleRootManager.getInstance(m);
      outputProjectProperties(m,xw);
      xw.field("BASE",m.getModuleFilePath());
      for (Module dm : mrm.getDependencies()) {
         xw.textElement("REFERENCES",dm.getName());
       }
      for (Module dm : mm.getModuleDependentModules(m)) {
         xw.textElement("USEDBY",dm.getName());
       }
      xw.end("PROJECT");
    }
}



private void outputProjectProperties(Module m,IvyXmlWriter xw)
{
   ModuleRootManager mrm = ModuleRootManager.getInstance(m);
   boolean isjava = false;
   boolean isandroid = false;
   Sdk sdk = mrm.getSdk();
   if (m.getModuleTypeName() == null || m.getModuleTypeName().equals("JAVA_MODULE")) 
       isjava = true;
   BubjetLog.logD("Module " + m.getName() + " TYPE = " + m.getModuleTypeName());
   BubjetLog.logD("Module " + m.getName() + " SDK = " + sdk + " " + sdk.getName() + " " +
         sdk.getSdkType());
   if (sdk.getName().contains("JavaSDK")) isjava = true;
   if (sdk.getName().contains("android")) isandroid = true;
   xw.field("ISJAVA",isjava);
   xw.field("ISANDROID",isandroid);
}



/********************************************************************************/
/*                                                                              */
/*      Handle OPENPROJECT command                                              */
/*                                                                              */
/********************************************************************************/

void openProject(String ws,String name,boolean fil,boolean pat,boolean cls,boolean opt,boolean imps,
      String bkg,IvyXmlWriter xw)
{
   Project p = findProject(ws);
   Module m = findModule(p,name);
   if (m == null) return;
   if (bkg != null) {
      ModuleThread pt = new ModuleThread(bkg,m,fil,pat,cls,opt);
      pt.start();
      if (xw != null) outputModule(m,false,false,false,false,false,xw);
    }
   else if (xw != null) {
      outputModule(m,fil,pat,cls,opt,imps,xw);
    }
}




private class ModuleThread extends Thread {

   private Module for_module;
   private boolean do_files;
   private boolean do_patterns;
   private boolean do_classes;
   private boolean do_options;
   private String return_id;
   
   ModuleThread(String bkg,Module p,boolean fil,boolean pat,boolean cls,boolean opt) {
      super("Bubjet_GetModuleInfo");
      return_id = bkg;
      for_module = p;
      do_files = fil;
      do_patterns = pat;
      do_classes = cls;
      do_options = opt;
    }
   
   @Override public void run() {
      Project p = for_module.getProject();
      IvyXmlWriter xw = app_service.getMonitor(p).beginMessage("PROJECTDATA");
      xw.field("BACKGROUND",return_id);
      outputModule(for_module,do_files,do_patterns,do_classes,do_options,false,xw);
      app_service.getMonitor(p).finishMessage(xw);
    }

}	// end of inner class Modulehread



/********************************************************************************/
/*                                                                              */
/*      Handle PREFERENCES command                                              */
/*                                                                              */
/********************************************************************************/

void outputPreferences(String ws,String proj,IvyXmlWriter xw)
{
   Project p = findProject(ws);
   Module m = findModule(p,proj);
   if (m == null) return;
   Map<String,String> prefs = getPreferences(m);
  
   xw.begin("PREFERENCES");
   for (Map.Entry<String,String> ent : prefs.entrySet()) {
      xw.begin("PREF");
      xw.field("NAME",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.end("PREF");
    }
   xw.end("PREFERENCES");
}



/********************************************************************************/
/*                                                                              */
/*      Handle BUILDPROJUECT command                                            */
/*                                                                              */
/********************************************************************************/

void buildProject(String ws,String proj,boolean clean,boolean full,boolean refresh,
      IvyXmlWriter xw)
{
   Project p = findProject(ws);
   Module m = findModule(p,proj);
   if (m == null) return;
   
   BubjetLog.logD("BUILD REQUEST " + Thread.currentThread() + " " + clean + " " + 
         full + " " + refresh);
   // need to hancle CLEAN, FULL, REFRESH
   
   
   if (refresh) {
//       ProjectManager pm = ProjectManager.getInstance();
//       pm.reloadProject(p);
      // closes and reopens -- has odd effects
    }
   
   BuildNotifier bn = new BuildNotifier();
   BuildAction act = new BuildAction(p,m,full|clean,bn,xw);
   CompilerManager cm = CompilerManager.getInstance(p);
   while (cm.isCompilationActive()) {
      synchronized (this) {
         try {
            wait(300);
          }
         catch (InterruptedException e) { }
       }
    }
   act.start();
   
   CompileContext ctx = bn.getResult();
   BuildResultAction acta = new BuildResultAction(p,ctx,xw);
   acta.start();
   
// CompilerManager cm = CompilerManager.getInstance(p);
// if (full) cm.rebuild(bn);
// else {
//    CompileScope csp = cm.createModulesCompileScope(new Module[] {m},true,true);
//    cm.compile(csp,bn);
//  }
}


private class BuildResultAction extends BubjetAction.Read {
   
   private Project for_project;
   private CompileContext for_context;
   private IvyXmlWriter xml_writer;
   
   BuildResultAction(Project p,CompileContext ctx,
         IvyXmlWriter xw) {
      for_project = p;
      for_context = ctx;
      xml_writer = xw;
    }
         
   @Override void process() {     
      int ct = 0;
      for (CompilerMessage msg : for_context.getMessages(CompilerMessageCategory.ERROR)) {
         if (ct++ < MAX_PROBLEM) BubjetUtil.outputProblem(for_project,msg,xml_writer);
       }
      for (CompilerMessage msg : for_context.getMessages(CompilerMessageCategory.WARNING)) {
         if (ct++ < MAX_PROBLEM) BubjetUtil.outputProblem(for_project,msg,xml_writer);
       }
      // want to handle TODO messages -- requires a search
    }
   
}       // end of inner class BuildProjectAction







private class BuildAction extends BubjetAction.Dispatch {

   private Project for_project;
   private boolean rebuild_all;
   private BuildNotifier build_notifier;
// private IvyXmlWriter xml_writer;
   
   BuildAction(Project p,Module mod,boolean full,BuildNotifier bn,IvyXmlWriter xw) {
      super(p,"Build " + p.getName());
      for_project = p;
      build_notifier = bn;
      rebuild_all = full;
//    rebuild_all = false;              // this doesn't seem to work correctly    
//    xml_writer = xw;
    }
   
   @Override void process() {
      // PsiDocumentManager.getInstance(for_project).commitAllDocuments();
      BubjetLog.logD("BUILD ACTION STARTED");
      CompilerManager cm = CompilerManager.getInstance(for_project);
      if (rebuild_all) {
         cm.rebuild(build_notifier);
       }
      else {
         CompileScope pscp = cm.createProjectCompileScope(for_project);
         cm.makeWithModalProgress(pscp,build_notifier);
       }
      BubjetLog.logD("BUILD ACTION DONE");
    }
   
}       // end of inner class BuildAction



private class BuildNotifier implements CompileStatusNotification {
   
   private boolean is_done;
   private CompileContext result_context;
   
   BuildNotifier() {
      is_done = false;
      result_context = null;
    }
   
   synchronized CompileContext getResult() {
      while (!is_done) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
      return result_context;
    }
   
   @Override public synchronized void finished(boolean aborted,int err, int warn,CompileContext ctx) {
      BubjetLog.logD("BUILD COMPILE DONE " + " " + aborted + " " + err + " " + warn + " " + ctx);
      is_done = true;
      result_context = ctx;
      notifyAll();
    }
   
}       // end of inner class BuildNotifier




/********************************************************************************/
/*                                                                              */
/*      Handle FINDPACKAGE command                                              */
/*                                                                              */
/********************************************************************************/

void findPackage(String ws,String proj,String pkg,IvyXmlWriter xw)
{
   Project p = findProject(ws);
   if (p == null) return;
   
   Module m = findModule(p,proj);
   GlobalSearchScope scp = null;
   if (m == null) scp = GlobalSearchScope.allScope(p);
   else scp = GlobalSearchScope.moduleScope(m);
   
   PackageIndex pidx = PackageIndex.getInstance(p);
   
   Query<VirtualFile> query = pidx.getDirsByPackageName(pkg,scp);
   for (VirtualFile vf : query) {
      xw.begin("PACKAGE");
      xw.field("NAME",pkg);
      xw.field("PATH",BubjetUtil.outputFileName(vf));
      xw.end("PACKAGE");
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle CREATEPACKAGE command                                            */
/*                                                                              */
/********************************************************************************/

void createPackage(String ws,String proj,String pkg,boolean frc,IvyXmlWriter xw)
{
   Project p = findProject(ws);
   if (p == null) return;
   
   Module m = findModule(p,proj);
   GlobalSearchScope scp = null;
   if (m == null) scp = GlobalSearchScope.allScope(p);
   else scp = GlobalSearchScope.moduleScope(m);
   
   ModuleRootManager mrm = ModuleRootManager.getInstance(m);
   PackageIndex pidx = PackageIndex.getInstance(p);
   PsiManager psim = PsiManager.getInstance(p);
   String xpkg = pkg;
   for (VirtualFile vf3 : mrm.getSourceRoots()) {
      BubjetLog.logD("SOURCE ROOT " + vf3);
      PsiDirectory pd = psim.findDirectory(vf3);
    }  
   
}




/********************************************************************************/
/*                                                                              */
/*      GETALLNAMES command                                                     */
/*                                                                              */
/********************************************************************************/

void getAllNames(String ws,String proj,String bid,Set<String> files,String bkg,
      IvyXmlWriter xw)
{
   Project p = findProject(ws);
   if (p == null) return;
   Module m = findModule(p,proj);
   
   NameThread nt = null;
   if (bkg != null) nt = new NameThread(p,bid,bkg,files);
   
   if (m != null) {
      handleAllNames(m,files,nt,xw);
    }
   else {
      BubjetProjectManager pm = app_service.getProjectManager();
      for (Module m1 : pm.getAllModules(p)) {
         handleAllNames(m1,files,nt,xw);
       }
    }
   
   if (nt != null) {
      nt.start();
    }
}

private void handleAllNames(Module m,Set<String> files,NameThread nt,IvyXmlWriter xw)
{
   if (m == null) return;
   
   BubjetLog.logD("Handle All Names for " + m);
   
   if (nt == null) outputModuleNames(m,files,xw);
   else nt.addModule(m);
}



private void outputModuleNames(Module m,Set<String> files,IvyXmlWriter xw)
{
   OutputModuleNamesAction act = new OutputModuleNamesAction(m,files,xw);
   act.start();
   act.finish();
}



private class OutputModuleNamesAction extends BubjetAction.SmartRead {
   
   private Module for_module;
   private Set<String> file_set;
   private IvyXmlWriter xml_writer;
   private String bump_id;
   private String name_id;
   private int name_counter;
   
   OutputModuleNamesAction(Module m,Set<String> files,IvyXmlWriter xw) { 
      super(m.getProject(),null);
      for_module = m;
      file_set = files;
      xml_writer = xw;
      bump_id = null;
      name_id = null;
      name_counter = 0;
    }
   
   OutputModuleNamesAction(Project p,Set<String> files,String bid,String nid) {
      super(p,null);
      for_module = null;
      file_set = files;
      xml_writer = null;
      bump_id = bid;
      name_id = nid;
      name_counter = 0;
    }
   
   @Override void process() {
      if (for_module == null) return;
      ModuleRootManager mrm = ModuleRootManager.getInstance(for_module);
      Set<VirtualFile> excl = new HashSet<>();
      PsiManager psim = PsiManager.getInstance(for_module.getProject());
      for (VirtualFile vf : mrm.getExcludeRoots()) {
         BubjetLog.logD("EXCLUDE " + vf + " " + BubjetUtil.outputFileName(vf));
         excl.add(vf);
       }
      setupWriter();
      BubjetUtil.outputJavaProject(for_module,xml_writer);
      Set<String> pkgs = new HashSet<>();
      for (VirtualFile vf3 : mrm.getSourceRoots()) {
         Collection<VirtualFile> cands = findSourceFiles(vf3,excl,null);
         BubjetLog.logD("Start with root " + vf3 + " " + cands.size());
         for (VirtualFile vf : cands) {
            if (!vf.isValid()) continue;
            setupWriter();
            File f = new File(BubjetUtil.outputFileName(vf));
            if (file_set != null && !file_set.isEmpty() && !file_set.contains(f.getPath())) {
               BubjetLog.logD("SKIP NAMES FOR SOURCE " + f);
               continue;
             }
            PsiFile pf = psim.findFile(vf);
            if (pf instanceof PsiJavaFile) {
               PsiJavaFile pjf = (PsiJavaFile) pf;
               if (pkgs.add(pjf.getPackageName())) {
                  BubjetUtil.outputJavaElement(for_module,pjf.getParent(),false,xml_writer);
                }
               xml_writer.begin("FILE");
               xml_writer.textElement("PATH",BubjetUtil.outputFileName(pf.getVirtualFile()));
               name_counter += BubjetUtil.outputJavaElement(for_module,pjf,true,xml_writer);
               xml_writer.end("FILE");
             }
          }
       }
    }
   
   void setModule(Module m) {
      for_module = m;
    }
   
   void finish() {
      finishWriter(true);
    }
   
   private void setupWriter() {
      if (name_id != null) {
         if (name_counter > MAX_NAMES) {
            finishWriter(false);
          }
         if (xml_writer == null) {
            xml_writer = app_service.getMonitor(for_module.getProject()).beginMessage("NAMES",bump_id);
            xml_writer.field("NID",name_id);
            name_counter = 0;
          }
       }
    }
   
   private void finishWriter(boolean end) {
      BubjetMonitor mon = app_service.getMonitor(for_module.getProject());
      if (xml_writer != null) {
         BubjetLog.logD("SEND NAMES " + name_counter + " " + xml_writer.getLength());
         String sts = mon.finishMessageWait(xml_writer);
         BubjetLog.logD("NAME STATUS: " + sts);
         xml_writer = null;
         name_counter = 0;
       }
      if (end) {
         BubjetLog.logD("FINISH NAMES FOR " + name_id);
         xml_writer = mon.beginMessage("ENDNAMES",bump_id);
         xml_writer.field("NID",name_id);
         mon.finishMessage(xml_writer);
         xml_writer = null;
         name_counter = 0;
       }
    }
   
}       // end of inner class OutputModuleNamesAction








   


private class NameThread extends Thread {

   private Project for_project;
   private String bump_id;
   private String name_id;
   private Set<String> file_set;
   private List<Module> do_modules;
   
   NameThread(Project p,String bid,String nid,Set<String> fset) {
      super("Bubjet_GetNames");
      for_project = p;
      bump_id = bid;
      name_id = nid;
      file_set = fset;
      do_modules = new ArrayList<>();
    }
   
   void addModule(Module m) {
      do_modules.add(m);
    }
   
   @Override public void run() {
      BubjetLog.logD("START NAMES FOR " + name_id);
     
      OutputModuleNamesAction act = new OutputModuleNamesAction(for_project,file_set,bump_id,name_id);
      for (Module m : do_modules) {
         act.setModule(m);
         act.start();
       }
      act.finish();
      
      BubjetLog.logD("FINISH BACKGROUND NAMES FOR " + name_id);
    }
   
}       // end of inner class NameThread



/********************************************************************************/
/*                                                                              */
/*      Output module information                                               */
/*                                                                              */
/********************************************************************************/

private void outputModule(Module m,boolean file,boolean path,boolean cls,boolean opt,boolean imps,
      IvyXmlWriter xw)
{
   if (m == null) return;
   ModuleRootManager mrm = ModuleRootManager.getInstance(m);
   ModuleManager mm = ModuleManager.getInstance(m.getProject());
   
   Set<VirtualFile> excl = new HashSet<>();
   for (VirtualFile vf : mrm.getExcludeRoots()) {
      BubjetLog.logD("EXCLUDE " + vf + " " + BubjetUtil.outputFileName(vf));
      excl.add(vf);
    }
   
   xw.begin("PROJECT");
   xw.field("NAME",m.getName());
   xw.field("WORKSPACE",m.getProject().getBasePath());
   VirtualFile vf = m.getModuleFile();
   VirtualFile vf1 = vf.getParent();
   xw.field("PATH",BubjetUtil.outputFileName(vf1));
   outputProjectProperties(m,xw);
   
   if (path) {
      xw.begin("CLASSPATH");
      addClassPaths(m,xw,null,false,false);
      xw.end("CLASSPATH");
      xw.begin("RAWPATH");
      addClassPaths(m,xw,null,false,true);
      xw.end("RAWPATH");
    }
   
   if (file) {
      xw.begin("FILES");
      for (VirtualFile vf3 : mrm.getSourceRoots()) {
         BubjetLog.logD("SOURCE ROOT " + vf3);
         addSourceFiles(vf3,excl,xw,null);
       }
      xw.end("FILES");
    }
   
   if (cls) {
      xw.begin("CLASSES");
      addClasses(m,excl,xw);
      xw.end("CLASSES");
    }
  
   for (Module dm : mrm.getDependencies()) {
      xw.textElement("REFERENCES",dm.getName());
    }
   for (Module dm : mm.getModuleDependentModules(m)) {
      xw.textElement("USEDBY",dm.getName());
    }
   
   if (opt) {
      Map<String,String> prefs = getPreferences(m);
      for (Map.Entry<String,String> ent : prefs.entrySet()) {
         xw.begin("OPTION");
         xw.field("NAME",ent.getKey());
         xw.field("VALUE",ent.getValue());
         xw.end("OPTION");
       }
    }
   
   if (imps) {
      ModAddImportsAction aia = new ModAddImportsAction(m,excl,xw);
      aia.start();
    }
   
   xw.end("PROJECT");
}



/********************************************************************************/
/*                                                                              */
/*      Output message information                                              */
/*                                                                              */
/********************************************************************************/







/********************************************************************************/
/*                                                                              */
/*      Handle class paths                                                      */
/*                                                                              */
/********************************************************************************/

private void addClassPaths(Module m,IvyXmlWriter xw,Set<Object> done,boolean nest,boolean sys)
{
   if (done == null) done = new HashSet<>();
   if (!done.add(m)) return;
   
   Set<String> spaths = new HashSet<>();
   ModuleRootManager mrm = ModuleRootManager.getInstance(m);
   BubjetLog.logD("Getting class paths for " + m.getName());
   for (ContentEntry cent : mrm.getContentEntries()) {
      VirtualFile vf1 = cent.getFile();
      if (!isValidDirectory(vf1)) continue;
      xw.begin("PATH");
      xw.field("TYPE","SOURCE");
      xw.field("ID",vf1.hashCode());
      String sp = BubjetUtil.outputFileName(vf1);
      spaths.add(sp);
      xw.textElement("SOURCE",sp);
      xw.end("PATH");
    }
   VirtualFile p1 = CompilerPaths.getModuleOutputDirectory(m,false);
   if (p1 != null && p1.exists() && done.add(p1)) {
      xw.begin("PATH");
      xw.field("TYPE","BINARY");
      xw.textElement("BINARY",BubjetUtil.outputFileName(p1));
      xw.end("PATH");
    }
   VirtualFile p2 = CompilerPaths.getModuleOutputDirectory(m,true);
   if (p2 != null && p2.exists() && done.add(p2)) {
      xw.begin("PATH");
      xw.field("TYPE","BINARY");
      xw.textElement("BINARY",BubjetUtil.outputFileName(p2));
      xw.end("PATH");
    }
   List<Library> libs = new ArrayList<>();
   mrm.orderEntries().forEachLibrary(lib -> libs.add(lib));
   for (Library lib : libs) {
      for (VirtualFile vf4 : lib.getFiles(OrderRootType.CLASSES)) {
         if (done.add(vf4)) {
            xw.begin("PATH");
            xw.field("ID",vf4.hashCode());
            if (nest) xw.field("NESTED",nest);
            xw.field("TYPE","LIBRARY");
            xw.textElement("BINARY",BubjetUtil.outputFileName(vf4));
            xw.end("PATH");
          }
       }
    }
   if (sys) {
      for (OrderEntry oent : mrm.getOrderEntries()) {
         VirtualFile [] vfs = oent.getFiles(OrderRootType.CLASSES);
         if (vfs.length == 0 || done.contains(vfs[0])) continue;
         BubjetLog.logD("SYSTEM " + oent.getOwnerModule() + " " + oent.getPresentableName());
         for (VirtualFile vf1 : vfs) {
            if (done.add(vf1)) {
               String url = vf1.getUrl();
               String fp = BubjetUtil.outputFileName(vf1);
               xw.begin("PATH");
               xw.field("TYPE","BINARY");
               xw.field("SYSTEM",true);
               if (url.startsWith("jrt:")) {
                  xw.field("MODULE",true);
                  int idx1 = fp.indexOf("!");
                  if (idx1 > 0) {
                     File m1 = new File(fp.substring(0,idx1));
                     File m2 = new File(m1,"jmods");
                     File m3 = new File(m2,fp.substring(idx1+1)+".jmod");
                     if (m3.exists()) fp = m3.getPath();
                     else BubjetLog.logD("Can't file module file for " + fp + " " + m3);
                   }
                  xw.field("BINARY",fp);
                }
               xw.end("PATH");
             }
          }
       }
    }
   
   List<Module> mods = new ArrayList<>();
   mrm.orderEntries().forEachModule(mod -> mods.add(mod));
   for (Module mod : mods) {
      addClassPaths(mod,xw,done,true,false);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle source files                                                     */
/*                                                                              */
/********************************************************************************/

Collection<VirtualFile> findSourceFiles(VirtualFile base,Set<VirtualFile> excl,Collection<VirtualFile> rslt)
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



private void addSourceFiles(VirtualFile base,Set<VirtualFile> excl,IvyXmlWriter xw,FileFilter ff) 
{
   Collection<VirtualFile> cands = findSourceFiles(base,excl,null);
   for (VirtualFile vf : cands) {
      if (!vf.isValid()) continue;
      File f = new File(BubjetUtil.outputFileName(vf));
      if (ff != null && !ff.accept(f)) continue;
      xw.begin("FILE");
      FileType vft = vf.getFileType();
      if (vft.getDefaultExtension().equals("java")) {
         xw.field("SOURCE",true);
       }
      else if (vft.getDefaultExtension().equals("class")) {
         xw.field("BINARY",true);
       }
      else xw.field("TYPENAME",vft.getName());
      if (!vf.isWritable()) xw.field("READONLY",true);
//    if (vf.isSymLink()) xw.field("LINKED",true);
      xw.field("PROJPATH",vf.getPath());
      xw.field("PATH",BubjetUtil.outputFileName(vf));
      xw.text(BubjetUtil.outputFileName(vf));
      xw.end("FILE");
    }
}


/********************************************************************************/
/*                                                                              */
/*      Handle listing classes                                                  */
/*                                                                              */
/********************************************************************************/

private void addClasses(Module m,Set<VirtualFile> excl,IvyXmlWriter xw)
{
   // build class name to source file map
   ModuleRootManager mrm = ModuleRootManager.getInstance(m);   
   Set<VirtualFile> vfs = new HashSet<>();
   for (VirtualFile vf3 : mrm.getSourceRoots()) {
      findSourceFiles(vf3,excl,vfs);
    }
   Map<String,VirtualFile> srcmap = new HashMap<>();
   for (VirtualFile vf4 : vfs) {
      if (vf4.getExtension() != null && vf4.getExtension().equals("java")) {
         String path = vf4.getPath();
         path = path.replace(File.separator,".");
         String [] comps = path.split("\\.");
         String nm = null;
         for (int i = comps.length-2; i > 0; --i) {
            if (nm == null) nm = comps[i];
            else nm = comps[i] + "." + nm;
            srcmap.put(nm,vf4);
          }
       }

    }
   Set<Object> done = new HashSet<>();
   
   VirtualFile p1 = CompilerPaths.getModuleOutputDirectory(m,false);
   
   addTopClasses(m,p1,done,srcmap,xw);
   VirtualFile p2 = CompilerPaths.getModuleOutputDirectory(m,true);
   addTopClasses(m,p2,done,srcmap,xw);
}


private void addTopClasses(Module m,VirtualFile vf,Set<Object> done,Map<String,VirtualFile> srcs,
      IvyXmlWriter xw)
{
   if (vf == null) return;
   if (!isValidDirectory(vf)) return;
   for (VirtualFile vf1 : vf.getChildren()) {
      addClasses(vf1,null,done,srcs,xw);
    }
}


private void addClasses(VirtualFile vf,String pfx,Set<Object> done,
      Map<String,VirtualFile> srcs,
      IvyXmlWriter xw)
{
   if (done.add(vf)) {
      if (vf.isDirectory()) {
         if (isValidDirectory(vf)) {
            if (pfx == null) pfx = vf.getName();
            else pfx = pfx + "." + vf.getName();
            xw.textElement("PACKAGE",pfx);
            for (VirtualFile vf1 : vf.getChildren()) {
               addClasses(vf1,pfx,done,srcs,xw);
             }
          }
       }
      else {
         FileType ft = vf.getFileType();
         if (ft.isBinary() && ft.getDefaultExtension().equals("class")) {
            String cnm = vf.getNameWithoutExtension();
            String fnm = (pfx == null ? cnm : pfx + "." + cnm);
            xw.begin("TYPE");
            xw.field("NAME",fnm);
            xw.field("BINARY",BubjetUtil.outputFileName(vf));
            VirtualFile src = srcs.get(fnm);
            if (src == null) {
               int idx = fnm.indexOf("$");
               if (idx > 0) {
                  String fnm1 = fnm.substring(0,idx);
                  src = srcs.get(fnm1);
                }
               if (src == null) BubjetLog.logD("No source found for " + fnm);
             }
            if (src != null) xw.field("SOURCE",BubjetUtil.outputFileName(src));
            xw.end("TYPE");
          }
       }
   }
}




/********************************************************************************/
/*                                                                              */
/*      Output Imports information                                              */
/*                                                                              */
/********************************************************************************/

private class ModAddImportsAction extends BubjetAction.Read {
   
   private Module for_module;
   private Set<VirtualFile> exclude_files;
   private IvyXmlWriter xml_writer;
   
   ModAddImportsAction(Module m,Set<VirtualFile> excl,IvyXmlWriter xw) {
      BubjetLog.logD("Create AddImportsAction " + m);
      for_module = m;
      exclude_files = excl;
      xml_writer = xw;
    }
   
   @Override void process() {
      BubjetLog.logD("Add imports action " + for_module);
      List<VirtualFile> srcs = new ArrayList<>();
      ModuleRootManager mrm = ModuleRootManager.getInstance(for_module);   
      for (VirtualFile vf3 : mrm.getSourceRoots()) {
         findSourceFiles(vf3,exclude_files,srcs);
       }
      
      PsiManager pm = PsiManager.getInstance(for_module.getProject());
      String havepkg = null;
      for (VirtualFile vf : srcs) {
         if (vf.getFileType() == JavaFileType.INSTANCE) {
            PsiFile ps = pm.findFile(vf);
            if (ps instanceof PsiJavaFile) {
               PsiJavaFile jps = (PsiJavaFile) ps;
               PsiImportList imps = jps.getImportList();
               String pkg = jps.getPackageName();
               if (havepkg != null && !havepkg.equals(pkg)) havepkg = null;
               for (PsiImportStatement istmt : imps.getImportStatements()) {
                  xml_writer.begin("IMPORT");
                  if (havepkg == null) {
                     xml_writer.field("PACKAGE",pkg);
                     havepkg = pkg;
                   }
                  if (istmt.isOnDemand()) xml_writer.field("DEMAND",true);
                  xml_writer.text(istmt.getQualifiedName());
                  xml_writer.end("IMPORT");
                }
               for (PsiImportStaticStatement istmt : imps.getImportStaticStatements()) {
                  xml_writer.begin("IMPORT");
                  if (havepkg == null) {
                     xml_writer.field("PACKAGE",pkg);
                     havepkg = pkg;
                   }
                  if (istmt.isOnDemand()) xml_writer.field("DEMAND",true);
                  xml_writer.field("STATIC",true);
                  xml_writer.text(istmt.getReferenceName());
                  xml_writer.end("IMPORT");
                }
             }
          }
       }
    }
   
}       // end of inner class ModAddImportsAction





/********************************************************************************/
/*                                                                              */
/*      Preference/Option methods                                               */
/*                                                                              */
/********************************************************************************/

private Map<String,String> getPreferences(Module m)
{
   GetPreferencesAction gpa = new GetPreferencesAction(m);
   gpa.start();
   return gpa.getResult();
}


private class GetPreferencesAction extends BubjetAction.Read {
   
   private Module for_module;
   private Map<String,String> result_map;
   
   GetPreferencesAction(Module m) {
      for_module = m;
      result_map = null;
    }
   
   Map<String,String> getResult()                       { return result_map; }
   
   @Override void process() {
      app_service.getProjectManager().waitForProject(for_module.getProject());
      
      Map<String,String> rslt = new HashMap<>();
      
      ModuleRootManager mrm = ModuleRootManager.getInstance(for_module);
      Sdk sdk = mrm.getSdk();
      
      CodeStyleSettingsManager csm = CodeStyleSettingsManager.getInstance(for_module.getProject());
      CodeStyleSettings css = csm.createSettings();
      BubblesSettings bss = null;
      // need to register using LanguageCodeStyleSettingsProvider extension point
   // BubblesSettings bss = css.getCustomSettings(BubblesSettings.class);
      CommonCodeStyleSettings ccss = css.getCommonSettings(JavaLanguage.INSTANCE);
      IndentOptions ind = css.getIndentOptions(JavaFileType.INSTANCE);
      
      formatOpt("tabulation.size",ind.TAB_SIZE,rslt);
      formatOpt("indentation.size",ind.INDENT_SIZE,rslt);
      formatOpt("indent_statements_compare_to_block",true,rslt);
      formatOpt("indent_statements_compare_to_body",true,rslt);
      formatOpt("indent_body_declaration_compare_to_type_header",
            !ccss.DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS,rslt);
      formatOpt("continuation_indent",ind.CONTINUATION_INDENT_SIZE,rslt);
      int arr = 0;
      if (ccss.ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION) arr = 1;
      formatOpt("alignment_for_expressions_in_array_initializer",arr,rslt);
      int cond = 2;
      if (ccss.ALIGN_MULTILINE_TERNARY_OPERATION) cond = 1;
      formatOpt("alignment_for_condition_expression",cond,rslt);
      formatOpt("brace_position_for_block",getBraceStyle(ccss.BRACE_STYLE),rslt);
      String arrs = "eol";
      if (ccss.ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE) arrs = "next_line_shifted";
      formatOpt("brace_position_for_array_initializer",arrs,rslt);
      formatOpt("brace_position_for_method_declaration",getBraceStyle(ccss.METHOD_BRACE_STYLE),rslt);
      formatOpt("brace_position_for_type_declaration",getBraceStyle(ccss.CLASS_BRACE_STYLE),rslt);
      
      formatOpt("indent_switchstatements_compare_to_switch",ccss.INDENT_CASE_FROM_SWITCH,rslt);
      formatOpt("indent_switchstatements_compare_to_cases",ccss.INDENT_BREAK_FROM_CASE,rslt);
      int parm = 1;
      if (ccss.ALIGN_MULTILINE_PARAMETERS) parm = 2;
      formatOpt("alignment_for_parameters_in_method_declaration",parm,rslt);
      int args = 1;
      if (ccss.ALIGN_MULTILINE_PARAMETERS_IN_CALLS) parm = 2;
      formatOpt("alignment_for_arguments_in_method_declaration",args,rslt);
      
      CompilerConfiguration cc = CompilerConfiguration.getInstance(for_module.getProject());
      LanguageLevel ll = LanguageLevelUtil.getEffectiveLanguageLevel(for_module);
      String llv1 = sdk.getVersionString();
      String llv2 = sdk.getName();
      String llv = ll.toString();
      BubjetLog.logD("COMPILER " + ll + " " + llv + " " + llv1 + " " + llv2);
      if (llv.startsWith("JDK_")) llv = llv.substring(4);
      llv = llv.replace("_",".");
      compilerOpt("source",llv,rslt);
      String tgt = cc.getBytecodeTargetLevel(for_module);
      if (tgt == null) tgt = llv;
      compilerOpt("codegen.targetPlatform",tgt,rslt);
      compilerOpt("compliance",llv,rslt);
      compilerOpt("problem.fatalOptionalError",false,rslt);
      compilerOpt("processAnnotations",cc.isAnnotationProcessorsEnabled(),rslt);
      
      if (bss != null) {
         bubblesOpt("useContractsForJava",bss.use_contracts,rslt);
         bubblesOpt("useJunit",bss.use_junit,rslt);
         bubblesOpt("useAssertions",bss.use_assertions,rslt);
       }
   
      boolean isandroid = false;
      if (sdk.getName().contains("android")) isandroid = true;   
      rslt.put("bedrock.useAndroid",Boolean.toString(isandroid));
      
      result_map = rslt;
    }
   
}       // end of inner class GetPreferencesAction

private String getBraceStyle(int v)
{
   if (v == CommonCodeStyleSettings.END_OF_LINE) return "eol";
   else if (v == CommonCodeStyleSettings.NEXT_LINE) return "next_line";
   else if (v == CommonCodeStyleSettings.NEXT_LINE_SHIFTED) return "next_line_shitfed";
   else if (v == CommonCodeStyleSettings.NEXT_LINE_SHIFTED2) return "next_line_shifted";
   else if (v == CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED) return "next_line_if_wrapped";
   return "eol";
}


private void formatOpt(String name,Object val,Map<String,String> opts)
{
   String pnm = "org.eclipse.jdt.core.formatter." + name;
   String pvl = String.valueOf(val);
   opts.put(pnm,pvl);
}

private void compilerOpt(String name,Object val,Map<String,String> opts)
{
   String pnm = "org.eclipse.jdt.core.compiler." + name;
   String pvl = String.valueOf(val);
   opts.put(pnm,pvl);
}

private void bubblesOpt(String name,Object val,Map<String,String> opts)
{
   String pnm = "edu.brown.cs.bubbles.bedrock." + name;
   String pvl = String.valueOf(val);
   opts.put(pnm,pvl);
}



/********************************************************************************/
/*                                                                              */
/*      Save projects at exit                                                   */
/*                                                                              */
/********************************************************************************/

void saveAll()
{
   for (Project p : open_projects.keySet()) {
      p.save();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

private boolean isValidDirectory(VirtualFile vf)
{
   if (vf == null) return false;
   if (!vf.exists()) return false;
   if (vf.getChildren().length == 0) return false;
   if (vf.getName().equals("bBACKUP")) return false;
   if (vf.getName().equals(".idea")) return false;
   return true;
}


private boolean isValidFile(VirtualFile vf)
{
   if (vf == null) return false;
   if (!vf.isValid()) return false;
   if (!vf.exists()) return false;
   if (vf.getName().equals("bCONTROL")) return false;
   if (vf.getName().equals("bBUFFERS")) return false;
   return true;
}








Project findProject(String ws)
{
   if (ws != null) {
      for (Project p : open_projects.keySet()) {
         if (p.getName().equals(ws) ||
               p.getBasePath().equals(ws)) return p;
       }
    }
   
   if (open_projects.size() == 1) {
      for (Project p : open_projects.keySet()) {
         return p;
       }
    }
   
   return null;
}


Module findModule(Project p,String nm)
{
   if (nm == null) return null;
   
   for (Module m : known_modules) {
      if (!m.getName().equals(nm)) continue;
      if (p != null && m.getProject() != p) continue;
      return m;
    }
   
   return null;
}



List<Module> getAllModules(Project p) 
{
   List<Module> rslt = new ArrayList<>();
   if (p == null) {
      for (Project p1 : open_projects.keySet()) {
         rslt.addAll(getAllModules(p1));
       }
    }
   else {
      for (Module m : known_modules) {
         if (p != null && m.getProject() != p) continue;
         rslt.add(m);
       }
    }
   
   return rslt;
}






/********************************************************************************/
/*                                                                              */
/*      Project Settings                                                        */
/*                                                                              */
/********************************************************************************/

private static final String     SETTINGS_TAG = "BUBJET";

private static class BubblesSettings extends CustomCodeStyleSettings {
   
   boolean use_contracts;
   boolean use_junit;
   boolean use_assertions;
   
   BubblesSettings(CodeStyleSettings cs) {
      super(SETTINGS_TAG,cs);
      use_contracts = false;
      use_junit = false;
      use_assertions = false;
    }
   
   @Override public void readExternal(Element par) {
      Element child = par.getChild(SETTINGS_TAG);
      use_contracts = getBool(child,"CONTRACTS");
      use_junit = getBool(child,"JUNIT");
      use_assertions = getBool(child,"ASSERTIONS");
    }
      
   @Override public void writeExternal(Element par,CustomCodeStyleSettings parset) {
      Element child = new Element(SETTINGS_TAG);
      child.setAttribute("CONTRACTS",Boolean.toString(use_contracts));
      child.setAttribute("JUNIT",Boolean.toString(use_junit));
      child.setAttribute("ASSERTIONS",Boolean.toString(use_assertions));
    }
   
   private boolean getBool(Element elt,String id) {
      try {
         if (elt == null) return false;
         Attribute att = elt.getAttribute(id);
         if (att == null) return false;
         return att.getBooleanValue();
       }
      catch (DataConversionException e) { }
      return false;
    }
   
}       // end of inner class BubblesSettings


}       // end of class BubjetProjectManager




/* end of BubjetProjectManager.java */

