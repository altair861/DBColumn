package com.arcsoft.db_processor;

import com.arcsoft.db_annotation.DBConstant;
import com.arcsoft.db_annotation.IDBConditionBuilder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;


public class DBClassCreatorProxy {
    private String mBindingClassName;
    private String mPackageName;
    private TypeElement mTypeElement;
    private String tableNameStr;
    private Map<String, VariableElement> mVariableElementMap = new HashMap<>();

    private ClassName mClassNameModel;
    private ClassName mClassNameContentValues;
    private ClassName mClassNameSQLiteDatabase;
    private ClassName mClassNameCursor;
    private TypeName mClassNameProtocol;
    private String primaryColumn = "";
    //是否有主键
    boolean primary = false;

    public DBClassCreatorProxy(Elements elementUtils, TypeElement classElement) {
        this.mTypeElement = classElement;
        PackageElement packageElement = elementUtils.getPackageOf(mTypeElement);
        String packageName = packageElement.getQualifiedName().toString();
        String className = mTypeElement.getSimpleName().toString();
        tableNameStr = className;
        this.mPackageName = DBConstant.MODEL_REGISTER_PKG + "." + packageName;
        this.mBindingClassName = className + DBConstant.PROCESSOR_CLASS_SUFFIX;
        mClassNameModel = ClassName.bestGuess(mTypeElement.getQualifiedName().toString());
        mClassNameContentValues = ClassName.bestGuess("android.content.ContentValues");
        mClassNameSQLiteDatabase = ClassName.bestGuess("net.sqlcipher.database.SQLiteDatabase");
        mClassNameCursor = ClassName.bestGuess("android.database.Cursor");
        mClassNameProtocol = ParameterizedTypeName.get(ClassName.bestGuess("com.arcsoft.db_annotation.IDBProtocol"), mClassNameSQLiteDatabase, mClassNameModel);

    }

    public void putElement(String columnName, VariableElement element) {
        mVariableElementMap.put(columnName, element);
    }

    public String[] getColumnNames(){
        String[] columnNames = new String[mVariableElementMap.size()];
        int index = 0;
        for (String columnName : mVariableElementMap.keySet()) {
            columnNames[index] = columnName;
            index ++;
        }
        return columnNames;
    }

    public boolean hasPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * 创建Java代码
     * javapoet
     *
     * @return
     */
    public TypeSpec generateJavaCode2() {
        StringBuilder sb = new StringBuilder();
        for (String columnName : mVariableElementMap.keySet()) {
            sb.append(String.format("\"%s\",", columnName));
        }

        FieldSpec columnsField = FieldSpec.builder(String[].class, "columns")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("{$N}", sb.toString().substring(0, sb.toString().length()-1))
                .build();

        FieldSpec tableName = FieldSpec.builder(String.class, "tableName")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .initializer("$S", tableNameStr)
                .build();




        TypeSpec bindingClass = TypeSpec.classBuilder(mBindingClassName)
                .addModifiers(Modifier.PUBLIC)
                .addField(columnsField)
                .addField(tableName)
                .addField(mClassNameSQLiteDatabase, "db", Modifier.PRIVATE)
                .addMethod(generateInit())
                .addMethod(generateCreate())
                .addMethod(generateInsertByModel())
                .addMethod(generateInsertByValues())
                .addMethod(generateInsertOrUpdate())
                .addMethod(generateInsertOrUpdates())
                .addMethod(generateDelete())
                .addMethod(generateDeleteByPrimary())
                .addMethod(generateUpdateByValues())
                .addMethod(generateUpdateByModel())
                .addMethod(generateQueryAll())
                .addMethod(generateSelect())
//                .addMethod(generateQueryByKey())
//                .addMethod(generateQueryByKeyAnd())
//                .addMethod(generateQueryByKeyOr())
                .addMethod(generateQueryByKeys())
                .addMethod(generateQueryByCondition())
                .addMethod(generateExist())
                .addMethod(generateIsExistTable())
                .addMethod(generateModel2Values())
                .addMethod(generateUpdateTableColumn())
                .addSuperinterface(mClassNameProtocol)
                .build();
        return bindingClass;

    }

    public void setPrimaryColumn(String primaryColumn){
        this.primaryColumn = primaryColumn;
    }


    private MethodSpec generateInit(){
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("init")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mClassNameSQLiteDatabase, "db")
                .returns(void.class);

        methodBuilder.addStatement("this.db = db");
        return methodBuilder.build();
    }

    private MethodSpec generateCreate(){
        StringBuilder sb = new StringBuilder();
        for (String columnName : mVariableElementMap.keySet()) {
            VariableElement element = mVariableElementMap.get(columnName);
            String type = element.asType().toString();
            if(columnName.equals(primaryColumn)){
                sb.append(String.format("%s %s primary key,", columnName, getColumnType(type)));
            }else{
                sb.append(String.format("%s %s,", columnName, getColumnType(type)));
            }
        }

        String createSqlStr = String.format("CREATE TABLE IF NOT EXISTS %s  (%s)",
                tableNameStr, sb.toString().substring(0, sb.toString().length() -1));

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class);

        methodBuilder.addStatement("db.execSQL($S)", createSqlStr);
        return methodBuilder.build();
    }

    private MethodSpec generateInsertByModel(){
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("insert")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addParameter(mClassNameModel, "model")
                .addStatement("$T values = model2Values(model)", mClassNameContentValues);

        methodBuilder.addStatement("return insert(values)");
        return methodBuilder.build();
    }
    private MethodSpec generateInsertByValues(){
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("insert")
                .addModifiers(Modifier.PUBLIC)
                .returns(long.class)
                .addParameter(mClassNameContentValues, "values");
        methodBuilder.addStatement("return db.insertOrThrow(tableName, null, values)");
        return methodBuilder.build();
    }

    private MethodSpec generateModel2Values(){
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("model2Values")
                .addModifiers(Modifier.PUBLIC)
                .returns(mClassNameContentValues)
                .addParameter(mClassNameModel, "model")
                .addStatement("$T values = new $T()", mClassNameContentValues, mClassNameContentValues);

        for (String columnName : mVariableElementMap.keySet()) {
            VariableElement element = mVariableElementMap.get(columnName);
            String name = upperFirstLatter(element.getSimpleName().toString());
            String type = element.asType().toString();
            if(isBooleanType(type)){
                methodBuilder.addStatement("values.put($S, model.is$N() ? 1 : 0)", columnName, name);
            }else{
                methodBuilder.addStatement("values.put($S, model.get$N())", columnName, name);
            }

        }
        methodBuilder.addStatement("return values");
        return methodBuilder.build();
    }

    private MethodSpec generateInsertOrUpdate() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("insertOrUpdate")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mClassNameModel, "model")
                .addStatement("$T values = model2Values(model)", mClassNameContentValues)
                .returns(long.class);

        methodBuilder.beginControlFlow("if(isExist(values.get($S)))", primaryColumn);
        methodBuilder.addStatement("return update(values)");
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("return insert(values)");
        methodBuilder.endControlFlow();

        return methodBuilder.build();
    }

    private MethodSpec generateInsertOrUpdates() {
        ClassName list = ClassName.get("java.util", "List");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, mClassNameModel);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("insertOrUpdates")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(listOfHoverboards, "models")
                .returns(void.class);

        methodBuilder.addStatement("db.beginTransaction()");
        methodBuilder.beginControlFlow("for ($T model : models)", mClassNameModel);
        methodBuilder.addStatement("insertOrUpdate(model)");
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("db.setTransactionSuccessful()");
        methodBuilder.addStatement("db.endTransaction()");
        return methodBuilder.build();
    }


    private boolean isBooleanType(String type){
        return type.endsWith("boolean");
    }

    private String getColumnType(String type){
        if(type.endsWith("int") || type.endsWith("boolean")){
            return "INTEGER";
        }else if(type.endsWith("long")){
            return "LONG";
        }else{
            return "TEXT";
        }
    }

    private MethodSpec generateQueryAll() {
        ClassName list = ClassName.get("java.util", "List");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, mClassNameModel);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("queryAll")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfHoverboards);

        methodBuilder.addStatement("return queryByCondition(null, null)");

        return methodBuilder.build();
    }

    /**
     *  public IDBConditonBuilder select(){
     *     return new Student_ConditionBuilder(this);
     *   }
     * @return
     */
    private MethodSpec generateSelect() {
        ClassName list = ClassName.bestGuess(getModelName() + DBConstant.PROCESSOR_CLASS_CONDITION_SUFFIX);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("select")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return new $T(this)", list)
                .returns(IDBConditionBuilder.class);
        return methodBuilder.build();
    }

    private MethodSpec generateQueryByKey() {
        ClassName list = ClassName.get("java.util", "List");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, mClassNameModel);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("query")
                .addParameter(String.class, "key")
                .addParameter(String.class, "value")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfHoverboards);

        methodBuilder.addStatement("return queryByCondition(key + \"=?\", new String[]{value})");

        return methodBuilder.build();
    }

    /**
     * private List<Student> query(String[] keys, String[] values, String conditionAppendType) {
     *     StringBuilder sb = new StringBuilder();
     *     for(int i = 0, length = keys.length; i < length; i ++){
     *       sb.append(keys[i] + " =? ");
     *       if(i != length - 1){
     *         sb.append(coditionAppendType);
     *       }
     *     }
     *     return queryByCondition(sb.toString(), values);
     *   }
     * @return
     */
    private MethodSpec generateQueryByKeys() {
        ClassName list = ClassName.get("java.util", "List");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, mClassNameModel);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("query")
                .addParameter(String[].class, "keys")
                .addParameter(String[].class, "values")
                .addParameter(String.class, "conditionAppendType")
                .addModifiers(Modifier.PRIVATE)
                .returns(listOfHoverboards);

        // methodBuilder.addStatement("$T<$T> results  = new $T<>()", List.class, mClassNameModel, ArrayList.class);
        methodBuilder.addStatement("$T sb = new StringBuilder()", StringBuilder.class);
        methodBuilder.beginControlFlow("for(int i = 0, length = keys.length; i < length; i ++)");
        methodBuilder.addStatement("sb.append(keys[i] + \" =? \")");
        methodBuilder.beginControlFlow("if(i != length - 1)");
        methodBuilder.addStatement("sb.append(conditionAppendType)");
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();

        methodBuilder.addStatement("return queryByCondition(sb.toString(), values)");

        return methodBuilder.build();
    }


    private MethodSpec generateQueryByKeyAnd() {
        ClassName list = ClassName.get("java.util", "List");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, mClassNameModel);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("queryAnd")
                .addParameter(String[].class, "keys")
                .addParameter(String[].class, "values")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfHoverboards);

        methodBuilder.addStatement("return query(keys, values, \"AND\")");

        return methodBuilder.build();
    }

    private MethodSpec generateQueryByKeyOr() {
        ClassName list = ClassName.get("java.util", "List");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, mClassNameModel);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("queryOr")
                .addParameter(String[].class, "keys")
                .addParameter(String[].class, "values")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfHoverboards);

        methodBuilder.addStatement("return query(keys, values, \"OR\")");

        return methodBuilder.build();
    }


    private MethodSpec generateQueryByCondition() {
        ClassName list = ClassName.get("java.util", "List");
        TypeName listOfHoverboards = ParameterizedTypeName.get(list, mClassNameModel);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("queryByCondition")
                .addParameter(String.class, "selection")
                .addParameter(String[].class, "selectionArgs")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfHoverboards);


        methodBuilder.addStatement("$T<$T> results  = new $T<>()", List.class, mClassNameModel, ArrayList.class);
        methodBuilder.addStatement("$T cursor = db.query(tableName, columns, selection, selectionArgs, null, null, null)", mClassNameCursor);
        methodBuilder.beginControlFlow("if(null != cursor)");
        methodBuilder.beginControlFlow("for (cursor.moveToFirst(); !cursor.isAfterLast();cursor.moveToNext())");
        methodBuilder.addStatement("$T item = new $T()", mClassNameModel, mClassNameModel);
        for (String columnName : mVariableElementMap.keySet()) {
            VariableElement element = mVariableElementMap.get(columnName);
            String name = upperFirstLatter(element.getSimpleName().toString());
            String type = element.asType().toString();
            boolean isBooleanType = isBooleanType(type);
            type = upperFirstLatter(type.substring(type.lastIndexOf(".") + 1));
            if (isBooleanType){
                methodBuilder.addStatement("item.set$N(1 == cursor.getInt(cursor.getColumnIndex($S)))", name, columnName);
            }else{
                methodBuilder.addStatement("item.set$N(cursor.get$N(cursor.getColumnIndex($S)))", name, type, columnName);
            }

        }
        methodBuilder.addStatement("results.add(item)");
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("if(null != cursor)");
        methodBuilder.addStatement("cursor.close()");
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("return results");

        return methodBuilder.build();
    }

    /**
     * String whereClauses = "id=?";
     * String [] whereArgs = {String.valueOf(2)};
     * //调用delete方法，删除数据
     * db.delete("stu_table", whereClauses, whereArgs);
     * @return
     */
    private MethodSpec generateDelete() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("delete")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "whereClauses")
                .addParameter(String[].class, "whereArgs")
                .returns(int.class);



        methodBuilder.addStatement("return db.delete(tableName, whereClauses, whereArgs)");

        return methodBuilder.build();
    }

    private MethodSpec generateDeleteByPrimary() {MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("deleteByPrimary")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "whereArgs")
                .returns(int.class);

        methodBuilder.addStatement("return delete(\"$N=?\", new String[]{whereArgs})", primaryColumn);

        return methodBuilder.build();
    }


    private MethodSpec generateExist() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("isExist")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Object.class, "whereArgs")
                .returns(boolean.class);

        methodBuilder.addStatement("$T cursor = null", mClassNameCursor);
        methodBuilder.beginControlFlow("try");
        methodBuilder.addStatement("cursor = db.rawQuery(\"SELECT COUNT(*) FROM $N WHERE $N=?\", new String[]{String.valueOf(whereArgs)})", tableNameStr, primaryColumn);
        methodBuilder.beginControlFlow("if(cursor.moveToFirst())");
        methodBuilder.addStatement("return cursor.getInt(0) > 0");
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("return false");
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("finally");
        methodBuilder.beginControlFlow("if(null != cursor)");
        methodBuilder.addStatement("cursor.close()");
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();

        return methodBuilder.build();
    }

    /**
     * String sql = "select count(*) as c from Sqlite_master  where type ='table' and name ='"+tableName.trim()+"' ";
     *             cursor = db.rawQuery(sql, null);
     *             if(cursor.moveToNext()){
     *                 int count = cursor.getInt(0);
     *                 if(count>0){
     *                     result = true;
     *                 }
     *             }
     * @return
     */
    private MethodSpec generateIsExistTable() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("isExistTable")
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class);

        methodBuilder.addStatement("$T cursor = null", mClassNameCursor);
        methodBuilder.beginControlFlow("try");
        methodBuilder.addStatement("cursor = db.rawQuery(\"SELECT COUNT(*) AS c FROM Sqlite_master  WHERE TYPE ='table' AND NAME ='$N'\", null)", tableNameStr);

        methodBuilder.beginControlFlow("if(cursor.moveToFirst())");
        methodBuilder.addStatement("return cursor.getInt(0) > 0");
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("return false");
        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("finally");
        methodBuilder.beginControlFlow("if(null != cursor)");
        methodBuilder.addStatement("cursor.close()");
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();

        return methodBuilder.build();
    }

    /**
     * db.update(tableName, values, whereClause, whereArgs)
     * @return
     */
    private MethodSpec generateUpdateByValues() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mClassNameContentValues, "values")
                .returns(long.class);

        methodBuilder.addStatement("return db.update(tableName, values, \"$N=?\", new String[]{String.valueOf(values.get($S))})", primaryColumn, primaryColumn);

        return methodBuilder.build();
    }

    private MethodSpec generateUpdateByModel() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("update")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(mClassNameModel, "model")
                .returns(long.class);

        methodBuilder.addStatement("return update(model2Values(model))");

        return methodBuilder.build();
    }


    /**
     *  public void updateTable(SQLiteDatabase db, String dbName){
     *     if(isExist(db, dbName)){
     *       Cursor cursor = db.rawQuery("SELECT * FROM User", null);
     *       String[] columnNames = cursor.getColumnNames();
     *       //查找新增的列
     *     }else {
     *       create(db);
     *     }
     *
     *   }
     */
    public MethodSpec generateUpdateTableColumn() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("updateTableColumn")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class);

        methodBuilder.beginControlFlow("if(isExistTable())");

        methodBuilder.addStatement("$T cursor = db.rawQuery(\"SELECT * FROM $N\", null)", mClassNameCursor, tableNameStr);
        methodBuilder.addStatement("String[] columnNames = cursor.getColumnNames()");
         methodBuilder.addStatement("cursor.close()");
        methodBuilder.addStatement("List<String> dbColumns = $T.asList(columnNames)", Arrays.class);
        methodBuilder.addStatement("$T<String, String> modelColumnsMap = new $T<>()", Map.class, HashMap.class);
        for (String columnName : mVariableElementMap.keySet()) {
            VariableElement element = mVariableElementMap.get(columnName);
            String type = element.asType().toString();
            methodBuilder.addStatement("modelColumnsMap.put($S,$S)", columnName, getColumnType(type));
        }
        methodBuilder.beginControlFlow("for(String modelColumn : modelColumnsMap.keySet())");
        methodBuilder.beginControlFlow("if(!dbColumns.contains(modelColumn))");

        methodBuilder.addStatement("String addColumn = String.format(\"ALTER TABLE $N ADD COLUMN %s %s\", modelColumn, modelColumnsMap.get(modelColumn))", tableNameStr);
        methodBuilder.addStatement("db.execSQL(addColumn)");
        methodBuilder.endControlFlow();
        methodBuilder.endControlFlow();




        methodBuilder.endControlFlow();
        methodBuilder.beginControlFlow("else");
        methodBuilder.addStatement("create()");
        methodBuilder.endControlFlow();

        return methodBuilder.build();
    }



    public String getPackageName() {
        return mPackageName;
    }

    public String getModelName(){
        return tableNameStr;
    }


    public ClassName getModelClassName(){
        return mClassNameModel;
    }

    public String upperFirstLatter(String letter){
        char[] chars = letter.toCharArray();
        if(chars[0]>='a' && chars[0]<='z'){
            chars[0] = (char) (chars[0]-32);
        }
        return new String(chars);
    }
}
