/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.*;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * GenericCatalog
 */
public class MySQLCatalog implements DBSCatalog, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer
{

    final TableCache tableCache = new TableCache();
    final ProceduresCache proceduresCache = new ProceduresCache();
    final TriggerCache triggerCache = new TriggerCache();
    final ConstraintCache constraintCache = new ConstraintCache(tableCache);
    final IndexCache indexCache = new IndexCache(tableCache);
    final EventCache eventCache = new EventCache();

    private MySQLDataSource dataSource;
    private String name;
    private MySQLCharset defaultCharset;
    private String defaultCollation;
    private String sqlPath;
    private boolean persisted;

    public MySQLCatalog(MySQLDataSource dataSource, ResultSet dbResult)
    {
        tableCache.setCaseSensitive(false);
        this.dataSource = dataSource;
        if (dbResult != null) {
            this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SCHEMA_NAME);
            defaultCharset = dataSource.getCharset(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_CHARACTER_SET_NAME));
            defaultCollation = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_COLLATION_NAME);
            sqlPath = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SQL_PATH);
            persisted = true;
        } else {
            defaultCharset = dataSource.getCharset("utf8");
            defaultCollation = "utf8_general_ci";
            sqlPath = "";
            persisted = false;
        }
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Property(viewable = true, order = 2)
    public MySQLCharset getDefaultCharset()
    {
        return defaultCharset;
    }

    public void setDefaultCharset(MySQLCharset defaultCharset)
    {
        this.defaultCharset = defaultCharset;
    }

    @Property(viewable = true, order = 3)
    public String getDefaultCollation()
    {
        return defaultCollation;
    }

    public void setDefaultCollation(String defaultCollation)
    {
        this.defaultCollation = defaultCollation;
    }

    @Property(viewable = true, order = 3)
    public String getSqlPath()
    {
        return sqlPath;
    }

    void setSqlPath(String sqlPath)
    {
        this.sqlPath = sqlPath;
    }

    public TableCache getTableCache()
    {
        return tableCache;
    }

    public ProceduresCache getProceduresCache()
    {
        return proceduresCache;
    }

    public TriggerCache getTriggerCache()
    {
        return triggerCache;
    }

    public ConstraintCache getConstraintCache()
    {
        return constraintCache;
    }

    public IndexCache getIndexCache()
    {
        return indexCache;
    }

    public EventCache getEventCache() {
        return eventCache;
    }

    @Association
    public Collection<MySQLTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, this, null);
    }

    @Association
    public Collection<MySQLTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, MySQLTable.class);
    }

    public MySQLTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, MySQLTable.class);
    }

    @Association
    public Collection<MySQLView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, MySQLView.class);
    }

    @Association
    public Collection<MySQLProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    public MySQLProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return proceduresCache.getObject(monitor, this, procName);
    }

    @Association
    public Collection<MySQLTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    public MySQLTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<MySQLEvent> getEvents(DBRProgressMonitor monitor)
        throws DBException
    {
        return eventCache.getAllObjects(monitor, this);
    }

    @Override
    public Collection<MySQLTableBase> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public MySQLTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return MySQLTable.class;
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        tableCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table constraints");
            constraintCache.getAllObjects(monitor, this);
        }
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        tableCache.clearCache();
        indexCache.clearCache();
        constraintCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        return this;
    }

    @Override
    public boolean isSystem()
    {
        return MySQLConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(getName()) || MySQLConstants.MYSQL_SCHEMA_NAME.equalsIgnoreCase(getName());
    }

    @Override
    public String toString()
    {
        return name + " [" + dataSource.getContainer().getName() + "]";
    }

    public static class TableCache extends JDBCStructLookupCache<MySQLCatalog, MySQLTableBase, MySQLTableColumn> {
        
        protected TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLTableBase object, @Nullable String objectName) throws SQLException {
            return session.prepareStatement(
                "SHOW FULL TABLES FROM " + DBUtils.getQuotedIdentifier(owner) +
                    (object == null && objectName == null ?
                        "" :
                        " LIKE '" + SQLUtils.escapeString(object != null ? object.getName() : objectName) + "'"));
        }

        @Override
        protected MySQLTableBase fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_TYPE);
            if (tableType != null && tableType.contains("VIEW")) {
                return new MySQLView(owner, dbResult);
            } else {
                return new MySQLTable(owner, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MySQLConstants.META_TABLE_COLUMNS)
                .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_ORDINAL_POSITION);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MySQLTableColumn fetchChild(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull MySQLTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLTableColumn(table, dbResult);
        }

    }

    /**
     * Index cache implementation
     */
    static class IndexCache extends JDBCCompositeCache<MySQLCatalog, MySQLTable, MySQLTableIndex, MySQLTableIndexColumn> {
        protected IndexCache(TableCache tableCache)
        {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_INDEX_NAME);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MySQLCatalog owner, MySQLTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MySQLConstants.META_TABLE_STATISTICS)
                .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_INDEX_NAME).append(",").append(MySQLConstants.COL_SEQ_IN_INDEX);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected MySQLTableIndex fetchObject(JDBCSession session, MySQLCatalog owner, MySQLTable parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String indexTypeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_INDEX_TYPE);
            DBSIndexType indexType;
            if (MySQLConstants.INDEX_TYPE_BTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_BTREE;
            } else if (MySQLConstants.INDEX_TYPE_FULLTEXT.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_FULLTEXT;
            } else if (MySQLConstants.INDEX_TYPE_HASH.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_HASH;
            } else if (MySQLConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_RTREE;
            } else {
                indexType = DBSIndexType.OTHER;
            }
            return new MySQLTableIndex(
                parent,
                indexName,
                indexType,
                dbResult);
        }

        @Nullable
        @Override
        protected MySQLTableIndexColumn[] fetchObjectRow(
            JDBCSession session,
            MySQLTable parent, MySQLTableIndex object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_SEQ_IN_INDEX);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLLATION);
            boolean nullable = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_NULLABLE));
            String subPart = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_SUB_PART);

            MySQLTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new MySQLTableIndexColumn[] { new MySQLTableIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                "A".equalsIgnoreCase(ascOrDesc),
                nullable,
                subPart)
            };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, MySQLTableIndex index, List<MySQLTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Constraint cache implementation
     */
    static class ConstraintCache extends JDBCCompositeCache<MySQLCatalog, MySQLTable, MySQLTableConstraint, MySQLTableConstraintColumn> {
        protected ConstraintCache(TableCache tableCache)
        {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_CONSTRAINT_NAME);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MySQLCatalog owner, MySQLTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT kc.CONSTRAINT_NAME,kc.TABLE_NAME,kc.COLUMN_NAME,kc.ORDINAL_POSITION\n" +
                "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kc WHERE kc.TABLE_SCHEMA=? AND kc.REFERENCED_TABLE_NAME IS NULL");
            if (forTable != null) {
                sql.append(" AND kc.TABLE_NAME=?");
            }
            sql.append("\nORDER BY kc.CONSTRAINT_NAME,kc.ORDINAL_POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected MySQLTableConstraint fetchObject(JDBCSession session, MySQLCatalog owner, MySQLTable parent, String constraintName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            if (constraintName.equals("PRIMARY")) {
                return new MySQLTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
            } else {
                return new MySQLTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.UNIQUE_KEY, true);
            }
        }

        @Nullable
        @Override
        protected MySQLTableConstraintColumn[] fetchObjectRow(
            JDBCSession session,
            MySQLTable parent, MySQLTableConstraint object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME);
            MySQLTableColumn column = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (column == null) {
                log.warn("Column '" + columnName + "' not found in table '" + parent.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                return null;
            }
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ORDINAL_POSITION);

            return new MySQLTableConstraintColumn[] { new MySQLTableConstraintColumn(
                object,
                column,
                ordinalPosition) };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, MySQLTableConstraint constraint, List<MySQLTableConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    /**
     * Procedures cache implementation
     */
    static class ProceduresCache extends JDBCStructLookupCache<MySQLCatalog, MySQLProcedure, MySQLProcedureParameter> {

        ProceduresCache()
        {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        @Override
        protected MySQLProcedure fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLProcedure(owner, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLProcedure procedure)
            throws SQLException
        {
            // Load procedure columns through MySQL metadata
            // There is no metadata table about proc/func columns -
            // it should be parsed from SHOW CREATE PROCEDURE/FUNCTION query
            // Lets driver do it instead of me
            return session.getMetaData().getProcedureColumns(
                owner.getName(),
                null,
                procedure == null ? null : procedure.getName(),
                "%").getSourceStatement();
        }

        @Override
        protected MySQLProcedureParameter fetchChild(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull MySQLProcedure parent, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.LENGTH);
            boolean notNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
            int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
            //int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
            //DBSDataType dataType = getDataSourceContainer().getInfo().getSupportedDataType(typeName);
            DBSProcedureParameterKind parameterType;
            switch (columnTypeNum) {
                case DatabaseMetaData.procedureColumnIn: parameterType = DBSProcedureParameterKind.IN; break;
                case DatabaseMetaData.procedureColumnInOut: parameterType = DBSProcedureParameterKind.INOUT; break;
                case DatabaseMetaData.procedureColumnOut: parameterType = DBSProcedureParameterKind.OUT; break;
                case DatabaseMetaData.procedureColumnReturn: parameterType = DBSProcedureParameterKind.RETURN; break;
                case DatabaseMetaData.procedureColumnResult: parameterType = DBSProcedureParameterKind.RESULTSET; break;
                default: parameterType = DBSProcedureParameterKind.UNKNOWN; break;
            }
            if (CommonUtils.isEmpty(columnName) && parameterType == DBSProcedureParameterKind.RETURN) {
                columnName = "RETURN";
            }
            return new MySQLProcedureParameter(
                parent,
                columnName,
                typeName,
                valueType,
                position,
                columnSize,
                scale, precision, notNull,
                    parameterType);
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @Nullable MySQLProcedure object, @Nullable String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + MySQLConstants.META_TABLE_ROUTINES +
                    "\nWHERE " + MySQLConstants.COL_ROUTINE_SCHEMA + "=?" +
                    (object == null && objectName == null ? "" : " AND " + MySQLConstants.COL_ROUTINE_NAME + "=?") +
                    "\nORDER BY " + MySQLConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }
    }

    static class TriggerCache extends JDBCObjectCache<MySQLCatalog, MySQLTrigger> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SHOW FULL TRIGGERS FROM " + DBUtils.getQuotedIdentifier(owner));
        }

        @Override
        protected MySQLTrigger fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetString(dbResult, "TABLE");
            MySQLTable triggerTable = CommonUtils.isEmpty(tableName) ? null : owner.getTable(session.getProgressMonitor(), tableName);
            return new MySQLTrigger(owner, triggerTable, dbResult);
        }

    }

    static class EventCache extends JDBCObjectCache<MySQLCatalog, MySQLEvent> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLCatalog owner)
            throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM information_schema.EVENTS WHERE EVENT_SCHEMA=?");
            dbStat.setString(1, DBUtils.getQuotedIdentifier(owner));
            return dbStat;
        }

        @Override
        protected MySQLEvent fetchObject(@NotNull JDBCSession session, @NotNull MySQLCatalog owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLEvent(owner, dbResult);
        }

    }

}
