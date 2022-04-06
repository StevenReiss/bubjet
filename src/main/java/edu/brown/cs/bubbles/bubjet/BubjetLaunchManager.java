/********************************************************************************/
/*                                                                              */
/*              BubjetLaunchManager.java                                        */
/*                                                                              */
/*      Manage launch configurations                                            */
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

import java.util.HashSet;
import java.util.Set;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.module.Module;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BubjetLaunchManager implements BubjetConstants, RunManagerListener
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService       app_service;
private Set<RunnerAndConfigurationSettings> launch_configs;
private Project for_project;

static Key<Boolean> BUBJET_LAUNCH_IGNORE_KEY = null;
static Key<String> BUBJET_LAUNCH_ORIG_ID = null;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetLaunchManager(BubjetApplicationService app,Project p)
{
   if (BUBJET_LAUNCH_IGNORE_KEY == null) {
      BUBJET_LAUNCH_IGNORE_KEY = new Key<>("edu.brown.cs.bubbles.bubjet.IGNORE");
      BUBJET_LAUNCH_ORIG_ID = new Key<>("edu.brown.cs.bubbles.bubjet.ORIGID");
    }
   
   app_service = app;
   for_project = p;
   launch_configs = new HashSet<>();
   
   RunManager rm = RunManager.getInstance(for_project);
   for (RunnerAndConfigurationSettings rcs : rm.getAllSettings()) {
      BubjetLog.logD("Add initial launch " + rcs.getName() + " " + rcs);
      launch_configs.add(rcs);
    }
   
   p.getMessageBus().connect().subscribe(RunManagerListener.TOPIC,this);
   
   BubjetLog.logD("Launch manager started for " + p);
}




/********************************************************************************/
/*                                                                              */
/*      Handle GETRUNCONFIG                                                     */
/*                                                                              */
/********************************************************************************/

void getRunConfigurations(Project p,Module m,IvyXmlWriter xw)
{
   GetRunConfigurationsAction act = new GetRunConfigurationsAction(xw);
   act.start();
}



private class GetRunConfigurationsAction extends BubjetAction.Read {
   
   private IvyXmlWriter xml_writer;
   
   GetRunConfigurationsAction(IvyXmlWriter xw) {
      xml_writer = xw;
    }
   
   @Override void process() {
      for (RunnerAndConfigurationSettings rcs : launch_configs) {
         BubjetUtil.outputLaunchConfiguration(rcs,xml_writer);
       }
    }
   
}       // end if inner class GetRunConfigurationsAction




/********************************************************************************/
/*                                                                              */
/*      Handle NEWRUNCONFIG                                                     */
/*                                                                              */
/********************************************************************************/

void getNewRunConfiguration(Project p,Module m,String name,
      String clone,String type,IvyXmlWriter xw)
{
   BubjetLog.logE("Not implemented yet: NEWRUNCONFIG");
}



/********************************************************************************/
/*                                                                              */
/*      Handle EDITRUNCONFIG                                                    */
/*                                                                              */
/********************************************************************************/

void editRunConfiguration(Project p,Module m,String launch,
      String prop,String value,IvyXmlWriter xw)
{
   RunnerAndConfigurationSettings rcs = findLaunch(p,launch);
   if (rcs == null) return;
   BubjetLog.logE("Not implemented yet: EDITRUNCONFIG");
   
}



/********************************************************************************/
/*                                                                              */
/*      Handle SAVERUNCONFIG                                                    */
/*                                                                              */
/********************************************************************************/

void saveRunConfiguration(Project p,Module m,String launch,IvyXmlWriter xw)
{
   RunnerAndConfigurationSettings rcs = findLaunch(p,launch);
   if (rcs == null) return;
   BubjetLog.logE("Not implemented yet: SAVERUNCONFIG");
}



/********************************************************************************/
/*                                                                              */
/*      Handle DELETERUNCONFIG                                                  */
/*                                                                              */
/********************************************************************************/

void deleteRunConfiguration(Project p,Module m,String launch,IvyXmlWriter xw)
{
   RunnerAndConfigurationSettings rcs = findLaunch(p,launch);
   if (rcs == null) return;
   BubjetLog.logE("Delete run configuration not implemented");
   // remove
}




/********************************************************************************/
/*                                                                              */
/*      Utility methods                                                         */
/*                                                                              */
/********************************************************************************/

RunnerAndConfigurationSettings findLaunch(Project p,String name)
{
   for (RunnerAndConfigurationSettings rcs : launch_configs) {
      RunConfiguration rc = rcs.getConfiguration();
      BubjetLog.logD("CHECK RCS " + rcs.getName() + " " + rcs.getUniqueID() + " " +
            rc.getName() + " " + rc.getUniqueID());
      if (p != null && rc.getProject() != p) continue;
      if (rc.getName().equals(name) ||
            String.valueOf(rc.getUniqueID()).equals(name) ||
            rcs.getName().equals(name) || 
            rcs.getUniqueID().equals(name)) {
         return rcs;
       }
    }
   BubjetLog.logD("LAUNCH CONFIGURATION " + name + " NOT FOUND");
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Event messaging                                                         */
/*                                                                              */
/********************************************************************************/

private void launchEvent(String what,RunnerAndConfigurationSettings ... rcss)
{
   for (RunnerAndConfigurationSettings rcs : rcss) {
      RunConfiguration rc = rcs.getConfiguration();
      Project p = rc.getProject();
      IvyXmlWriter xw = app_service.getMonitor(p).beginMessage("LAUNCHCONFIGEVENT");
      xw.begin("LAUNCH");
      xw.field("REASON",what);
      BubjetUtil.outputLaunchConfiguration(rcs,xw);
      xw.end("LAUNCH");
      app_service.getMonitor(p).finishMessage(xw);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Event handling                                                          */
/*                                                                              */
/********************************************************************************/


@Override public void runConfigurationSelected(RunnerAndConfigurationSettings config)
{
   BubjetLog.logD("LAUNCH runConfigurationSelected " + config.getName());
}


@Override public void runConfigurationAdded(RunnerAndConfigurationSettings config)
{
   BubjetLog.logD("LAUNCH runConfigurationAdded " + config.getName());
   launch_configs.add(config);
   launchEvent("ADD",config);
}


@Override public void runConfigurationRemoved(RunnerAndConfigurationSettings config)
{
   BubjetLog.logD("LAUNCH runConfigurationRemoved " + config.getName());
   launch_configs.remove(config);
   launchEvent("REMOVE",config);
}


@Override public void runConfigurationChanged(RunnerAndConfigurationSettings config)
{
   BubjetLog.logD("LAUNCH runConfigurationChanged " + config.getName());
   launchEvent("CHANGE",config);
}


@Override public void runConfigurationChanged(RunnerAndConfigurationSettings config,java.lang.String oldid)
{
   BubjetLog.logD("LAUNCH runConfigurationChanged " + config.getName() + " " + oldid);
   launchEvent("CHANGE",config);
   // possibly remove oldid
}


@Override public void beginUpdate()
{
   BubjetLog.logD("LAUNCH beginUpdate configurations");
}


@Override public void endUpdate()
{
   BubjetLog.logD("LAUNCH endUpdate configurations");
}


@Override public void stateLoaded(RunManager msg,boolean isfirstload)
{
   BubjetLog.logD("LAUNCH stateLoaded " + isfirstload + " " + msg);
}


}       // end of class BubjetLaunchManager




/* end of BubjetLaunchManager.java */

