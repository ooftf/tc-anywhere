package com.ooftf.tca.plugin

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
        System.err.println("optClass::" + optClass.absolutePath)
        System.err.println("file::" + file.absolutePath)
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
        ClassWriter cw = new ClassWriter(cr, 0)//ClassWriter.COMPUTE_FRAMES

        ClassVisitor cv = new MyClassVisitor(Opcodes.ASM7, cw, item)
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
            System.out.println("visitMethod::${name}::${desc}")
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
            if (extension.methodName.contains(name + desc)) { //注入代码到指定的方法之中
                if (!desc.endsWith("V")) {
                    throw RuntimeException("only support methods with no return value ${extension.className}::${name}")
                }
                return new AdviceAdapter(Opcodes.ASM7, mv, access, name, desc) {
                    Label beginLabel = new Label()
                    Label endLabel = new Label()
                    // 方法进入时修改字节码
                    protected void onMethodEnter() {
                        mark(beginLabel)

                    }

                    @Override
                    void visitMaxs(int maxStack, int maxLocals) {
                        System.out.println("in visitMaxs")

                        super.visitMaxs(maxStack, maxLocals)
                        System.out.println("out visitMaxs")
                    }
// 方法退出时修改字节码
                    protected void onMethodExit(int opcode) {
                        //判断不是以一场结束
                        if (ATHROW != opcode) {
                            System.out.println("!!!!!!ATHROW")
                            //mark(endLabel)
                            //加载正常的返回值
                            //returnValue()
                            //push((Type) null);
                            //只有一个参数就是返回值
                            /*Type t = Type.getType("Lcom/ooftf/iorderfix/MyInter;")
                            invokeStatic(t, new Method("print", "( )V"));*/
                            //returnValue()
                            mark(endLabel)
                            push((Type) null);
                            catchException(beginLabel, endLabel, Type.getType(Throwable.class))
                        } else {
                            System.out.println("ATHROW")
                        }
                    }
                }
            }
            return mv
        }
    }
}