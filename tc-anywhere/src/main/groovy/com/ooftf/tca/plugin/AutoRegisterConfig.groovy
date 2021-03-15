package com.ooftf.tca.plugin
/**
 * aop的配置信息
 * @author billy.qi* @since 17/3/28 11:48
 */
class AutoRegisterConfig {

    public ArrayList<RegisterInfo> registerInfo = []

    AutoRegisterConfig() {}

    public void tryCatch(String className, String methodName) {
        registerInfo.add(new RegisterInfo(className, methodName))
    }

}