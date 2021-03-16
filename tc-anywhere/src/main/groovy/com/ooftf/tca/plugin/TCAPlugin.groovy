package com.ooftf.tca.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 自动注册插件入口
 * @author billy.qi* @since 17/3/14 17:35
 */
public class TCAPlugin implements Plugin<Project> {
    public static final String EXT_NAME = 'tca'

    @Override
    public void apply(Project project) {
        /**
         * 注册transform接口
         */
        def isApp = project.plugins.hasPlugin(AppPlugin)
        project.extensions.create(EXT_NAME, TCAExtensions)
        if (isApp) {
            println 'project(' + project.name + ') apply tc-anywhere plugin'
            def android = project.extensions.getByType(AppExtension)
            def transformImpl = new TCATransform(project)
            android.registerTransform(transformImpl)
            project.afterEvaluate {
                init(project, transformImpl)//此处要先于transformImpl.transform方法执行
            }
        }
    }

    static void init(Project project, TCATransform transformImpl) {
        TCAExtensions config = project.extensions.findByName(EXT_NAME) as TCAExtensions
        transformImpl.config = config
    }

}
