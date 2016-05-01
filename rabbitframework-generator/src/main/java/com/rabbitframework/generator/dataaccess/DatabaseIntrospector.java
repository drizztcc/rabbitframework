package com.rabbitframework.generator.dataaccess;

import com.rabbitframework.generator.builder.Configuration;
import com.rabbitframework.generator.builder.TableConfiguration;
import com.rabbitframework.generator.builder.TableType;
import com.rabbitframework.generator.exceptions.GeneratorException;
import com.rabbitframework.generator.mapping.EntityMapping;
import com.rabbitframework.generator.mapping.EntityProperty;
import com.rabbitframework.generator.utils.JavaBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DatabaseIntrospector {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseIntrospector.class);
    private DatabaseMetaData databaseMetaData;
    private Configuration configuration;

    public DatabaseIntrospector(DatabaseMetaData databaseMetaData, Configuration configuration) {
        this.databaseMetaData = databaseMetaData;
        this.configuration = configuration;
    }

    public List<EntityMapping> introspectTables() {
        logger.debug("call introspectTables();");
        List<EntityMapping> entityMappings = null;
        try {
            //获取产品名称
            String productName = databaseMetaData.getDatabaseProductName();
            logger.debug("productName:" + productName);
            TableConfiguration tableConfiguration = configuration.getTableConfiguration();
            TableType tableType = tableConfiguration.getTableType();
            if (tableType == TableType.ALL) {
                tableConfiguration.setTableMapping(getTablesName());
            }
            entityMappings = introspectTables(tableConfiguration);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GeneratorException(e);
        }
        return entityMappings;
    }

    private List<EntityMapping> introspectTables(TableConfiguration tableConfiguration) {
        Map<String, String> tableMapping = tableConfiguration.getTableMapping();
        List<EntityMapping> entityMappings = new ArrayList<EntityMapping>();
        Set<Map.Entry<String, String>> sets = tableMapping.entrySet();
        for (Map.Entry<String, String> entry : sets) {
            String tableName = entry.getKey();
            String objectName = entry.getValue();
            EntityMapping entityMapping = getColumns(tableName, objectName);
            calculateExtraColumnInformation(entityMapping);
            entityMappings.add(entityMapping);
        }
        return entityMappings;
    }

    private void calculateExtraColumnInformation(EntityMapping entityMapping) {
        List<EntityProperty> entityProperties = entityMapping.getEntityProperties();
        for (EntityProperty entityProperty : entityProperties) {
            entityProperty.setJavaProperty(JavaBeanUtils.ConverDbNameToPropertyName(entityProperty.getColumnName(), true));
        }
    }

    public EntityMapping getColumns(String tableName, String objectName) {
        ResultSet resultSet = null;
        EntityMapping entityMapping = new EntityMapping();
        entityMapping.setTableName(tableName);
        entityMapping.setObjectName(JavaBeanUtils.ConverDbNameToPropertyName(objectName, true));
        List<EntityProperty> entityProperties = new ArrayList<EntityProperty>();
        entityMapping.setEntityProperties(entityProperties);
        try {
            resultSet = databaseMetaData.getColumns(null, null, tableName, null);
            while (resultSet.next()) {
                EntityProperty tableColumn = new EntityProperty();
                int jdbcType = resultSet.getInt("DATA_TYPE");
                int length = resultSet.getInt("COLUMN_SIZE");
                String columnName = resultSet.getString("COLUMN_NAME");
                boolean nullable = resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                int scale = resultSet.getInt("DECIMAL_DIGITS");
                String remarks = resultSet.getString("REMARKS");
                String defaultValue = resultSet.getString("COLUMN_DEF");
                String tableCat = resultSet.getString("TABLE_CAT");
                String tableSchem = resultSet.getString("TABLE_SCHEM");
                String tableNameDb = resultSet.getString("TABLE_NAME");
                logger.debug("jdbcType:" + jdbcType + ",length:" + length + ",columnName:" + columnName + ",nullable:"
                        + nullable + ",scale:" + scale + ",remarks:" + remarks + "defaultValue:" + defaultValue + ",tableCat:" + tableCat +
                        ",tableSchem:" + tableSchem + ",tableNameDb:" + tableNameDb);
                tableColumn.setJdbcType(jdbcType);
                tableColumn.setLength(length);
                tableColumn.setColumnName(columnName);
                tableColumn
                        .setNullable(nullable);
                tableColumn.setScale(scale);
                tableColumn.setRemarks(remarks);
                tableColumn.setDefaultValue(defaultValue);
                entityProperties.add(tableColumn);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GeneratorException(e);
        } finally {
            close(resultSet);
        }
        return entityMapping;
    }


    public void close(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (Exception e) {
            }
        }
    }

    public Map<String, String> getTablesName() {
        ResultSet resultSet = null;
        Map<String, String> map = new HashMap<String, String>();
        try {
            resultSet = databaseMetaData.getTables(configuration.getEnvironment().getDatabaseName(), null, null, new String[]{"TABLE"});
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                String objectName = JavaBeanUtils.ConverDbNameToPropertyName(tableName, true);
                map.put(tableName, objectName);

            }
        } catch (Exception e) {

        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                }
            }
        }
        return map;
    }
}