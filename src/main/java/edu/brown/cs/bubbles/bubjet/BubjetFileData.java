/********************************************************************************/
/*                                                                              */
/*              BubjetFileData.java                                             */
/*                                                                              */
/*      Bubjet information about an open file                                   */
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
import com.intellij.openapi.editor.impl.ImaginaryEditor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DiffLog;
import com.intellij.psi.text.BlockSupport;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

import com.intellij.openapi.editor.event.DocumentEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;


class BubjetFileData implements BubjetConstants, DocumentListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService app_service;
private Project for_project;
private Module for_module;
private String file_name;
private VirtualFile for_file;
private PsiFile psi_file;
private Document psi_document;
private long last_modified;
private Map<String,PrivateBufferData> buffer_map;
private Map<String,UserData> buffer_users;
private String line_separator;
private String current_bid;
private BubjetDummyEditor dummy_editor;
private int start_edit;
private int end_edit;
private int edit_delta;

private static final int	DEAD_COUNT = 10;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetFileData(BubjetApplicationService app,Project p,VirtualFile vf)
{
   app_service = app;
   for_project = p;
   for_file = vf;
   psi_file = BubjetUtil.getPsiFile(for_project,vf);
   for_module = BubjetUtil.findModuleForFile(vf,for_project);   
   if (for_module == null) {
      for_module = BubjetUtil.findModuleForFile(psi_file);
      BubjetLog.logD("MODULE FOR PSI FILE " + for_module);
    }  
   buffer_map = new HashMap<>();
   buffer_users = new HashMap<>();
   line_separator = null;
   psi_document = FileDocumentManager.getInstance().getDocument(vf);
   psi_document.addDocumentListener(this);
   last_modified = psi_document.getModificationStamp();
   current_bid = null;
   dummy_editor = new BubjetDummyEditor();
   start_edit = end_edit = -1;
   edit_delta = 0;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getFileName()                    { return BubjetUtil.outputFileName(for_file); }
Module getModule()                      { return for_module; }
Project getProject()                    { return for_project; }
BubjetMonitor getMonitor() {
   return app_service.getMonitor(for_project);
}

PsiFile getPsiFile()                    { return psi_file; }
VirtualFile getVirutalFile()            { return for_file; }
Document getDocument()                  { return psi_document; }
Editor getEditor()                      { return dummy_editor; }

PsiFile getPsiFile(String bid)
{
   if (bid == null) return psi_file;
   PrivateBufferData pbd = buffer_map.get(bid);
   if (pbd == null) return psi_file;
   return pbd.getPsiFile();
}


Document getDocument(String bid) 
{
   if (bid == null) return psi_document;
   PrivateBufferData pbd = buffer_map.get(bid);
   if (pbd == null) return psi_document;
   return pbd.getDocument();
}



BubjetElider checkElider(String bid) 
{
   if (isPrivateId(bid)) return null;
   return getUser(bid).getElider(false);
}

BubjetElider getElider(String bid)
{
   if (isPrivateId(bid)) return null;
   return getUser(bid).getElider(true);
}

void clearElider(String bid)
{
   if (isPrivateId(bid)) return;
   getUser(bid).clearElider();
}


String getLineSeparator() {
   if (line_separator == null) checkLineSeparator();
   return line_separator;
}

boolean isPrivateId(String bid)
{
   if (bid == null) return false;
   if (buffer_map.get(bid) != null) return true;
   if (bid.startsWith("PID_")) return true;
   return false;
}

boolean isCurrent(String bid,int id) 
{
   if (isPrivateId(bid)) return true;
   if (getUser(bid) == null) return true;
   return getUser(bid).isCurrent(id);
}

boolean setCurrentId(String bid,int id) 
{
   BubjetLog.logD("SET CURRENT ID " + bid + " " + id);
   if (isPrivateId(bid) || id <= 0) return true;
   return getUser(bid).setCurrentId(id);
}


synchronized boolean hasChanged()
{
   if (psi_document == null) return false;
   BubjetLog.logD("CHECK CHANGED " + psi_document.getModificationStamp() +
         " " + last_modified);
   return psi_document.getModificationStamp() > last_modified;
}


synchronized String getCurrentContents()
{
   return psi_document.getText();
}


synchronized int getLength()
{
   return psi_document.getTextLength();
}

String getContents(String bid) 
{
   return getCurrentContents();
}


/********************************************************************************/
/*                                                                              */
/*      Edit commands                                                           */
/*                                                                              */
/********************************************************************************/

void textEdit(String bid,EditData ed) 
{
   Document d = getDocument(bid);
   boolean chngfg = hasChanged();
   current_bid = bid;
   String rep = ed.getText();
   BubjetLog.logD("TEXT EDIT " + ed.getOffset() + " " + ed.getEndOffset() + " " + rep);
   if (start_edit < 0 || ed.getOffset() < start_edit) start_edit = ed.getOffset();
   if (end_edit < 0 || ed.getEndOffset() + edit_delta > end_edit) end_edit = ed.getEndOffset() + edit_delta;
   if (rep == null) {
      d.deleteString(ed.getOffset(),ed.getEndOffset());
      edit_delta -= ed.getEndOffset() - ed.getOffset();
    }
   else if (ed.getLength() == 0) {
      d.insertString(ed.getOffset(),rep);
    }
   else {
      d.replaceString(ed.getOffset(),ed.getEndOffset(),rep);
      edit_delta -= ed.getEndOffset() - ed.getOffset(); 
      edit_delta += rep.length();
    }
   current_bid = null;
   if (!chngfg && hasChanged() && !isPrivateId(bid)) {
      BubjetMonitor bm = app_service.getMonitor(for_project);
      IvyXmlWriter xw = bm.beginMessage("FILECHANGE");
      xw.field("FILE",BubjetUtil.outputFileName(for_file));
      bm.finishMessage(xw);
    }
   if (!isPrivateId(bid)) getUser(bid).noteEdit(ed);
   else {
      PrivateBufferData pbd = buffer_map.get(bid);
      pbd.update();
    }
}




/********************************************************************************/
/*                                                                              */
/*      Update commands                                                         */
/*                                                                              */
/********************************************************************************/

void updatePsiFile()
{
   BlockSupport bs = BlockSupport.getInstance(for_project);
   PsiFile pf = getPsiFile();
   
   String ns = pf.getNode().getText();
   BubjetLog.logD("CHECK " + pf.getText().length() + " " + psi_document.getText().length() + " " +
         ns.length() + " " + start_edit + " " + end_edit + " " + edit_delta + " " + 
         for_file.getModificationStamp() + " " + 
         pf.getText().equals(psi_document.getText()));
   
   TextRange rng = new TextRange(0,ns.length());
   ProgressIndicatorBase pi = new ProgressIndicatorBase();
   try {
      DiffLog dl = bs.reparseRange(pf,pf.getNode(),rng,psi_document.getText(),pi,ns);
      BubjetLog.logD("REPARSE " + dl);
    }
   catch (Throwable t) {
      BubjetLog.logE("REPARSE FAILED",t);
    }

// bs.reparseRange(pf,0,getLength(),pf.getText());
   
// PsiDocumentManager pdm = PsiDocumentManager.getInstance(for_project);
// pdm.commitDocument(default_buffer);
}



void refresh()
{
   psi_file.getManager().reloadFromDisk(psi_file);
}


void saveFile()
{
   PsiDocumentManager pdm = PsiDocumentManager.getInstance(for_project);
   pdm.commitDocument(psi_document);
}


/********************************************************************************/
/*                                                                              */
/*      User management                                                         */
/*                                                                              */
/********************************************************************************/


void beginUser(String bid)
{
   if (bid != null) getUser(bid);
}


boolean removeUser(String bid)
{
   if (bid == null) return true;
   synchronized (buffer_users) {
      buffer_users.remove(bid);
    }
   BubjetLog.logD("REMOVE USER " + bid + " for " + file_name);
   return buffer_users.isEmpty();
}



private UserData getUser(String bid)
{
   if (bid == null) return null;
   
   synchronized (buffer_users) {
      UserData ud = buffer_users.get(bid);
      if (ud == null) {
         if (buffer_map.get(bid) != null || bid.startsWith("PID_")) {
            BubjetLog.logX("Attempt to get user for private buffer");
            return null;
          }
         ud = new UserData(bid,psi_file,false);
         buffer_users.put(bid,ud);
       }
      return ud;
    }
}




/********************************************************************************/
/*                                                                              */
/*      Private buffer methods                                                  */
/*                                                                              */
/********************************************************************************/

boolean createPrivateBuffer(String sid,String opid) 
{
   PrivateBufferData bd = null;
   
   synchronized (buffer_map) {
      bd = buffer_map.get(sid);
      if (bd != null) return false;
      BubjetLog.logD("Create private buffer " + sid);
      bd = new PrivateBufferData(sid,psi_document);
      buffer_map.put(sid,bd);
    }
   
   PrivateBufferUpdateAction upd = new PrivateBufferUpdateAction(bd);
   upd.start();
   
   return true;
}

void removePrivateBuffer(String bid)
{
   BubjetLog.logD("Remove private buffer " + bid);
   synchronized (buffer_map) {
      PrivateBufferData bd = buffer_map.remove(bid);
      if (bd != null) bd.free();
    }
}



/********************************************************************************/
/*                                                                              */
/*      Document listener methods                                               */
/*                                                                              */
/********************************************************************************/

@Override public void beforeDocumentChange(DocumentEvent evt) { 
   BubjetLog.logD("BubjetFileData beforeDocumentChange " + evt);
}

@Override public void documentChanged(DocumentEvent evt) { 
   BubjetLog.logD("BubjetFileData documentChange " + evt);
   BubjetMonitor bm = app_service.getMonitor(for_project);
   BubjetLog.logD("DOC CHANGE " + evt.getMoveOffset() + " " + evt.getOffset() + " " +
         evt.getOldLength() + " " + evt.getNewLength() + " >" +
            evt.getOldFragment() + "< >" + evt.getNewFragment() + "< " +
            evt.isWholeTextReplaced() + " " + evt.getOldTimeStamp());
   
   List<String> del = null;
   for (UserData ud : buffer_users.values()) {
      if (ud.getBubblesId().equals(current_bid)) continue;
      IvyXmlWriter xw = bm.beginMessage("EDIT",ud.getBubblesId());
      xw.field("FILE",BubjetUtil.outputFileName(for_file));
      xw.field("LENGTH",evt.getOldLength());
      xw.field("OFFSET",evt.getOffset());
      if (evt.isWholeTextReplaced()) {
         xw.field("COMPLETE",true);
         byte [] data = evt.getDocument().getText().getBytes();
         xw.bytesElement("CONTENTS",data);
       }
      else {
         xw.cdata(evt.getNewFragment().toString());
       }
      String rslt = bm.finishMessageWait(xw,5000);
      if (ud.noteAlive(rslt != null)) {
         if (del == null) del = new ArrayList<>();
         del.add(ud.getBubblesId());
       }
    }
}
@Override public void bulkUpdateStarting(Document doc) {  
   BubjetLog.logD("BubjetFileData bulkUpdateStarting " + doc);
}

@Override public void bulkUpdateFinished(Document doc) {
   BubjetLog.logD("BubjetFileData bulkUpdateFinished");
}



/********************************************************************************/
/*                                                                              */
/*      Compute line separator                                                  */
/*                                                                              */
/********************************************************************************/


private void checkLineSeparator() {
   line_separator = FileDocumentManager.getInstance().getLineSeparator(for_file,for_project);
   
   if (line_separator == null) line_separator = System.getProperty("line.separator");
}



/********************************************************************************/
/*                                                                              */
/*      Information about a user                                                */
/*                                                                              */
/********************************************************************************/

private class UserData {

   private String bubbles_id;
   private int current_id;
   private BubjetElider elision_data;
   private int dead_count;
   
   UserData(String bid,PsiFile base,boolean pvt) {
      BubjetLog.logD("Create user for " +
            BubjetFileData.this.getFileName() + " " + bid + " " + pvt);
      bubbles_id = bid;
      current_id = 0;
      dead_count = 0;
      elision_data = null;
    }
   
   String getBubblesId()                { return bubbles_id; }
   
   synchronized boolean setCurrentId(int id) {
      if (current_id > id) return false;
      current_id = id;
      return true;
    }
   synchronized boolean isCurrent(int id) {
      if (id <= 0) return true;
      if (id == current_id) return true;
      return false;
    }
   
   void clearElider()				{ elision_data = null; }
   synchronized BubjetElider getElider(boolean force) {
      if (elision_data == null && force) {
         elision_data = new BubjetElider();
       }
      return elision_data;
    }
   
   void noteEdit(EditData ed) {
      if (elision_data != null) elision_data.noteEdit(ed);
    }

   boolean noteAlive(boolean alive) {
      if (alive) dead_count = 0;
      else if (++dead_count >= DEAD_COUNT) return true;
      return false;
    }
   
}	// end of innerclass UserData




/********************************************************************************/
/*                                                                              */
/*      Information about a private buffer                                      */
/*                                                                              */
/********************************************************************************/

private class PrivateBufferData {
   
   private String buffer_id;
   private Document our_document;
   private PsiFile our_psifile;
   
   PrivateBufferData(String bid,Document d) {
      buffer_id = bid;
      EditorFactory ef = EditorFactory.getInstance();
      our_document = ef.createDocument(d.getCharsSequence());
      our_psifile = null;
    }
   
   synchronized PsiFile getPsiFile() {
      while (psi_file == null) {
          try {
             wait(1000);
           }
          catch (InterruptedException e) { }
       }
      return psi_file;
    }
   
   Document getDocument()                               { return our_document; }
   
   void free() {
      our_document = null;
      our_psifile = null;
    }
   
   void update() {
      BlockSupport bs = BlockSupport.getInstance(for_project);
      PsiDocumentManager pdm = PsiDocumentManager.getInstance(for_project);
      if (our_psifile == null) {
         our_psifile = pdm.getPsiFile(our_document);
       }
      else {
         String ns = our_psifile.getNode().getText();
         TextRange rng = new TextRange(0,ns.length());
         ProgressIndicatorBase pi = new ProgressIndicatorBase();
         try {
            DiffLog dl = bs.reparseRange(our_psifile,our_psifile.getNode(),rng,
                  our_psifile.getText(),pi,ns);
            BubjetLog.logD("REPARSE " + dl);
          }
         catch (Throwable t) {
            BubjetLog.logE("REPARSE FAILED",t);
          }       
       }
      BubjetErrorPass ep = new BubjetErrorPass(BubjetFileData.this,buffer_id,0,null);
      ep.process();
    }
   
}       // end of inner class PrivateBufferData



private class PrivateBufferUpdateAction extends BubjetAction.BackgroundRead {
   
   private PrivateBufferData private_buffer;
   
   PrivateBufferUpdateAction(PrivateBufferData pd) {
      super(for_project,null);
      private_buffer = pd;
    }
   
   @Override void process() {
      private_buffer.update();
    }
}



/********************************************************************************/
/*                                                                              */
/*      DummyEditor                                                             */
/*                                                                              */
/********************************************************************************/

private class BubjetDummyEditor extends ImaginaryEditor {
   
   private static final long serialVersionUID = 1;
  
   BubjetDummyEditor() {
      super(for_project,psi_document);
    }
   
   @Override public boolean isViewer()          { return false; }
   
   
   
   
}       // end of inner class BubjetDummyEditor



}       // end of class BubjetFileData




/* end of BubjetFileData.java */

