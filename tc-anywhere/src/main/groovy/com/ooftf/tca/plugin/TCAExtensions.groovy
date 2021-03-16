package com.ooftf.tca.plugin
/**
 * aop的配置信息
 * @author billy.qi* @since 17/3/28 11:48
 */
class TCAExtensions {

    public ArrayList<TCInfo> registerInfo = []

    TCAExtensions() {}

    public void tc(String className, String[] methodName) {
        registerInfo.add(new TCInfo(className, methodName))
    }

}