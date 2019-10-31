package com.arcsoft.db_processor;

import com.arcsoft.db_annotation.DBConstant;
import com.arcsoft.db_annotation.IDBRegisterModel;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;


public class DBModelClassRegisterProxy {
    private String mPackageName;
    private String mBindingClassName;
    private Set<String> keySets;

    public DBModelClassRegisterProxy(Set<String> keySets, String moduleName) {
        this.keySets = keySets;
        this.mPackageName = DBConstant.MODEL_REGISTER_PKG_CONSTANT;
        this.mBindingClassName = DBConstant.MODEL_REGISTER_CLASS + "$" + moduleName;
    }



    /**
     * 创建Java代码
     * javapoet
     *
     * @return
     */
    public TypeSpec generateJavaCode2() {
        StringBuilder sb = new StringBuilder();
        for (String path : keySets) {
            sb.append(String.format("\"%s\",", path));
        }

        FieldSpec modelsField = FieldSpec.builder(String[].class, "modelPaths")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("{$N}", sb.toString().substring(0, sb.toString().length()-1))
                .build();

        TypeSpec bindingClass = TypeSpec.classBuilder(mBindingClassName)
                .addModifiers(Modifier.PUBLIC)
                .addField(modelsField)
                .addSuperinterface(IDBRegisterModel.class)
                .addMethod(generateGetPath())
                .build();
        return bindingClass;

    }

    public MethodSpec generateGetPath() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getPaths")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String[].class);

        methodBuilder.addStatement("return modelPaths");

        return methodBuilder.build();
    }

    public String getPackageName() {
        return mPackageName;
    }


}
