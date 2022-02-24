/****************************************************************************************/
/*											*/
/*		BubjetVirtualFileListener.java						*/
/*											*/
/*	Listen for virtual file changes 						*/
/*											*/
/****************************************************************************************/

package edu.brown.cs.bubbles.bubjet;


import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;


public class BubjetVirtualFileListener implements VirtualFileListener, BulkFileListener {


/****************************************************************************************/
/*											*/
/*	Callback methods								*/
/*											*/
/****************************************************************************************/

@Override public void propertyChanged(VirtualFilePropertyEvent evt)
{ 
   BubjetLog.logD("propertyChanged " + evt);
}



@Override public void contentsChanged(VirtualFileEvent evt)
{  
   BubjetLog.logD("contentsChanged " + evt);
}



@Override public void fileCreated(VirtualFileEvent evt)
{ 
   BubjetLog.logD("fileCreated " + evt);
}


@Override public void beforeFileDeletion(VirtualFileEvent evt)
{ 
   BubjetLog.logD("beforeFileDeletion " + evt);
}


@Override public void fileDeleted(VirtualFileEvent evt)
{
   BubjetLog.logD("fileDeleted " + evt);
}


@Override public void beforeFileMovement(VirtualFileMoveEvent evt)
{ 
   BubjetLog.logD("beforeFileMovement " + evt);
}


@Override public void fileMoved(VirtualFileMoveEvent evt)
{ 
   BubjetLog.logD("fileMoved " + evt);
}


@Override public void fileCopied(VirtualFileCopyEvent evt)
{
   BubjetLog.logD("fileCopied " + evt);
   
   fileCreated(evt);
}



}	// end of BubjetVirtualFileListener



/* end of BubjetVirtualFileListener.java */


