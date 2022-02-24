/********************************************************************************/
/*                                                                              */
/*              BubjetProgressIndicator.java                                    */
/*                                                                              */
/*      Report progress to Code Bubbles                                         */
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.intellij.openapi.progress.StandardProgressIndicator;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.progress.util.ProgressIndicatorListener;
import com.intellij.openapi.project.Project;

import edu.brown.cs.ivy.xml.IvyXmlWriter;



class BubjetProgressIndicator extends AbstractProgressIndicatorExBase
        implements ProgressIndicatorListener, StandardProgressIndicator
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String task_id;
private Project for_project;
private double cur_fraction;

private static AtomicInteger task_counter = new AtomicInteger(1);
private static AtomicLong    serial_number = new AtomicLong(1);

private static final long serialVersionUID = 1;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetProgressIndicator(String id,Project p)
{
   for_project = p;
   if (getText() == null) setText(id);
   cur_fraction = 0;
   
   installToProgress(this);  
   
   task_id = Integer.toString(task_counter.incrementAndGet());
   
   BubjetLog.logD("Progress monitor " + id + " " + task_id);
   
   makeReport("BEGIN");
}



/********************************************************************************/
/*                                                                              */
/*      Action methods                                                          */
/*                                                                              */
/********************************************************************************/

@Override public void cancelled()
{ 
   cur_fraction = 0;
   makeReport("CANCEL");
}


@Override public void stopped()
{
   cur_fraction = 0;
   makeReport("DONE");
}


public void onFractionChanged(double f)
{ 
   cur_fraction = f;
   makeReport("WORKED");
}


/********************************************************************************/
/*                                                                              */
/*      Generate report to bubbles                                              */
/*                                                                              */
/********************************************************************************/

private void makeReport(String typ)
{
   BubjetLog.logD("MAKE PROGRESS REPORT " + typ + " " + cur_fraction);
   BubjetMonitor mon = BubjetBundle.getMonitor(for_project);
   IvyXmlWriter xw = mon.beginMessage("PROGRESS");
   xw.field("KIND",typ);
   xw.field("TASK",getText());
   if (getText2() != null) xw.field("SUBTASK",getText2());
   xw.field("ID",task_id);
   xw.field("S",serial_number.incrementAndGet());
   if (cur_fraction > 0) {
      xw.field("WORK",((int) cur_fraction*100)); 
    }
   mon.finishMessage(xw);
}




}       // end of class BubjetProgressIndicator




/* end of BubjetProgressIndicator.java */

