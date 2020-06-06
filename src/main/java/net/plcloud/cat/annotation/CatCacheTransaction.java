package net.plcloud.cat.annotation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CatCacheTransaction {
    String name() default "";
    String server() default "rocket";

}