package com.ooftf.tca.plugin
/**
 * aop的配置信息
 * @author billy.qi* @since 17/3/28 11:48
 */
class TCInfo {
    //以下是可配置参数
    String className = ''
    String[] methodName = ''

    TCInfo(String className, String[] methodName) {
        this.className = className
        this.methodName = methodName
    }

}