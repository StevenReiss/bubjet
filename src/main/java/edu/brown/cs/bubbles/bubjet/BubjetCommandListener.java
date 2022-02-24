/****************************************************************************************/
/*											*/
/*		BubjetCommandListener.java						*/
/*											*/
/*	Listen for commands								*/
/*											*/
/****************************************************************************************/

package edu.brown.cs.bubbles.bubjet;


import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.command.CommandEvent;



public class BubjetCommandListener implements CommandListener {


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

@Override public void commandStarted(CommandEvent evt)
{ 
   BubjetLog.logD("commandStarted " + evt.getCommandName() + " " + evt.getDocument() + " " +
   evt.getCommand());
}


@Override public void beforeCommandFinished(CommandEvent evt)
{ 
   BubjetLog.logD("beforeCommandFinished " + evt.getCommandName());
}


@Override public void commandFinished(CommandEvent evt)
{ 
   BubjetLog.logD("commandFinished " + evt);
}


@Override public void undoTransparentActionStarted()
{ 
   BubjetLog.logD("undoTransparentActionStarted");
}


@Override public void beforeUndoTransparentActionFinished()
{ 
   BubjetLog.logD("beforeUndoTransparentActionFinished");
}


@Override public void undoTransparentActionFinished()
{
   BubjetLog.logD("undoTransparentActionFinished");
}



}	// end of BubjetCommandListener



/* end of BubjetCommandListener.java */


