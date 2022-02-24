/********************************************************************************/
/*                                                                              */
/*              BubjetTest.java                                                 */
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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.xml.IvyXml;

public class BubjetTest implements BubjetConstants, MintConstants
{


/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   BubjetTest bt = new BubjetTest(args);
   
   bt.test00();
}




/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private MintControl     mint_control;
private String          work_space;
private boolean         names_done;

private static final String BUBJET_ID = "BUBJET_test_000001";




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private BubjetTest(String [] args)
{
   work_space = "tutorial";
   if (args.length > 0) work_space = args[0];
   
   names_done = false;
   String mid = BUBJET_MINT_ID;
   mid = mid.replace("@@@",work_space);   
   mint_control = MintControl.create(mid,MintSyncMode.ONLY_REPLIES);
   mint_control.register("<BUBJET SOURCE='IDEA' TYPE='_VAR_0' />",new MessageHandler());
   
   System.err.println("BUBJETTEST: STARTING " + mid);
}





/********************************************************************************/
/*										*/
/*	Test cases								*/
/*										*/
/********************************************************************************/

private void test00()
{
   List<String> projects = new ArrayList<>();
   sendCommand("PING",null,null,null);
   
   sendCommand("ENTER",null,null,null);
   sendCommand("LOGLEVEL",null,"LEVEL='DEBUG",null);
   sendCommand("GETHOST",null,null,null);
   sendCommand("EDITPARAM",null,"NAME='AUTOELIDE' VALUE='TRUE'",null);
   sendCommand("EDITPARAM",null,"NAME='ELIDEDELAY' VALUE='250'",null);
   
   ReplyHandler rh = sendCommand("PROJECTS",null,null,null);
   Element rslt = rh.waitForXml();
   if (rslt != null) {
      for (Element pelt : IvyXml.children(rslt,"PROJECT")) {
         projects.add(IvyXml.getAttrString(pelt,"NAME"));
       }
    }
   int ctr = 0;
   for (String proj : projects) {
      sendCommand("OPENPROJECT",proj,"FILES='t' PATHS='t' CLASSES='t' OPTIONS='t' IMPORTS='t'",null);
      sendCommand("PREFERENCES",proj,null,null);
      sendCommand("BUILDPROJECT",proj,"CLEAN='1' FULL='1' REFRESH='1'",null);
      sendCommand("FINDPACKAGE",proj,"NAME='edu.brown.cs.ivy.jcomp'",null);
      sendCommand("FINDPACKAGE",proj,"NAME='java.lang'",null);
      String nid = "NAME_" + (++ctr);
      names_done = false;
      sendCommand("GETALLNAMES",proj,"BACKGROUND='" + nid + "'",null);
      waitForNames();
      if (proj.equals("ivy")) {
         sendCommand("FINDREFERENCES",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExecQuery.java' " +
               "START='5041' END='5041' RONLY='T' EXACT='T' EQUIV='T'",null);
         sendCommand("FINDDEFINITIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExecQuery.java' " +
               "START='5041' END='5041'",null);
         sendCommand("FINDREFERENCES",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExecQuery.java' " +
               "START='5600' END='5600' RONLY='T' EXACT='T' EQUIV='T'",null);
         sendCommand("FINDREFERENCES",proj,
               "FILE='/Volumes[/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExecQuery.java' " +
               "START='5600' END='5600' WONLY='T' EXACT='T' EQUIV='T'",null);
         sendCommand("FINDDEFINITIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "START='16702' END='16702'",null);
         sendCommand("FINDDEFINITIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/jcomp/JcompSymbol.java' " +
               "START='8197' END='8197' IMPLS='T'",null);
         sendCommand("GETFULLYQUALIFIEDNAME",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "START='16702' END='16702'",null);
         sendCommand("PATTERNSEARCH",proj,
               "PATTERN='edu.brown.cs.ivy.leash.LeashIndex' " + 
               "FOR='CONSTRUCTOR' DEFS='T' REFS='F'",null);
         sendCommand("PATTERNSEARCH",proj,
               "PATTERN='edu.brown.cs.ivy.leash.LeashIndex.queryStatements' " + 
               "FOR='METHOD' DEFS='T' REFS='F'",null);    
         sendCommand("PATTERNSEARCH",proj,
               "PATTERN='edu.brown.cs.ivy.leash.LeashIndex.queryStatements(String,int,int)' " + 
                  "FOR='METHOD' DEFS='T' REFS='F'",null);    
         sendCommand("PATTERNSEARCH",proj,
               "PATTERN='edu.brown.cs.ivy.file.IvyLog' " + 
               "FOR='TYPE' DEFS='T' REFS='F'",null);    
         sendCommand("PATTERNSEARCH",proj,
               "PATTERN='edu.brown.cs.ivy.leash.LeashIndex' " + 
               "FOR='CLASS' DEFS='T' REFS='F'",null);   
         sendCommand("PATTERNSEARCH",proj,
               "PATTERN='edu.brown.cs.ivy.petal.PetalArcHelper.*" + 
                  "FOR='FIELD' DEFS='T' REFS='F'",null);    
         sendCommand("PATTERNSEARCH",proj,
               "PATTERN='Leash*' " + 
               "FOR='TYPE' DEFS='T' REFS='F'",null);    
         sendCommand("FINDHIERARCHY",proj,"PACKAGE='javax.management.remote",null);
         sendCommand("FINDHIERARCHY",proj,"CLASS='edu.brown.cs.ivy.jcomp.JcompType'",null);
         sendCommand("FINDHIERARCHY",proj,null,null);
         sendCommand("SEARCH",proj,"FLAGS='16' PATTERN='Attempt to use'",null);
         sendCommand("FINDBYKEY",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "KEY='ivy!M!edu.brown.cs.ivy.exec.IvyExec!getOutputStream!()Ljava/io/OutputStream;'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='T' PREFIX='F' STATICS='F' IMPORTS='F' PACKAGE='F' TOPDECLS='F' " +
               "FIELDS='F' ALL='F'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='F' PREFIX='T' STATICS='F' IMPORTS='F' PACKAGE='F' TOPDECLS='F' " +
               "FIELDS='F' ALL='F'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='F' PREFIX='F' STATICS='T' IMPORTS='F' PACKAGE='F' TOPDECLS='F' " +
               "FIELDS='F' ALL='F'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='F' PREFIX='F' STATICS='F' IMPORTS='T' PACKAGE='F' TOPDECLS='F' " +
               "FIELDS='F' ALL='F'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='F' PREFIX='F' STATICS='F' IMPORTS='F' PACKAGE='T' TOPDECLS='F' " +
               "FIELDS='F' ALL='F'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='F' PREFIX='F' STATICS='F' IMPORTS='F' PACKAGE='F' TOPDECLS='T' " +
               "FIELDS='F' ALL='F'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='F' PREFIX='F' STATICS='F' IMPORTS='F' PACKAGE='F' TOPDECLS='T' " +
               "FIELDS='F' ALL='T'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='F' PREFIX='F' STATICS='F' IMPORTS='F' PACKAGE='F' TOPDECLS='F' " +
               "FIELDS='T' ALL='F'",
               null);
         sendCommand("FINDREGIONS",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/exec/IvyExec.java' " +
               "CLASS='edu.brown.cs.ivy.exec.IvyExec' " +
               "COMPUNTI='F' PREFIX='F' STATICS='F' IMPORTS='F' PACKAGE='F' TOPDECLS='F' " +
               "FIELDS='F' ALL='T'",
               null);
         sendCommand("ADDLINEBREAKPOINT",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/cinder/CinderInstrumenter.java' " +
               "LINE='425",
               null);
         sendCommand("CLEARLINEBREAKPOINT",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/cinder/CinderInstrumenter.java' " +
               "LINE='425",
               null);
         sendCommand("STARTFILE",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/cinder/CinderTaj.java' ID='17'",
               null);
         delay(10);
         sendCommand("ELIDESET",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/cinder/CinderTaj.java' " +
               "COMPUTE='true'",
               "<REGION START='2952' END='11999' />");
         sendCommand("EDITFILE",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/cinder/CinderTaj.java' ID='19' " +
               "NEWLINE='true'",
               "<EDIT START='6971' END='6971'><![CDATA[\n]]></EDIT>");
//       delay(10);
         sendCommand("EDITFILE",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/cinder/CinderTaj.java' ID='20' " +
               "NEWLINE='true'",
               "<EDIT START='6972' END='6972'><![CDATA[   double x = y]]></EDIT>");
         delay(10);
         sendCommand("EDITFILE",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/cinder/CinderTaj.java' ID='20' " +
               "NEWLINE='true'",
               "<EDIT START='6988' END='6988'><![CDATA[;]]></EDIT>");
         delay(1);
         sendCommand("EDITFILE",proj,
               "FILE='/Volumes/Geode-2/pro/ivy/javasrc/edu/brown/cs/ivy/cinder/CinderTaj.java' ID='20' " +
               "NEWLINE='true'",
               "<EDIT START='6972' END='6989' />"); 
         delay(2);
         sendCommand("BUILDPROJECT",proj,"CLEAN='0' FULL='0' REFRESH='0'",null);
       }
      else {
         sendCommand("SEARCH",proj,"FLAGS='16' PATTERN='IvyXml'",null);
       }
      sendCommand("GETRUNCONFIG",proj,null,null);
      sendCommand("GETALLBREAKPOINTS",proj,null,null);
      sendCommand("SAVEWORKSPACE",null,null,null);
    }
}



private void delay(int sec)
{
   try {
      Thread.sleep(sec*1000);
    }
   catch (InterruptedException e) { }
}


/********************************************************************************/
/*										*/
/*	Mint handling routines							*/
/*										*/
/********************************************************************************/

private ReplyHandler sendCommand(String cmd,String proj,String flds,String args)
{
   ReplyHandler rh = new ReplyHandler(cmd);
   
   if (work_space != null) {
      String wsq = "WS='" + work_space + "'";
      if (flds == null) flds = wsq;
      else flds += " " + wsq;
    }
   String msg = "<BUBBLES DO='" + cmd + "'";
   msg += " BID='" + BUBJET_ID + "'";
   if (proj != null) msg += " PROJECT='" + proj + "'";
   if (flds != null) msg += " " + flds;
   msg += " LANG='Intellij'";
   msg +=  ">";
   if (args != null) msg += args;
   msg += "</BUBBLES>";
   
   System.err.println("BUBJETTEST: BEGIN COMMAND " + cmd);
   System.err.println("BUBJETTEST: SENDING: " + msg);
   
   mint_control.send(msg,rh,MINT_MSG_FIRST_NON_NULL);
   
   rh.print();
   
   return rh;
}




/********************************************************************************/
/*										*/
/*	Routines for handling run events					*/
/*										*/
/********************************************************************************/

private static int action_count = 0;


private void processRunEvent(Element re)
{
   String kind = IvyXml.getAttrString(re,"KIND");
   Element thr = IvyXml.getChild(re,"THREAD");
   Element tgt = IvyXml.getChild(re,"TARGET");
   
   if (kind.equals("CREATE")) {
      if (thr != null) {
	 // handle new thread
       }
      else if (tgt != null) {
	 // handle new target
       }
    }
   else if (kind.equals("RESUME")) {
    }
   else if (kind.equals("CHANGE")) {
    }
   else if (kind.equals("SUSPEND")) {
      if (thr != null) {
	 String tid = IvyXml.getAttrString(thr,"ID");
	 String ttag = IvyXml.getAttrString(thr,"TAG");
	 String fid = null;
	 ReplyHandler rh = sendCommand("GETSTACKFRAMES",null,"THREAD='" + tid + "'",null);
	 Element sfinfo = rh.waitForXml();
	 Element sfs = IvyXml.getChild(sfinfo,"STACKFRAMES");
	 for (Element sft : IvyXml.children(sfs,"THREAD")) {
	    if (!tid.equals(IvyXml.getAttrString(sft,"ID"))) continue;
	    for (Element sff : IvyXml.children(sft,"STACKFRAME")) {
	       fid = IvyXml.getAttrString(sff,"ID");
	       break;
	     }
	  }
         
	 switch (action_count) {
	    case 0 :
	       sendCommand("VARVAL",null,"THREAD='" + tid + "' FRAME='" + fid + "' VAR='var'",null);
	       sendCommand("VARVAL",null,"THREAD='" + ttag + "' FRAME='" + fid + "' VAR='args'",null);
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='STEP_INTO'",null);
	       break;
	    case 1 :
	       sendCommand("VARVAL",null,"THREAD='" + tid + "' FRAME='" + fid + "' VAR='this'",null);
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='STEP_RETURN'",null);
	       break;
	    case 2 :
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='STEP_OVER'",null);
	       break;
	    case 3 :
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='RESUME'",null);
	       break;
	    case 4 :
	       sendCommand("DEBUGACTION",null,"THREAD='" + tid + "' ACTION='TERMINATE'",null);
	       break;
	  }
	 ++action_count;
       }
    }
}


private synchronized void processNameEvent(String evt,String nid)
{
   if (evt.equals("ENDNAMES")) {
      names_done = true;
      notifyAll();
    }
}


private synchronized void waitForNames()
{
   while (!names_done) {
      try {
         wait(10000);
       }
      catch (InterruptedException e) { }
    }
   
   names_done = false;
}



/********************************************************************************/
/*										*/
/*	Handle messages from eclipse						*/
/*										*/
/********************************************************************************/

public class MessageHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      System.err.println("BEDROCKTEST: MESSAGE FROM IDEA:");
      System.err.println(msg.getText());
      System.err.println("BEDROCKTEST: End of MESSAGE");
      String typ = args.getArgument(0);
      if (typ.equals("RUNEVENT")) {
         Element elt = msg.getXml();
         for (Element re : IvyXml.children(elt,"RUNEVENT")) {
            processRunEvent(re);
          }
       }
      else if (typ.equals("NAMES")) {
         System.err.println("Received names");
       }
      else if (typ.equals("ENDNAMES")) {
         Element elt = msg.getXml();
         processNameEvent(typ,IvyXml.getAttrString(elt,"NID"));
       }
      msg.replyTo();
    }
   
}	// end of inner class MessageHandler




/********************************************************************************/
/*										*/
/*	Reply handlers								*/
/*										*/
/********************************************************************************/

private static class ReplyHandler extends MintDefaultReply {

   private String cmd_name;
   
   ReplyHandler(String what) {
      cmd_name = what;
    }
   
   @Override public synchronized void handleReply(MintMessage msg,MintMessage rply) {
      System.err.println("BUBJETTEST: Msg reply");
      super.handleReply(msg,rply);
    }
   
   void print() {
      String rslt = waitForString();
      if (rslt == null) {
         System.err.println("BUBJETTEST: NO REPLY FOR " + cmd_name);
       }
      else {
         System.err.println("BUBJETTEST: REPLY FOR " + cmd_name + ":");
         System.err.println(rslt);
         System.err.println("BEDROCKTEST: End of REPLY");
       }
    }
   
}	// end of inner class ReplyHandler



}       // end of class BubjetTest




/* end of BubjetTest.java */

