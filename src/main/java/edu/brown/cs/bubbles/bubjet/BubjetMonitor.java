/********************************************************************************/
/*                                                                              */
/*              BubjetMonitor.java                                              */
/*                                                                              */
/*      Bubbles-JetBrains interface using MINT                                  */
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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import edu.brown.cs.ivy.exec.IvySetup;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BubjetMonitor implements BubjetConstants, MintConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService app_service;
private MintControl     mint_control;
private MintSetup       mint_setup;
private Object          send_sema;
private boolean         shutdown_mint;
private boolean         doing_exit;
private boolean         platform_ready;
private int             num_clients;
private Project         work_space;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetMonitor(BubjetApplicationService app,Project p)
{
   app_service = app;
   work_space = p;
   
   BubjetLog.logD("Bubjet monitor started");
   
   String hm = System.getProperty("user.home");
   File f1 = new File(hm);
   File f2 = new File(f1,".bubbles");
   File f3 = new File(f2,".ivy");
   File f4 = new File(f1,".ivy");
   
   if (f2.exists()) {
      if (!f3.exists() || !IvySetup.setup(f3)) {
	 if (!f4.exists() || !IvySetup.setup(f4)) {
	    IvySetup.setup();
	  }
       }
    }
   
   mint_setup = null;
   mint_control = null;
   send_sema = new Object();
   shutdown_mint = false;
   doing_exit = false;
   platform_ready = true;
   num_clients = 0;
   
   setupMint();
}



/********************************************************************************/
/*                                                                              */
/*      Control methods                                                         */
/*                                                                              */
/********************************************************************************/

String getMintName()
{
   return mint_control.getMintName();
}



void stopMint()
{
   IvyXmlWriter xw = beginMessage("STOP");
   finishMessage(xw);
   
   BubjetLog.logI("Stop called");
   
   if (!doing_exit) {
      doing_exit = true;
      shutdown_mint = true;
      if (mint_control != null) mint_control.shutDown();
    }
}



void forceExit()
{
   BubjetLog.logD("FORCE EXIT");
   doing_exit = true;
   app_service.exit();
   BubjetLog.logD("Stopping application");
   
   shutdown_mint = true;
}



/********************************************************************************/
/*                                                                              */
/*      Message sending                                                         */
/*                                                                              */
/********************************************************************************/

private void sendMessage(String msg)
{
   String s = msg;
// if (s.length() > 50) s = s.substring(0,50);
   BubjetLog.logD("Send message no wait: " + s);
   
   synchronized (send_sema) {
      if (mint_control != null && !doing_exit)
	 mint_control.send(msg);
    }
}


private String sendMessageWait(String msg,long delay)
{
   MintDefaultReply rply = new MintDefaultReply();
   
   synchronized (send_sema) {
      if (mint_control != null && !doing_exit) {
	 mint_control.send(msg,rply,MINT_MSG_FIRST_NON_NULL);
       }
      else return null;
    }
   
   String s = msg;
// if (s.length() > 50) s = s.substring(0,50);
   BubjetLog.logD("Send message: " + s);
   
   return rply.waitForString(delay);
}



IvyXmlWriter beginMessage(String typ)
{
   return beginMessage(typ,null);
}


IvyXmlWriter beginMessage(String typ,String bid)
{
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("BUBJET");
   xw.field("SOURCE","IDEA");
   xw.field("TYPE",typ);
   if (bid != null) xw.field("BID",bid);
   
   return xw;
}


void finishMessage(IvyXmlWriter xw)
{
   xw.end("BUBJET");
   
   sendMessage(xw.toString());
}



String finishMessageWait(IvyXmlWriter xw)
{
   return finishMessageWait(xw,0);
}


String finishMessageWait(IvyXmlWriter xw,long delay)
{
   if (xw == null) return null;
   
   xw.end("BUBJET");
   
   return sendMessageWait(xw.toString(),delay);
}




/********************************************************************************/
/*                                                                              */
/*      Mint setup                                                              */
/*                                                                              */
/********************************************************************************/

private void setupMint()
{
   BubjetLog.logD("Setup mint called");
   
   synchronized (this) {
      if (mint_control != null) return;
      if (mint_setup != null) return;
      mint_setup = new MintSetup();
    }
   
   BubjetBundle.runInBackground(mint_setup);
//    mint_setup.start();
}


MintControl waitForMint()
{
   if (mint_control != null) return mint_control;
   synchronized (this) {
      while (mint_control == null) {
         try {
            wait(1000);
          }
         catch (InterruptedException e) { }
       }
    }
   
   return mint_control;
}



private class MintSetup extends Thread {
   
   MintSetup() {
      super("MINT setup thread");
    }
   
   @Override public void run() {
      BubjetLog.logD("MINT SETUP started");
      
      IvySetup.setup();
      
      if (mint_control != null) return;
      
      String mintname = System.getProperty("edu.brown.cs.bubbles.MINT");
      if (mintname == null) mintname = System.getProperty("edu.brown.cs.bubbles.mint");
      if (mintname == null) {
         String wsname = work_space.getBasePath();
         if (wsname.endsWith(File.separator)) wsname = wsname.substring(0,wsname.length()-1);
         int idx = wsname.lastIndexOf(File.separator);
         if (idx > 0) wsname = wsname.substring(idx+1);
         if (wsname == null) wsname = "";
         else wsname = wsname.replace(" ","_");
         mintname = BUBJET_MINT_ID;
         mintname = mintname.replace("@@@",wsname);
       }
      if (mintname == null) mintname = BUBJET_MESSAGE_ID;
      
      MintControl mc = MintControl.create(mintname,MintSyncMode.SINGLE);
      mc.register("<BUBBLES DO='_VAR_1' LANG='Idea' />",new CommandHandler());
      synchronized (BubjetMonitor.this) {
         mint_control = mc;
         BubjetMonitor.this.notifyAll();
       }
      
      BubjetLog.logD("Mint setup as " + mintname);
    }

}       // end of inner class MintSetup




/********************************************************************************/
/*                                                                              */
/*      Command handling                                                        */
/*                                                                              */
/********************************************************************************/

private String handleCommand(String cmd,String proj,Element xml) throws BubjetException
{
   BubjetLog.logI("Handle command " + cmd + " for " + proj);
   BubjetLog.logD("COMPLETE COMMAND: " + IvyXml.convertXmlToString(xml));
   
   long start = System.currentTimeMillis();
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("RESULT");
   
   if (shutdown_mint && !cmd.equals("PING")) {
      xw.close();
      if (cmd.equals("SAVEWORKSPACE")) return null;
      throw new BubjetException("Command during exit");
    }
   
   String ws = IvyXml.getAttrString(xml,"WS");
   
   switch (cmd) {
      case "PING" :
	 if (doing_exit || shutdown_mint) xw.text("EXIT");
         else if (!platform_ready) xw.text("UNSET");
         else xw.text("PONG");
	 break;
      case "ENTER" :
//       BedrockApplication.enterApplication();
	 ++num_clients;
	 xw.text(Integer.toString(num_clients));
	 break;
      case "EXIT" :
	 BubjetLog.logD("EXIT Request received " + num_clients + " " + doing_exit);
	 if (--num_clients <= 0) {
	    xw.text("EXITING");
	    forceExit();
	  }
	 break;
      case "PROJECTS" :
      case "OPENPROJECT" :
      case "EDITPROJECT" :
      case "CREATEPROJECT" :
      case "IMPORTPROJECT" :
      case "BUILDPROJECT" :
      case "CREATEPACKAGE" :
      case "FINDPACKAGE" :
      case "PREFERENCES" :
      case "SETPREFERENCES" :
      case "GETALLNAMES" :
         handleProjectCommand(cmd,ws,proj,xml,xw);
         break;
      case "FINDDEFINITIONS" :
      case "FINDREFERENCES" :
      case "PATTERNSEARCH" :
      case "SEARCH" :
      case "GETFULLYQUALIFIEDNAME" :
      case "FINDHIERARCHY" :
      case "FINDBYKEY" :
      case "FINDREGIONS" :
         handleSearchCommands(cmd,ws,proj,xml,xw);
         break;
      case "GETRUNCONFIG" :
      case "NEWRUNCONFIG" :
      case "EDTIRUNCONFIG" :
      case "SAVERUNCONFIG" :
      case "DELETERUNCONFIG" :
         handleLaunchCommands(cmd,ws,proj,xml,xw);
         break;
      case "GETALLBREAKPOINTS" :
      case "ADDLINEBREAKPOINT" :
      case "ADDEXCEPTIONBREAKPOINT" :
      case "EDITBREAKPOINT" :
      case "CLEARALLLINEBREAKPOINTS" :
      case "CLEARLINEBREAKPOINT" :
         handleBreakCommands(cmd,ws,proj,xml,xw);
         break;
      case "EDITPARAM" :
      case "STARTFILE" :
      case "FINISHFILE" :
      case "EDITFILE" :
      case "COMMIT" :
      case "ELIDESET" :
      case "FILEELIDE" :
      case "GETCOMPLETIONS" :
         handleEditCommands(cmd,ws,proj,xml,xw);
         break;
      case "START" :
      case "DEBUGACTION" :
      case "CONSOLEINPUT" :
      case "GETSTACKFRAMES" :
      case "VARVAL" :
      case "VARDETAIL" :
      case "EVALUATE" :
         // handle exec commands
         break;
      case "CREATEPRIVATE" :
      case "PRIVATEEDIT" :
      case "REMOVEPRIVATE" :
         // handle private edit commands
         break;
      case "QUICKFIX" :
         // handle suggestions
         break;
      case "MOVEELEMENT" :
      case "RENAMERESOURCE" :
      case "EXTRACTMETHOD" :
      case "FORMATCODE" :
      case "FIXIMPORTS" :
      case "REFACTOR" :
      case "DELETE" :
         // handle refactorings
         break;
      case "CALLPATH" :
      case "CREATECLASS" :
         // handle bubbles search commands
         break;
      case "LOGLEVEL" :
      case "GETHOST" :
      case "GETPROXY" :
      case "SAVEWORKSPACE" :
         handleUtilityCommands(cmd,ws,proj,xml,xw);
         break;
      default :
	 xw.close();
	 throw new BubjetException("Unknown plugin command " + cmd);   
    }
   
   xw.end("RESULT");
   
   long delta = System.currentTimeMillis() - start;
   
   String rslt = xw.toString();
   xw.close();
   
// if (rslt.length() > 102300)
//    BubjetLog.logD("Result (" + delta + ") = " + rslt.substring(0,1023) + " ...");
// else
      BubjetLog.logD("Result (" + delta + ") = " + rslt);
   
   return rslt;
}



private void handleProjectCommand(String cmd,String ws,String proj,Element xml,IvyXmlWriter xw)
{
   BubjetProjectManager pm = app_service.getProjectManager();
   
   switch (cmd) {
      case "PROJECTS" :
         pm.listProjects(ws,xw);
         break;
      case "OPENPROJECT" :
         pm.openProject(ws,proj,IvyXml.getAttrBool(xml,"FILES",false),
	       IvyXml.getAttrBool(xml,"PATHS",false),
	       IvyXml.getAttrBool(xml,"CLASSES",false),
	       IvyXml.getAttrBool(xml,"OPTIONS",false),
	       IvyXml.getAttrBool(xml,"IMPORTS",false),
	       IvyXml.getAttrString(xml,"BACKGROUND"),xw);
         break;
      case "PREFERENCES" :
         pm.outputPreferences(ws,proj,xw);
         break;
      case "BUILDPROJECT" :
         pm.buildProject(ws,proj,
               IvyXml.getAttrBool(xml,"CLEAN"),
               IvyXml.getAttrBool(xml,"FULL"),
               IvyXml.getAttrBool(xml,"REFRESH"),xw); 
         break;
      case "FINDPACKAGE" :
         pm.findPackage(ws,proj,IvyXml.getAttrString(xml,"NAME"),xw);
         break;
      case "CREATEPACKAGE" :
         pm.createPackage(ws,proj,IvyXml.getAttrString(xml,"NAME"),
               IvyXml.getAttrBool(xml,"FORCE",false),xw);
         break;
      case "SETPREFERENCES" :
      case "EDITPROJECT" :
      case "CREATEPROJECT" :
      case "IMPORTPROJECT" :
         break;
      case "GETALLNAMES" :
         pm.getAllNames(ws,proj,IvyXml.getAttrString(xml,"BID","*"),
               getSet(xml,"FILE"),
               IvyXml.getAttrString(xml,"BACKGROUND"),xw);
         break;
    }
}



private void handleSearchCommands(String cmd,String ws,String proj,Element xml,IvyXmlWriter xw)
{
   BubjetProjectManager pm = app_service.getProjectManager();
   BubjetSearchManager sm = app_service.getSearchManager();
   
   Project p = pm.findProject(ws);
   Module m = pm.findModule(p,proj);
   
   switch (cmd) {
      case "FINDDEFINITIONS" :
         sm.handleFindAll(p,m,IvyXml.getAttrString(xml,"FILE"),
               IvyXml.getAttrInt(xml,"START"),IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",false),
	       IvyXml.getAttrBool(xml,"IMPLS",false),
	       IvyXml.getAttrBool(xml,"EQUIV",false),
	       IvyXml.getAttrBool(xml,"EXACT",false),
	       IvyXml.getAttrBool(xml,"SYSTEM",false),
	       IvyXml.getAttrBool(xml,"TYPE",false),
	       false,false,
	       xw);
         break;
      case "FINDREFERENCES" :
         sm.handleFindAll(p,m,IvyXml.getAttrString(xml,"FILE"),
               IvyXml.getAttrInt(xml,"START"),
	       IvyXml.getAttrInt(xml,"END"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",true),
	       IvyXml.getAttrBool(xml,"IMPLS",false),
	       IvyXml.getAttrBool(xml,"EQUIV",false),
	       IvyXml.getAttrBool(xml,"EXACT",false),
	       IvyXml.getAttrBool(xml,"SYSTEM",false),
	       IvyXml.getAttrBool(xml,"TYPE",false),
	       IvyXml.getAttrBool(xml,"RONLY",false),
	       IvyXml.getAttrBool(xml,"WONLY",false),
	       xw);
         break;
      case "PATTERNSEARCH" :
         sm.handleJavaSearch(p,m,IvyXml.getAttrString(xml,"BID","*"),
               IvyXml.getAttrString(xml,"PATTERN"),
	       IvyXml.getAttrString(xml,"FOR"),
	       IvyXml.getAttrBool(xml,"DEFS",true),
	       IvyXml.getAttrBool(xml,"REFS",true),
	       IvyXml.getAttrBool(xml,"IMPLS",false),
	       IvyXml.getAttrBool(xml,"EQUIV",false),
	       IvyXml.getAttrBool(xml,"EXACT",false),
	       IvyXml.getAttrBool(xml,"SYSTEM",false),
	       xw);
         break;
      case "SEARCH" :
         sm.handleTextSearch(p,m,IvyXml.getAttrInt(xml,"FLAGS",0),
	       IvyXml.getTextElement(xml,"PATTERN"),
	       IvyXml.getAttrInt(xml,"MAX",MAX_TEXT_SEARCH_RESULTS),
	       xw);
         break;
      case "GETFULLYQUALIFIEDNAME" :
         sm.handleGetFullyQualifiedName(p,m,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"START"),
	       IvyXml.getAttrInt(xml,"END"),xw);
	 break;
      case "FINDHIERARCHY" :
         sm.handleFindHierarchy(p,m,IvyXml.getAttrString(xml,"PACKAGE"),
	       IvyXml.getAttrString(xml,"CLASS"), xw);
	 break;
      case "FINDBYKEY" :
         sm.handleFindByKey(p,m,
	       IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"KEY"),
	       IvyXml.getAttrString(xml,"FILE"), xw);
         break;
      case "FINDREGIONS" :
         sm.handleFindRegions(p,m,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrBool(xml,"PREFIX",false),
	       IvyXml.getAttrBool(xml,"STATICS",false),
	       IvyXml.getAttrBool(xml,"COMPUNIT",false),
	       IvyXml.getAttrBool(xml,"IMPORTS",false),
	       IvyXml.getAttrBool(xml,"PACKAGE",false),
	       IvyXml.getAttrBool(xml,"TOPDECLS",false),
	       IvyXml.getAttrBool(xml,"FIELDS",false),
	       IvyXml.getAttrBool(xml,"ALL",false),xw);
         break;
    }
}



private void handleLaunchCommands(String cmd,String ws,String proj,Element xml,IvyXmlWriter xw)
{
   BubjetProjectManager pm = app_service.getProjectManager();
   
   Project p = pm.findProject(ws);
   Module m = pm.findModule(p,proj);
   
   BubjetLaunchManager lm = app_service.getLaunchManager(p);
   
   switch (cmd) {
      case "GETRUNCONFIG" :
         lm.getRunConfigurations(p,m,xw);
         break;
      case "NEWRUNCONFIG" :
         lm.getNewRunConfiguration(p,m,
               IvyXml.getAttrString(xml,"NAME"),
               IvyXml.getAttrString(xml,"CLONE"),
	       IvyXml.getAttrString(xml,"TYPE","Java Application"),xw);
         break;
      case "EDTIRUNCONFIG" :
         lm.editRunConfiguration(p,m,IvyXml.getAttrString(xml,"LAUNCH"),
	       IvyXml.getAttrString(xml,"PROP"),
	       IvyXml.getAttrString(xml,"VALUE"),xw);
         break;
      case "SAVERUNCONFIG" :
         lm.saveRunConfiguration(p,m,IvyXml.getAttrString(xml,"LAUNCH"),xw);
         break;
      case "DELETERUNCONFIG" :
         lm.deleteRunConfiguration(p,m,IvyXml.getAttrString(xml,"LAUNCH"),xw);
         break;
    }
}



private void handleBreakCommands(String cmd,String ws,String proj,Element xml,IvyXmlWriter xw)
        throws BubjetException
{
   BubjetProjectManager pm = app_service.getProjectManager();
   
   Project p = pm.findProject(ws);
   Module m = pm.findModule(p,proj);
   
   if (p == null) throw new BubjetException("Unknown working set " + ws);
   
   BubjetBreakpointManager bm = app_service.getBreakpointManager(p);
   
   switch (cmd) {
      case "GETALLBREAKPOINTS" :
         bm.handleGetAllBreakpoints(p,m,xw);
         break;
      case "ADDLINEBREAKPOINT" :
         bm.handleAddLineBreakpoint(p,m,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getTextElement(xml,"FILE"),
	       IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrInt(xml,"LINE"),
	       IvyXml.getAttrBool(xml,"SUSPENDVM",false),
	       IvyXml.getAttrBool(xml,"TRACE",false));
         break;
      case "ADDEXCEPTIONBREAKPOINT" :
         bm.handleAddExceptionBreakpoint(p,m,IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrBool(xml,"CAUGHT",false),
	       IvyXml.getAttrBool(xml,"UNCAUGHT",true),
	       IvyXml.getAttrBool(xml,"CHECKED",false),
	       IvyXml.getAttrBool(xml,"SUSPENDVM",false),
	       IvyXml.getAttrBool(xml,"SUBCLASS",true));
         break;
      case "EDITBREAKPOINT" :
         bm.handleEditBreakpoint(p,m,IvyXml.getAttrInt(xml,"ID"),
	       IvyXml.getAttrString(xml,"PROP"),
	       IvyXml.getAttrString(xml,"VALUE"),
	       IvyXml.getAttrString(xml,"PROP1"),
	       IvyXml.getAttrString(xml,"VALUE1"),
	       IvyXml.getAttrString(xml,"PROP2"),
	       IvyXml.getAttrString(xml,"VALUE2"));
         break;
      case "CLEARALLLINEBREAKPOINTS" :
         bm.handleClearLineBreakpoint(p,m,null,null,0);
         break;
      case "CLEARLINEBREAKPOINT" :
         bm.handleClearLineBreakpoint(p,m,IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrString(xml,"CLASS"),
	       IvyXml.getAttrInt(xml,"LINE"));
         break;
    }
}




private void handleEditCommands(String cmd,String ws,String proj,Element xml,IvyXmlWriter xw)
        throws BubjetException
{
   BubjetProjectManager pm = app_service.getProjectManager();
   Project p = pm.findProject(ws);
   if (p == null) {
      BubjetLog.logE("Can't find project " + ws);
    }
   Module m = pm.findModule(p,proj);
   
   BubjetEditManager em = app_service.getEditManager(p);
   
   switch (cmd) {
      case "EDITPARAM" :
         em.handleEditParameter(p,m,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"NAME"),
	       IvyXml.getAttrString(xml,"VALUE"));
         break;
      case "STARTFILE" :
         em.handleStartFile(p,m,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"ID"),
	       IvyXml.getAttrBool(xml,"CONTENTS",false),xw);
         break;
      case "FINISHFILE" :
         em.handleFinishFile(p,m,IvyXml.getAttrString(xml,"BID"),
	       IvyXml.getAttrString(xml,"FILE"));
         break;
      case "EDITFILE" :
         em.handleEditFile(p,m,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"ID"),
	       getEditSet(xml),xw);
         break;
      case "COMMIT" :
         em.handleCommit(p,m,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrBool(xml,"REFRESH",false),
	       IvyXml.getAttrBool(xml,"SAVE",false),
	       IvyXml.getAttrBool(xml,"COMPILE",false),
	       getElements(xml,"FILE"),xw);
         break;
      case "ELIDESET" :
         em.handleElideSet(p,m,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrBool(xml,"COMPUTE",true),
	       getElements(xml,"REGION"),xw);
         break;
      case "FILEELIDE" :
         em.handleFileElide(p,m,IvyXml.getBytesElement(xml,"FILE"),xw);
         break;
      case "GETCOMPLETIONS" :
         em.handleGetCompletions(p,m,IvyXml.getAttrString(xml,"BID","*"),
	       IvyXml.getAttrString(xml,"FILE"),
	       IvyXml.getAttrInt(xml,"OFFSET"),xw);
         break;
    }
} 


/********************************************************************************/
/*                                                                              */
/*      Handle Utility Commands                                                 */
/*                                                                              */
/********************************************************************************/

private void handleUtilityCommands(String cmd,String ws,String proj,Element xml,IvyXmlWriter xw)
{
   BubjetProjectManager pm = app_service.getProjectManager();
   Project p = pm.findProject(ws);
// Module m = pm.findModule(p,proj);
   
   switch (cmd) {
      case "LOGLEVEL" :
         BubjetLog.LogLevel lvl = IvyXml.getAttrEnum(xml,"LEVEL",BubjetLog.LogLevel.ERROR);
         BubjetLog.setLogLevel(lvl);
         break;
      case "GETHOST" :
         String h1 = null;
         String h2 = null;
         String h3 = null;
	 try {
	    InetAddress lh = InetAddress.getLocalHost();
	    h1 = lh.getHostAddress();
	    h2 = lh.getHostName();
	    h3 = lh.getCanonicalHostName();
	  }
	 catch (IOException e) { }
	 if (h1 != null) xw.field("ADDR",h1);
	 if (h2 != null) xw.field("NAME",h2);
	 if (h3 != null) xw.field("CNAME",h3);
	 break;
      case "GETPROXY" :
         break;
      case "SAVEWORKSPACE" :
         p.save();
         break;
    }
}





/********************************************************************************/
/*                                                                              */
/*      Command decoding                                                        */
/*                                                                              */
/********************************************************************************/

private Set<String> getSet(Element xml,String key)
{
   Set<String> items = null;
   
   for (Element c : IvyXml.children(xml,key)) {
      String v = IvyXml.getText(c);
      if (v == null || v.length() == 0) continue;
      if (items == null) items = new HashSet<String>();
      items.add(v);
    }
   
   return items;
}


private List<EditData> getEditSet(Element xml)
{
   List<EditData> edits = new ArrayList<EditData>();
   
   for (Element c : IvyXml.children(xml,"EDIT")) {
      EditDataImpl edi = new EditDataImpl(c);
      edits.add(edi);
    }
   
   return edits;
}


private List<Element> getElements(Element xml,String key)
{
   List<Element> elts = null;
   
   for (Element c : IvyXml.children(xml,key)) {
      if (elts == null) elts = new ArrayList<>();
      elts.add(c);
    }
   
   return elts;
}



/********************************************************************************/
/*                                                                              */
/*      Commmand handler receiever                                              */
/*                                                                              */
/********************************************************************************/


private class CommandHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg, MintArguments args) {
      String cmd = args.getArgument(1);
      Element xml = msg.getXml();
      String proj = IvyXml.getAttrString(xml,"PROJECT");
      
      String rslt = null;
      
      try {
         rslt = handleCommand(cmd,proj,xml);
       }
      catch (BubjetException e) {
         String xmsg = "BUBJET: error in command " + cmd + ": " + e;
         BubjetLog.logE(xmsg,e); 
         rslt = "<ERROR><![CDATA[" + xmsg + "]]></ERROR>";
       }
      catch (Throwable t) {
         String xmsg = "BUBJET: Problem processing command " + cmd + ": " + t + " " +
            doing_exit + " " + shutdown_mint + " " +  num_clients;
         BubjetLog.logE(xmsg);
         System.err.println(xmsg);
         t.printStackTrace();
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         t.printStackTrace(pw);
         Throwable xt = t;
         for (	; xt.getCause() != null; xt = xt.getCause());
         if (xt != null && xt != t) {
            rslt += "\n";
            xt.printStackTrace(pw);
          }
         BubjetLog.logE("TRACE: " + sw.toString());
         rslt = "<ERROR>";
         rslt += "<MESSAGE>" + xmsg + "</MESSAGE>";
         rslt += "<EXCEPTION><![CDATA[" + t.toString() + "]]></EXCEPTION>";
         rslt += "<STACK><![CDATA[" + sw.toString() + "]]></STACK>";
         rslt += "</ERROR>";
       }
      
      msg.replyTo(rslt);
      
      if (shutdown_mint) mint_control.shutDown();
    }

}	// end of subclass CommandHandler


private static class EditDataImpl implements EditData {

   private int start_offset;
   private int end_offset;
   private String edit_text;
   
   EditDataImpl(Element e) {
      start_offset = IvyXml.getAttrInt(e,"START");
      end_offset = IvyXml.getAttrInt(e,"END",start_offset);
      edit_text = IvyXml.getText(e);
      if (edit_text != null && edit_text.length() == 0) edit_text = null;
      if (edit_text != null && IvyXml.getAttrBool(e,"ENCODE")) {
         byte [] bytes = IvyXml.stringToByteArray(edit_text);
         edit_text = new String(bytes);
       }
    }
   
   @Override public int getOffset()			{ return start_offset; }
   @Override public int getLength()			{ return end_offset - start_offset; }
   @Override public String getText()			{ return edit_text; }

}	// end of innerclass EditDataImpl

}       // end of class BubjetMonitor




/* end of BubjetMonitor.java */

