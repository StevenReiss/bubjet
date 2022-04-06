/********************************************************************************/
/*                                                                              */
/*              BubjetSettings.java                                             */
/*                                                                              */
/*      description of class                                                    */
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
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;

@State(name="edu.brown.cs.bubbles.bubjet.BubjetSettings",
      storages= @Storage("BubblesSettings.xml")
)
public class BubjetSettings implements PersistentStateComponent<BubjetSettings>, BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private boolean use_contracts;
private boolean use_junit;
private boolean use_assertions;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public static BubjetSettings getInstance() 
{
   return ApplicationManager.getApplication().getService(BubjetSettings.class);
} 


public BubjetSettings()
{
   BubjetLog.logD("BubjetSettings initialized without a project");
}


public BubjetSettings(Project p)
{
   BubjetLog.logD("BubjetSettings service initalized for " + p.getName());
   
   use_contracts = false;
   use_junit = false;
   use_assertions = false;
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean getUseContracts()                       { return use_contracts; }
boolean getUseJunit()                           { return use_junit; }
boolean getUseAssertions()                      { return use_assertions; }

void setUseContracts(boolean fg)                { use_contracts = fg; }
void setUseJunit(boolean fg)                    { use_junit = fg; }
void setUseAssertions(boolean fg)               { use_assertions = fg; }



/********************************************************************************/
/*                                                                              */
/*      State methods                                                           */
/*                                                                              */
/********************************************************************************/

@Override public BubjetSettings getState()              { return this; }


@Override public void loadState(BubjetSettings state) 
{
   BubjetLog.logD("Load state for BubjetSettings: " + state);
   XmlSerializerUtil.copyBean(state,this);
}




}       // end of class BubjetSettings




/* end of BubjetSettings.java */

