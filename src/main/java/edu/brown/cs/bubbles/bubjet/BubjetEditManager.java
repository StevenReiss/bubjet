/********************************************************************************/
/*                                                                              */
/*              BubjetEditManager.java                                          */
/*                                                                              */
/*      Handle editing commands                                                 */
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

import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ForwardDependenciesBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import com.intellij.openapi.editor.event.DocumentEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.Icon;

import org.w3c.dom.Element;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.completion.BatchConsumer;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionInitializationUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProcess;
import com.intellij.codeInsight.completion.CompletionProcessEx;
import com.intellij.codeInsight.completion.CompletionResult;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.OffsetMap;
import com.intellij.codeInsight.completion.OffsetsInFile;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
// import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;


class BubjetEditManager implements BubjetConstants, DocumentListener, EditorFactoryListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService app_service;
private Project for_project;
private Map<String,ParamSettings> param_map;
private Map<VirtualFile,BubjetFileData> file_map;
private ForwardDependenciesBuilder depend_builder;
private Map<PsiFile,UpdateType> analysis_queue;
private Set<PsiFile> active_files;

private static boolean open_editor = false;
private static boolean alt_complete = true;
private static CompletionType completion_type = CompletionType.BASIC;



// private static boolean edit_compile = true;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetEditManager(BubjetApplicationService app,Project p)
{
   app_service = app;
   for_project = p;
   param_map = new HashMap<>();
   file_map = new HashMap<>();
   
   EditorFactory efac = EditorFactory.getInstance();
   efac.getEventMulticaster().addDocumentListener(this,p);
   efac.addEditorFactoryListener(this,p);
   
   BuildDependencies bdact = new BuildDependencies();
   bdact.start();
   
   analysis_queue = new LinkedHashMap<>();
   active_files = new HashSet<>();
   
   AnalysisThread at = new AnalysisThread(true);
   at.start();
}


private class BuildDependencies extends BubjetAction.SmartBackgroundRead {
   
   BuildDependencies() {
      super(for_project,null);
    }
   
   @Override void process() {
      ForwardDependenciesBuilder fdb = new ForwardDependenciesBuilder(for_project,
            new AnalysisScope(for_project),1);
      fdb.analyze();
      depend_builder = fdb;
    }
}




/********************************************************************************/
/*                                                                              */
/*      Handle EDITPARAM command                                                */
/*                                                                              */
/********************************************************************************/

void handleEditParameter(Project p,Module m,String bid,String name,String value) throws BubjetException
{
   if (name == null) return;
   else if (name.equals("AUTOELIDE")) {
      setAutoElide(bid,(value != null && value.length() > 0 && "tTyY1".indexOf(value.charAt(0)) >= 0));
    }
   else if (name.equals("ELIDEDELAY")) {
      try {
	 setElideDelay(bid,Long.parseLong(value));
       }
      catch (NumberFormatException e) {
	 throw new BubjetException("Bad elide delay value: " + value);
       }
    }
   else {
      throw new BubjetException("Unknown editor parameter " + name);
    }
}



private void setAutoElide(String id,boolean v)	{ getParameters(id).setAutoElide(v); }
private void setElideDelay(String id,long v)	{ getParameters(id).setElideDelay(v); }

private ParamSettings getParameters(String id)
{
   ParamSettings ps = param_map.get(id);
   if (ps == null) {
      ps = new ParamSettings();
      param_map.put(id,ps);
    }
   return ps;
}


private boolean getAutoElide(String id) { return getParameters(id).getAutoElide(); }

private long getElideDelay(String id)	{ return getParameters(id).getElideDelay(); }




/********************************************************************************/
/*                                                                              */
/*      Handle STARTFILE command                                                */
/*                                                                              */
/********************************************************************************/

void handleStartFile(Project p,Module m,String bid,String file,int id,
      boolean cnts,IvyXmlWriter xw)
        throws BubjetException
{
   StartFileAction sfa = new StartFileAction(p,m,bid,file,id,cnts,xw);
   sfa.start();
}



private class StartFileAction extends BubjetAction.Command {
   
   private Module for_module;
   private String bubbles_id;
   private VirtualFile for_file;
   private int edit_id;
   private boolean show_contents;
   private IvyXmlWriter xml_writer;
   
   StartFileAction(Project p,Module m,String bid,String file,int id,
         boolean cnts,IvyXmlWriter xw) {
      super(p,null,null);
      for_module = m;
      bubbles_id = bid;
      for_file = BubjetUtil.getVirtualFile(file);
      edit_id = id;
      show_contents = cnts;
      xml_writer = xw;
    }
         
   @Override void process() throws BubjetException {
      BubjetFileData fd = findFile(for_file,bubbles_id);
      if (fd == null) {
         throw new BubjetException("Compilation unit for file " + for_file + " not available in " + for_module);
       }
      fd.setCurrentId(bubbles_id,edit_id);
      BubjetLog.logD("OPEN file " + for_file + " " + fd.hasChanged() + " " + bubbles_id + " " + edit_id);
      
      String lsep = fd.getLineSeparator();
      if (lsep.equals("\n")) xml_writer.field("LINESEP","LF");
      else if (lsep.equals("\r\n")) xml_writer.field("LINESEP","CRLF");
      else if (lsep.equals("\r")) xml_writer.field("LINESEP","CR");
      
      if (fd.getModule() != null) {
         xml_writer.field("PROJECT",fd.getModule().getName());
       }
      
      if (show_contents || fd.hasChanged()) {
         String s = fd.getCurrentContents();
         if (s == null) xml_writer.emptyElement("EMPTY");
         else {
            byte [] data = s.getBytes();
            xml_writer.bytesElement("CONTENTS",data);
          }
       }
      else xml_writer.emptyElement("SUCCESS");
      
      if (open_editor) {
         FileEditorManager fem = FileEditorManager.getInstance(for_project);
         if (!fem.isFileOpen(fd.getVirutalFile())) {
            BubjetLog.logD("Attempt to open file " + fd.getVirutalFile());
            FileEditor [] eds = fem.openFile(fd.getVirutalFile(),false,true);
            BubjetLog.logD("Returned " + eds.length);
          }
       }
    }

}       // end of inner class StartFileAction




/********************************************************************************/
/*                                                                              */
/*      Handle FINISHFILE command                                               */
/*                                                                              */
/********************************************************************************/

void handleFinishFile(Project p,Module m,String bid,String file)
{ 
   FinishFileAction act = new FinishFileAction(p,m,bid,file);
   act.start();
}



private class FinishFileAction extends BubjetAction.Command {
   
   private VirtualFile for_file;
   private String bubbles_id;
   
   FinishFileAction(Project p,Module m,String bid,String file) {
      super(p,null,null);
      for_file = BubjetUtil.getVirtualFile(file);
      bubbles_id = bid;
    }
   
   @Override void process() {
      BubjetFileData fd = file_map.get(for_file);
      if (fd == null) return;
      if (fd.removeUser(bubbles_id)) file_map.remove(for_file);
      if (open_editor) {
         FileEditorManager fem = FileEditorManager.getInstance(for_project);
         fem.closeFile(for_file);
       }
    }
   
}       // end of inner class FinishFileAction




/********************************************************************************/
/*                                                                              */
/*      Handle EDITFILE command                                                 */
/*                                                                              */
/********************************************************************************/

void handleEditFile(Project p,Module m,String bid,String file,int id,
      List<EditData> edits,
      IvyXmlWriter xw)
   throws BubjetException
{
   VirtualFile vf = BubjetUtil.getVirtualFile(file);
   BubjetFileData fd = findFile(vf,bid);
   if (fd == null) throw new BubjetException("File " + file + " not available");
   fd.setCurrentId(bid,id);
   
   EditFileAction efa = new EditFileAction(p,m,bid,vf,fd,id,edits,xw);
   efa.start();
}


private class EditFileAction extends BubjetAction.Command {
   
   private BubjetFileData file_data;
   private String buffer_id;
   private int edit_id;
   private List<EditData> edit_list;
   private IvyXmlWriter xml_writer;
   
   EditFileAction(Project p,Module m,String bid,VirtualFile vf,BubjetFileData fd,int id,
         List<EditData> edits,IvyXmlWriter xw) {
      super(p,"TextEdit",fd.getDocument());
      file_data = fd;
      buffer_id = bid;
      edit_id = id;
      edit_list = edits;
      xml_writer = xw;
    }
   
   @Override void process() throws BubjetException {
      synchronized (analysis_queue) {
         active_files.add(file_data.getPsiFile(buffer_id));
         analysis_queue.remove(file_data.getPsiFile(buffer_id));
       }
      for (EditData eds : edit_list) {
         file_data.textEdit(buffer_id,eds);
       }
      PostEditAction pea = new PostEditAction(file_data,buffer_id,edit_id);
      pea.start();
      
      xml_writer.emptyElement("SUCCESS");
    }
   
}       // end of inner class EditFileAction



private class PostEditAction extends BubjetAction.BackgroundRead implements CompileStatusNotification {
   
   private BubjetFileData file_data;
   private String bubbles_id;
   private int edit_id;
   private BubjetMonitor our_monitor;
   
   PostEditAction(BubjetFileData fd,String bid,int id) {
      super(fd.getProject(),"*");
      file_data = fd;
      bubbles_id = bid;
      edit_id = id;
      our_monitor = app_service.getMonitor(for_project);
    }
   
   @Override void process() {
      BubjetLog.logD("RUN POST EDIT ACTION");
      if (!file_data.isCurrent(bubbles_id,edit_id)) return;
      long delay = getElideDelay(bubbles_id);
      BubjetLog.logD("EDIT DELAY = " + delay);
      if (delay > 0) {
         synchronized (this) {
            try { wait(delay); }
            catch (InterruptedException e) { }
          }
       }
      
      if (!file_data.isCurrent(bubbles_id,edit_id)) return;
      
//    outputPsiErrors();
      
//    CompilerManager cm = CompilerManager.getInstance(for_project);
//    VirtualFile [] vfs = new VirtualFile[] { file_data.getVirutalFile() };
//    synchronized (compiler_lock) {
//       if (!file_data.isCurrent(bubbles_id,edit_id)) return;
//       for (int i = 0; i < 10; ++i) {
//          if (cm.isCompilationActive()) {
//             try {
//                compiler_lock.wait(500);
//                BubjetLog.logD("WAIT FOR COMPILER");
//              }
//             catch(InterruptedException e) { }
//           }
//          else break;
//        }
//       if (!file_data.isCurrent(bubbles_id,edit_id)) return;
//       if (!cm.isCompilationActive()) {
//          BubjetLog.logD("START COMPILE " + file_data.getVirutalFile());
//          if (edit_compile) cm.compile(vfs,this);
//        }
//     }
      
//    CommitAction cma = new CommitAction(file_data,null);
//    cma.start();
      
      file_data.updatePsiFile();
      if (depend_builder != null) {
         Set<PsiFile> dep = depend_builder.getDependencies().get(file_data.getPsiFile());
         addToQueue(dep,UpdateType.RECHECK);
       }
      
      if (!file_data.isCurrent(bubbles_id,edit_id)) return;
      outputElision();
      
      if (!file_data.isCurrent(bubbles_id,edit_id)) return;
      BubjetErrorPass ep = new BubjetErrorPass(file_data,bubbles_id,edit_id,null);
      ep.process();
      
      removeFromQueue(file_data.getPsiFile());
      
      BubjetLog.logD("POST EDIT DONE");
    }
   
   @Override public void finished(boolean aborted,int err,int warn,CompileContext ctx) {
      BubjetLog.logD("POSTEDIT CompileFinished " + aborted + " " + err + " " + warn + " " +
        edit_id + " " + file_data.isCurrent(bubbles_id,edit_id));
      if (aborted) {
         if (file_data.isPrivateId(bubbles_id)) {
            IvyXmlWriter xw = our_monitor.beginMessage("PRIVATEERROR",bubbles_id);
            xw.field("FILE",file_data.getFileName());
            xw.field("ID",edit_id);
            xw.field("FAILURE",true);
            our_monitor.finishMessage(xw);
          }
         return;
       }
      if (!file_data.isCurrent(bubbles_id,edit_id)) return;
      String what = (file_data.isPrivateId(bubbles_id) ? "PRIVATEERROR" : "FILEERROR");
      IvyXmlWriter xw = app_service.getMonitor(for_project).beginMessage(what);
      xw.field("FILE",BubjetUtil.outputFileName(file_data.getVirutalFile()));
      xw.field("PROJECT",file_data.getModule().getName());
      xw.begin("MESSAGES");
      for (CompilerMessage msg : ctx.getMessages(CompilerMessageCategory.ERROR)) {
         BubjetUtil.outputProblem(for_project,msg,xw);
       }
      for (CompilerMessage msg : ctx.getMessages(CompilerMessageCategory.WARNING)) {
         BubjetUtil.outputProblem(for_project,msg,xw);
       }
      xw.end("MESSAGES");
      app_service.getMonitor(for_project).finishMessage(xw);
    }
   
   // private void outputPsiErrors() {
   // String what = (file_data.isPrivateId(bubbles_id) ? "PRIVATEERROR" : "FILEERROR");
   // IvyXmlWriter xw = app_service.getMonitor(for_project).beginMessage(what);
   // xw.field("FILE",BubjetUtil.outputFileName(file_data.getVirutalFile()));
   // xw.field("PROJECT",file_data.getModule().getName());
   // xw.begin("MESSAGES");
   // PsiFile pf = file_data.getPsiFile();
   // for (PsiErrorElement err : PsiTreeUtil.collectElementsOfType(pf,PsiErrorElement.class)) {
   //    BubjetLog.logD("FOUND ERROR ELEMENT: " + err + " " + err.getContainingFile() + " " +
   //          err.getTextRange() + " " + err.getParent() + " " + err.getParent().getTextRange() + " " +
   //          err.getChildren().length + " " + err.getErrorDescription());
   //    BubjetUtil.outputProblem(for_project,file_data.getDocument(),err,xw);
   //  }
   // xw.end("MESSAGES");
   // app_service.getMonitor(for_project).finishMessage(xw);
   //  }
   
   private void outputElision() {
      if (!file_data.isCurrent(bubbles_id,edit_id)) return;
      BubjetLog.logD("CHECK ELIDE " + getAutoElide(bubbles_id));
      if (getAutoElide(bubbles_id)) {
         BubjetElider be = file_data.checkElider(bubbles_id);
         if (be != null) {
            IvyXmlWriter xw = our_monitor.beginMessage("ELISION",bubbles_id);
            xw.field("FILE",file_data.getFileName());
            xw.field("ID",edit_id);
            xw.begin("ELISION");
            if (be.computeElision(file_data.getPsiFile(),xw)) {
               if (file_data.isCurrent(bubbles_id,edit_id)) {
                  xw.end("ELISION");
                  our_monitor.finishMessage(xw);
                }
             }
          }
       } 
    }
   
}       // end of inner class PostEditAction






/********************************************************************************/
/*                                                                              */
/*      Handle COMMIT command                                                   */
/*                                                                              */
/********************************************************************************/

void handleCommit(Project p,Module m,String bid,boolean refresh,boolean save,
      boolean compile,Collection<Element> files,IvyXmlWriter xw)
{
   CommitAction act = new CommitAction(p,m,bid,refresh,save,compile,files,xw);
   act.start();
}



private class CommitAction extends BubjetAction.WriteDispatch {
   
   private boolean do_refresh;
   private boolean do_save;
   private boolean do_compile;
   
   CommitAction(Project p,Module m,String bid,boolean refresh,boolean save,
         boolean compile,Collection<Element> files,IvyXmlWriter xw) {
      do_refresh = refresh;
      do_save = save;   
      do_compile = compile;
    } 
   
   @Override void process() {
      if (do_refresh) {
         for (BubjetFileData bfd : file_map.values()) {
            bfd.refresh();
          }
       }
      if (do_save) {
         for (BubjetFileData bfd : file_map.values()) {
            bfd.saveFile();
          }
       }
      if (do_compile) {
         BubjetLog.logD("COMMIT COMPILE " + depend_builder);
         Set<PsiFile> comp = new LinkedHashSet<>();
         for (BubjetFileData bfd : file_map.values()) {
            bfd.updatePsiFile();
            comp.add(bfd.getPsiFile());
            if (depend_builder != null) {
               Set<PsiFile> dep = depend_builder.getDependencies().get(bfd.getPsiFile());
               comp.addAll(dep);
             }
          }
         addToQueue(comp,UpdateType.RECHECK);
       }
    }
   
}       // end of inner class CommitAction



/********************************************************************************/
/*                                                                              */
/*      Handle ELIDESET and FILEELIDE commands                                  */
/*                                                                              */
/********************************************************************************/

void handleElideSet(Project p,Module m,String bid,String file,boolean compute,
        List<Element> regions,IvyXmlWriter xw) throws BubjetException
{
   VirtualFile vf = BubjetUtil.getVirtualFile(file);
   BubjetFileData fd = findFile(vf,bid);
   if (fd == null) throw new BubjetException("File " + file + " not available");
   BubjetElider be = null;
   if (regions != null) {
      be = fd.getElider(bid);
      be.clearElideData();
      for (Element r : regions) {
         double pr = IvyXml.getAttrDouble(r,"PRIORITY",-1);
	 int soff = IvyXml.getAttrInt(r,"START");
	 int eoff = IvyXml.getAttrInt(r,"END");
	 if (soff < 0 || eoff < 0) throw new BubjetException("Missing start or end offset for elision region");
	 if (pr >= 0) be.addElidePriority(soff,eoff,pr);
	 else be.addElideRegion(soff,eoff);
       }
    }
   else if (compute) {
      be = fd.checkElider(bid);
    }
   else {
      fd.clearElider(bid);
    }
   
   if (compute) {
      ElideSetAction esa = new ElideSetAction(p,vf,be,xw);
      esa.start();
    }
}


private class ElideSetAction extends BubjetAction.SmartRead {
  
   private Project for_project;
   private VirtualFile for_file;
   private BubjetElider use_elider;
   private IvyXmlWriter xml_writer;
   
   ElideSetAction(Project p,VirtualFile vf,BubjetElider be,IvyXmlWriter xw) {
      super(p,null);
      for_project = p;
      for_file = vf;
      use_elider = be;
      xml_writer = xw;
    }
   
   @Override void process() {
      xml_writer.begin("ELISION");
      if (use_elider != null) {
         use_elider.computeElision(PsiManager.getInstance(for_project).findFile(for_file),xml_writer);
       }
      xml_writer.end("ELISION");
    }
}



void handleFileElide(Project p,Module m,byte [] cnts,IvyXmlWriter xw)
{
   BubjetElider be = new BubjetElider();
   if (cnts == null) return;
   be.addElideRegion(0,cnts.length);
   String scnts = new String(cnts);
   VirtualFile vf = new LightVirtualFile("TEMP_ELIDE",JavaFileType.INSTANCE,scnts);
   PsiManager pm = PsiManager.getInstance(p);
   PsiFile pf = pm.findFile(vf);
   be.computeElision(pf,xw);
}



/********************************************************************************/
/*                                                                              */
/*      Handle GETCOMPLETIONS command                                           */
/*                                                                              */
/********************************************************************************/

void handleGetCompletions(Project p,Module m,String bid,String file,int offset,
      IvyXmlWriter xw)
{
   VirtualFile vf = BubjetUtil.getVirtualFile(file);
   BubjetFileData fd = findFile(vf,bid);
   if (alt_complete) {
      AltGetCompletionsAction act = new AltGetCompletionsAction(fd,offset,bid,xw);
      act.start();
    }
   else {
      GetCompletionsAction act = new GetCompletionsAction(fd,offset,bid,xw);
      act.start();
    }
}


private class GetCompletionsAction extends BubjetAction.Read {
   
   private BubjetFileData file_data;
   private int use_offset;
   private IvyXmlWriter xml_writer;
   
   GetCompletionsAction(BubjetFileData fd,int offset,String bid,IvyXmlWriter xw) {
      file_data = fd;
      use_offset = offset;
      xml_writer = xw;
    } 
   
   @Override public void process() {
      PsiFile pf = file_data.getPsiFile();
      Editor ed = file_data.getEditor();
      Caret caret = ed.getCaretModel().getCurrentCaret();
      caret.setSelection(use_offset,use_offset);
      
      CompletionInitializationContext ctx = new CompletionInitializationContext(ed,caret,pf.getLanguage(),
            pf,CompletionType.BASIC,1);
      OffsetsInFile off = new OffsetsInFile(pf,ctx.getOffsetMap());
    
      BubjetCompletionProcess proc = new BubjetCompletionProcess(file_data,ctx);
      CompletionParameters p1 = CompletionInitializationUtil.createCompletionParameters(ctx,proc,off);
      proc.setParameters(p1);
      CompletionParameters params = createParameters(pf,ed,use_offset,proc);
      CompletionParameters p2 = createParameters(pf,ed,use_offset-1,proc);
      CompletionParameters p3 = params.withType(CompletionType.SMART);
      CompletionParameters p4 = p2.withType(CompletionType.SMART);
      CompletionService service = CompletionService.getCompletionService();
//    file_data.getDocument().insertString(0,"");
      xml_writer.begin("COMPLETIONS");
      BubjetLog.logD("P0");
      service.performCompletion(params,new CompleteProc(xml_writer,use_offset));
      BubjetLog.logD("P1");
      service.performCompletion(p1,new CompleteProc(xml_writer,use_offset));
      BubjetLog.logD("P2");
      service.performCompletion(p2,new CompleteProc(xml_writer,use_offset));
      BubjetLog.logD("P3");
      service.performCompletion(p3,new CompleteProc(xml_writer,use_offset));
      BubjetLog.logD("P4");
      service.performCompletion(p4,new CompleteProc(xml_writer,use_offset));
//    CodeCompletionHandlerBase hb1 = CodeCompletionHandlerBase.createHandler(CompletionType.BASIC,
//          true,false,true);
//    CodeCompletionHandlerBase hb2 = CodeCompletionHandlerBase.createHandler(CompletionType.SMART,
//          true,false,true);
//    BubjetLog.logD("HB1");
//    hb1.invokeCompletion(for_project,ed);
//    BubjetLog.logD("HB2");
//    hb2.invokeCompletion(for_project,ed);
      xml_writer.end("COMPLETIONS");
    }
   
}       // end of inner class GetCompletionsAction



private class AltGetCompletionsAction extends BubjetAction.WriteDispatch {

   private BubjetFileData file_data;
   private int use_offset;
   private IvyXmlWriter xml_writer;
   
   AltGetCompletionsAction(BubjetFileData fd,int offset,String bid,IvyXmlWriter xw) {
      file_data = fd;
      use_offset = offset;
      xml_writer = xw;
    } 
   
   @Override public void process() {
      PsiFile pf = file_data.getPsiFile();
      Editor ed = file_data.getEditor();
      Caret caret = ed.getCaretModel().getCurrentCaret();
      caret.setSelection(use_offset,use_offset);
      CompletionInitializationContext ctx = new CompletionInitializationContext(ed,caret,pf.getLanguage(),
            pf,completion_type,1);
      OffsetsInFile off = new OffsetsInFile(pf,ctx.getOffsetMap());
   
      BubjetCompletionProcess proc = new BubjetCompletionProcess(file_data,ctx);
      CompletionParameters p1 = CompletionInitializationUtil.createCompletionParameters(ctx,proc,off);
      proc.setParameters(p1);
      OffsetsInFile hostoff = CompletionInitializationUtil.insertDummyIdentifier(ctx,proc).get();
      BubjetLog.logD("HOST OFFSETS " + hostoff.getFile() + " " + pf + " " + (pf == hostoff.getFile()) +
         " " + hostoff.getOffsets());
      CompletionParameters p2 = CompletionInitializationUtil.createCompletionParameters(ctx,proc,hostoff);
      
      xml_writer.begin("COMPLETIONS"); 
      try {
         CompletionService service = CompletionService.getCompletionService();
         BubjetLog.logD("P2");
         service.performCompletion(p2,new CompleteProc(xml_writer,use_offset));
   //    BubjetLog.logD("P3");
   //    service.performCompletion(p3,new CompleteProc(xml_writer));
       }
      catch (Throwable t) {
         BubjetLog.logE("Problem doing completion",t);
       }
      xml_writer.end("COMPLETIONS");
    }
   
}       // end of inner class AltGetCompletionsAction

private CompletionParameters createParameters(PsiFile file,Editor ed,int offset,CompletionProcess proc)
{
   PsiElement elt = file.findElementAt(offset);
   
   BubjetLog.logD("COMPPARAM " + file.findElementAt(offset) + " "  + file.findElementAt(offset-1) + " " + 
         file.findElementAt(offset-2) + " " + elt.getParent() + " " + offset + " " +
         elt.getTextRange() + " " + elt.getParent().getText());
   
   return new CompletionParameters(elt,file,CompletionType.BASIC,offset,0,ed,proc);
}


private class BubjetCompletionProcess implements CompletionProcessEx {
   
   private BubjetFileData file_data;
   private CompletionInitializationContext cur_context;
   private CompletionParameters cur_parameters;
   
   BubjetCompletionProcess(BubjetFileData fd,CompletionInitializationContext ctx) {
      file_data = fd;
      cur_context = ctx;
      cur_parameters = null;
    }
   
   @Override public Project getProject()        { return for_project; }
   
   @Override public Editor getEditor()          { return file_data.getEditor(); }
   
   @Override public Caret getCaret() {
      return getEditor().getCaretModel().getCurrentCaret();
    }
   
   @Override public OffsetMap getOffsetMap() {
      return cur_context.getOffsetMap();
    }
   
   @Override public OffsetsInFile getHostOffsets() {
      return new OffsetsInFile(file_data.getPsiFile(),getOffsetMap());
    }
   
   @Override public Lookup getLookup()          { return null; }
   
   @Override public void registerChildDisposable(Supplier<? extends Disposable> child) {
      BubjetLog.logD("REGISTER CHILD DISPOSABLE " + child);
    }
   
   @Override public void itemSelected(LookupElement item,char c) {
      BubjetLog.logD("ITEM SELECTED " + item + " " + c);
    }
   
   @Override public void addAdvertisement(String message,Icon icon) {
      BubjetLog.logD("ADD ADVERTISEMENT " + message);
    }
   
   @Override public CompletionParameters getParameters() {
      return cur_parameters;
    }
   
   @Override public void setParameters(CompletionParameters p) {
      cur_parameters = p;
    }
   
   @Override public void scheduleRestart() {
      BubjetLog.logD("SCHEDULE RESTART");
    }
   
   @Override public void prefixUpdated() {
      BubjetLog.logD("PREFIX UPDATED");
    }
   
   @Override public void addWatchedPrefix(int start,ElementPattern<String> cond) {
      BubjetLog.logD("ADD WATCHED PREFIX " + start + " " + cond + " " +
         cur_context.getInvocationCount() + " " + cur_context + " " + cur_parameters);
    }
   
   @Override public boolean isAutopopupCompletion() {
      return false;
    }
   
}       // end of inner class BubjetComplete



private class CompleteProc implements BatchConsumer<CompletionResult> {
   
   private IvyXmlWriter xml_writer;
   private int num_complete;
   private int start_offset;
   
   CompleteProc(IvyXmlWriter xw,int off) {
      xml_writer = xw;
      num_complete = 0;
      start_offset = off;
    }
   
   @Override public void startBatch() {
      BubjetLog.logD("COMPLETION START");
      num_complete = 0;
    }
   
   @Override public void consume(CompletionResult r) {
      LookupElement le = r.getLookupElement();
      if (le.getAutoCompletionPolicy() == AutoCompletionPolicy.NEVER_AUTOCOMPLETE) return;
      PrefixMatcher pm = r.getPrefixMatcher();
      if (!pm.isStartMatch(le)) return;
      
      BubjetLog.logD("COMPLETION RESULT " + r + " " + pm.isStartMatch(le) + " " +
          le.getUserDataString());
      
      BubjetUtil.outputCompletion(r,num_complete++,start_offset,xml_writer);
    }
   
   @Override public void endBatch() {
      BubjetLog.logD("COMPLETION END");
    }
   
}       // end of inner class CompleteProc




/********************************************************************************/
/*                                                                              */
/*      Handle CREATEPRIVATE command                                            */
/*                                                                              */
/********************************************************************************/

void handleCreatePrivate(Project p,Module m,String bid,String pid,String file,
      String frompid,IvyXmlWriter xw)
{
   VirtualFile vf = BubjetUtil.getVirtualFile(file);
   BubjetFileData fd = findFile(vf,bid);
   CreatePrivateAction cpa = new CreatePrivateAction(fd,pid,frompid);
   cpa.start();
   xw.text(cpa.getBufferPid());
}



private class CreatePrivateAction extends BubjetAction.Read {

   private BubjetFileData file_data;
   private String buffer_pid;
   private String from_pid;
   
   CreatePrivateAction(BubjetFileData fd,String pid,String frompid) {
      file_data = fd;
      buffer_pid = pid;
      from_pid = frompid;
    }
   
   String getBufferPid()                        { return buffer_pid; }
   
   @Override void process() {
      if (buffer_pid == null) {
         for (int i = 0; i < 100; ++i) {
            int v = (int) Math.random()*10000000;
            String pid = "PID_" + v;
            if (file_data.createPrivateBuffer(pid,from_pid)) break;
          }
       }
    }
   
}



/********************************************************************************/
/*                                                                              */
/*      Handle REMOVEPRIVATE command                                            */
/*                                                                              */
/********************************************************************************/

void handleRemovePrivate(Project p,Module m,String bid,String file)
{
   VirtualFile vf = BubjetUtil.getVirtualFile(file);
   BubjetFileData fd = findFile(vf,null);
   if (fd != null) fd.removePrivateBuffer(bid);
}
/********************************************************************************/
/*                                                                              */
/*      File data manipulation                                                  */
/*                                                                              */
/********************************************************************************/

synchronized BubjetFileData findFile(VirtualFile vf,String bid)
{
   if (vf == null) return null;
   
   BubjetFileData fd = file_map.get(vf);
   if (fd == null && bid != null) {
      BubjetLog.logD("START FILE " + vf + " " + bid);
      fd = new BubjetFileData(app_service,for_project,vf);
      file_map.put(vf,fd);
    }
   else if (fd == null) return null;
   
   fd.beginUser(bid);
  
   return fd;
}



/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations                                         */
/*                                                                              */
/********************************************************************************/

@Override public void beforeDocumentChange(DocumentEvent evt)
{ 
   BubjetLog.logD("EditManager beforeDocumentChange " + evt);
}


@Override public void documentChanged(DocumentEvent evt)
{ 
   BubjetLog.logD("EditManager documentChange " + evt);
}


@Override public void bulkUpdateStarting(Document doc)
{  
   BubjetLog.logD("EditManager bulkUpdateStarting " + doc);
}


@Override public void bulkUpdateFinished(Document doc)
{
   BubjetLog.logD("EditManager bulkUpdateFinished");
}


@Override public void editorCreated(EditorFactoryEvent evt)
{
   BubjetLog.logD("Editor created " + evt.getEditor());
}


@Override public void editorReleased(EditorFactoryEvent evt)
{
   BubjetLog.logD("Editor released " + evt.getEditor());
}



/********************************************************************************/
/*                                                                              */
/*      Parameter Settings                                                      */
/*                                                                              */
/********************************************************************************/

private static class ParamSettings {

   private boolean auto_elide;
   private long    elide_delay;
   
   ParamSettings() {
      auto_elide = false;
      elide_delay = 0;
    }
   
   boolean getAutoElide()		{ return auto_elide; }
   long getElideDelay() 		{ return elide_delay; }
   
   void setAutoElide(boolean fg)	{ auto_elide = fg; }
   void setElideDelay(long v)		{ elide_delay = v; }
   
}	// end of inner class ParamSettings


  

/********************************************************************************/
/*                                                                              */
/*      Background Compile Error Checking Thread                                */
/*                                                                              */
/********************************************************************************/

void addToQueue(Collection<PsiFile> fset,UpdateType ut)
{
   if (fset == null) return;
   
   synchronized(analysis_queue) {
      for (PsiFile pf : fset) addToQueue(pf,ut);
      analysis_queue.notifyAll();
    }
}



void addToQueue(PsiFile f,UpdateType ut)
{
   if (f == null) return; 
   
   BubjetLog.logD("Add to analysis queue: " + ut + " " + f);
   
   synchronized (analysis_queue) {
      if (active_files.contains(f)) return;
      UpdateType utold = analysis_queue.get(f);
      if (utold == null) analysis_queue.put(f,ut);
      else if (ut == utold) return;
      else if (ut.ordinal() > utold.ordinal()) analysis_queue.put(f,ut);
    }
}


void addFilesToQueue(Collection<VirtualFile> vfset,UpdateType ut)
{
   if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
      BubjetLog.logE("READ ACCESS DENIED on files add");
      return;
    }
   
   synchronized (analysis_queue) {
      for (VirtualFile vf : vfset) {
         PsiFile pf = BubjetUtil.getPsiFile(for_project,vf);
         addToQueue(pf,ut);
       }
      analysis_queue.notifyAll();
    }
}


private void removeFromQueue(PsiFile f)
{
   if (f == null) return;
   
   synchronized (analysis_queue) {
      active_files.remove(f);
    }
}


class AnalysisThread extends Thread {
  
   private boolean do_progress;
   private BubjetProgressIndicator progress_indicator;
   
   AnalysisThread(boolean prog) {
      super("Bubjet Compiler Analysis Thread");
      setDaemon(true);
      do_progress = prog;
    }
   
   @Override public void run() {
      BubjetLog.logD("Analysis queue started");
      DumbService ds = DumbService.getInstance(for_project);
      BubjetLog.logD("Analysis queue running");
      double maxsz = 0;
      for ( ; ; ) {
         maxsz = Math.max(maxsz,analysis_queue.size());
         UpdateType ut = null;
         PsiFile pf = null;
         synchronized (analysis_queue) {
            while (analysis_queue.isEmpty()) {
               try {
                  analysis_queue.wait();
                }
               catch (InterruptedException e) { }
             }
            maxsz = Math.max(maxsz,analysis_queue.size());
            for (Iterator<PsiFile> it = analysis_queue.keySet().iterator(); it.hasNext(); ) {
               pf = it.next();
               ut = analysis_queue.get(pf);
               it.remove();
               break;
             }
          }
         if (pf == null) continue;
         
         if (do_progress && maxsz > 0) {
            if (progress_indicator == null) {
               progress_indicator = new BubjetProgressIndicator("FileAnalysis",for_project);
               progress_indicator.setText("File Analysis");
               progress_indicator.start();
             }
            else {
               progress_indicator.setFraction((maxsz - analysis_queue.size() - 1)/maxsz);
             }
            progress_indicator.setText2(pf.getName());
          }
         
         BubjetLog.logD("Analyze File " + ut + " " + pf);
         AnalysisRunner arun = new AnalysisRunner(pf,ut);
         ds.runReadActionInSmartMode(arun);
         if (progress_indicator != null && maxsz > 0) {
            if (analysis_queue.isEmpty()) {
               maxsz = 0;
               progress_indicator.stop();
               progress_indicator = null;
             } 
            else {
               progress_indicator.setFraction((maxsz - analysis_queue.size())/maxsz);
             }
          }
       }
    }
}


private class AnalysisRunner implements Runnable {
   
   private PsiFile for_file;
   private boolean do_update;
   
   AnalysisRunner(PsiFile pf,UpdateType ut) {
      for_file = pf;
      do_update = ut == UpdateType.CHECK;
    }
   
   @Override public void run() {
      BubjetLog.logD("ANALYSIS UPDATE FOR FILE " + for_file);
      BubjetErrorPass ep = new BubjetErrorPass(BubjetEditManager.this,for_file,do_update); 
      ep.start();
    }
}








}       // end of class BubjetEditManager




/* end of BubjetEditManager.java */

