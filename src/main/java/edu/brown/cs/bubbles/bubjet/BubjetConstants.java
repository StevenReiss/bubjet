/********************************************************************************/
/*                                                                              */
/*              BubjetConstants.java                                            */
/*                                                                              */
/*      Constants and global definitions for BUBBLES-JetBrains interface        */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.bubbles.bubjet;

public interface BubjetConstants
{



/********************************************************************************/
/*										*/
/*	Mint constants								*/
/*										*/
/********************************************************************************/

// Must Match BumpConstants.BUMP_MINT_NAME
String	BUBJET_MESSAGE_ID = "BUBBLES_" + System.getProperty("user.name").replace(" ","_");
String  BUBJET_MINT_ID = "BUBBLES_" + System.getProperty("user.name").replace(" ","_") + "_@@@";

String  BUBJET_RESOURCE_ID = "idea";



/********************************************************************************/
/*                                                                              */
/*      Internal Constants                                                      */
/*                                                                              */
/********************************************************************************/

int MAX_TEXT_SEARCH_RESULTS = 256;

int MAX_PROBLEM = 4096;
int MAX_NAMES = 1200;



/********************************************************************************/
/*										*/
/*	Edit information							*/
/*										*/
/********************************************************************************/

interface EditData {

   public int getOffset();
   public int getLength();
   public String getText();
   default int getEndOffset()           { return getOffset() + getLength(); }
   
}	// end of inner interface EditData


}       // end of interface BubjetConstants




/* end of BubjetConstants.java */

