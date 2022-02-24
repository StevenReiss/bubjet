/********************************************************************************/
/*                                                                              */
/*              BubjetLog.java                                                  */
/*                                                                              */
/*      Logging for bubbles interface to jetbrains intellij                     */
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

import edu.brown.cs.ivy.file.IvyLog;


class BubjetLog extends IvyLog
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

static void setup() 
{
   setupLogging("BUBJET",false);
   setLogLevel(LogLevel.DEBUG);
   setLogFile("/Users/spr/bubjet.out");
   useStdErr(true);
}


}       // end of class BubjetLog




/* end of BubjetLog.java */

