package net.plcloud.cat.dubbo;

import com.dianping.cat.Cat;

public class DubboCat {
    
    private static boolean isEnable=true;

    /**
     * 禁用dubbo cat
     */
    public static void disable(){
        isEnable=false;
    }

    /**
     * 启用dubbo cat
     */
    public static void enable(){
        isEnable=true;
    }

    /**
     * 是否有效
     * @return
     */
    public static boolean isEnable(){
        return Cat.getManager().isCatEnabled()&&isEnable;
    }
}
