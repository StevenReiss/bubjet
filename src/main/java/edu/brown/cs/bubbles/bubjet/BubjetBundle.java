/****************************************************************************************/
/*											*/
/*		BubjetBundle.java							*/
/*											*/
/*	Main entry point for bubbles plugin bundle					*/
/*											*/
/****************************************************************************************/


package edu.brown.cs.bubbles.bubjet;


import com.intellij.DynamicBundle;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

import java.util.function.Supplier;

public class BubjetBundle extends DynamicBundle {



/****************************************************************************************/
/*											*/
/*	Private Storage 								*/
/*											*/
/****************************************************************************************/

private static final String BUNDLE = "messages.Bubjet";
public static final BubjetBundle INSTANCE = new BubjetBundle();


private BubjetBundle()
{
   super(BUNDLE);
   
   BubjetLog.logD("Bubjet Bundle Started");
   
   Application app = ApplicationManager.getApplication();
   EditorFactory efac = EditorFactory.getInstance();
   efac.getEventMulticaster().addDocumentListener(new BubjetDocumentListener(),app);
   
   BubjetApplicationService appser = getAppService();
   if (appser == null) {
      BubjetLog.logD("Can't create application service");
    }
   
   MessageBus mbus = app.getMessageBus();
   mbus.connect().subscribe(VirtualFileManager.VFS_CHANGES,new BubjetFileListener());
   mbus.connect().subscribe(XDebuggerManager.TOPIC,new BubjetDebuggerListener());
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

public static BubjetApplicationService getAppService()
{
   Application app = ApplicationManager.getApplication();
   return app.getService(BubjetApplicationService.class);
}


public static BubjetMonitor getMonitor(Project p)
{
   return getAppService().getMonitor(p);
}


public static void runInBackground(Runnable r)
{
   Application app = ApplicationManager.getApplication();
   app.executeOnPooledThread(r);
}



/****************************************************************************************/
/*											*/
/*	Standard methods								*/
/*											*/
/****************************************************************************************/

public static String message(String key,Object ... params)
{
   return INSTANCE.getMessage(key,params);
}



public static Supplier<String> messagePointer(String key,Object ... params)
{
   return INSTANCE.getLazyMessage(key,params);
}



}




/* end of BubjetBundle.java */
