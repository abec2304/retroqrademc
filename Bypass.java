// compile with -XDenableSunApiLintControl
// or -XDignore.symbol.file

// TODO: ONLY SEND PASSWORD TO SERVER WITH CORRECT PUBLIC KEY
// TODO: add skin changer
// TODO: reduce duration of Looking for update
// TODO: re-evaluate net/minecraft/launcher/updater/Argument serialization
// TODO: note that launcher's logger may intertwine messages with System.err.println
// TODO: match method descriptors, instead of just names
// NOTE: string w/ password is converted to bytes[] in peformPostRequest, should occur earlier
// TODO: add ?_t=Date.now() to version list request
// TODO: in createArgumentsSubstitutor, use gameDirectory with classpath to make relative URLs - duplicate constructClassPath method (MinecraftGameRunner)
// ^ doesn't seem to work
// TODO: maybe add Minecraft Forge versions to version list
// TODO: remove "Writing POST data to"
// TODO: download bootstrap jar if it doesn't exist
// TODO: switch to launcher.jar.lzma from https://launchermeta.mojang.com/mc/launcher.json and use SHA-1
// TODO: finish mockup for new splash page
// TODO: add status of mojang's auth servers, etc. - See net.minecraft.launcher.ui.bottombar.StatusPanelForm
// TODO: more stackmap frames?
// TODO: investigate https://bugs.mojang.com/projects/MCL/issues/MCL-7178
// old versions of Minecraft miss the correct arguments for authentication: ${auth_session} AKA --session token:${auth_access_token}:${auth_uuid}
// TODO: stop launcher re-serializing version JSONs - use ETag (MD5) to validate
// TODO: modify version name - see getVersionName in net.minecraft.launcher.LauncherConstants
// TODO: add downloadServerVersion to net.minecraft.launcher.updater.MinecraftVersionManager
// TODO: add "startServer" to profile manager
// TODO: add server jar download + fix issue with v. old versions not being downloadable (no, apparently old server jars just don't exist)

package pkg.mod3843;

import com.sun.org.apache.bcel.internal.classfile.Attribute;
import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.Code;
import com.sun.org.apache.bcel.internal.classfile.CodeException;
import com.sun.org.apache.bcel.internal.classfile.Constant;
import com.sun.org.apache.bcel.internal.classfile.ConstantClass;
import com.sun.org.apache.bcel.internal.classfile.ConstantCP;
import com.sun.org.apache.bcel.internal.classfile.ConstantMethodref;
import com.sun.org.apache.bcel.internal.classfile.ConstantNameAndType;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.classfile.ConstantString;
import com.sun.org.apache.bcel.internal.classfile.ConstantUtf8;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.LocalVariableTable;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Unknown;
import com.sun.org.apache.bcel.internal.generic.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.math.BigInteger;

import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

import java.nio.ByteBuffer;

import java.security.CodeSource;
import java.security.cert.Certificate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter.FilterBypass;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import javax.swing.text.StringContent;

public class Bypass extends URLClassLoader {

    /**
     * Uses relative forms of the specified URLs to construct a URLClassLoader.
     * Parent is the extension classloader. JAR of this class added to classpath.
     *
     * @param urls array of URLs
     */
    public Bypass(URL[] urls) {
        super(relativize(urls), extLoader());
        
        self();
    }

    /**
     * Entry point when ran from command line.
     * Creates instance of this class with bootstrap JAR on class path, then starts bootstrap.
     *
     * @param args array of arguments
     */
    public static void main(String[] args) {
        File bootstrapJar = new File(getUserDir(), "Minecraft.jar");
        
        if(!bootstrapJar.isFile()) {
            System.err.println("nothing to bootstrap");
            return;
        }
        
        Bypass cl;
        try {
            cl = new Bypass(new URL[]{bootstrapJar.toURI().toURL()});
        } catch(MalformedURLException mue) {
            mue.printStackTrace();
            return;
        }
        
        if("static".equals(getProperty("patch"))) {
            ArrayList<Closeable> toClose = new ArrayList<Closeable>();
            try {
                dump(args[0], toClose, cl);
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
            
            for(java.io.Closeable close : toClose) {
                try {
                    close.close();
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            
            return;
        }
        
        try {
            Class<?> clazz = Class.forName("net.minecraft.bootstrap.Bootstrap", false, cl);
            clazz.getMethod("main", String[].class).invoke(null, (Object)args);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /** maximum length of account password (in codepoints, since Mojang's encoding is unknown) */
    private static final int MAX_PASSWORD_LEN = 256;
    
    /** name of package for internal classes */
    private static final String MOD_PKG = "pkg.mod3843";
    
    /** name of transparent JButton class */
    private static final String TRANSPARENT_BUTTON_CLASS = MOD_PKG + ".TransparentButton";
    
    /** name of transparent JLabel class */
    private static final String TRANSPARENT_LABEL_CLASS = MOD_PKG + ".TransparentLabel";
    
    /** name of transparent JPanel class */
    private static final String TRANSPARENT_PANEL_CLASS = MOD_PKG + ".TransparentPanel";
    
    /** name of dirt TexturedPanel class */
    private static final String TEXTURED_PANEL_CLASS = MOD_PKG + ".DirtPanel";
    
    /** array of internal class names */
    private final String[] modClasses = {
        TRANSPARENT_BUTTON_CLASS,
        TRANSPARENT_LABEL_CLASS,
        TRANSPARENT_PANEL_CLASS,
        TEXTURED_PANEL_CLASS,
    };
    
    /** an empty JAR manifest */
    private final Manifest emptyManifest = new Manifest();
    
    /** Hashtable for caching Class instances */
    private final Hashtable<String, Class<?>> classCache = new Hashtable<String, Class<?>>();
    
    /** names of classes transformed with BCEL */
    private final HashSet<String> javaClasses = new HashSet<String>();
    
    /** array containing JVM arguments - first 32-bit, then 64 */
    private final String[] javaArgs;
    
    /** Generate Java arguments */
    {
        final String s = (""
            + "G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1Re"
            + "servePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize="
        );
        
        final int i = 1, j = 2;
        
        javaArgs = new String[] {
            "-Xmx" + 1*i + s + 16*i + "M",
            "-Xmx" + 1*j + s + 16*j + "M"
        };
    }
    
    private static String getUserDir() {
        try {
            return new File("").getCanonicalPath();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        
        return System.getProperty("user.dir");
    }
    
    /**
     * Makes URLs relative.
     * Prevents issues with exclamation points in paths.
     *
     * @param urls array of URLs
     * @return     array of relativized URLs
     */
    private static URL[] relativize(URL[] urls) {
        String userDir = getUserDir();
        // TODO: avoid File's toURI (?)
        URI baseURI = URI.create(new File(userDir).toURI().toString().substring(5));
        System.err.println("base URI[" + baseURI + "]");
        
        for(int i = 0; i < urls.length; i++) {
            URL url = urls[i];
            System.err.println("URL[" + url + "]");
            if(url == null || !"file".equals(url.getProtocol()))
                continue;
            
            try {
                URI uri = new URI(url.toString().substring(5));
                URI resolve = baseURI.resolve(uri);
                URI relative = baseURI.relativize(resolve);
                String s = relative.toString();
                if(relative == uri && s.indexOf("!") == -1)
                    continue;
                else if(relative != resolve)
                    s = "./".concat(s);
                
                urls[i] = new URL("file:".concat(s.replace("!", "%21")));
                // TODO: fix URL changed message for absolute URL of class in parent folder, etc.
                System.err.println("-->[" + urls[i] + "]");
            } catch(MalformedURLException mue) {
                mue.printStackTrace();
            } catch(URISyntaxException ute) {
                ute.printStackTrace();
            }
        }
        
        return urls;
    }
    
    /**
     * Gets extension ClassLoader.
     *
     * @return extension ClassLoader
     */
    private static ClassLoader extLoader() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        for(;;) {
            if(cl.getParent() == null)
                return cl;
            cl = cl.getParent();
        }
    }
    
    /** Adds JAR for this class to classpath. */
    void self() {
        String entry = getClass().getName().replace('.', '/').concat(".class");
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL url = cl.getResource(entry);
        
        String protocol;
        // TODO: print error
        if(url == null || !"jar".equals(protocol = url.getProtocol()))
            return;
        
        String s = url.toString();
        // remove "jar:" and "!/"
        s = s.substring(4, s.lastIndexOf(entry) - 2);

        // fix UNC paths
        if(s.length() > 7) {
            if(s.startsWith("file://") && s.charAt(7) != '/')
                s = "file://".concat(s.substring(5));
        }

        try {
            // make escaping consistent with userDir
            URL clean = new File(new URI(s)).toURI().toURL();
            addURL(relativize(new URL[]{clean})[0]);
        } catch(URISyntaxException ute) {
            ute.printStackTrace();
        } catch(MalformedURLException mue) {
            mue.printStackTrace();
        }
    }
    
    // get BCEL representation of class from bytecode
    private JavaClass getJavaClass(String name, Object[] source) {
        synchronized(javaClasses) {
            if(javaClasses.contains(name)) {
                System.err.println("singleton violation for/".concat(name));
                return null;
            }
            javaClasses.add(name);
        }
        
        URL url = getResource(name.replace('.', '/').concat(".class"));
        if(url == null)
            return null;
        
        URLConnection uc;
        InputStream is;
        try {
            is = (uc = url.openConnection()).getInputStream();
        } catch(IOException ioe) {
            return null;
        }
        
        if(uc instanceof JarURLConnection) {
            try {
                source[1] = ((JarURLConnection)(source[0] = uc)).getJarEntry();
            } catch(IOException ioe) {
            }
        }
        
        try {
            JavaClass jc = new ClassParser(is, name).parse();
            /*if(!name.endsWith("$Serializer"))
                jc.setMajor(51); ///*/
            return jc;
        } catch(IOException ioe) {
        } finally {
            try {
                is.close();
            } catch(IOException ion) {
            }
        }
        
        return null;
    }
    
    /**
     * Creates a Method instance with the specified arguments and clear the instruction list
     *
     * @param cp constant pool generator
     * @param s1      method name
     * @param s2      method signature
     * @param i       index of Code identifier
     * @param il      instruction list
     * @param acc     access flags
     * @param nStack  number of stack items
     * @param nLocals number of local variables
     * @return        method instance
     */
    private Method getMethod(ConstantPoolGen cp, String s1, String s2, int i, InstructionList il, int acc, int nStack, int nLocals) {
        int nameIndex = cp.addUtf8(s1);
        int descIndex = cp.addUtf8(s2);
        byte[] b = il.getByteCode();
        il.dispose();
        Code code = new Code(i, b.length, nStack, nLocals, b, null, null, cp.getConstantPool());
        return new Method(acc, nameIndex, descIndex, new Attribute[]{code}, cp.getConstantPool());
    }
    
    /**
     * Creates a public, non-abstract class extending JPanel, with two public constructors - LayoutManager and default.
     * isOpaque is overridden to return false.
     *
     * @param name class name
     * @return     class bytes
     */
    private byte[] createPanel(String name) {
        final int acc = 1;
        final String superName = "javax/swing/JPanel";
        final String initName = "<init>";
        final String initDesc = "(Ljava/awt/LayoutManager;)V";
        final String defaultInitDesc = "()V";
        
        ConstantPoolGen cpGen = new ConstantPoolGen();
        int thisClass = cpGen.addClass(name);
        int superClass = cpGen.addClass(superName);
        int iCode = cpGen.addUtf8("Code");
        InstructionList il = new InstructionList();
        
        int superRef = cpGen.addMethodref(superName, initName, initDesc);
        il.append(new ALOAD(0));
        il.append(new ALOAD(1));
        il.append(new INVOKESPECIAL(superRef));
        il.append(new RETURN());
        Method constructor = getMethod(cpGen, initName, initDesc, iCode, il, acc, 2, 2);
        
        superRef = cpGen.addMethodref(superName, initName, defaultInitDesc);
        il.append(new ALOAD(0));
        il.append(new INVOKESPECIAL(superRef));
        il.append(new RETURN());
        Method defaultConstructor = getMethod(cpGen, initName, defaultInitDesc, iCode, il, acc, 1, 1);
        
        il.append(new ICONST(0));
        il.append(new IRETURN());
        Method opaque = getMethod(cpGen, "isOpaque", "()Z", iCode, il, acc, 1, 1);
        
        Method[] methods = {constructor, defaultConstructor, opaque};
        
        JavaClass jc = new JavaClass(thisClass, superClass, name, 49, 0, acc, cpGen.getFinalConstantPool(), null, null, methods, new Attribute[0]);
        
        return jc.getBytes();
    }
    
    /**
     * Creates a public, non-abstract class extending TexturedPanel, with a default public constructor.
     * The parent constructor is invoked with the path to a tiling dirt graphic.
     *
     * @param name class name
     * @return     class bytes
     */
    private byte[] createPanel2(String name) {
        final int acc = 1;
        final String superName = "net/minecraft/launcher/ui/TexturedPanel";
        final String superDesc = "(Ljava/lang/String;)V";
        final String initName = "<init>";
        final String initDesc = "()V";
        
        ConstantPoolGen cpGen = new ConstantPoolGen();
        int thisClass = cpGen.addClass(name);
        int superClass = cpGen.addClass(superName);
        int iCode = cpGen.addUtf8("Code");
        InstructionList il = new InstructionList();
        
        int superRef = cpGen.addMethodref(superName, initName, superDesc);
        int dirt = cpGen.addString("/dirt.png");
        il.append(new ALOAD(0));
        il.append(new LDC(dirt));
        il.append(new INVOKESPECIAL(superRef));
        il.append(new RETURN());
        Method constructor = getMethod(cpGen, initName, initDesc, iCode, il, acc, 2, 2);
        
        Method[] methods = {constructor};
        
        JavaClass jc = new JavaClass(thisClass, superClass, name, 49, 0, acc, cpGen.getFinalConstantPool(), null, null, methods, new Attribute[0]);
        
        return jc.getBytes();
    }
    
    /**
     * Creates a public, non-abstract class extending JButton, with a public constructor taking String
     * isOpaque is overridden to return false.
     *
     * @param name class name
     * @return     class bytes
     */
    private byte[] createButton(String name) {
        final int acc = 1;
        final String superName = "javax/swing/JButton";
        final String superDesc = "(Ljava/lang/String;)V";
        final String initName = "<init>";
        
        ConstantPoolGen cpGen = new ConstantPoolGen();
        int thisClass = cpGen.addClass(name);
        int superClass = cpGen.addClass(superName);
        int iCode = cpGen.addUtf8("Code");
        InstructionList il = new InstructionList();
        
        int superRef = cpGen.addMethodref(superName, initName, superDesc);
        il.append(new ALOAD(0));
        il.append(new ALOAD(1));
        il.append(new INVOKESPECIAL(superRef));
        il.append(new RETURN());
        Method constructor = getMethod(cpGen, initName, superDesc, iCode, il, acc, 2, 2);
        
        il.append(new ICONST(0));
        il.append(new IRETURN());
        Method opaque = getMethod(cpGen, "isOpaque", "()Z", iCode, il, acc, 1, 1);
        
        Method[] methods = {constructor, opaque};
        
        JavaClass jc = new JavaClass(thisClass, superClass, name, 49, 0, acc, cpGen.getFinalConstantPool(), null, null, methods, new Attribute[0]);
        
        return jc.getBytes();
    }
    
    // create label class
    private byte[] createLabel(String name) {
        final int acc = 1;
        final String superName = "javax/swing/JLabel";
        final String initName = "<init>";
        final String initDesc = "(Ljava/lang/String;)V";
        final String initDescA = "(Ljava/lang/String;I)V";
        
        ConstantPoolGen cpGen = new ConstantPoolGen();
        int thisClass = cpGen.addClass(name);
        int superClass = cpGen.addClass(superName);
        int iCode = cpGen.addUtf8("Code");
        InstructionList il = new InstructionList();
        
        int superRef = cpGen.addMethodref(superName, initName, initDesc);
        il.append(new ALOAD(0));
        il.append(new ALOAD(1));
        il.append(new INVOKESPECIAL(superRef));
        il.append(new RETURN());
        Method constructor = getMethod(cpGen, initName, initDesc, iCode, il, acc, 2, 2);
        
        int superRefA = cpGen.addMethodref(superName, initName, initDescA);
        il.append(new ALOAD(0));
        il.append(new ALOAD(1));
        il.append(new ILOAD(2));
        il.append(new INVOKESPECIAL(superRefA));
        il.append(new RETURN());
        Method constructorA = getMethod(cpGen, initName, initDescA, iCode, il, acc, 3, 3);
        
        il.append(new ICONST(0));
        il.append(new IRETURN());
        Method opaque = getMethod(cpGen, "isOpaque", "()Z", iCode, il, acc, 1, 1);

        int whiteRef = cpGen.addFieldref("java/awt/Color", "WHITE", "Ljava/awt/Color;");
        il.append(new GETSTATIC(whiteRef));
        il.append(new ARETURN());
        Method foreground = getMethod(cpGen, "getForeground", "()Ljava/awt/Color;", iCode, il, acc, 1, 1);
        
        Method[] methods = {constructor, constructorA, opaque, foreground};
        
        JavaClass jc = new JavaClass(thisClass, superClass, name, 49, 0, acc, cpGen.getFinalConstantPool(), null, null, methods, new Attribute[0]);
        
        return jc.getBytes();
    }
    
    /**
     * Modifies the launcher class to not clean up "orphaned" versions.
     *
     * @param name   class name
     * @param object source information
     * @return       class bytes
     */
    private byte[] patchLauncher(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Method cov;
        Method[] methods = jc.getMethods();
        // scan for method
        c: {
            for(Method m : methods) {
                if("cleanupOrphanedVersions".equals(m.getName())) {
                    cov = m;
                    break c;
                }
            }
            return null;
        }
        
        // create constant pool generator
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        
        // scan for reference to contains
        Code code = cov.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        InstructionHandle insertion;
        int len1;
        c: {
            for(InstructionHandle ih : handles) {
                Instruction insn = ih.getInstruction();
                if(!(insn instanceof INVOKEINTERFACE))
                    continue;
                
                InvokeInstruction ii = (InvokeInstruction)insn;
                if(!"contains".equals(ii.getMethodName(cpGen)))
                    continue;
                
                insertion = ih.getPrev();
                len1 = ii.getLength();
                break c;
            }
            return null;
        }
        
        // remove existing call
        try {
            il.delete(insertion.getNext());
        } catch(TargetLostException tle) {
            return null;
        }
        
        // add reference to new method
        final String desc = "(Ljava/util/Set;Ljava/lang/Object;)Z";
        int index = cpGen.addMethodref(getClass().getName(), "setContains", desc);
        
        // calculate new instruction length
        Instruction slide = new INVOKESTATIC(index);
        int len2 = slide.getLength();
        
        // start instruction list with new method call
        InstructionList intercept = new InstructionList();
        intercept.append(slide);
        
        // TODO: avoid NOPs
        // pad difference in size with NOPs
        while(len1 > len2) {
            --len1;
            intercept.append(new NOP());
        }
        
        // add new instructions
        il.append(insertion, intercept);
        
        // update code
        code.setCode(il.getByteCode());
        
        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // fix issue preventing game being played offline
    private byte[] patchSerializer(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Method[] methods = jc.getMethods();
        Method ds;
        // scan for method
        c: {
            for(Method m : methods) {
                if("deserialize".equals(m.getName())) {
                    ds = m;
                    break c;
                }
            }
            return null;
        }
        
        Code code = ds.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        
        ConstantPool cp = jc.getConstantPool();
        ConstantPoolGen cpGen = new ConstantPoolGen(cp.getConstantPool());
        
        final byte CONST_STRING = 8;
        int consIndex;
        InstructionHandle prev;
        c: {
            for(InstructionHandle ih : handles) {
                Instruction instruction = ih.getInstruction();
                if(!(instruction instanceof LDC))
                    continue;
                    
                consIndex = ((LDC)instruction).getIndex();
                if(!(cp.getConstant(consIndex) instanceof ConstantString))
                    continue;
                if(!"rules".equals(cp.getConstantString(consIndex, CONST_STRING)))
                    continue;
                
                prev = ih.getPrev();
                break c;
            }
            return null;
        }
        
        // create instruction list for subroutine
        InstructionList ilnew = new InstructionList();
        
        // create prologue with initial instruction
        InstructionList prologue = new InstructionList();
        prologue.append(new ACONST_NULL());
        
        boolean patchBranch = true;
        boolean finishPrologue = true;
        InstructionHandle rubberband = null;
        
        for(InstructionHandle next = prev; next != null; next = next.getNext()) {
            Instruction nextIns = next.getInstruction();
            if(nextIns instanceof LDC && ((LDC)nextIns).getIndex() == consIndex) {
                // add reference with correct string constant
                int proper = cpGen.addString("compatibilityRules");
                ilnew.append(new LDC(proper));
            } else if(nextIns instanceof BranchInstruction) {
                if(patchBranch && nextIns instanceof IFEQ) {
                    // add branch with prologue code
                    IFEQ branchOrig = (IFEQ)nextIns;
                    rubberband = branchOrig.getTarget();
                    branchOrig.setTarget(ilnew.getStart());
                    ilnew.append(new IFEQ(prologue.getStart()));
                    patchBranch = false;
                    continue;
                }
                
                BranchInstruction branchCopy = (BranchInstruction)nextIns.copy();
                ilnew.append(branchCopy);
            } else {
                if(finishPrologue && nextIns instanceof ALOAD && rubberband != null) {
                    ALOAD aload = (ALOAD)nextIns;
                    if(aload.getIndex() == 3) {
                        InstructionHandle rulesHandle = next.getPrev();
                        Instruction rules = rulesHandle.getInstruction();
                        if(rules instanceof ALOAD) {
                            // finish writing prologue
                            prologue.append(new ASTORE(((ALOAD)rules).getIndex()));
                            prologue.append(new GOTO(rubberband));
                            finishPrologue = false;
                        }
                    }
                }
                
                // avoid modifying original instructions
                ilnew.append(nextIns.copy());
            }

            // end block at goto
            if(nextIns instanceof GOTO)
                break;
        }
        
        // require prologue to patch class
        if(finishPrologue)
            return null;
        
        il.append(ilnew);
        il.append(prologue);
        code.setCode(il.getByteCode());
        
        code.setAttributes(code.getAttributes());

        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
        
    }
    
    // insert filter for required files of versions
    // TODO: change, this is only used to determine which files to download
    private byte[] patchVersion(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Method grf;
        Method[] methods = jc.getMethods();
        c: {
            for(Method m : methods) {
                if("getRequiredFiles".equals(m.getName())) {
                    grf = m;
                    break c;
                }
            }
            return null;
        }
        
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        if(cpGen != null) return null; ///
        
        // add reference to new method
        final String desc = "(Ljava/util/Set;Ljava/lang/String;)Ljava/util/Set;";
        int index = cpGen.addMethodref(getClass().getName(), "filterRequiredFiles", desc);
        int idIndex = cpGen.addFieldref(name, "id", "Ljava/lang/String;");
        
        Code code = grf.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        c: {
            for(InstructionHandle ih : handles) {
                if(!(ih.getInstruction() instanceof ARETURN))
                    continue;
                
                // add call to new method
                InstructionList intercept = new InstructionList();
                intercept.append(new ALOAD(0));
                intercept.append(new GETFIELD(idIndex));
                intercept.append(new INVOKESTATIC(index));
                il.insert(ih, intercept);
                break c;
            }
            return null;
        }
        
        // update code
        code.setCode(il.getByteCode());
        
        // force method size to be recalculated
        code.setAttributes(code.getAttributes());
        
        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // intercept uncaught exceptions from threads
    private byte[] patchInner(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;

        // determine if correct anonymous inner class from interface
        String[] interfaceNames = jc.getInterfaceNames();
        final String uceName = Thread.UncaughtExceptionHandler.class.getName();
        c: {
            for(String inter : interfaceNames) {
                if(uceName.equals(inter)) {
                    break c;
                }
            }
            return null;
        }

        Method ue;
        Method[] methods = jc.getMethods();
        // scan for method
        c: {
            for(Method m : methods) {
                if("uncaughtException".equals(m.getName())) {
                    ue = m;
                    break c;
                }
            }
            return null;
        }
        
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        
        // add reference to new method
        final String desc = "(Ljava/lang/Thread;Ljava/lang/Throwable;)V";
        int index = cpGen.addMethodref(getClass().getName(), "uncaughtException", desc);
        
        Code code = ue.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        c: {
            for(InstructionHandle ih : handles) {
                if(!(ih.getInstruction() instanceof RETURN))
                    continue;
                
                // TODO: examine bytecode
                // add call to new method
                InstructionList extra = new InstructionList();
                extra.append(new ALOAD(1));
                extra.append(new ALOAD(2));
                extra.append(new INVOKESTATIC(index));
                il.insert(ih, extra);
                break c;
            }
            return null;
        }
        
        // update code
        code.setCode(il.getByteCode());
        
        // force method size to be recalculated
        code.setAttributes(code.getAttributes());
        
        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // blank password
    private byte[] patchRequest(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Method[] methods = jc.getMethods();
        Method init;
        // scan for constructor
        c: {
            for(Method m : methods) {
                if("<init>".equals(m.getName())) {
                    init = m;
                    break c;
                }
            }
            return null;
        }
        
        // create constant pool generator
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        
        // scan for reference to getAgent
        Code code = init.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        c: {
            for(InstructionHandle ih : handles) {
                Instruction insn = ih.getInstruction();
                if(!(insn instanceof INVOKEVIRTUAL))
                    continue;
                
                InvokeInstruction ii = (InvokeInstruction)insn;
                if(!"getAgent".equals(ii.getMethodName(cpGen)))
                    continue;
                
                // NOTE: class name here uses /, not .
                int index = cpGen.addMethodref(getClassName(cpGen, ii), "setPassword", "(Ljava/lang/String;)V");
                
                InstructionList blank = new InstructionList();
                blank.append(ih.getPrev().getInstruction());
                blank.append(new LDC(cpGen.addString(" ")));
                blank.append(new INVOKEVIRTUAL(index));
                
                // write after agent stored
                il.append(ih.getNext(), blank);

                break c;
            }
            return null;
        }
        
        // update code
        code.setCode(il.getByteCode());
        
        // force method size to be recalculated
        code.setAttributes(code.getAttributes());
        
        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // replace class loader with this class
    private byte[] patchBootstrap(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;

        ConstantPool cp = jc.getConstantPool();
        Constant[] constants = cp.getConstantPool();

        // create generator for adding new constant pool entries
        ConstantPoolGen cpGen = new ConstantPoolGen(constants);
        
        // replace URLClassLoader reference with reference to this class
        // TODO: remap instead.
        c: {
            for(Constant cons : constants) {
                if(cons instanceof ConstantClass) {
                    ConstantClass cc = (ConstantClass)cons;
                    if("java/net/URLClassLoader".equals(cc.getBytes(cp))) {
                        int index = cpGen.addUtf8(getClass().getName().replace('.', '/'));
                        cc.setNameIndex(index);
                        break c;
                    }
                }
            }
            return null;
        }

        final String productName = "Minecraft";
        final String internalBranding = productName + ".Bootstrap";
        final String externalBranding = productName + " Launcher";
        
        for(Constant cons : constants) {
            if(!(cons instanceof ConstantString))
                continue;
            
            String s = ((ConstantString)cons).getBytes(cp);
            if(s.contains(internalBranding)) {
            } else if(s.equals(externalBranding)) {
            } else {
                continue;
            }
            
            int index = cpGen.addUtf8(s.replace(productName, "retroqrade"));
            ((ConstantString)cons).setStringIndex(index);
        }
        
        Method[] methods = jc.getMethods();
        Method sl;
        
        // scan for startLauncher method
        c: {
            for(Method method : methods) {
                if(!"startLauncher".equals(method.getName()))
                    continue;
                sl = method;
                break c;
            }
            return null;
        }

        // NOTE: BCEL in Java <9 fails to properly decode ldc_w, e.g. when followed by aastore

        Code code = sl.getCode();
        
        // get exception table
        CodeException[] ce = code.getExceptionTable();
        if(ce == null || ce.length < 1)
            return null;

        // get local variable table
        LocalVariableTable lvt = code.getLocalVariableTable();
        if(lvt == null || lvt.getTableLength() < 1)
            return null;
        
        // get byte code index for first exception handler
        int catcherIndex = ce[0].getHandlerPC();
        
        // get byte code index for first local variable
        int variableIndex = lvt.getLocalVariableTable()[0].getStartPC();

        byte[] rawCode = code.getCode();
        
        // get code before first local variable
        byte[] epilogue = new byte[variableIndex];
        System.arraycopy(rawCode, 0, epilogue, 0, epilogue.length);
        InstructionList ilPre = new InstructionList(epilogue);
        
        // get code from exception handler onwards
        byte[] prologue = new byte[rawCode.length - catcherIndex];
        System.arraycopy(rawCode, catcherIndex, prologue, 0, prologue.length);
        InstructionList ilPost = new InstructionList(prologue);
        
        final String appendName = "append";
        final String appendDesc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
        final String appendDescNew = "(Ljava/lang/StringBuilder;Ljava/lang/Object;)Ljava/lang/StringBuilder;";
        
        final String thisClass = getClass().getName().replace('.', '/');
        
        // print Throwable
        c: {
            for(InstructionHandle ih : ilPost.getInstructionHandles()) {
                if(!(ih.getInstruction() instanceof INVOKEVIRTUAL)) continue;

                InvokeInstruction ii = (InvokeInstruction)ih.getInstruction();
                
                String className = getClassName(cpGen, ii);
                if(!className.equals("java/lang/StringBuilder"))
                    continue;
                
                if(!appendName.equals(ii.getName(cpGen)) || !appendDesc.equals(ii.getSignature(cpGen)))
                    continue;

                int index = cpGen.addMethodref(thisClass, appendName, appendDescNew);
                ih.setInstruction(new INVOKESTATIC(index));
                
                break c;
            }
            return null;
        }

        final String uriName = "toURI";
        final String uriDesc = "()Ljava/net/URI;";
        final String uriDescNew = "(Ljava/io/File;)Ljava/net/URI;";
        
        // intercept toURI
        c: {
            for(InstructionHandle ih : ilPre.getInstructionHandles()) {
                if(!(ih.getInstruction() instanceof INVOKEVIRTUAL)) continue;

                InvokeInstruction ii = (InvokeInstruction)ih.getInstruction();
                
                String className = getClassName(cpGen, ii);
                if(!className.equals("java/io/File"))
                    continue;
                
                if(!uriName.equals(ii.getName(cpGen)) || !uriDesc.equals(ii.getSignature(cpGen)))
                    continue;

                int index = cpGen.addMethodref(thisClass, uriName, uriDescNew);
                ih.setInstruction(new INVOKESTATIC(index));
                
                break c;
            }
            return null;
        }
        
        // merge code
        byte[] presequence = ilPre.getByteCode();
        System.arraycopy(presequence, 0, rawCode, 0, presequence.length);
        byte[] resequence = ilPost.getByteCode();
        System.arraycopy(resequence, 0, rawCode, catcherIndex, resequence.length);

        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());

        return jc.getBytes();
    }

    // add new JVM args
    private byte[] patchRunner(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Constant[] constants = jc.getConstantPool().getConstantPool();
        boolean b = false;
        c: {
            for(int i = 0; i < constants.length; i++) {
                if(!(constants[i] instanceof ConstantUtf8))
                    continue;
                
                ConstantUtf8 consu = (ConstantUtf8)constants[i];
                String s = consu.getBytes();
                if(s.startsWith("-Xmx1G ")) {
                    constants[i] = new ConstantUtf8(javaArgs[1]);
                } else if(s.startsWith("-Xmx512M ")) {
                    constants[i] = new ConstantUtf8(javaArgs[0]);
                } else {
                    continue;
                }
                
                if(b & (b = true))
                    break c;
            }
            return null;
        }
        
        return jc.getBytes();
    }
    
    // TODO: move
    private static void remapInstantiation(Code code, ConstantPoolGen cpGen, Map<String, String> map) {
        InstructionList il = new InstructionList(code.getCode());
        
        for(InstructionHandle ih : il.getInstructionHandles()) {
            Instruction insn = ih.getInstruction();
            
            if(insn instanceof NEW) {
                NEW neww = (NEW)insn;
                String s = map.get(neww.getLoadClassType(cpGen).getClassName());
                if(s == null)
                    continue;
                int index = cpGen.addClass(s);
                neww.setIndex(index);
            } else if(insn instanceof INVOKESPECIAL) {
                InvokeInstruction ii = (InvokeInstruction)insn;
                String s = map.get(ii.getLoadClassType(cpGen).getClassName());
                if(s == null)
                    continue;
                String rName = ii.getName(cpGen);
                if(!"<init>".equals(rName))
                    continue;
                int index = cpGen.addMethodref(s, rName, ii.getSignature(cpGen));
                ii.setIndex(index);
            }
        }
        
        code.setCode(il.getByteCode());
    }
    
    private byte[] patchTabPanel(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool());
        
        Method[] methods = jc.getMethods();
        Method init;
        // scan for method
        c: {
            for(Method m : methods) {
                if("createInterface".equals(m.getName())) {
                    init = m;
                    break c;
                }
            }
            return null;
        }
        
        final String desc = "(Ljavax/swing/JTabbedPane;Ljava/lang/String;Ljavax/swing/JComponent;)V";
        
        Code code = init.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        c: {
            for(InstructionHandle ih : handles) {
                Instruction insn = ih.getInstruction();
                if(!(insn instanceof INVOKEVIRTUAL))
                    continue;
                
                InvokeInstruction ii = (InvokeInstruction)insn;
                String methodName = ii.getMethodName(cpGen);
                if(!"addTab".equals(methodName))
                    continue;
                
                int index = cpGen.addMethodref(getClass().getName(), methodName, desc);
                ih.setInstruction(new INVOKESTATIC(index));
                
                ///break c;
            }
            ///return null;
        }
        
        // update code
        code.setCode(il.getByteCode());
        
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // translucency
    // TODO: add intelligent mapping for super call in initialization
    private byte[] patchBar(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("javax.swing.JPanel", TRANSPARENT_PANEL_CLASS);
        map.put("javax.swing.JLabel", TRANSPARENT_LABEL_CLASS);
        map.put("javax.swing.JButton", TRANSPARENT_BUTTON_CLASS);
        
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool());
        
        // patch methods
        for(Method m : jc.getMethods()) {
            String mName = m.getName();
            if("<init>".equals(mName)) {
            } else if("wrapSidePanel".equals(mName)) {
            } else if("createInterface".equals(mName)) {
            } else {
                continue;
            }
            
            remapInstantiation(m.getCode(), cpGen, map);
        }
        
        if(name.endsWith(".BottomBarPanel")) {
            map.clear();
            map.put("javax.swing.JPanel", TEXTURED_PANEL_CLASS);
            
            for(Method m : jc.getMethods()) {
                String mName = m.getName();
                if("<init>".equals(mName)) {
                    Code code = m.getCode();
                    InstructionList il = new InstructionList(code.getCode());
                    for(InstructionHandle ih : il.getInstructionHandles()) {
                        Instruction insn = ih.getInstruction();
                        // TODO: make safe
                        if(insn instanceof INVOKESPECIAL) {
                            InvokeInstruction ii = (InvokeInstruction)insn;
                            String cls = map.values().iterator().next();
                            int index = cpGen.addMethodref(cls, ii.getName(cpGen), ii.getSignature(cpGen));
                            ii.setIndex(index);
                            break;
                        }
                    }
                    code.setCode(il.getByteCode());
                }
            }
        }
        
        // change super class
        String mapped = map.get(jc.getSuperclassName());
        if(mapped != null) {
            int index = cpGen.addClass(mapped);
            jc.setSuperclassNameIndex(index);
        }
        
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // texture panel and prevent old launcher message
    private byte[] patchLauncherPanel(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        // create constant pool generator
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        
        // get methods
        Method[] methods = jc.getMethods();
        
        Method cli;
        // scan for method
        c: {
            for(Method m : methods) {
                if("createLauncherInterface".equals(m.getName())) {
                    cli = m;
                    break c;
                }
            }
            return null;
        }
        
        Code code = cli.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        c: {
            for(InstructionHandle ih : handles) {
                // search for if_icmpge instruction
                if(!(ih.getInstruction() instanceof IF_ICMPGE))
                    continue;
                
                o: {
                    Instruction prevInst = ih.getPrev().getInstruction();
                    if(prevInst instanceof BIPUSH) {
                        if(((BIPUSH)prevInst).getValue().intValue() == 100)
                            break o;
                    }
                    continue;
                }
                
                BranchInstruction bi = (BranchInstruction)ih.getInstruction();
                
                // invert bootstrap version check
                ih.setInstruction(new IF_ICMPLT(bi.getTarget()));
                break c;
            }
            return null;
        }
        
        c: {
            final String origName = "setHorizontalAlignment";
            final String origDesc = "(I)V";
            final String newDesc = "(Ljavax/swing/JLabel;I)V";
            
            for(InstructionHandle ih : handles) {
                if(!(ih.getInstruction() instanceof INVOKEVIRTUAL)) continue;

                InvokeInstruction ii = (InvokeInstruction)ih.getInstruction();
                
                String className = getClassName(cpGen, ii);
                if(!className.equals("javax/swing/JLabel"))
                    continue;
                
                if(!origName.equals(ii.getName(cpGen)) || !origDesc.equals(ii.getSignature(cpGen)))
                    continue;

                // alter instance of JLabel
                int index = cpGen.addMethodref(getClass().getName(), origName, newDesc);
                ih.setInstruction(new INVOKESTATIC(index));
                break c;
            }
            return null;
        }
        
        c: {
            final String origName = "setLayout";
            final String origDesc = "(Ljava/awt/LayoutManager;)V";
            final String newDesc = "(Ljavax/swing/JPanel;Ljava/awt/LayoutManager;)V";
            
            for(InstructionHandle ih : handles) {
                if(!(ih.getInstruction() instanceof INVOKEVIRTUAL)) continue;

                InvokeInstruction ii = (InvokeInstruction)ih.getInstruction();
                
                String className = getClassName(cpGen, ii);
                if(!className.equals("javax/swing/JPanel"))
                    continue;
                
                if(!origName.equals(ii.getName(cpGen)) || !origDesc.equals(ii.getSignature(cpGen)))
                    continue;

                // alter instance of JLabel
                int index = cpGen.addMethodref(getClass().getName(), origName, newDesc);
                ih.setInstruction(new INVOKESTATIC(index));
                break c;
            }
            return null;
        }
        
        int transPanelClass = cpGen.addClass(TRANSPARENT_PANEL_CLASS);
        final String normalName = JPanel.class.getName();
        
        // make panels transparent
        for(InstructionHandle ih : handles) {
            Instruction insn = ih.getInstruction();
            if(!(insn instanceof NEW)) {
                if(insn instanceof INVOKESPECIAL) {
                    InvokeInstruction ii = (InvokeInstruction)insn;
                    // TODO: cleanup ...
                    if(!getClassName(cpGen, ii).equals(normalName.replace('.', '/')))
                        continue;
                    int transRef = cpGen.addMethodref(TRANSPARENT_PANEL_CLASS, ii.getName(cpGen), ii.getSignature(cpGen));
                    ii.setIndex(transRef);
                }
                continue;
            }
            
            NEW neww = (NEW)insn;
            // TODO: can also use getLoadClassType(cpGen).getClassName()
            if(!neww.getType(cpGen).getSignature().equals("L" + normalName.replace('.', '/') + ";"))
                continue;
            
            neww.setIndex(transPanelClass);
        }
        
        code.setCode(il.getByteCode());
        
        // add textured panel reference
        final String texturedName = TEXTURED_PANEL_CLASS;
        int texturedClass = cpGen.addClass(texturedName);
        
        c: {
            if(normalName.equals(jc.getSuperclassName())) {
                jc.setSuperclassNameIndex(texturedClass);
                break c;
            }
            return null;
        }
        
        Method init;
        // scan for constructor
        c: {
            for(Method m : methods) {
                if("<init>".equals(m.getName())) {
                    init = m;
                    break c;
                }
            }
            return null;
        }
        
        // fix super call
        // TODO: change to remap code..
        Code codeInit = init.getCode();
        InstructionList ilInit = new InstructionList(codeInit.getCode());
        c: {
            for(InstructionHandle ih : ilInit.getInstructionHandles()) {
                Instruction insn = ih.getInstruction();
                if(!(insn instanceof INVOKESPECIAL))
                    continue;
                
                InvokeInstruction ii = (InvokeInstruction)insn;
                
                int index = cpGen.addMethodref(texturedName, ii.getName(cpGen), ii.getSignature(cpGen));
                
                ii.setIndex(index);
                
                break c;
            }
            return null;
        }
        
        // update code
        codeInit.setCode(ilInit.getByteCode());
        
        // force method size to be recalculated
        ///codeInit.setAttributes(codeInit.getAttributes());
        
        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // replace default JVM args
    private byte[] patchProfile(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Constant[] constants = jc.getConstantPool().getConstantPool();
        c: {
            for(int i = 0; i < constants.length; i++) {
                if(!(constants[i] instanceof ConstantUtf8))
                    continue;
                
                ConstantUtf8 consu = (ConstantUtf8)constants[i];
                if(consu.getBytes().startsWith("-Xmx1G ")) {
                    constants[i] = new ConstantUtf8(javaArgs[1]);
                    break c;
                }
            }
            return null;
        }
        
        Method[] methods = jc.getMethods();
        Method ci;
        // scan for method
        c: {
            for(Method m : methods) {
                if("createInterface".equals(m.getName())) {
                    ci = m;
                    break c;
                }
            }
            return null;
        }
        
        Code code = ci.getCode();
        InstructionList il = new InstructionList(code.getCode());
        c: {
            // makes javaArgsField not resize pane
            final String methodName = "add";
            final String methodDesc = "(Ljavax/swing/JPanel;Ljava/awt/Component;Ljava/lang/Object;)V";
            
            ConstantPoolGen cpGen = new ConstantPoolGen(constants);
            for(InstructionHandle ih : il.getInstructionHandles()) {
                Instruction insn = ih.getInstruction();
                if(!(insn instanceof GETFIELD))
                    continue;
                
                FieldInstruction fi = (FieldInstruction)insn;
                String fieldName = fi.getFieldName(cpGen);
                if(!"javaArgsField".equals(fieldName))
                    continue;
                
                if((ih = ih.getNext()) == null || (ih = ih.getNext()) == null)
                    continue;
                
                insn = ih.getInstruction();
                if(!(insn instanceof INVOKEVIRTUAL))
                    continue;
                
                InvokeInstruction ii = (InvokeInstruction)insn;
                

                if(!ii.getName(cpGen).equals(methodName))
                    continue;
                
                String className = getClass().getName();
                int index = cpGen.addMethodref(className, methodName, methodDesc);
                ih.setInstruction(new INVOKESTATIC(index));
                jc.setConstantPool(cpGen.getFinalConstantPool());
                break c;
            }
            return null;
        }
        
        // update code
        code.setCode(il.getByteCode());
        
        return jc.getBytes();
    }
    
    private byte[] patchForm(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Method[] methods = jc.getMethods();
        Method init;
        // scan for constructor
        c: {
            for(Method m : methods) {
                if("<init>".equals(m.getName())) {
                    init = m;
                    break c;
                }
            }
            return null;
        }
        
        // create constant pool generator
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        
        // replace JPasswordField instantiation
        Code code = init.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        c: {
            for(InstructionHandle ih : handles) {
                Instruction insn = ih.getInstruction();
                if(!(insn instanceof NEW))
                    continue;
                
                NEW neww = (NEW)insn;
                if(!neww.getType(cpGen).getSignature().equals("Ljavax/swing/JPasswordField;"))
                    continue;

                InstructionHandle ih2 = ih.getNext();
                if(ih2.getInstruction() instanceof DUP) {
                    InstructionHandle ih3 = ih2.getNext();
                    if(ih3.getInstruction() instanceof INVOKESPECIAL) {
                        final String docDesc = "(Ljavax/swing/JPanel;)Ljavax/swing/text/PlainDocument;";
                        int docIndex = cpGen.addMethodref(getClass().getName(), "newDocument", docDesc);
                        final String fieldDesc = "(Ljavax/swing/text/PlainDocument;)Ljavax/swing/JPasswordField;";
                        int fieldIndex = cpGen.addMethodref(getClass().getName(), "newField", fieldDesc);
                        ih.setInstruction(new DUP());
                        ih2.setInstruction(new INVOKESTATIC(docIndex));
                        ih3.setInstruction(new INVOKESTATIC(fieldIndex));
                        break c;
                    }
                }
            }
            return null;
        }
        
        // update code
        code.setCode(il.getByteCode());
        
        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }

    // better logger
    private byte[] patchControl(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Method ca;
        // scan for callAppender
        c: {
            for(Method m : jc.getMethods()) {
                if("callAppender".equals(m.getName())) {
                    ca = m;
                    break c;
                }
            }
            return null;
        }
        
        // create constant pool generator
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        
        // print concealed message
        Code code = ca.getCode();
        InstructionList il = new InstructionList(code.getCode());
        InstructionHandle[] handles = il.getInstructionHandles();
        final String marker = "Attempted to append to non-started appender ";
        c: {
            for(InstructionHandle ih : handles) {
                Instruction insn = ih.getInstruction();
                if(!(insn instanceof NEW))
                    continue;
                
                NEW neww = (NEW)insn;
                if(!neww.getType(cpGen).getSignature().equals("Ljava/lang/StringBuilder;"))
                    continue;
                
                final InstructionHandle[] block = new InstructionHandle[4];
                block[0] = ih;
                int blockSize = insn.getLength();
                
                for(int i = 1; i < block.length; i++) {
                    InstructionHandle next = block[i - 1].getNext();
                    if(next == null) {
                        block[0] = null;
                        break;
                    }
                    block[i] = next;
                    blockSize += next.getInstruction().getLength();
                }
                
                if(block[0] == null)
                    continue;
                
                Instruction last = block[block.length - 1].getInstruction();
                if(!(last instanceof LDC))
                    continue;
                
                if(!isConstantString(cpGen.getConstantPool(), ((LDC)last).getIndex(), marker))
                    continue;
                
                final String desc = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
                int reveal = cpGen.addMethodref(getClass().getName(), "stringBuilder", desc);
                
                // TODO: remove NOP
                block[0].setInstruction(new NOP());
                block[1].setInstruction(new ALOAD(1));
                block[2].setInstruction(new INVOKESTATIC(reveal));
                
                int newBlockSize = 0;
                for(InstructionHandle handle : block)
                    newBlockSize += handle.getInstruction().getLength();
                
                // TODO: no NOPs
                while(newBlockSize < blockSize) {
                    il.append(block[0], new NOP());
                    newBlockSize++;
                }
                
                break c;
            }
            return null;
        }
        
        // update code
        code.setCode(il.getByteCode());
        
        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    private byte[] patchLauncherConstants(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;

        Method gvn;
        c: {
            for(Method method : jc.getMethods()) {
                if(!"getVersionName".equals(method.getName()))
                    continue;
                gvn = method;
                break c;
            }
            return null;
        }
        
        Code code = gvn.getCode();
        InstructionList il = new InstructionList(code.getCode());
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool());
        
        c: {
            final String methodName = "getImplementationVersion";
            final String methodDesc = "(Ljava/lang/Package;)Ljava/lang/String;";
            for(InstructionHandle ih : il.getInstructionHandles()) {
                Instruction insn = ih.getInstruction();
                if(insn instanceof INVOKEVIRTUAL) {
                    InvokeInstruction ii = (InvokeInstruction)insn;
                    if(methodName.equals(ii.getName(cpGen))) {
                        int index = cpGen.addMethodref(getClass().getName(), methodName, methodDesc); 
                        ih.setInstruction(new INVOKESTATIC(index));
                        break c;
                    }
                }
            }
            return null;
        }
        
        code.setCode(il.getByteCode());
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // alter look and feel
    private byte[] patchInterface(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;
        
        Method[] methods = jc.getMethods();
        Method slnf;
        // scan for setLookAndFeel
        c: {
            for(Method m : methods) {
                if("setLookAndFeel".equals(m.getName())) {
                    slnf = m;
                    break c;
                }
            }
            return null;
        }
        
        // create constant pool generator
        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        
        final String productName = "Minecraft";
        final String titlePrefix = productName + " Launcher ";
        
        c: {
            ConstantPool cp = cpGen.getConstantPool();
            for(Constant cons : cp.getConstantPool()) {
                if(!(cons instanceof ConstantString))
                    continue;
                
                String s = ((ConstantString)cons).getBytes(cp);
                if(titlePrefix.equals(s)) {
                } else {
                    continue;
                }
                
                int index = cpGen.addUtf8(s.replace(productName, "retroqrade"));
                ((ConstantString)cons).setStringIndex(index);
                break c;
            }
            return null;
        }
        
        // scan for reference to setLookAndFeel
        c: {
            Code code = slnf.getCode();
            InstructionList il = new InstructionList(code.getCode());
            for(InstructionHandle ih : il.getInstructionHandles()) {
                Instruction insn = ih.getInstruction();
                if(!(insn instanceof INVOKESTATIC))
                    continue;
                
                InvokeInstruction ii = (InvokeInstruction)insn;
                String methodName = ii.getMethodName(cpGen);
                if(!"setLookAndFeel".equals(methodName))
                    continue;
                
                int index = cpGen.addMethodref(getClass().getName(), methodName, ii.getSignature(cpGen));
                
                ih.setInstruction(new INVOKESTATIC(index));

                code.setCode(il.getByteCode());
                break c;
            }
            return null;
        }
        
        Method initFrame;
        // scan for setLookAndFeel
        c: {
            for(Method m : methods) {
                if("initializeFrame".equals(m.getName())) {
                    initFrame = m;
                    break c;
                }
            }
            return null;
        }
        
        c: {
            final String methodClass = "javax.imageio.ImageIO";
            final String methodName = "read";
            final String methodDesc = "(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;";
            
            Code code = initFrame.getCode();
            InstructionList il = new InstructionList(code.getCode());
            for(InstructionHandle ih : il.getInstructionHandles()) {
                Instruction insn = ih.getInstruction();
                
                if(!(insn instanceof INVOKESTATIC))
                    continue;
                
                InvokeInstruction ii = (InvokeInstruction)insn;
                if(methodClass.equals(ii.getLoadClassType(cpGen).getClassName())) {
                    if(methodName.equals(ii.getName(cpGen))) {
                        int index = cpGen.addMethodref(getClass().getName(), methodName, methodDesc);
                        ih.setInstruction(new INVOKESTATIC(index));
                        code.setCode(il.getByteCode());
                        break c;
                    }
                }
            }
            
            return null;
        }
        
        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
        
        return jc.getBytes();
    }
    
    // allow JSON response spoofing
    // TODO: add new class for this ??
    private byte[] patchService(String name, Object[] source) {
        JavaClass jc = getJavaClass(name, source);
        if(jc == null)
            return null;

        Method ppr;
        Method[] methods = jc.getMethods();
        // scan for method
        c: {
            for(int i = 0; i < methods.length; i++) {
                if(!"performPostRequest".equals(methods[i].getName()))
                    continue;
                ppr = methods[i];
                break c;
            }
            return null;
        }

        ConstantPoolGen cpGen = new ConstantPoolGen(jc.getConstantPool().getConstantPool());
        
        // add reference to new method
        final String desc = "(Ljava/lang/Object;Ljava/net/URL;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
        int index = cpGen.addMethodref(getClass().getName(), "fakeRequest", desc);
        
        // get first notNull check from code
        Code codeContainer = ppr.getCode();
        byte[] code = codeContainer.getCode();
        // TODO: don't rely on exact offsets
        byte[] first5 = new byte[]{code[0], code[1], code[2], code[3], code[4]};
        InstructionList epilogue = new InstructionList(first5);
        
        InstructionList il = new InstructionList();
        
        // add dummy instruction for stackmap purposes
        il.append(new NOP());
        
        il.append(new ALOAD(0));
        il.append(new ALOAD(1));
        il.append(new ALOAD(2));
        il.append(new ALOAD(3));
        il.append(new INVOKESTATIC(index));
        il.append(new DUP());
        il.append(new ASTORE(4));
        il.append(new IFNULL(epilogue.getStart()));
        il.append(new ALOAD(4));
        il.append(new ARETURN());
        il.append(epilogue);

        final byte INS_GOTO = (byte)167;
        final byte INS_NOOP = (byte)0;
        
        byte[] b = il.getByteCode();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteStream);
        try {
            // write code with new jump
            out.write(code);
            out.write(b);
            out.writeByte(INS_GOTO);
            out.writeShort(-(code.length + b.length - first5.length + 2));
            out.flush();
        } catch(IOException ioe) {
            return null;
        }
        
        // overwrite notNull check
        byte[] newCode = byteStream.toByteArray();
        int offset = code.length + 1;
        newCode[0] = INS_GOTO;
        // TODO: use putShort
        newCode[1] = (byte)((offset >> 8) & 0xFF);
        newCode[2] = (byte)((offset) & 0xFF);
        newCode[3] = INS_NOOP;
        newCode[4] = INS_NOOP;
        codeContainer.setCode(newCode);
        Attribute[] attributes = codeContainer.getAttributes();
        
        // scan for stack map index
        ConstantPool cp = cpGen.getConstantPool();
        int smIndex;
        c: {
            for(int i = 0; i < attributes.length; i++) {
                // get attribute and resolve name
                final byte CONST_UTF8 = 1;
                Attribute attr = attributes[i];
                int nameIndex = attr.getNameIndex();
                // TODO: don't depend on constant type
                String s = cp.constantToString(nameIndex, CONST_UTF8);

                // ignore other attributes
                if(!"StackMapTable".equals(s))
                    continue;
                
                // store index for later use
                smIndex = i;
                break c;
            }
            return null;
        }
        
        // attempt to dump existing stack map
        Attribute smAttribute = attributes[smIndex];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            smAttribute.dump(dos);
            dos.flush();
        } catch(IOException ioe) {
            return null;
        }
        
        // skip name index and attribute length
        byte[] tmp = baos.toByteArray();
        byte[] smBytes = new byte[tmp.length - 6];
        System.arraycopy(tmp, 6, smBytes, 0, smBytes.length);
        
        // create new attribute container
        Unknown unknown = new Unknown(smAttribute.getNameIndex(), smBytes.length, smBytes, cp);
        
        // merge new frames
        try {
            // noDeltas = deltas + 1
            int[] deltas = walkStackMap(smBytes);
            int noDeltas = deltas.length;
            
            // TODO: simplify offsets
            int patchOffset = offset - deltas[noDeltas - 1] - 1;
            baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
            // write new number of frames
            dos.writeShort(noDeltas + 3);
            // write new initial frame
            dos.write(3);
            // write new beginning for old initial frame
            dos.write(smBytes[2]);
            dos.writeShort(deltas[0] - 4);
            // write remaining old frame data
            dos.write(smBytes, 5, smBytes.length - 5);
            // write new frame
            dos.write(249);
            dos.writeShort(patchOffset - 1);
            // write new frame
            dos.write(249);
            dos.writeShort(0);
            // write new frame
            dos.write(b.length - 7);
            
            // get new attribute bytes
            dos.flush();
            tmp = baos.toByteArray();
            
            // update attribute and length
            unknown.setBytes(tmp);
            unknown.setLength(tmp.length);
        } catch(IOException ioe) {
            return null;
        }
        
        // replace stack map and force its use
        attributes[smIndex] = unknown;
        
        // force size to be recalculated
        codeContainer.setAttributes(attributes);

        // update constant pool
        jc.setConstantPool(cpGen.getFinalConstantPool());
  
        return jc.getBytes();
    }
    
    // unpack StackMapTable
    private int[] walkStackMap(byte[] b) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
        
        final short SAME_MIN = 0;
        final short SAME_MAX = 63;
        
        // has 1 item
        final short SAME_LOCALS_1_STACK_MIN = 64;
        final short SAME_LOCALS_1_STACK_MAX = 127;
        
        final short RESERVED_MIN = 128;
        final short RESERVED_MAX = 246;
        
        // has delta + 1 item
        final short SAME_LOCALS_1_STACK_W = 247;
        
        // has delta
        final short CHOP_MIN = 248;
        final short CHOP_MAX = 250;
        
        // has delta
        final short SAME_W = 251;
        
        // has delta + n items
        final short APPEND_MIN = 252;
        final short APPEND_MAX = 254;
        
        // has delta, x items, y items
        final short FULL_FRAME = 255;
        
        final short TYPE_OBJECT = 7;
        final short TYPE_UNITIALIZED = 8;
        
        int offset = -1;
        
        int numberOfEntries = dis.readUnsignedShort();
        int[] deltas = new int[numberOfEntries + 1];
        for(int i = 0; i < numberOfEntries; i++) {
            int frameType = dis.readUnsignedByte();
            final int offsetDelta;
            int numberOfLocals = 0;
            int numberOfStackItems = 0;
            if(frameType >= SAME_MIN && frameType <= SAME_MAX) {
                offsetDelta = frameType - SAME_MIN;
            } else if(frameType >= SAME_LOCALS_1_STACK_MIN && frameType <= SAME_LOCALS_1_STACK_MAX) {
                numberOfStackItems = 1;
                offsetDelta = frameType - SAME_LOCALS_1_STACK_MIN;
            } else if(frameType >= RESERVED_MIN && frameType <= RESERVED_MAX) {
                return null;
            } else if(frameType == SAME_LOCALS_1_STACK_W) {
                numberOfStackItems = 1;
                offsetDelta = dis.readUnsignedShort();
            } else if(frameType >= CHOP_MIN && frameType <= CHOP_MAX) {
                offsetDelta = dis.readUnsignedShort();
            } else if(frameType == SAME_W) {
                offsetDelta = dis.readUnsignedShort();
            } else if(frameType >= APPEND_MIN && frameType <= APPEND_MAX) {
                numberOfLocals = frameType - 251;
                offsetDelta = dis.readUnsignedShort();
            } else if(frameType == FULL_FRAME) {
                offsetDelta = dis.readUnsignedShort();
                numberOfLocals = dis.readUnsignedShort();
                numberOfStackItems = -1;
            } else {
                // shouldn't get here
                return null;
            }

            for(int j = 0; j < numberOfLocals; j++) {
                int verificationType = dis.readByte();
                if(verificationType == TYPE_OBJECT || verificationType == TYPE_UNITIALIZED)
                    dis.readShort();
            }

            // number of stack items may be after locals
            if(numberOfStackItems == -1)
                numberOfStackItems = dis.readUnsignedShort();

            for(int j = 0; j < numberOfStackItems; j++) {
                int verificationType = dis.readByte();
                if(verificationType == TYPE_OBJECT || verificationType == TYPE_UNITIALIZED)
                    dis.readShort();
            }

            deltas[i] = offsetDelta;
            offset += offsetDelta + 1;
        }
        
        deltas[numberOfEntries] = offset;
        return deltas;
    }
    
    // write a short to an array
    private static void putShort(byte[] b, int i, int s) {
        b[i + 0] = (byte)((s >> 8) & 0xFF);
        b[i + 1] = (byte)((s) & 0xFF);
    }

    // ...
    private Class<?> override(String name, byte[] b, Object[] source) throws ClassNotFoundException {        
        Manifest manifest = null;
        URL url = null;
        CodeSource cs = null;
        if(source[0] instanceof JarURLConnection) {
            JarURLConnection juc = (JarURLConnection)source[0];
            try {
                manifest = juc.getManifest();
            } catch(IOException ioe) {
            }
            url = juc.getJarFileURL();
            if(source[1] instanceof JarEntry) {
                JarEntry je = (JarEntry)source[1];
                cs = new CodeSource(url, je.getCodeSigners());
            }
        }
        
        // define new package if necessary
        int pkgEnd = Math.max(0, name.lastIndexOf('.'));
        String pkgName = name.substring(0, pkgEnd);
        Package pkg = getPackage(pkgName);
        if(pkg == null)
            definePackage(pkgName, null != manifest ? manifest : emptyManifest, url);

        Class<?> c;
        try {
            // specifying null for name would bypass certificate check 
            c = defineClass(name, b, 0, b.length, cs);
        } catch(Throwable t) {
            System.err.println("corrupt class/".concat(name));
            t.printStackTrace();
            c = super.loadClass(name);
        }

        classCache.put(name, c);
        return c;
    }
    
    private byte[] getCompiled(String name, Object[] source) {
        return(
            name.equals("net.minecraft.bootstrap.Bootstrap") ? patchBootstrap(name, source) :
            name.equals("com.mojang.authlib.HttpAuthenticationService") ? patchService(name, source) :
            name.equals("com.mojang.authlib.yggdrasil.request.AuthenticationRequest") ? patchRequest(name, source) :
            name.equals("net.minecraft.launcher.Launcher") ? patchLauncher(name, source) :
            name.startsWith("net.minecraft.launcher.Launcher$") ? patchInner(name, source) :
            name.equals("net.minecraft.launcher.LauncherConstants") ? patchLauncherConstants(name, source) :
            name.equals("net.minecraft.launcher.SwingUserInterface") ? patchInterface(name, source) :
            name.equals("net.minecraft.launcher.game.MinecraftGameRunner") ? patchRunner(name, source) :
            name.equals("net.minecraft.launcher.ui.LauncherPanel") ? patchLauncherPanel(name, source) :
            name.equals("net.minecraft.launcher.ui.BottomBarPanel") ? patchBar(name, source) :
            name.equals("net.minecraft.launcher.ui.bottombar.PlayButtonPanel") ? patchBar(name, source) :
            name.equals("net.minecraft.launcher.ui.bottombar.PlayerInfoPanel") ? patchBar(name, source) :
            name.equals("net.minecraft.launcher.ui.bottombar.ProfileSelectionPanel") ? patchBar(name, source) :
            name.equals("net.minecraft.launcher.ui.popups.login.LogInForm") ? patchForm(name, source) :
            name.equals("net.minecraft.launcher.ui.popups.profile.ProfileJavaPanel") ? patchProfile(name, source) :
            name.equals("net.minecraft.launcher.ui.tabs.LauncherTabPanel") ? patchTabPanel(name, source) :
            name.equals("net.minecraft.launcher.updater.Argument$Serializer") ? patchSerializer(name, source) :
            name.equals("net.minecraft.launcher.updater.CompleteMinecraftVersion") ? patchVersion(name, source) :
            name.equals("org.apache.logging.log4j.core.config.AppenderControl") ? patchControl(name, source) :
            name.equals(TEXTURED_PANEL_CLASS) ? createPanel2(name) :
            name.equals(TRANSPARENT_PANEL_CLASS) ? createPanel(name) :
            name.equals(TRANSPARENT_LABEL_CLASS) ? createLabel(name) :
            name.equals(TRANSPARENT_BUTTON_CLASS) ? createButton(name) :
            null
        );
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if(name == null)
            return super.loadClass(null);
        
        // check cache first
        Class<?> clazz = classCache.get(name);
        if(clazz != null) {
            return clazz;
        }
        
        // patch necessary classes
        Object[] source = {null, null};
        byte[] compiled = getCompiled(name, source);

        if(compiled != null)
            return override(name, compiled, source);
        
        return super.loadClass(name);
    }
    
    public static void addTab(JTabbedPane pane, String name, JComponent component) {
        pane.addTab(name, component);
        component.setOpaque(true);
    }
    
    public static void setLayout(JPanel panel, java.awt.LayoutManager layout) {
        panel.setLayout(layout);
        
        Border matte = new MatteBorder(0, 0, 2, 0, Color.BLACK);
        panel.setBorder(matte);
    }
    
    // TODO: remove
    static java.lang.reflect.Field TARGET;
    static {
        try {
            TARGET = Thread.class.getDeclaredField("target");
            TARGET.setAccessible(true);
        } catch(Exception e) {
        }
    }
    
    // process uncaught exception from launcher thread
    public static void uncaughtException(Thread t, Throwable e) throws Exception {
        System.err.println("thread/".concat(t.toString()));
        System.err.println(TARGET.get(t));
    }
    
    // filter required files for a game version
    public static Set<String> filterRequiredFiles(Set<String> set, String id) {
        return set;
    }
    
    // check if version is installed
    public static boolean setContains(Set<?> set, Object o) {
        return true;
    }
    
    /**
     * Attempts to set Nimbus Look and Feel
     *
     * @param className class name for fallback
     */
    public static void setLookAndFeel(String className) throws Exception {
        for(LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                return;
            }
        }
        
        UIManager.setLookAndFeel(className);
    }
    
    private static int codePointCount(Document document, int off, int len, Segment segment) throws BadLocationException {
        document.getText(off, len, segment);
        return Character.codePointCount(segment.array, segment.offset, segment.count);
    }

    // for document filter
    public static boolean isAllowed(FilterBypass fb, String s, int off, int len) throws BadLocationException {
        Segment segment = new Segment();
        Document document = fb.getDocument();
        int documentLength = codePointCount(document, 0, document.getLength(), segment);
        if(documentLength + s.codePointCount(0, s.length()) - codePointCount(document, off, len, segment) <= MAX_PASSWORD_LEN)
            return true;
        
        Toolkit.getDefaultToolkit().beep();
        return false;
    }
    
    public static PlainDocument newDocument(JPanel panel) {
        return new PlainDocument(new StringContent(MAX_PASSWORD_LEN * 2 + 2));
    }
    
    public static JPasswordField newField(PlainDocument pd) {
        return new JPasswordField(); // TODO: refactor/remove
    }
    
    public static void setText(JPasswordField field, String s) {
        char[] arr = new char[MAX_PASSWORD_LEN * 2];
        Arrays.fill(arr, '\0');
        field.setText(new String(arr));
        field.setText(s);
    }
    
    public static byte[] trim(ByteBuffer bb, byte[] bytes) {
        byte[] trimmed = new byte[bb.position()];
        System.arraycopy(bytes, 0, trimmed, 0, trimmed.length);
        Arrays.fill(bytes, (byte)0);
        
        return trimmed;
    }
    
    public static String strip(char[] safe, Segment segment) {
        // fill filter array, blank original characters
        int j = 0;
        for(int i = 0; i < safe.length; i++) {
            char c = segment.array[segment.offset + i];
            if(c >= 'a' && c <= 'z'){}
            else if(c >= 'A' && c <= 'Z'){}
            else if(c >= '0' && c <= '9'){}
            else {
                c = 'z';
                continue;
            }
            safe[j++] = c;
            segment.array[segment.offset + i] = c = 'z';
        }
        
        // create string from blanked array, blank array entirely
        String s = segment.toString();
        Arrays.fill(segment.array, '\0');
        
        return s;
    }
    
    public static Segment getPassword(JPasswordField field) {
        Document document = field.getDocument();
        Segment segment = new Segment();
        segment.setPartialReturn(true);
        try {
            document.getText(0, document.getLength(), segment);
            segment.array = segment.array.clone();
        } catch(BadLocationException ble) {
        }
        return segment;
    }
    
    public static void setHorizontalAlignment(JLabel label, int n) {
        label.setHorizontalAlignment(n);
        label.setBackground(new Color(0, 0, 0, 96));
        label.setOpaque(true);
    }
    
    // TODO: improve
    public static String fakeRequest(Object o, URL url, String s1, String s2) throws IOException {
        System.err.println("url/".concat(url.toString()));
        
        String s = url.toString();
        if(s.endsWith("/validate") || s.endsWith("/refresh")) {
            if(getProperty("sinkhole") != null) { // TODO: better name
                throw new IOException("sinkhole");
            }
        }
        
        return null;
    }

    // TODO: use..
    public static OutputStream getOutputStream(HttpURLConnection conn) throws IOException {
        OutputStream out = conn.getOutputStream();
        if(!(conn instanceof HttpsURLConnection) || !"authserver.mojang.com".equals(conn.getURL().getHost()))
            return out;
        
        Certificate endEntity = ((HttpsURLConnection)conn).getServerCertificates()[0];
        byte[] candidate = endEntity.getPublicKey().getEncoded();

        if(Arrays.equals(candidate, getPublicKey()))
            return out;
        
        throw new IOException("Public key mismatch");
    }
    
    public static StringBuilder append(StringBuilder sb, Object o) {
        if(o instanceof Throwable)
            ((Throwable)o).printStackTrace();
        
        return sb.append(o);
    }
    
    public static java.awt.image.BufferedImage read(InputStream is) throws IOException {
        final String rawData = (""
            + "1mhrjn39qg3euscpp5yajeh4ryzz6lxpvfs4rsjnqe6umnfa7f5wtzq8qgqoaebxvd3oo4y133e7o4m4"
            + "4oyskmquloe5nmi1nl3i8ohwx6izaa7z4mwmr12o1ai140ytlxvgcubhxmleq83gvtyza28dixb9btvp"
            + "lgaj78hh4ijkn12ohjjnmd3kcfgaf24pdes4uu7keg44pjlcsfdlylyg1lcez7q4nqvgu139q1bqs0y6"
            + "67lv8gusx1qyu57179sotmsylcbo2wdfeaybci44wbprxtv1nhvq7lf1hw5bfq8b45ns2s8rkoc74etn"
            + "3yp7upwbzsu05knw3iugt9kkcacbdmdyawabjd6p66nn4odcwwfhcrfdsccq1uqa0ahr1342xp427dtz"
            + "5uoh59gfzyb6sqg283icd2zt25759oysesto9y6131uwe72cawgwba825li4220oshm4e2fl4iv7lzl9"
            + "0h43lwxhqa03853pyyjzd5mgul94chbcpql2nv66f8k8xc3hkug0o3ajdfq87hnzh795xrb15mnoeovk"
            + "sbl016zbq3xr7gaor1l0k6ghgc3w0m5lwwlcd47wroo5jlxfotjqgqrbeul8vwi"
        );
        
        byte[] decoded = new BigInteger(rawData, 36).toByteArray();
        decoded[0] = -0x77;
        
        return javax.imageio.ImageIO.read(new ByteArrayInputStream(decoded));
    }
    
    // called when attempting to append to closed Log4J handler
    public static StringBuilder stringBuilder(Object o) {
        return new StringBuilder();
    }
    
    public static void add(JPanel panel, Component comp, Object constraints) {
        comp.setPreferredSize(comp.getMinimumSize());
        panel.add(comp, constraints);
    }
    
    public static String getImplementationVersion(Package pkg) {
        String implVersion = pkg.getImplementationVersion();
        return implVersion == null ? null : implVersion.concat("-r0");
    }
    
    public static URI toURI(File f) {
        try {
            f = f.getCanonicalFile();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        
        return f.toURI();
    }
    
    // TODO: fix doc
    /** Initialize public key value */
    private static byte[] getPublicKey() {
        final String s = (""
            + "64rkhj2q7mydisqvp66rx9tkju5lntye5uic7ssu0e6i9vkzxfs9gwfhqvrs2bnh99h94642mrxc8zcs"
            + "cizx41zxk70fkb14sdc3cr8woqqye921ng3mu2canz0q39n19vpe4f2qpwrcljncdbjfu9ee23qwitky"
            + "gh6rkp9zb7a7fqatdtjmxztzvf0d0fmpn4oaqsmczxzyjpzo8gmz8hiklp9fekcy7x7p1sprs91p2wzr"
            + "4ctkbxgir0i3dhks8sit6msmj6siklkry7ymhko1hxyh810cg0b8s21pmbdjtnx071d6ezrfnvkrxxgs"
            + "956ejrkb3j1tat9xie5x21jcr1rk390i01se7pa0ii62mio3vvcnxkdzrkwno1ftgmlfi5fgwvoqtpui"
            + "v0uwq8e7rwwbbp27hsu5iq0o492j1hq7pscqfs9mydxop0rb4qosg1"
        );
        
        // decode from string and restore first byte
        byte[] publicKey = new BigInteger(s, 36).toByteArray();
        publicKey[0] = 0x30;
        
        return publicKey;
    }
    
    private static boolean isConstantString(ConstantPool cp, int index, String s) {
        final byte CONST_STRING = 8;
        if(!(cp.getConstant(index) instanceof ConstantString))
            return false;
        
        return cp.getConstantString(index, CONST_STRING).equals(s);
    }
    
    // returns name of class referenced by InvokeInstruction, with slashes
    private static String getClassName(ConstantPoolGen cpGen, InvokeInstruction ii) {
        ConstantPool cp = cpGen.getConstantPool();
        Constant constant = cp.getConstant(ii.getIndex());
        if(!(constant instanceof ConstantCP))
            throw new IllegalArgumentException();
        return getClassName(cp, (ConstantCP)constant);
    }
    
    private static String getClassName(ConstantPool cp, ConstantCP ccp) {
        final byte CONST_CLASS = 7;
        return cp.getConstantString(ccp.getClassIndex(), CONST_CLASS);
    }
    
    private static String getProperty(String name) {
        return System.getProperty(MOD_PKG + "." + name);
    }
    
    // TODO: cleanup
    private static void dump(String inJar, ArrayList<Closeable> toClose, Bypass cl) throws IOException {
        File f = new File(inJar);
        URL url = f.toURI().toURL();
        System.err.println("input/".concat(url.toString()));
        cl.addURL(url);
        
        java.io.FileInputStream fis = new java.io.FileInputStream(f);
        toClose.add(fis);
        java.util.jar.JarInputStream jis = new java.util.jar.JarInputStream(fis);
        toClose.add(jis);
        FileOutputStream fos = new FileOutputStream("dump/patched_" + System.currentTimeMillis() + ".jar");
        java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(fos);
        toClose.add(jos);
        toClose.add(fos);
        
        // TODO: fix missing manifest
        for(;;) {
            JarEntry entry = jis.getNextJarEntry();
            if(entry == null) break;
            String entryName = entry.getName();
            jos.putNextEntry(new JarEntry(entryName));
            if(entryName.endsWith(".class")) {
                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                byte[] data = cl.getCompiled(className, new Object[2]);
                if(data != null) {
                    System.err.println("modified/".concat(className));
                    jos.write(data);
                    continue;
                }
            }
            
            byte[] data = new byte[8192];
            for(;;) {
                int read = jis.read(data);
                if(read == -1) break;
                jos.write(data, 0, read);
            }
        }
        
        for(String s : cl.modClasses) {
            jos.putNextEntry(new JarEntry(s.replace('.', '/') + ".class"));
            jos.write(cl.getCompiled(s, new Object[2]));
            System.err.println("created/".concat(s));
        }
    }

}
