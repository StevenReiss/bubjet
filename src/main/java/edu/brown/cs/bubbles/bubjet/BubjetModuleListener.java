/********************************************************************************/
/*                                                                              */
/*              BubjetModuleListener.java                                       */
/*                                                                              */
/*      Listen for module events                                                */
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

import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.project.Project;


import com.intellij.openapi.module.Module;


public class BubjetModuleListener implements ModuleListener, ModuleRootListener 
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Project for_project;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public BubjetModuleListener()
{
   for_project = null;
   BubjetLog.logD("Module listener created");
}



public BubjetModuleListener(Project p)
{
   for_project = p;
   BubjetLog.logD("Module listener created for " + p);
}


/********************************************************************************/
/*                                                                              */
/*      Abstract Method Implementations for ModuleListener                      */
/*                                                                              */
/********************************************************************************/

@Override public void moduleAdded(Project proj,Module mod)
{
   BubjetLog.logD("moduleAdded " + proj + " " + mod + " " + for_project);
}


@Override public void beforeModuleRemoved(Project proj,Module mod)
{
   BubjetLog.logD("beforeModuleRemoved " + proj + " " + mod + " " + for_project);
}


@Override public void moduleRemoved(Project proj,Module mod)
{
   BubjetLog.logD("moduleRemoved " + proj + " " + mod + " " + for_project);
}


// public void modulesRenamed(Project proj,List<? extends Module> mods,Function<? super Module,String> oldnamer)
// {
// BubjetLog.logD("modulesRenamed " + proj);
// }


/********************************************************************************/
/*                                                                              */
/*      Abstract methods for ModuleRootListener                                 */
/*                                                                              */
/********************************************************************************/

@Override public void beforeRootsChange(ModuleRootEvent evt)
{
   BubjetLog.logD("beforeRootsChange " + evt);
}



@Override public void rootsChanged(ModuleRootEvent evt)
{
   BubjetLog.logD("rootsChanged " + evt);
}



}       // end of class BubjetModuleListener




/* end of BubjetModuleListener.java */

