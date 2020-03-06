package com.arcsoft.db_processor;

import com.arcsoft.db_annotation.DBConstant;
import com.arcsoft.db_annotation.IDBConditionBuilder;
import com.arcsoft.db_annotation.IDBProtocol;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;


public class DBConditionClassCreatorProxy {
    private String mBindingClassName;
    private String[] columnNames;
    private ClassName modelClassName;

    public DBConditionClassCreatorProxy(String modelName, ClassName modelClassName, String[] columnNames) {
        this.columnNames = columnNames;
        this.modelClassName = modelClassName;
        this.mBindingClassName = modelName + DBConstant.PROCESSOR_CLASS_CONDITION_SUFFIX;
    }

    /**
     * 创建Java代码
     * javapoet
     *
     * @return
     */
    public TypeSpec generateJavaCode() {
        FieldSpec selectionStrField = FieldSpec.builder(StringBuilder.class, "selectionStr")
                .addModifiers(Modifier.PRIVATE)
                .initializer("new StringBuilder()")
                .build();

        FieldSpec valuesField = FieldSpec.builder(ParameterizedTypeName.get(List.class, String.class), "values")
                .addModifiers(Modifier.PRIVATE)
                .initializer("new $T<>()", ArrayList.class)
                .build();

        FieldSpec protocolField = FieldSpec.builder(IDBProtocol.class, "protocol")
                .addModifiers(Modifier.PRIVATE)
                .build();


        TypeSpec.Builder builder = TypeSpec.classBuilder(mBindingClassName)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(IDBConditionBuilder.class), modelClassName))
                .addModifiers(Modifier.PUBLIC)
                .addField(selectionStrField)
                .addField(valuesField)
                .addField(protocolField)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(IDBProtocol.class, "protocol")
                        .addStatement("this.protocol = protocol")
                        .build())
                .addMethod(generateWhere())
                .addMethod(generateAnd())
                .addMethod(generateOr())
                .addMethod(generateQuery());

        createColumnClass(builder);


        return builder.build();

    }

    private void createColumnClass(TypeSpec.Builder builder) {
        for (String columnName : columnNames) {
            builder.addType(TypeSpec.classBuilder(columnName)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addMethod(MethodSpec.methodBuilder("is")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(String[].class)
                            .addParameter(String.class, "value")
                            .addStatement("String[] selection = new String[2]")
                            .addStatement("selection[0] = \"$N=?\"", columnName)
                            .addStatement("selection[1] = value")
                            .addStatement("return selection")
                            .build())
                    .build());
        }
    }


    private MethodSpec generateWhere() {
        return MethodSpec.methodBuilder("where")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String[].class, "selection")
                .addStatement("selectionStr.append(selection[0])")
                .addStatement("values.add(selection[1])")
                .addStatement(" return this")
                .returns(IDBConditionBuilder.class)
                .build();
    }

    private MethodSpec generateAnd() {
        return MethodSpec.methodBuilder("and")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String[].class, "selection")
                .addStatement("selectionStr.append(\" AND \")")
                .addStatement("where(selection)")
                .addStatement(" return this")
                .returns(IDBConditionBuilder.class)
                .build();
    }


    private MethodSpec generateOr() {
        return MethodSpec.methodBuilder("or")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String[].class, "selection")
                .addStatement("selectionStr.append(\" OR \")")
                .addStatement("where(selection)")
                .addStatement(" return this")
                .returns(IDBConditionBuilder.class)
                .build();
    }


    private MethodSpec generateQuery() {
        return MethodSpec.methodBuilder("query")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return protocol.queryByCondition(selectionStr.toString(), values.toArray(new String[values.size()]))")
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), modelClassName))
                .build();
    }

}
