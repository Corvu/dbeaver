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
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MySQLCharset
 */
public class MySQLCharset implements DBPNamedObject, Comparable<MySQLCharset>{

    private String name;
//    private String description;
//    private int maxLength;
    private List<String> collations = new ArrayList<>();
    private String defaultCollation;

    public MySQLCharset(ResultSet dbResult)
        throws SQLException
    {
        //super(dataSource);
        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CHARSET);
//        this.description = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DESCRIPTION);
//        this.maxLength = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_MAX_LEN);
    }

    void addCollation(String collation, boolean isDefault)
    {
        collations.add(collation);
        Collections.sort(collations);
        if (isDefault) {
        	defaultCollation = collation;
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public List<String> getCollations()
    {
        return collations;
    }

//    @Property(viewable = true, order = 2)
    public String getDefaultCollation()
    {
        return defaultCollation;
    }

	@Override
	public int compareTo(MySQLCharset o) {
		if (this == o) return 0;
		
		if (name == null && o.name != null) {
			return -1;
		}
		
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return name;
	}

    /*public MySQLCollation getCollation(String name) {
        for (MySQLCollation collation : collations) {
            if (collation.getName().equals(name)) {
                return collation;
            }
        }
        return null;
    }*/

    /*@Property(viewable = true, order = 3)
    public int getMaxLength()
    {
        return maxLength;
    }*/

    /*@Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }*/

}
