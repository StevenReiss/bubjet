/********************************************************************************/
/*                                                                              */
/*              BubjetPreferenceManager.java                                    */
/*                                                                              */
/*      Handle preferences for project and code bubbles                         */
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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.module.LanguageLevelUtil;
import com.intellij.openapi.module.Module;

import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class BubjetPreferenceManager implements BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private BubjetApplicationService        app_service;
private Project                         for_project;

private static Map<String,String> option_map;

static {
   option_map = new HashMap<>();
   option_map.put("tabulation.size","TAB_SIZE");
   option_map.put("indentation.size","INDENT_SIZE");
   option_map.put("indent_body_declaration_compare_to_type_header",
         "DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS");
   option_map.put("continuation_indent","CONTINUATION_INDENT_SIZE");
   option_map.put("alignment_for_expressions_in_array_initializer",
         "*ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION");
   option_map.put("alignment_for_condition_expression",
         "*ALIGN_MULTILINE_TERNARY_OPERATION");
   option_map.put("brace_position_for_block","*BRACE_STYLE");
   option_map.put("brace_position_for_array_initializer",
         "*ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE");
   option_map.put("brace_position_for_method_declaration","*METHOD_BRACE_STYLE");
   option_map.put("brace_position_for_type_declaration","*CLASS_BRACE_STYLE");
   option_map.put("indent_switchstatements_compare_to_switch","INDENT_CASE_FROM_SWITCH");
   option_map.put("indent_switchstatements_compare_to_cases","INDENT_BREAK_FROM_CASE");
   option_map.put("alignment_for_parameters_in_method_declaration",
         "*ALIGN_MULTILINE_PARAMETERS");
   option_map.put("alignment_for_arguments_in_method_declaration",
         "*ALIGN_MULTILINE_PARAMETERS_IN_CALLS");
   option_map.put("useContractsForJava","useContractsForJava");
   option_map.put("useJunit","useJunit");
   option_map.put("useAssertions","useAssertions");
}



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

BubjetPreferenceManager(BubjetApplicationService app,Project p)
{
   app_service = app;
   for_project = p;
}



/********************************************************************************/
/*                                                                              */
/*      Handle PREFERENCES command                                              */
/*                                                                              */
/********************************************************************************/

void handlePreferences(Project p,Module m,IvyXmlWriter xw) 
{
   Map<String,String> prefs = getPreferences(m);
   
   xw.begin("PREFERENCES");
   for (Map.Entry<String,String> ent : prefs.entrySet()) {
      xw.begin("PREF");
      xw.field("NAME",ent.getKey());
      xw.field("VALUE",ent.getValue());
      xw.end("PREF");
    }
   xw.end("PREFERENCES");
}




/********************************************************************************/
/*                                                                              */
/*      Handle SETPREFERENCES command                                           */
/*                                                                              */
/********************************************************************************/

void handleSetPreferences(Project p,Module m,Element opts,IvyXmlWriter xw)
{
   Map<String,String> iopts = new HashMap<>();
   for (Element opt : IvyXml.children(opts,"OPTION")) {
      String nm = IvyXml.getAttrString(opt,"NAME");
      String val = IvyXml.getAttrString(opt,"VALUE");
      String nnm = option_map.get(nm);
      if (nnm == null) {
         BubjetLog.logE("Unknown eclipse option: " + opt);
         continue;
       }
      else if (nnm.startsWith("*")) {
         nnm = nnm.substring(1);
         switch (nnm) {
            case "ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION" :
            case "ALIGN_MULTILINE_ARRAY_TERNARY_OPERATION" :
               if (val.equals("1")) val = "true";
               else val = "false";
               break;
            case "BRACE_STYLE" :
            case "METHOD_BRACE_STYLE" :
            case "CLASS_BRACE_STYLE" :
               switch (val) {
                  case "eol" :
                     val = String.valueOf(CommonCodeStyleSettings.END_OF_LINE);
                     break;
                  case "next_line" :
                     val = String.valueOf(CommonCodeStyleSettings.NEXT_LINE);
                     break;
                  case "next_line_shifted" :
                     val = String.valueOf(CommonCodeStyleSettings.NEXT_LINE_SHIFTED);
                     break;
                  case "next_line_if_wrapped" :
                     val = String.valueOf(CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED);
                     break;
                  default :
                     BubjetLog.logE("Unknown code style value: " + val);
                     val = String.valueOf(CommonCodeStyleSettings.END_OF_LINE);
                     break;
                }
               break;
            case "ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE" :
               if (val.equals("eol")) val = "false";
               else val = "true";
               break;
            case "ALIGN_MULTILINE_PARAMETERS" :
            case "ALIGN_MUTLILINE_PARAMETERS_IN_CALLS" :
               if (val.equals("2")) val = "true";
               else val = "false";
               break;
            default :
               BubjetLog.logE("Unknown code style mapping for " + nm + " " + nnm + "=" + val);
               val = null;
               break;
          }
       }
      if (nnm != null && val != null) {
         iopts.put(nnm,val);
       }
    }
   for (Element opt : IvyXml.children(opts,"IDEAOPTION")) {
      iopts.put(IvyXml.getAttrString(opt,"NAME"),IvyXml.getAttrString(opt,"VALUE"));
    }
   
   SetPreferencesAction act = new SetPreferencesAction(p,m,iopts);
   act.start();
}



private static class SetPreferencesAction extends BubjetAction.Read {

   private Project for_project;
   private Map<String,String> idea_options;
   
   SetPreferencesAction(Project p,Module m,Map<String,String> iopts) {
      for_project = p;
      idea_options = iopts;
    }
   
   @Override void process() {
      CodeStyleSettingsManager csm = CodeStyleSettingsManager.getInstance(for_project);
      CodeStyleSettings css = csm.createSettings();
      BubjetSettings bss = for_project.getService(BubjetSettings.class);
      CommonCodeStyleSettings ccss = css.getCommonSettings(JavaLanguage.INSTANCE);
      IndentOptions ind = css.getIndentOptions(JavaFileType.INSTANCE);
//    CompilerConfiguration cc = CompilerConfiguration.getInstance(for_project);
      for (Map.Entry<String,String> ent : idea_options.entrySet()) {
         String nm = ent.getKey();
         String svl = ent.getValue();
         int ivl = 0;
         boolean bvl = false;
         if (svl != null) {
            if (svl.startsWith("-") || Character.isDigit(svl.charAt(0))) {
               try {
                  ivl = Integer.parseInt(svl);
                }
               catch (NumberFormatException e) { }
             }
            if ("tT1yY".indexOf(svl.charAt(0)) >= 0) bvl = true;
          }
         switch (nm) {
            case "TAB_SIZE" :
               ind.TAB_SIZE = ivl;
               break;
            case "INDENT_SIZE" :
               ind.INDENT_SIZE = ivl;
               break;
            case "DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS" :
               ccss.DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS = bvl;
               break;
            case "CONTINUATION_INDENT_SIZE" :
               ind.CONTINUATION_INDENT_SIZE = ivl;
               break;
            case "ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION" :
               ccss.ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION = bvl;
               break;
            case "ALIGN_MULTILINE_TERNARY_OPERATION" :
               ccss.ALIGN_MULTILINE_TERNARY_OPERATION = bvl;
               break;
            case "BRACE_STYLE" :
               ccss.BRACE_STYLE = ivl;
               break;
            case "ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE" :
               ccss.ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE = bvl;
               break;
            case "METHOD_BRACE_STYLE" :
               ccss.METHOD_BRACE_STYLE = ivl;
               break;
            case "CLASS_BRACE_STYLE" :
               ccss.CLASS_BRACE_STYLE = ivl;
               break;
            case "INDENT_CASE_FROM_SWITCH" :
               ccss.INDENT_CASE_FROM_SWITCH = bvl;
               break;
            case "INDENT_BREAK_FROM_CASE" :
               ccss.INDENT_BREAK_FROM_CASE = bvl;
               break;    
            case "ALIGN_MULTILINE_PARAMETERS" :
               ccss.ALIGN_MULTILINE_PARAMETERS = bvl;
               break;
            case "ALIGN_MULTILINE_PARAMETERS_IN_CALLS" :
               ccss.ALIGN_MULTILINE_PARAMETERS_IN_CALLS = bvl;
               break;
            case "processAnnotations" :
               break;
            case "useContractsForJava" :
               bss.setUseContracts(bvl);
               break;
            case "useJunit" :
               bss.setUseJunit(bvl);
               break;
            case "useAssertions" :
               bss.setUseAssertions(bvl);
               break;
            default :
               BubjetLog.logE("Unknown option setting " + nm + " = " + svl);
               break;
          }
       }
    }

}       // end of inner class SetPreferences


/********************************************************************************/
/*                                                                              */
/*      Preference/Option methods                                               */
/*                                                                              */
/********************************************************************************/

Map<String,String> getPreferences(Module m)
{
   GetPreferencesAction gpa = new GetPreferencesAction(m);
   gpa.start();
   return gpa.getResult();
}


private class GetPreferencesAction extends BubjetAction.Read {

   private Module for_module;
   private Map<String,String> result_map;
   
   GetPreferencesAction(Module m) {
      for_module = m;
      result_map = null;
    }
   
   Map<String,String> getResult()                       { return result_map; }
   
   @Override void process() {
      app_service.getProjectManager().waitForProject(for_project);
      
      Map<String,String> rslt = new HashMap<>();
      
      CodeStyleSettingsManager csm = CodeStyleSettingsManager.getInstance(for_project);
      CodeStyleSettings css = csm.createSettings();
      BubjetSettings bss = for_project.getService(BubjetSettings.class);
      BubjetLog.logD("BubjetSettings retrieved: " + bss);
      CommonCodeStyleSettings ccss = css.getCommonSettings(JavaLanguage.INSTANCE);
      IndentOptions ind = css.getIndentOptions(JavaFileType.INSTANCE);
      
      formatOpt("tabulation.size",ind.TAB_SIZE,rslt);
      formatOpt("indentation.size",ind.INDENT_SIZE,rslt);
      formatOpt("indent_statements_compare_to_block",true,rslt);
      formatOpt("indent_statements_compare_to_body",true,rslt);
      formatOpt("indent_body_declaration_compare_to_type_header",
            !ccss.DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS,rslt);
      formatOpt("continuation_indent",ind.CONTINUATION_INDENT_SIZE,rslt);
      int arr = 0;
      if (ccss.ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION) arr = 1;
      formatOpt("alignment_for_expressions_in_array_initializer",arr,rslt);
      int cond = 2;
      if (ccss.ALIGN_MULTILINE_TERNARY_OPERATION) cond = 1;
      formatOpt("alignment_for_condition_expression",cond,rslt);
      formatOpt("brace_position_for_block",getBraceStyle(ccss.BRACE_STYLE),rslt);
      String arrs = "eol";
      if (ccss.ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE) arrs = "next_line_shifted";
      formatOpt("brace_position_for_array_initializer",arrs,rslt);
      formatOpt("brace_position_for_method_declaration",getBraceStyle(ccss.METHOD_BRACE_STYLE),rslt);
      formatOpt("brace_position_for_type_declaration",getBraceStyle(ccss.CLASS_BRACE_STYLE),rslt);
      
      formatOpt("indent_switchstatements_compare_to_switch",ccss.INDENT_CASE_FROM_SWITCH,rslt);
      formatOpt("indent_switchstatements_compare_to_cases",ccss.INDENT_BREAK_FROM_CASE,rslt);
      int parm = 1;
      if (ccss.ALIGN_MULTILINE_PARAMETERS) parm = 2;
      formatOpt("alignment_for_parameters_in_method_declaration",parm,rslt);
      int args = 1;
      if (ccss.ALIGN_MULTILINE_PARAMETERS_IN_CALLS) parm = 2;
      formatOpt("alignment_for_arguments_in_method_declaration",args,rslt);
      
      CompilerConfiguration cc = CompilerConfiguration.getInstance(for_project);
      if (for_module != null) {
         LanguageLevel ll = LanguageLevelUtil.getEffectiveLanguageLevel(for_module);
         Sdk sdk = BubjetUtil.getSdk(for_module);
         String llv1 = sdk.getVersionString();
         String llv2 = sdk.getName();
         String llv = ll.toString();
         BubjetLog.logD("COMPILER " + ll + " " + llv + " " + llv1 + " " + llv2);
         if (llv.startsWith("JDK_")) llv = llv.substring(4);
         llv = llv.replace("_",".");
         compilerOpt("source",llv,rslt);
         String tgt = cc.getBytecodeTargetLevel(for_module);
         if (tgt == null) tgt = llv;
         compilerOpt("codegen.targetPlatform",tgt,rslt);
         compilerOpt("compliance",llv,rslt);
         boolean isandroid = false;
         if (sdk.getName().contains("android")) isandroid = true;   
         rslt.put("bedrock.useAndroid",Boolean.toString(isandroid));
       }
      compilerOpt("problem.fatalOptionalError",false,rslt);
      compilerOpt("processAnnotations",cc.isAnnotationProcessorsEnabled(),rslt);
      
      if (bss != null) {
         bubblesOpt("useContractsForJava",bss.getUseContracts(),rslt);
         bubblesOpt("useJunit",bss.getUseJunit(),rslt);
         bubblesOpt("useAssertions",bss.getUseAssertions(),rslt);
       }
      
      result_map = rslt;
    }
   
}       // end of inner class GetPreferencesAction




private String getBraceStyle(int v)
{
   if (v == CommonCodeStyleSettings.END_OF_LINE) return "eol";
   else if (v == CommonCodeStyleSettings.NEXT_LINE) return "next_line";
   else if (v == CommonCodeStyleSettings.NEXT_LINE_SHIFTED) return "next_line_shitfed";
   else if (v == CommonCodeStyleSettings.NEXT_LINE_SHIFTED2) return "next_line_shifted";
   else if (v == CommonCodeStyleSettings.NEXT_LINE_IF_WRAPPED) return "next_line_if_wrapped";
   return "eol";
}


private void formatOpt(String name,Object val,Map<String,String> opts)
{
   String pnm = "org.eclipse.jdt.core.formatter." + name;
   String pvl = String.valueOf(val);
   opts.put(pnm,pvl);
}

private void compilerOpt(String name,Object val,Map<String,String> opts)
{
   String pnm = "org.eclipse.jdt.core.compiler." + name;
   String pvl = String.valueOf(val);
   opts.put(pnm,pvl);
}

private void bubblesOpt(String name,Object val,Map<String,String> opts)
{
   String pnm = "edu.brown.cs.bubbles.bedrock." + name;
   String pvl = String.valueOf(val);
   opts.put(pnm,pvl);
}




}       // end of class BubjetPreferenceManager




/* end of BubjetPreferenceManager.java */

