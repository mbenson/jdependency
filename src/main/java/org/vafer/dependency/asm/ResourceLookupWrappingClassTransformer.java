package org.vafer.dependency.asm;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.vafer.dependency.Console;

public final class ResourceLookupWrappingClassTransformer implements ClassTransformer {

	private final Console console;
	
	public ResourceLookupWrappingClassTransformer() {
		console = null;
	}

	public ResourceLookupWrappingClassTransformer( final Console pConsole ) {
		console = pConsole;		
	}
	
	public final class WrappingClassAdapter extends ClassAdapter implements Opcodes {
		
		private String current;
	
		public WrappingClassAdapter(ClassVisitor cv) {
			super(cv);
		}
	
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
	
			current = name;
		}
	
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
	
			final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
	
			return new MethodWrapper(mv);
		}
	
		private class MethodWrapper extends MethodAdapter {
	
			private final MethodVisitor mv;
	
			public MethodWrapper(final MethodVisitor pMv) {
				super(pMv);
				mv = pMv;
			}
	
			
			// static java.lang.Class java.lang.Class.forName(java.lang.String)
			// static java.lang.Class java.lang.Class.forName(java.lang.String, boolean, java.lang.ClassLoader)
			// static java.net.URL java.lang.ClassLoader.getSystemResource(java.lang.String)
			// static java.io.InputStream java.lang.ClassLoader.getSystemResourceAsStream(java.lang.String)
			// java.net.URL java.lang.ClassLoader.getResource(java.lang.String)
			// java.io.InputStream java.lang.ClassLoader.getResourceAsStream(java.lang.String)
			
			private final Set methods = new HashSet() {{
				add("forName");
				add("getSystemResource");
				add("getSystemResourceAsStream");
				add("getResource");
				add("getResourceAsStream");
			}};
	
			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
	
				if (methods.contains(name)) {
					if (console != null) {
						console.println("rewriting call " + current + " " + owner + "." + name + " " + desc);
					}				
				}
	
				mv.visitMethodInsn(opcode, owner, name, desc);
			}
		}
	}

	public byte[] transform( final byte[] pClazzBytes ) {
        try {
            
            final ClassReader r = new ClassReader(pClazzBytes);
            final ClassWriter w = new ClassWriter(true);
            r.accept(new WrappingClassAdapter(w), false);
            return w.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }	}
}