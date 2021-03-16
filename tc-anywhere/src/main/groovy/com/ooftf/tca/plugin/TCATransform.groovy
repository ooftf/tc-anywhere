package com.ooftf.tca.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 *
 * @author billy.qi* @since 17/3/21 11:48
 */
class TCATransform extends Transform {

    Project project
    TCAExtensions config;

    TCATransform(Project project) {
        this.project = project
    }


    @Override
    String getName() {
        return "try-catch"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 是否支持增量编译
     * @return
     */
    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs
                   , Collection<TransformInput> referencedInputs
                   , TransformOutputProvider outputProvider
                   , boolean isIncremental) throws IOException, TransformException, InterruptedException {
        project.logger.warn("start auto-register transform...")
        project.logger.warn(config.toString())
        outputProvider.deleteAll()


        println("auto-register-----------isIncremental:${isIncremental}--------------------------\n")

        // 遍历输入文件
        inputs.each { TransformInput input ->
            // 遍历jar
            input.jarInputs.each { JarInput jarInput ->
                // 获得输入文件
                File src = jarInput.file
                System.out.println("try-catch 1111111111111111")
                //遍历jar的字节码类文件，找到需要自动注册的类
                File dest = getDestFile(jarInput, outputProvider)
                System.out.println("try-catch 1212121212121")
                FileUtils.copyFile(src, dest)
                System.out.println("try-catch 222222222222" + dest.absolutePath)
                if (dest.isFile() && shouldProcessClass(dest)) {

                    CodeInsertProcessor.insertInitCodeTo(config.registerInfo, dest)
                }
                System.out.println("try-catch 3333333333333")
            }
            // 遍历目录
            input.directoryInputs.each { DirectoryInput directoryInput ->
                long dirTime = System.currentTimeMillis();
                // 获得产物的目录
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                long scanTime = System.currentTimeMillis();
                // 处理完后拷到目标文件
                FileUtils.copyDirectory(directoryInput.file, dest)
                dest.eachFileRecurse { File file ->
                    if (file.isFile() && shouldProcessClass(file)) {
                        CodeInsertProcessor.insertDirectoryCodeTo(config.registerInfo, file,dest.absolutePath)
                    }
                }

                println "auto-register cost time: ${System.currentTimeMillis() - dirTime}, scan time: ${scanTime - dirTime}"
            }
        }

        //project.logger.error("register cost time: " + (finishTime - time) + " ms")
    }

    boolean shouldProcessClass(File file) {
//        println('classes:' + entryName)
        if (!file.isFile()) {
            return false
        }
        if (file.name.endsWith(".class")) {
            return true
        }
        if (file.name.endsWith(".jar")) {
            return true
        }
        return false
    }

    static File getDestFile(JarInput jarInput, TransformOutputProvider outputProvider) {
        def destName = jarInput.name
        // 重名名输出文件,因为可能同名,会覆盖
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4)
        }
        // 获得输出文件
        File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        return dest
    }

}