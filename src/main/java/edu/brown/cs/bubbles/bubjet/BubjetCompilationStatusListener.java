/****************************************************************************************/
/*											*/
/*		BubjetCompilationStatusListener.java					*/
/*											*/
/*	Listen for compiler results							*/
/*											*/
/****************************************************************************************/

package edu.brown.cs.bubbles.bubjet;


import com.intellij.build.BuildProgressListener;
import com.intellij.build.events.BuildEvent;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.project.Project;

import edu.brown.cs.ivy.xml.IvyXmlWriter;


public class BubjetCompilationStatusListener implements CompilationStatusListener, 
        BuildProgressListener, BubjetConstants {



/********************************************************************************/
/*                                                                              */
/*      Private storage                                                         */
/*                                                                              */
/********************************************************************************/

private Project for_project;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetCompilationStatusListener(Project project)
{
   for_project = project;
   BubjetLog.logD("CompilationStatusListener created for " + project);
}


/****************************************************************************************/
/*											*/
/*	Callback methods								*/
/*											*/
/****************************************************************************************/

@Override public void compilationFinished(boolean abt,int err,int warn,CompileContext ctx)
{
   BubjetLog.logD("compilationFinished " + abt + " " +
         err + " " + warn + " " + ctx.getRebuildReason() + " " + 
        ctx.getMessages(CompilerMessageCategory.ERROR).length);
}


@Override public void automakeCompilationFinished(int err,int warn,CompileContext ctx)
{ 
   BubjetLog.logD("automakeCompilationFinished " +
         err + " " + warn + " " + ctx.getMessages(CompilerMessageCategory.ERROR));
   BubjetApplicationService bas = BubjetBundle.getAppService();
   BubjetMonitor bm = bas.getMonitor(for_project);
   IvyXmlWriter xw = bm.beginMessage("AUTOBUILDDONE");
   xw.begin("MESSAGES");
   int ct = 0;
   for (CompilerMessage msg : ctx.getMessages(CompilerMessageCategory.ERROR)) {
      if (ct++ < MAX_PROBLEM) BubjetUtil.outputProblem(for_project,msg,xw);
    }
   for (CompilerMessage msg : ctx.getMessages(CompilerMessageCategory.WARNING)) {
      if (ct++ < MAX_PROBLEM) BubjetUtil.outputProblem(for_project,msg,xw);
    }
   xw.end("MESSAGES");
   bm.finishMessage(xw);
}


@Override public void fileGenerated(String outroot,String relpath)
{
   BubjetLog.logD("fileGenerated " + outroot + " " + relpath);
}



/********************************************************************************/
/*                                                                              */
/*      Build Progress events                                                   */
/*                                                                              */
/********************************************************************************/

@Override public void onEvent(Object bid,BuildEvent evt)
{
   BubjetLog.logD("BUILD EVENT " + bid + " " + evt);

   ProgressIndicator pi = CoreProgressManager.getInstance().getProgressIndicator();
   if (pi == null) pi = CoreProgressManager.getGlobalProgressIndicator();
   if (pi != null) {
      BubjetLog.logD("BUILD PROGRESS " + pi.getFraction() + " " +
            pi.getText() + " " + pi.getText2());
    }
   else {
       BubjetLog.logD("BUILD NO PROGRESS");
    }
}



}	// end of BubjetVirtualFileListener



/* end of BubjetVirtualFileListener.java */



