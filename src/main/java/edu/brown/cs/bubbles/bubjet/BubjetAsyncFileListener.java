/****************************************************************************************/
/*											*/
/*		BubjetAsyncFileListener.java						*/
/*											*/
/*	Listen for async file changes							*/
/*											*/
/****************************************************************************************/

package edu.brown.cs.bubbles.bubjet;


import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;

import java.util.List;


public class BubjetAsyncFileListener implements AsyncFileListener {


/****************************************************************************************/
/*											*/
/*	Callback methods								*/
/*											*/
/****************************************************************************************/

@Override public ChangeApplier prepareChange(List<? extends VFileEvent> events)
{
   BubjetLog.logD("prepareChange " + events);
   return new Changer(events);
}



private class Changer implements ChangeApplier {

   public List<? extends VFileEvent> for_events;

   Changer(List<? extends VFileEvent> evts) {
      for_events = evts;
    }


   @Override public void beforeVfsChange() {
      BubjetLog.logD("beforeVfsChange " + for_events.size());
    }

   @Override public void afterVfsChange() {
      BubjetLog.logD("afterVfsChange " + for_events.size());
    }

}	// end of inner class Changer



}	// end of BubjetProjectManagerListener



/* end of BubjetProjectManagerListener.java */

