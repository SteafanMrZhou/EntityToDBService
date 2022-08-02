package com.steafan.cits.service.user.service.impl;

import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Steafan(zyf)
 * @create 2022/7/30 20:02
 * @desc 根据数据库实体类创建数据库和对应的数据库表
 */
@Log4j2
public class EntityToDBService {

    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    private static final String DB_HOST = "jdbc:mysql://127.0.0.1:3306/?";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
    private static final String DB_NAME = "cits";

    private static Connection mConnection = null;


    static {
        try {
            Class.forName(JDBC_DRIVER);
            mConnection = DriverManager.getConnection(DB_HOST, DB_USER, DB_PASS);
        } catch (ClassNotFoundException | SQLException e) {
            log.error("没有找到数据库驱动程序，或当前数据库驱动程序版本不兼容，或数据库URL地址账号信息错误");
        }
    }

    /**
     * 从指定的package中获取所有的Class
     *
     * @param packageName 包路径
     * @return 扫描到的Class类型对象
     */
    private static List<Class<?>> getClassesByPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    classes.addAll(findClassByPackageDirectory(packageName, filePath));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    /**
     * 从指定的package中获取所有的Class，实际实现方法
     *
     * @param packageName 包路径
     * @param packagePath 包下文件路径
     * @return 扫描到的Class类型对象
     */
    private static List<Class<?>> findClassByPackageDirectory(String packageName, String packagePath) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>(0);
        }

        File[] dirs = dir.listFiles();
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (File file : dirs) {
            // 如果是目录，则继续扫描：请根据实际需求决定是否扫描子目录中的实体
//            if (file.isDirectory()) {
//                classes.addAll(findClassByDirectory(packageName + "." + file.getName(),
//                        file.getAbsolutePath()));
//            }
            if (file.getName().endsWith(".class")) {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return classes;
    }

    /**
     * 根据项目中的实体类生成数据库和对应的数据表
     *
     * @param dbName 需要生成的数据库名称，如果该名称已经存在，那么该服务会直接终止，不会继续生成数据表
     * @param packageName 项目实体类所在的包路径，例如：com.steafan.cits.pojo
     * @return 数据库或数据表生成结果
     * @throws SQLException SQL执行异常
     */
    public static String entityToDB(String dbName, String packageName) throws SQLException {
        String dataBaseIsExistsSQL = "SHOW DATABASES LIKE \"" + DB_NAME + "\"";
        PreparedStatement dataBaseIsExistsPreparedStatement = mConnection.prepareStatement(dataBaseIsExistsSQL);
        ResultSet dataBaseIsExistsResultSet = dataBaseIsExistsPreparedStatement.executeQuery();
        if (dataBaseIsExistsResultSet.next()) {
            return "当前数据库" + DB_NAME + "已经存在，无需重复创建";
        } else {
            String dataBaseCreateSQL = "CREATE DATABASE" + " " + dbName;
            PreparedStatement dataBaseCreatePreparedStatement = mConnection.prepareStatement(dataBaseCreateSQL);
            int dataBaseCreateResultCount = dataBaseCreatePreparedStatement.executeUpdate();
            if (dataBaseCreateResultCount > 0) {
                String dataBaseUseSQL = "USE" + " " + dbName;
                mConnection.prepareStatement(dataBaseUseSQL).execute();
                List<Class<?>> clazzsFromPackageList = getClassesByPackage(packageName);
                for (Class<?> sourceEntity : clazzsFromPackageList) {
                    String dataTableIsExistsSQL = "SHOW TABLES LIKE \"" + dealEntityForTableName(sourceEntity) + "\"";
                    PreparedStatement dataTableIsExistsPreparedStatement = mConnection.prepareStatement(dataTableIsExistsSQL);
                    if (dataTableIsExistsPreparedStatement.executeQuery().next()) {
                        return "当前数据表已存在，请检查后重新执行该操作";
                    }
                    StringBuilder dataTableCreateSQLBuilder = new StringBuilder();
                    dataTableCreateSQLBuilder.append("CREATE TABLE " + dealEntityForTableName(sourceEntity) + "(");
                    dataTableCreateSQLBuilder.append(dealEntityFieldsForTableParams(sourceEntity.getDeclaredFields()));
                    dataTableCreateSQLBuilder.append(")");
                    PreparedStatement dataTableCreatePreparedStatement = mConnection.prepareStatement(dataTableCreateSQLBuilder.toString());
                    int resultCount = dataTableCreatePreparedStatement.executeUpdate();
                    if (resultCount == 0) {
                        log.info("数据表" + dealEntityForTableName(sourceEntity) + "创建成功");
                    } else {
                        log.info("数据表" + dealEntityForTableName(sourceEntity) + "创建失败");
                    }
                }
            } else {
                return "数据库" + DB_NAME + "创建失败";
            }
        }
        return "";
    }

    /**
     * 根据实体对象字段属性生成对应数据库SQL
     *
     * @param declaredFields 实体对象字段数组
     * @return 实体对象字段对应数据库SQL
     */
    private static String dealEntityFieldsForTableParams(Field[] declaredFields) {
        String tableTypeAndLength = "";
        if (!ArrayUtils.isEmpty(declaredFields)) {
            for (Field field : declaredFields) {
                if (field.getType() == String.class) {
                    tableTypeAndLength += dealEntityFieldsNameForTableParamsName(field.getName()) + " " + "VARCHAR(255),";
                } else if (field.getName().equals("id") && (field.getType() == int.class || field.getType() == Integer.class)) {
                    tableTypeAndLength = "id INT(11) PRIMARY KEY AUTO_INCREMENT,";
                } else if (field.getType() == int.class || field.getType() == Integer.class) {
                    tableTypeAndLength += dealEntityFieldsNameForTableParamsName(field.getName()) + " " + "INT(11),";
                } else if (field.getType() == BigDecimal.class) {
                    tableTypeAndLength += dealEntityFieldsNameForTableParamsName(field.getName()) + " " + "DECIMAL(10,2),";
                }
            }
            return tableTypeAndLength.substring(0, tableTypeAndLength.lastIndexOf(","));
        }
        return "获取实体对象字段失败，或实体对象中不存在任何字段";
    }

    /**
     * 根据实体中字段名称生成符合规范的数据表字段名称
     *
     * @param fieldName 实体中字段名称
     * @return 符合规范的数据表字段名称
     */
    private static String dealEntityFieldsNameForTableParamsName(String fieldName) {
        if (!StringUtils.isEmpty(fieldName)) {
            char[] filedNameCharArray = fieldName.toCharArray();
            List<String> filedNameUpperCharacterList = Lists.newArrayList();
            for (char filedNameCharacter : filedNameCharArray) {
                if (Character.isUpperCase(filedNameCharacter)) {
                    filedNameUpperCharacterList.add(filedNameCharacter + "");
                }
            }
            if (!CollectionUtils.isEmpty(filedNameUpperCharacterList)) {
                for (String fileNameUpperCharacter : filedNameUpperCharacterList) {
                    fieldName = fieldName.replace(fileNameUpperCharacter, "_" + fileNameUpperCharacter.toLowerCase());
                }
                return fieldName;
            } else {
                return fieldName;
            }
        }
        return "获取字段名称异常，或实体中没有任何字段";
    }

    /**
     * 根据实体名称生成符合规范的数据库表名称
     *
     * @param sourceEntity 实体源class对象
     * @return 符合规范的数据库表名称
     */
    private static String dealEntityForTableName(Class<?> sourceEntity) {
        String clazzSimpleName = sourceEntity.getSimpleName();
        if (StringUtils.isEmpty(clazzSimpleName)) {
            return "获取类名失败，请重新获取";
        }
        char[] clazzSimpleNameCharArray = clazzSimpleName.toCharArray();
        List<String> upperCaseCharacterList = Lists.newArrayList();
        for (char clazzSimpleNameCharacter : clazzSimpleNameCharArray) {
            if (Character.isUpperCase(clazzSimpleNameCharacter)) {
                upperCaseCharacterList.add(clazzSimpleNameCharacter + "");
            }
        }
        if (!CollectionUtils.isEmpty(upperCaseCharacterList)) {
            if (upperCaseCharacterList.size() == 1) {
                return clazzSimpleName.toLowerCase();
            } else if (upperCaseCharacterList.size() > 1) {
                String dealClazzCharacterName = "";
                String dealFinalClazzCharacterName = "";
                for (String upperCaseCharacter : upperCaseCharacterList) {
                    if (clazzSimpleName.startsWith(upperCaseCharacter)) {
                        dealClazzCharacterName = clazzSimpleName.replace(clazzSimpleName.substring(0, 1), upperCaseCharacter);
                    } else {
                        dealFinalClazzCharacterName = dealClazzCharacterName.replace(upperCaseCharacter, "_" + upperCaseCharacter.toLowerCase()).toLowerCase();
                    }
                }
                return dealFinalClazzCharacterName;
            }
        }
        return "实体类命名不符合驼峰桥规范，请修改后使用";
    }

}
