package com.ooftf.iorderfix.plugin


import org.apache.commons.io.IOUtils
import org.objectweb.asm.*
import org.objectweb.asm.commons.AdviceAdapter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 *
 * @author billy.qi* @since 17/3/20 11:48
 */
class CodeInsertProcessor {
    ArrayList<RegisterInfo> extension

    private CodeInsertProcessor(ArrayList<RegisterInfo> extension) {
        this.extension = extension
    }

    static void insertInitCodeTo(ArrayList<RegisterInfo> extension, File file) {
        System.out.println("insertInitCodeTo${file.absolutePath}")
        if (extension != null && !extension.isEmpty()) {
            System.out.println("CodeInsertProcessor")
            CodeInsertProcessor processor = new CodeInsertProcessor(extension)
            processor.generateCodeIntoJarFile(file)

        }
    }

    static void insertDirectoryCodeTo(ArrayList<RegisterInfo> extension, File file, String root) {
        System.out.println("insertDirectoryCodeTo${file.absolutePath}")
        if (extension != null && !extension.isEmpty()) {
            System.out.println("insertDirectoryCodeTo")
            CodeInsertProcessor processor = new CodeInsertProcessor(extension)
            processor.generateCodeIntoClassFile(file, root)
        }
    }

    //处理jar包中的class代码注入
    private File generateCodeIntoJarFile(File jarFile) {
        if (jarFile) {
            def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")
            if (optJar.exists())
                optJar.delete()
            def file = new JarFile(jarFile)
            Enumeration enumeration = file.entries()
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar))
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                ZipEntry zipEntry = new ZipEntry(entryName)
                InputStream inputStream = file.getInputStream(jarEntry)
                jarOutputStream.putNextEntry(zipEntry)
                System.out.println("generateCodeIntoJarFile - findRegisterInfo")
                String className = jarEntry.getName().replace('/', '.').replace('\\', '.').replace(File.separator, '.')
                def clazz = findRegisterInfo(className)
                if (clazz != null) {
                    println('generate code into:' + entryName)
                    def bytes = doGenerateCode(inputStream, clazz)
                    jarOutputStream.write(bytes)
                } else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                inputStream.close()
                jarOutputStream.closeEntry()
            }
            jarOutputStream.close()
            file.close()

            if (jarFile.exists()) {
                jarFile.delete()
            }
            optJar.renameTo(jarFile)
        }
        return jarFile
    }

    RegisterInfo findRegisterInfo(String className) {
        System.out.println("findRegisterInfo--start::${className}")
        if (className == null) {
            return null
        }
        if (!className.endsWith(".class")) {
            return null
        }
        className = className.substring(0, className.lastIndexOf('.'))
        def result = extension.find { item ->
            item.className == className
        }
        System.out.println("findRegisterInfo${className}::${result == null}")
        return result
    }
    /**
     * 处理class的注入
     * @param file class文件
     * @return 修改后的字节码文件内容
     */
    private byte[] generateCodeIntoClassFile(File file, String root) {
        def optClass = new File(file.getParent(), file.name + ".opt")
        String className = file.getAbsolutePath().replace(root, "").replace('/', '.').replace('\\', '.').replace(File.separator, '.')
        if (className.startsWith(".")) {
            className = className.substring(1, className.length())
        }
        def item = findRegisterInfo(className)
        if (item == null) {
            return null
        }
        FileInputStream inputStream = new FileInputStream(file)
        FileOutputStream outputStream = new FileOutputStream(optClass)

        def bytes = doGenerateCode(inputStream, item)
        outputStream.write(bytes)
        inputStream.close()
        outputStream.close()
        if (file.exists()) {
            file.delete()
        }
        optClass.renameTo(file)
        return bytes
    }

    private byte[] doGenerateCode(InputStream inputStream, RegisterInfo item) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ClassVisitor cv = new MyClassVisitor(Opcodes.ASM6, cw, item)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    class MyClassVisitor extends ClassVisitor {
        RegisterInfo extension

        MyClassVisitor(int api, ClassVisitor cv, RegisterInfo item) {
            super(api, cv)
            this.extension = item
        }

        void visit(int version, int access, String name, String signature,
                   String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
        }

        @Override
        MethodVisitor visitMethod(int access, String name, String desc,
                                  String signature, String[] exceptions) {
            System.out.println("visitMethod::${name}")
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)

            if (name == extension.methodName) { //注入代码到指定的方法之中
                return new AdviceAdapter(Opcodes.ASM6, mv, access, name, desc) {
                    private Label from = new Label(),
                                  to = new Label(),
                                  target = new Label();
                    // 方法进入时修改字节码
                    protected void onMethodEnter() {
                        //标志：try块开始位置
                        visitLabel(from);
                        visitTryCatchBlock(from,
                                to,
                                target,
                                "java/lang/Exception");
                    }

                    // 访问局部变量和操作数栈
                    public void visitMaxs(int maxStack, int maxLocals) {
                        //标志：try块结束
                        mv.visitLabel(to);

                        //标志：catch块开始位置
                        mv.visitLabel(target);
                        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object["java/lang/Exception"]);

                        // 异常信息保存到局部变量
                        int local = newLocal(Type.LONG_TYPE);
                        mv.visitVarInsn(ASTORE, local);

                        // 抛出异常
                        mv.visitVarInsn(ALOAD, local);
                        mv.visitInsn(ATHROW);
                        super.visitMaxs(maxStack, maxLocals);
                    }

                    // 方法退出时修改字节码
                    protected void onMethodExit(int opcode) {

                    }
                }
                /*boolean _static = (access & Opcodes.ACC_STATIC) > 0
                mv = new MyMethodVisitor(Opcodes.ASM6, mv, _static)*/
            }
            return mv
        }
    }

    class MyMethodVisitor extends MethodVisitor {
        boolean _static;

        MyMethodVisitor(int api, MethodVisitor mv, boolean _static) {
            super(api, mv)
            this._static = _static;
        }
        Label start = new Label()
        Label end = new Label()
        Label handler = new Label()
        //Opcodes.IRETURN
        @Override
        void visitCode() {
            super.visitCode()
            System.out.println("visitCode")
            visitLabel(start)
        }

        @Override
        void visitInsn(int opcode) {
            System.out.println("visitInsn::${opcode}")
            super.visitInsn(opcode)
        }

        @Override
        void visitMaxs(int maxStack, int maxLocals) {
            System.out.println("visitMaxs::${maxStack}::${maxLocals}")
            //标志：try块结束
            mv.visitLabel(end);

            //标志：catch块开始位置
            mv.visitLabel(handler);
            // mv.visitTryCatchBlock(start, end, handler, "java/lang/Exception")
            /*def obj = new Object[1]
            obj[0] = "java/lang/Exception"*/
            //mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, obj);

            // 异常信息保存到局部变量
            /*     int local = mv.visitTypeInsn(Opcodes.NEW, Type.LONG_TYPE.className)

                 mv.visitVarInsn(Opcodes.ASTORE, local)*/

            // 抛出异常
            /*  mv.visitVarInsn(ALOAD, local);
              mv.visitInsn(ATHROW);*/
            super.visitMaxs(maxStack, maxLocals)
            //super.visitMaxs(maxStack + 4, maxLocals)
        }
    }
}