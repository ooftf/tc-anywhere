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
    ArrayList<TCInfo> extension

    private CodeInsertProcessor(ArrayList<TCInfo> extension) {
        this.extension = extension
    }

    static void insertInitCodeTo(ArrayList<TCInfo> extension, File file) {
        if (extension != null && !extension.isEmpty()) {
            CodeInsertProcessor processor = new CodeInsertProcessor(extension)
            processor.generateCodeIntoJarFile(file)

        }
    }

    static void insertDirectoryCodeTo(ArrayList<TCInfo> extension, File file, String root) {
        if (extension != null && !extension.isEmpty()) {
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

    TCInfo findRegisterInfo(String className) {
        System.out.println("tac-scanClass::${className}")
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

    private byte[] doGenerateCode(InputStream inputStream, TCInfo item) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)//ClassWriter.COMPUTE_FRAMES

        ClassVisitor cv = new MyClassVisitor(Opcodes.ASM7, cw, item)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        return cw.toByteArray()
    }

    class MyClassVisitor extends ClassVisitor {
        TCInfo extension

        MyClassVisitor(int api, ClassVisitor cv, TCInfo item) {
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
            System.out.println("tac-scanMethod::${name}::${desc}")
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
                    // 方法退出时修改字节码
                    protected void onMethodExit(int opcode) {
                        //判断不是以一场结束
                        if (ATHROW != opcode) {
                            mark(endLabel)
                            push((Type) null);
                            catchException(beginLabel, endLabel, Type.getType(Throwable.class))
                        } else {
                        }
                    }
                }
            }
            return mv
        }
    }
}