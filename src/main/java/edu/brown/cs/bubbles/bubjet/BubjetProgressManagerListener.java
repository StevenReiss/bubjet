/****************************************************************************************/
/*											*/
/*		BubjetProgressManagerListener.java					*/
/*											*/
/*	Listen for progress indications 						*/
/*											*/
/****************************************************************************************/

package edu.brown.cs.bubbles.bubjet;


import com.intellij.openapi.progress.ProgressManagerListener;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.ProgressIndicator;



public class BubjetProgressManagerListener implements ProgressManagerListener {


/****************************************************************************************/
/*											*/
/*	Private Storage 								*/
/*											*/
/****************************************************************************************/


/****************************************************************************************/
/*											*/
/*	Callback methods								*/
/*											*/
/****************************************************************************************/

public void beforeTaskStart(Task t,ProgressIndicator ind)
{ 
   BubjetLog.logD("beforeTaskStart " + t);
}


public void afterTaskStart(Task t,ProgressIndicator ind)
{
   BubjetLog.logD("afterTaskStart " + t + " " + ind.getFraction());
}


public void beforeTaskFinished(Task t)
{
   BubjetLog.logD("beforeTaskFinished " + t);
}


public void afterTaskFinished(Task t)
{ 
   BubjetLog.logD("afterTaskFinished " + t + " " + t.getTitle() + " " + t.getProject());
}


public void onTaskFinished(Task t,boolean sts,Throwable cause)
{ 
   BubjetLog.logD("onTaskFinished " + t + " " + sts + " " + cause);
}


public void onTaskRunnableCreated(Task t,ProgressIndicator ind,Runnable r)
{
   BubjetLog.logD("onTaskRunnableCreated " + t);
}




}	// end of BubjetDocumentListener



/* end of BubjetCommandListener.java */




