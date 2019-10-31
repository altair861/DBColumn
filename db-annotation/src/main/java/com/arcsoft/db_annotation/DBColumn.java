package com.arcsoft.db_annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface DBColumn {
    /**
     * 设置列名
     * @return
     */
    String name() default "";

    /**
     * 是否为主键
     * @return
     */
    boolean primary() default false;
}
