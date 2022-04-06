/********************************************************************************/
/*                                                                              */
/*              BubjetDebug.java                                                */
/*                                                                              */
/*      Interface to JDI debugger objects                                       */
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.engine.jdi.ThreadReferenceProxy;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;




class BubjetDebug implements BubjetConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



private static Map<Type,BType> type_map;
private static Map<Value,BValue> value_map;
private static Map<BubjetDebugProcess,BNull> null_map;
private static Map<Object,BVariable> variable_map;
private static Map<BubjetDebugProcess,BTarget> target_map;
private static Map<StackFrame,BStackFrame> frame_map;
private static Map<Method,BMethod> method_map;


static {
   type_map = new WeakHashMap<>();
   value_map = new WeakHashMap<>();
   null_map = new WeakHashMap<>();
   variable_map = new WeakHashMap<>();
   target_map = new WeakHashMap<>();
   frame_map = new WeakHashMap<>();
   method_map = new WeakHashMap<>();
}





/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

static BThread getThread(BubjetDebugProcess bdp,ThreadReferenceProxy trp) 
{
   return getThread(bdp,trp.getThreadReference());
}

static BThread getThread(BubjetDebugProcess bdp,ThreadReference tr)
{
   return (BThread) getValue(bdp,tr);
}


static BType getType(BubjetDebugProcess bdp,Type t)
{
   if (t == null) return null;
   
   synchronized (type_map) {
      BType bt = type_map.get(t);
      if (bt == null) {
         bt = new BType(bdp,t);
         type_map.put(t,bt);
       }
      return bt;
    }
}


static BType getType(BubjetDebugProcess bdp,String classnm)
{
   DebugProcess dproc = bdp.getProcess();
   EvaluationContext ectx = bdp.getDebuggerContext().createEvaluationContext();
   try {
      ClassType typ = (ClassType) dproc.findClass(ectx,classnm,null);
      return getType(bdp,typ);
    }
   catch (Throwable t) {
      return null;
    }
}


static BValue getValue(BubjetDebugProcess bdp,Value v)
{
   if (v == null) {
      synchronized (null_map) {
         BNull bn = null_map.get(bdp);
         if (bn == null) {
            bn = new BNull(bdp);
            null_map.put(bdp,bn);
          }
         return bn;
       }
    }
   
   synchronized (value_map) {
      BValue bv = value_map.get(v);
      if (bv == null) {
         if (v instanceof PrimitiveValue) {
            bv = new BPrimitive(bdp,(PrimitiveValue) v);
          }
         else if (v instanceof ThreadReference) {
            bv = new BThread(bdp,(ThreadReference) v);
          }
         else if (v instanceof ArrayReference) {
            bv = new BArray(bdp,(ArrayReference) v);
          }
         else if (v instanceof ClassObjectReference) {
            bv = new BClass(bdp,(ClassObjectReference) v);
          }
         else if (v instanceof StringReference) {
            bv = new BString(bdp,(StringReference) v);
          }
         else {
            bv = new BObject(bdp,(ObjectReference) v);
          }
       }
      if (bv != null) value_map.put(v,bv);
      
      return bv;
    }
}


static BValue getPrimitiveValue(BubjetDebugProcess bdp,Object o) 
{
   VirtualMachine vm = bdp.getVirtualMachine();
   Value v = null;
   if (o == null) v = null;
   else if (o instanceof Boolean) v = vm.mirrorOf(((Boolean) o).booleanValue());
   else if (o instanceof Byte) v = vm.mirrorOf(((Byte) o).byteValue());
   else if (o instanceof Character) v = vm.mirrorOf(((Character) o).charValue());
   else if (o instanceof Double) v = vm.mirrorOf(((Double) o).doubleValue());
   else if (o instanceof Float) v = vm.mirrorOf(((Float) o).floatValue());
   else if (o instanceof Integer) v = vm.mirrorOf(((Integer) o).intValue());
   else if (o instanceof Long) v = vm.mirrorOf(((Long) o).longValue());
   else if (o instanceof Short) v = vm.mirrorOf(((Short) o).shortValue());
   else if (o instanceof String) v = vm.mirrorOf(((String) o));
   else {
      BubjetLog.logE("Unknown type for conversion args" + o);
      v = null;
    }
   return getValue(bdp,v);
}


static BObject getObject(BubjetDebugProcess bdp,ObjectReference or)
{
   return (BObject) getValue(bdp,or);
}


static BField getField(BubjetDebugProcess bdp,Field f)
{
   return (BField) getVariable(bdp,f);
}


static BVariable getVariable(BubjetDebugProcess bdp,Object var)
{
   if (var == null) return null;
   
   synchronized (variable_map) {
      BVariable bv = variable_map.get(var);
      if (bv == null) {
         if (var instanceof Field) {
            bv = new BField(bdp,((Field) var));
          }
         else if (var instanceof LocalVariable) {
            bv = new BLocal(bdp,(LocalVariable) var);
          }
         else if (var instanceof ClassType) {
            bv = new BThis(bdp,(ClassType) var);
          }
         else if (var instanceof ObjectReference) {
            bv = new BThis(bdp,(ClassType) ((ObjectReference) var).type());
          }
         variable_map.put(var,bv);
       }
      return bv;
    }
}



static BTarget getTarget(BubjetDebugProcess bdp)
{
   synchronized (target_map) {
      BTarget bt = target_map.get(bdp);
      if (bt == null) {
         bt = new BTarget(bdp);
         target_map.put(bdp,bt);
       }
      return bt;
    }
}


static BStackFrame getStackFrame(BubjetDebugProcess bdp,StackFrame f)
{
   if (f == null) return null;
   
   synchronized (frame_map) {
      BStackFrame bsf = frame_map.get(f);
      if (bsf == null) {
         bsf = new BStackFrame(bdp,f);
         frame_map.put(f,bsf);
       }
      return bsf;
    }
}


static BMethod getMethod(BubjetDebugProcess bdp,Method m)
{
   if (m == null) return null;
   
   synchronized (method_map) {
      BMethod bm = method_map.get(m);
      if (bm == null) {
         bm = new BMethod(bdp,m);
         method_map.put(m,bm);
       }
      return bm;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Clean up when process is done                                           */
/*                                                                              */
/********************************************************************************/

static void removeProcess(BubjetDebugProcess proc)
{
   removeAll(proc,type_map);
   removeAll(proc,value_map);
   removeAll(proc,null_map);
   removeAll(proc,variable_map);
   removeAll(proc,target_map);
   removeAll(proc,frame_map);
   removeAll(proc,method_map);
}


private static void removeAll(BubjetDebugProcess proc,Map<?,? extends BMirror> map) 
{
   synchronized (map) {
      for (Iterator<? extends BMirror> it = map.values().iterator(); it.hasNext(); ) {
         BMirror bm = it.next();
         if (bm.getProcess() == proc) it.remove();
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Basic object                                                            */
/*                                                                              */
/********************************************************************************/

private static class BMirror {
   
   protected BubjetDebugProcess debug_process;
   
   protected BMirror(BubjetDebugProcess bdp) {
      debug_process = bdp;
    }
   
   BubjetDebugProcess getProcess()      { return debug_process; }
   
   protected BField getField(Field f)   { return BubjetDebug.getField(debug_process,f); }
   protected BMethod getMethod(Method m) { return BubjetDebug.getMethod(debug_process,m); }
   protected BStackFrame getStackFrame(StackFrame f) {
      return BubjetDebug.getStackFrame(debug_process,f);
    }
   protected BTarget getTarget()        { return BubjetDebug.getTarget(debug_process); }
   protected BThread getThread(ThreadReference t) { 
      return BubjetDebug.getThread(debug_process,t);
    }
   protected BType getType(Type t)      { return BubjetDebug.getType(debug_process,t); }
   protected BType getType(String t)    { return BubjetDebug.getType(debug_process,t); }
   protected BValue getValue(Value v)   { return BubjetDebug.getValue(debug_process,v); }
   protected BObject getObject(ObjectReference r) { 
      return BubjetDebug.getObject(debug_process,r);
    }
   protected BVariable getVariable(Object v) { 
      return BubjetDebug.getVariable(debug_process,v);
    }
   
}       // end of inner class BMirror




/********************************************************************************/
/*                                                                              */
/*      BTarget -- VM or equivalent                                             */
/*                                                                              */
/********************************************************************************/

static class BTarget extends BMirror {
   
   VirtualMachine vm_ref;
   
   BTarget(BubjetDebugProcess bdp) {
      super(bdp);
      vm_ref = bdp.getVirtualMachine();
    }
   
   String geUniqueId()          { return String.valueOf(vm_ref.hashCode()); }
   boolean isTerminated()       { return !vm_ref.process().isAlive();  }
   
}       // end of inner class BTarget




/********************************************************************************/
/*                                                                              */
/*      BValue -- value reference                                               */
/*                                                                              */
/********************************************************************************/

static abstract class BValue extends BMirror {
   
   BValue(BubjetDebugProcess bdp) {
      super(bdp);
    }
   
   
   abstract BType getValueType();
   abstract Value getValueRef();
   
   String getText() { 
      if (getValueRef() == null) return "null";
      return String.valueOf(getValueRef()); 
   }
   
   @Override public String toString() {
      return getText();
    }
   
}       // end of inner class BValue



/********************************************************************************/
/*                                                                              */
/*      BObject -- object reference                                             */
/*                                                                              */
/********************************************************************************/

static class BObject extends BValue {
   
   private ObjectReference object_ref;
   
   BObject(BubjetDebugProcess bdp,ObjectReference or) {
      super(bdp);
      object_ref = or;
    }
   
   ObjectReference getObjectRef()       { return object_ref; }
   Value getValueRef()                  { return object_ref; }
   
   BType getValueType()                 { return getType(object_ref.referenceType()); }
   
   String getUniqueId() { 
      return String.valueOf(object_ref.uniqueID()); 
    }
   
   BValue getFieldValue(String fldnm) {
      Field fld = object_ref.referenceType().fieldByName(fldnm);
      if (fld == null) return null;
      Value v = object_ref.getValue(fld);
      return getValue(v);
    }
   
   
   Map<BField,BValue> getFieldValues(List<BField> flds) {
      Map<BField,BValue> rslt = new HashMap<>();
      for (BField bf : flds) {
         rslt.put(bf,getValue(object_ref.getValue(bf.getFieldRef())));
       }
      return rslt;
    }
   
   Map<BField,BValue> getFieldValues() {
      return getFieldValues(getValueType().getFields());
    }
   
   BValue invokeMethod(BMethod bm,List<BValue> bargs,BThread bt) { 
      bt.saveFrames();
      Method m = bm.getMethodRef();
      List<Value> args = new ArrayList<>();
      if (bargs != null) {
         for (BValue bv : bargs) args.add(bv.getValueRef());
       }
      ThreadReference tr = bt.getThreadRef();
      ObjectReference or = getObjectRef();
      try {
         Value rslt = or.invokeMethod(tr,m,args,
               ObjectReference.INVOKE_SINGLE_THREADED);
         BValue bv = getValue(rslt);
         return bv;
       }
      catch (Throwable t) {
         BubjetLog.logE("Exception during method invocation",t);
       }
      return null;
    }
   
   
}       // end of inner class BObject



/********************************************************************************/
/*                                                                              */
/*      BThread -- representation of a thread                                   */
/*                                                                              */
/********************************************************************************/

static class BThread extends BObject {
   
   private Map<String,Integer> saved_frames;
   
   BThread(BubjetDebugProcess bdp,ThreadReference t) {
      super(bdp,t);
      BubjetLog.logD("Create BTHREAD for " + t + " " + t.uniqueID() + " " +
            t.name() + " " + t.isSuspended());
      saved_frames = new HashMap<>();
    }
   
   
   
   ThreadReference getThreadRef()  { return (ThreadReference) getObjectRef(); }
   String getName()             { return getThreadRef().name(); }
   String getGroupName()        { return getThreadRef().threadGroup().name(); }
   boolean isSuspended()        { return getThreadRef().isSuspended(); }
   boolean isAtBreakpoint()     { return getThreadRef().isAtBreakpoint(); }
   boolean isTerminated() {
      return getThreadRef().status() == ThreadReference.THREAD_STATUS_ZOMBIE;
    }
   
   int getFrameCount() {
      try {
         return getThreadRef().frameCount(); 
       }
      catch (IncompatibleThreadStateException e) {
         return 0;
       }
    }
   
   List<BStackFrame> getFrames()        { return getFrames(null); }
      
   List<BStackFrame> getFrames(String match) {
      List<BStackFrame> rslt = new ArrayList<>();
      if (isSuspended()) return null;
      try {
         int ct = 0;
         for (StackFrame frm : getThreadRef().frames()) {
            BStackFrame bsf = getStackFrame(frm);
            if (matchFrame(bsf,match,ct++)) rslt.add(bsf);
          }
       }
      catch (IncompatibleThreadStateException e) { 
         return null;
       }
      
      return rslt;
    }
   
   BStackFrame getFrame(String match) {
      try {
         int ct = 0;
         for (StackFrame frm : getThreadRef().frames()) {
            BStackFrame bsf = getStackFrame(frm);
            if (matchFrame(bsf,match,ct++)) return bsf;
          }
       }
      catch (IncompatibleThreadStateException e) { }
      return null;
    }
   
   BStackFrame getTopFrame() {
      try {
         if (getThreadRef().frameCount() == 0) return null;
         StackFrame frm = getThreadRef().frame(0);
         return getStackFrame(frm);
       }
      catch (IncompatibleThreadStateException e) { }
      return null;
    }
   
   
   
   List<BObject> getOwnedMonitors() {
      List<BObject> rslt = new ArrayList<>();
      try {
         for (ObjectReference or : getThreadRef().ownedMonitors()) {
            rslt.add(getObject(or));
          }
         return rslt;
       }
      catch (IncompatibleThreadStateException e) { 
         return null;
       }
    }
   
   BValue invokeStaticMethod(String cls,String method,String sign,List<BValue> args) {
      BType bt = getType(cls);
      if (bt == null) return null;
      return bt.invokeStaticMethod(method,sign,args,this);
    }
   
   BValue invokeMethod(String method,String sign,BObject arg0,List<BValue> args) {
      if (arg0 == null) return null;
      BType bt = arg0.getValueType();
      if (bt == null) return null;
      BMethod bm = bt.getMethodByName(method,sign);
      return arg0.invokeMethod(bm,args,this);
   }
   
   void resume() { 
      getThreadRef().resume();  
    }
   
   void suspend() { 
      getThreadRef().suspend(); 
    }
   
   void popFrames(BStackFrame bsf) {
      try {
         StackFrame sf = bsf.getFrameRef();
         getThreadRef().popFrames(sf);
       }
      catch (IncompatibleThreadStateException e) { }
    }
   
   boolean match(String name) {
      if (name == null) return true;
      if (name.equals(getUniqueId())) return true;
      if (name.equals(getName())) return true;
      if (name.equals("*")) return true;
      
      return false;
    }
   
   private boolean matchFrame(BStackFrame sf,String match,int ct) {
      if (match == null) return true;
      Integer oct = saved_frames.get(match);
      if (oct != null && ct == oct) return true;
      if (sf.match(match)) return true;
      return false;
    }
   
   void saveFrames() {
      int ct = 0;
      try {
         for (StackFrame frm : getThreadRef().frames()) {
            BubjetLog.logD("Save Frame " + getUniqueId() + " " + frm.hashCode() + " " + ct);
            saved_frames.put(String.valueOf(frm.hashCode()),ct++);
          }
       }
      catch (IncompatibleThreadStateException e) { }
    }
   
   void freeFrames() {
      BubjetLog.logD("Free Saved Frames for " + getUniqueId());
      saved_frames.clear();
    }

   
}       // end of inner class BThread



static class BStackFrame extends BMirror {
   
   private StackFrame frame_ref;
   
   BStackFrame(BubjetDebugProcess proc,StackFrame frm) {
      super(proc);
      frame_ref = frm;
      BubjetLog.logD("Create BSTACKFRAME " + frm + " " + frm.hashCode() + " " +
            frm.location() + " " + frm.thread() + " " + frm.thread().uniqueID());
            
    }
   
   BMethod getMethod() {
      return getMethod(frame_ref.location().method()); 
    }
   
   private StackFrame getFrameRef()     { return frame_ref; }
   
   String getMethodName()       { return frame_ref.location().method().name(); }
   String getUniqueId()         { return String.valueOf(frame_ref.hashCode()); }
   String getClassName()        { return frame_ref.location().declaringType().name(); }
   String getFullName() {
      return getClassName() + "." + getMethodName(); 
    }
   int getLineNumber()          { return frame_ref.location().lineNumber(); } 
   
   String getSourcePath() { 
      try {
         return frame_ref.location().sourcePath(); 
       }
      catch (AbsentInformationException e) { }
      return null;
    }
   
   File getSourceFile(boolean all) {
      try {
         String path = frame_ref.location().sourcePath();
         BubjetApplicationService appser = BubjetBundle.getAppService();
         BubjetProjectManager pm = appser.getProjectManager();
         String pnm = pm.findSourceFile(debug_process.getProject(),path,all);
         if (pnm != null) return new File(pnm);
       }
      catch (AbsentInformationException e) { }
      return null;
    }
   
   List<BVariable> getVariables() {
      List<BVariable> rslt = new ArrayList<>();
      try {
         for (LocalVariable lv : frame_ref.visibleVariables()) {
            rslt.add(getVariable(lv));
          }
         if (!getMethod().isStatic() && !getMethod().isNative()) {
            rslt.add(getVariable(frame_ref.thisObject()));
          }
         return rslt;
       }
      catch (AbsentInformationException e) { 
         return null;
       }
    }
   
   BValue getVariableValue(BVariable v) {
      Value val = null;
      BLocal lcl = v.getLocal();
      BThis ths = v.getThis();
      if (lcl != null) {
         val = frame_ref.getValue(lcl.getLocalRef());
       }
      else if (ths != null) {
         val = frame_ref.thisObject();
       }
      else return null;
      return getValue(val);
    }
   
   BValue getThisValue() {
      return getValue(frame_ref.thisObject());
    }
   
   BValue getVariableValue(String name) {
      if (name == null) return null;
      if (name.equals("this")) return getThisValue();
      try {
         LocalVariable lv = frame_ref.visibleVariableByName(name);
         if (lv != null) {
            return getValue(frame_ref.getValue(lv));
          }
       }
      catch (AbsentInformationException e) { }
      
      return null;
    }
   
   BVariable getStackVariable(String name) {
      if (name.equals("this")) {
         return getVariable(frame_ref.thisObject());
       }
      else {
         try {
            return getVariable(frame_ref.visibleVariableByName(name));
          }
         catch (AbsentInformationException e) { }
       }
      
      return null;
   }
   
   private boolean match(String name) {
      if (name == null) return true;
      if (name.equals("*")) return true;
      if (name.equals(getUniqueId())) return true;
      
      return false;
    }
   
}       // end of inner class BStackFrame



/********************************************************************************/
/*                                                                              */
/*      BArray -- representation of an array                                    */
/*                                                                              */
/********************************************************************************/

static class BArray extends BObject {
   
   BArray(BubjetDebugProcess bdp,ArrayReference a) {
      super(bdp,a);
    }
   
   ArrayReference getArrayRef()    { return (ArrayReference) getObjectRef(); }
   int getLength()              { return getArrayRef().length(); }
   BValue getIndexValue(int idx) {
      Value v = getArrayRef().getValue(idx);
      return getValue(v);
    }
   
}       // end of inner class BArray 



/********************************************************************************/
/*                                                                              */
/*      BString -- representation of a string                                   */
/*                                                                              */
/********************************************************************************/

static class BString extends BObject {
   
   BString(BubjetDebugProcess bdp,StringReference s) {
      super(bdp,s);
    }
   
   
   
   String getString()           { return ((StringReference) getObjectRef()).value(); }
   
}       // end of inner class BString




/********************************************************************************/
/*                                                                              */
/*      BClass -- representation of a class                                     */
/*                                                                              */
/********************************************************************************/

static class BClass extends BObject {
   
   BClass(BubjetDebugProcess bpd,ClassObjectReference c) {
      super(bpd,c);
    }
   
   BType getReflectedType() {
      return getType(((ClassObjectReference) getObjectRef()).reflectedType());
    }
   
}       // end of inner class BClass


/********************************************************************************/
/*                                                                              */
/*      Null values                                                             */
/*                                                                              */
/********************************************************************************/

static class BNull extends BValue {
   
   BNull(BubjetDebugProcess bdp) {
      super(bdp);
    }
   
   BType getValueType()              { return null; }
   Value getValueRef()             { return null; }
   
}       // end of inner class BNull




/********************************************************************************/
/*                                                                              */
/*      Primitive Values                                                        */
/*                                                                              */
/********************************************************************************/

static class BPrimitive extends BValue {
   
   private PrimitiveValue primitive_value;
   
   BPrimitive(BubjetDebugProcess bdp,PrimitiveValue pv) {
      super(bdp);
      primitive_value = pv;
    }
   
   BType getValueType() {
      return getType(primitive_value.type());
    }
   Value getValueRef()          { return primitive_value; }
   
   boolean booleanValue()       { return primitive_value.booleanValue(); }
   byte byteValue()             { return primitive_value.byteValue(); }
   char charValue()             { return primitive_value.charValue(); }
   short shortValue()           { return primitive_value.shortValue(); }
   int intValue()               { return primitive_value.intValue(); }
   long longValue()             { return primitive_value.longValue(); }
   float floatValue()           { return primitive_value.floatValue(); }
   double doubleValue()         { return primitive_value.doubleValue(); }
   
}       // end of inner class BPrimitive





/********************************************************************************/
/*                                                                              */
/*      BType -- type reference                                                 */
/*                                                                              */
/********************************************************************************/

static class BType extends BMirror {
   
   private Type type_ref;
   
   BType(BubjetDebugProcess bdp,Type t) {
      super(bdp);
      type_ref = t;
    }
   
   String getSignature()                        { return type_ref.signature(); }
   String getName()                             { return type_ref.name(); }
   
   List<BField> getFields() {
      if (type_ref instanceof ClassType) {
         ClassType ct = (ClassType) type_ref;
         List<BField> rslt = new ArrayList<>();
         for (Field f : ct.fields()) {
            rslt.add(getField(f));
          }
         return rslt;
       }
      return null;
    }
   
   BMethod getMethodByName(String method,String sign) {
      if (type_ref instanceof ClassType) {
         ClassType ct = (ClassType) type_ref;
         Method m = ct.concreteMethodByName(method,sign);
         return getMethod(m);
       }
      return null;
    }
   
   BValue invokeStaticMethod(String method,String sign,List<BValue> args,BThread thread) {
      BValue rslt = invokeStaticMethod(getMethodByName(method,sign),args,thread);
      return rslt;
    }
   
   
   
   BValue invokeStaticMethod(BMethod bm,List<BValue> bargs,BThread bt) {
      if (type_ref instanceof ClassType && bm != null && bt != null) {
         bt.saveFrames();
         ClassType ct = (ClassType) type_ref;
         Method m = bm.getMethodRef();
         List<Value> args = new ArrayList<>();
         for (BValue bv : bargs) args.add(bv.getValueRef());
         ThreadReference tr = bt.getThreadRef();
         try {
            Value rslt = ct.invokeMethod(tr,m,args,ClassType.INVOKE_SINGLE_THREADED);
            BValue bv = getValue(rslt);
            return bv;
          }
         catch (Throwable t) {
            BubjetLog.logE("Exception during invoke method",t);
          }
       }
      return null;
    }
   
}       // end of inner class BType




/********************************************************************************/
/*                                                                              */
/*      BVariable -- fields, this, local variables                              */
/*                                                                              */
/********************************************************************************/

static abstract class BVariable extends BMirror {
   
   BVariable(BubjetDebugProcess bdp) {
      super(bdp);
    }
   
   abstract BType getType();
   abstract String getName();
   
   BField getField()                    { return null; }
   BLocal getLocal()                    { return null; }
   BThis getThis()                      { return null; }
   
   @Override public String toString()   { return getName(); }
   
}       // end of inner class BVariable



static class BField extends BVariable {
   
   private Field field_ref;
   
   BField(BubjetDebugProcess bdp,Field f) {
      super(bdp);
      field_ref = f;
    }
   
   Field getFieldRef()                  { return field_ref; }
   
   BType getType() {
      try {
         return getType(field_ref.type());
       }
      catch (Exception e) { }
      return null;
    }
   
   BField getField()                    { return this; }
   
   boolean isStatic()                   { return field_ref.isStatic(); }
   String getName()                     { return field_ref.name(); }
   String getSignature()                { return field_ref.signature(); }
   String getGenericSignature()         { return field_ref.genericSignature(); }
   boolean isSynthetic()                { return field_ref.isSynthetic(); }
   BType getDeclaringType() {
      return getType(field_ref.declaringType());
    }
   
}       // end of inner class BField



static class BThis extends BVariable {
   
   private ClassType class_type;
   
   BThis(BubjetDebugProcess bdp,ClassType ct) {
      super(bdp);
    }
   
   BThis getThis()                      { return this; }
   BType getType() {
      return getType(class_type);
    }
   String getName()                     { return "this"; }
   
}       // end of inner class BThis



static class BLocal extends BVariable {
   
   private LocalVariable var_ref;
   
   BLocal(BubjetDebugProcess bdp,LocalVariable lv) {
      super(bdp);
      var_ref = lv;
    }
   
   BLocal getLocal()                    { return this; }
   String getName()                     { return var_ref.name(); }
   LocalVariable getLocalRef()          { return var_ref; }
   
   BType getType() {
      try {
         return getType(var_ref.type());
       }
      catch (ClassNotLoadedException e) {
         return null;
       }
    }
   
}       // end of inner class BLocal



/********************************************************************************/
/*                                                                              */
/*      Reflective Information                                                  */
/*                                                                              */
/********************************************************************************/

static class BMethod extends BMirror {
   
   private Method method_ref;
   
   BMethod(BubjetDebugProcess bdp,Method m) {
      super(bdp);
      method_ref = m;
    }
   
   Method getMethodRef()                  { return method_ref; }
   
   boolean isStatic()                   { return method_ref.isStatic(); }
   String getName()                     { return method_ref.name(); }
   String getSignature()                { return method_ref.signature(); }
   String getGenericSignature()         { return method_ref.genericSignature(); }
   boolean isSynthetic()                { return method_ref.isSynthetic(); }
   boolean isConstructor()              { return method_ref.isConstructor(); }
   boolean isNative()                   { return method_ref.isNative(); }
   boolean isStaticInitializer()        { return method_ref.isStaticInitializer(); }
   boolean isVarArgs()                  { return method_ref.isVarArgs(); }
   
   List<String> getArgumentTypeNames() {
      return method_ref.argumentTypeNames();
    }
   
   BType getDeclaringType() {
      return getType(method_ref.declaringType());
    }
   
}       // end of inner class BMethod




}       // end of class BubjetDebug




/* end of BubjetDebug.java */

