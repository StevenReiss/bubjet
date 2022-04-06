/********************************************************************************/
/*                                                                              */
/*              BubjetTask.java                                                 */
/*                                                                              */
/*      Runnable that works with intellij                                       */
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.Invoker;

abstract class BubjetAction implements BubjetConstants, Runnable
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Project                 for_project;
private String                  progress_name;
private boolean                 need_read;
private boolean                 need_write;
private boolean                 need_dispatch;
private boolean                 run_background;
private boolean                 smart_action;
private String                  command_name;
private Document                command_document;

private ProgressIndicator       progress_indicator;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetAction(Project p,boolean read,boolean write,String prog,boolean dispatch,boolean bkg,boolean smart,
      String cmd,Document doc)
{
   for_project = p;
   progress_name = prog;
   need_read = read;
   need_write = write;
   need_dispatch = dispatch;
   run_background = bkg;
   smart_action = smart;
   command_name = cmd;
   command_document = doc;
}



/********************************************************************************/
/*                                                                              */
/*      Start the task                                                          */
/*                                                                              */
/********************************************************************************/

void start()
{
   if (run_background && command_name == null) {
      if (smart_action) {
         smart_action = false;
         DumbService ds = DumbService.getInstance(for_project);
         ds.smartInvokeLater(this);
       }
      else if (need_dispatch) {
         ApplicationManager.getApplication().invokeLater(this);
       }
      else {
         Invoker inv = null;
         if (need_read) inv = Invoker.forBackgroundPoolWithReadAction(for_project);
         else inv = Invoker.forBackgroundPoolWithoutReadAction(for_project);
         inv.invoke(this);
       }
    }
   else if (command_name != null) {
      if (!ApplicationManager.getApplication().isDispatchThread()) {
         ApplicationManager.getApplication().invokeLater(this);
       }
      else {
         CommandProcessor cmdproc = CommandProcessor.getInstance();
         if (cmdproc.getCurrentCommand() == null) {
            cmdproc.executeCommand(for_project,this,command_name,null,command_document);
          }
         else run();
       }
    }
   else if (need_read && smart_action) {
      smart_action = false;
      DumbService ds = DumbService.getInstance(for_project);
      ds.runReadActionInSmartMode(this);
    }
   else run();
}





/********************************************************************************/
/*                                                                              */
/*      Task controller                                                         */
/*                                                                              */
/********************************************************************************/

@Override public final void run()
{
   BubjetLog.logD("CHECK ACTION " + need_dispatch + " " + progress_name + " " +
         smart_action + " " + need_read + " " + need_write + " " + command_name + " " +
         run_background + " " + progress_indicator + " " +
         ApplicationManager.getApplication().isDispatchThread() + " " +
         ApplicationManager.getApplication().isWriteThread() + " " + 
         ApplicationManager.getApplication().isWriteAccessAllowed() + " " +
         ApplicationManager.getApplication().isReadAccessAllowed() + " " + 
         CommandProcessor.getInstance().getCurrentCommand());
   if (progress_name != null && progress_indicator == null) {
      if (progress_name.equals("") || progress_name.equals("*")) {
         progress_indicator = new ProgressIndicatorBase();
         BubjetLog.logD("Create Hidden ProgressIndicator");
       }
      else {
         progress_indicator = new BubjetProgressIndicator(progress_name,for_project);
         BubjetLog.logD("CREATE PROGRESS INDICATOR for " + for_project.getName());
       }
      ProgressManager.getInstance().runProcess(this,progress_indicator);
      return;
    }
   if (smart_action) {
      DumbService ds = DumbService.getInstance(for_project);
      ds.waitForSmartMode();
    }
   if (need_dispatch && !ApplicationManager.getApplication().isDispatchThread()) {
      BubjetLog.logD("RUN DISPATCH");
      if (run_background) {
         ApplicationManager.getApplication().invokeLater(this);
       }
      else {
         ApplicationManager.getApplication().invokeAndWait(this);
       }
      return;
    }
   if (command_name != null) {
      CommandProcessor cmdproc = CommandProcessor.getInstance();
      if (cmdproc.getCurrentCommand() == null) {
         cmdproc.executeCommand(for_project,this,command_name,null,command_document);
         return;
       }
    }
  
   if (need_write && !ApplicationManager.getApplication().isWriteAccessAllowed()) {
      ApplicationManager.getApplication().runWriteAction(this);
      return;
    }
   if (need_read && !ApplicationManager.getApplication().isReadAccessAllowed()) {
      ApplicationManager.getApplication().runReadAction(this);
      return;
    }
   
   BubjetLog.logD("RUN ACTION " + need_dispatch + " " + progress_name + " " +
         smart_action + " " + need_read + " " + need_write + " " + command_name + " " +
         ApplicationManager.getApplication().isDispatchThread() + " " +
         ApplicationManager.getApplication().isWriteThread() + " " + 
         ApplicationManager.getApplication().isWriteAccessAllowed() + " " +
         ApplicationManager.getApplication().isReadAccessAllowed() + " " + 
         CommandProcessor.getInstance().getCurrentCommand());
   try {
      process();
    }
   catch (Throwable t) {
      BubjetLog.logE("Task threw exeception",t);
    }
}






abstract void process() throws BubjetException;



/********************************************************************************/
/*                                                                              */
/*      Simplified Actions                                                      */
/*                                                                              */
/********************************************************************************/

public static abstract class Read extends BubjetAction {
   
   Read() {
      super(null,true,false,null,false,false,false,null,null);
    }
   
   Read(Project p,String task) {
      super(p,true,false,task,false,false,false,null,null);
    }
   
}       // end of inner class Read



public static abstract class SmartRead extends BubjetAction {
   
   SmartRead(Project p,String task) {
      super(p,true,false,task,false,false,true,null,null);
    }

}       // end of inner class SmartRead


public static abstract class SmartBackgroundRead extends BubjetAction {
   
   SmartBackgroundRead(Project p,String task) {
      super(p,true,false,task,false,true,true,null,null);
    }
}


public static abstract class WriteDispatch extends BubjetAction {

   WriteDispatch() {
      super(null,false,true,null,true,false,false,null,null);
    }
   
}       // end of inner class WriteDispatch



public static abstract class ReadDispatch extends BubjetAction {

   ReadDispatch() {
      super(null,true,false,null,true,false,false,null,null);
    }

}       // end of inner class ReadDispatch

public static abstract class Write extends BubjetAction {

   Write() {
      super(null,false,true,null,false,false,false,null,null);
    }

}       // end of inner class Write


public static abstract class Command extends BubjetAction {

   Command(Project p,String cmd,Document doc) {
      super(p,false,true,null,true,false,false,cmd,doc);
    }
   
}       // end of inner class Write


public static abstract class SimpleCommand extends BubjetAction {
   
   SimpleCommand(Project p,String cmd,Document doc) {
      super(p,false,false,null,true,false,false,cmd,doc);
    }

}       // end of inner class Write


public static abstract class Background extends BubjetAction {
   
   Background(Project p,String task) {
      super(p,false,false,task,false,true,false,null,null);
    }
   
}       // end of inner class Background



public static abstract class BackgroundWrite extends BubjetAction {

   BackgroundWrite(Project p,String task) {
      super(p,false,true,task,false,true,false,null,null);
    }

}       // end of inner class Background


public static abstract class BackgroundRead extends BubjetAction {
   
   BackgroundRead(Project p,String task) {
      super(p,true,false,task,false,true,false,null,null);
    }
   
}       // end of inner class BackgroundRead


public static abstract class Dispatch extends BubjetAction {
   
   Dispatch(Project p,String task) {
      super(p,false,false,task,true,true,false,null,null);
    }
   
   Dispatch(Project p,String task,boolean smart) {
      super(p,false,false,task,true,true,smart,null,null);
    }
  
}       // end of inner class Dispatch



}       // end of class BubjetTask




/* end of BubjetTask.java */

