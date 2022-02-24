/****************************************************************************************/
/*											*/
/*		BubjetDocumentListener.java						*/
/*											*/
/*	Listen for document changes							*/
/*											*/
/****************************************************************************************/

package edu.brown.cs.bubbles.bubjet;


import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.Document;


public class BubjetDocumentListener implements DocumentListener {


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

@Override public void beforeDocumentChange(DocumentEvent evt)
{ 
   BubjetLog.logD("beforeDocumentChange " + evt);
}


@Override public void documentChanged(DocumentEvent evt)
{ 
   BubjetLog.logD("documentChange " + evt);
}


@Override public void bulkUpdateStarting(Document doc)
{  
   BubjetLog.logD("bulkUpdateStarting " + doc);
}


@Override public void bulkUpdateFinished(Document doc)
{
   BubjetLog.logD("bulkUpdateFinished");
}



}	// end of BubjetDocumentListener



/* end of BubjetCommandListener.java */



