package com.arcsoft.db_processor;

import com.arcsoft.db_annotation.DBColumn;
import com.arcsoft.db_annotation.DBConstant;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;


@AutoService(Processor.class)
public class DBProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Elements mElementUtils;
    private String mModuleName;
    private Map<String, DBClassCreatorProxy> mProxyMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mElementUtils = processingEnv.getElementUtils();
        Map<String, String> options = processingEnv.getOptions();
        if(null != options){
            mModuleName = options.get(DBConstant.DBCOLUMN_OPTIONS_KEY);
            for(String key : options.keySet()){
                mMessager.printMessage(Diagnostic.Kind.NOTE, "options:" + key + ":" + options.get(key));
            }
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(DBColumn.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        mMessager.printMessage(Diagnostic.Kind.NOTE, "DBProcessor processing...");
        mProxyMap.clear();
        //是否有主键
        boolean primary = false;
        //得到所有的注解
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(DBColumn.class);
        if(elements.isEmpty()){
            mMessager.printMessage(Diagnostic.Kind.NOTE, "##############annotate is empty");
            return false;
        }
        for (Element element : elements) {
            VariableElement variableElement = (VariableElement) element;
            TypeElement classElement = (TypeElement) variableElement.getEnclosingElement();
            String fullClassName = classElement.getQualifiedName().toString();
            mMessager.printMessage(Diagnostic.Kind.NOTE, String.format(">>>>>>>>>>className=[%s]", fullClassName));
            //elements的信息保存到mProxyMap中
            DBClassCreatorProxy proxy = mProxyMap.get(fullClassName);
            if (proxy == null) {
                proxy = new DBClassCreatorProxy(mElementUtils, classElement);
                mProxyMap.put(fullClassName, proxy);
            }
            DBColumn bindAnnotation = variableElement.getAnnotation(DBColumn.class);
            String columnName = bindAnnotation.name();
            if(null == columnName || columnName.isEmpty()){
                columnName = variableElement.getSimpleName().toString();
            }
            if(!primary){
                primary = bindAnnotation.primary();
                if(primary){
                    proxy.setPrimaryColumn(columnName);
                    mMessager.printMessage(Diagnostic.Kind.NOTE, "##############setPrimaryColumn################" + columnName);
                }
            }else if(bindAnnotation.primary()){
                //throw new RuntimeException("Only one primary key you can set!");
            }

            mMessager.printMessage(Diagnostic.Kind.NOTE, String.format(">>>>>>>>>>default=[%s], columnName=[%s], primary=[%s]", variableElement.getSimpleName().toString(), columnName, primary));
            proxy.putElement(columnName, variableElement);
        }
        if(!primary){
            throw new RuntimeException("you should set primary key!");
        }

        //通过javapoet生成
        for (String key : mProxyMap.keySet()) {
            DBClassCreatorProxy proxyInfo = mProxyMap.get(key);
            buildJaveFile(proxyInfo.getPackageName(), proxyInfo.generateJavaCode2());

            DBConditionClassCreatorProxy conditionProxyInf = new DBConditionClassCreatorProxy(proxyInfo.getModelName(), proxyInfo.getModelClassName(), proxyInfo.getColumnNames());
            buildJaveFile(proxyInfo.getPackageName(), conditionProxyInf.generateJavaCode());
        }

        DBModelClassRegisterProxy proxyInfo = new DBModelClassRegisterProxy(mProxyMap.keySet(), mModuleName);
        buildJaveFile(proxyInfo.getPackageName(), proxyInfo.generateJavaCode2());
        mMessager.printMessage(Diagnostic.Kind.NOTE, "process finish ...");
        return true;
    }

    private void buildJaveFile(String pkgName, TypeSpec typeSpec){
        JavaFile javaFile = JavaFile.builder(pkgName, typeSpec).build();
        try {
            //　生成文件
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
