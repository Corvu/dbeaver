/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tools.transfer.stream.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

// Apache POI is an API for Microsoft Documents
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
 * Abstract Excel Exporter
 */
public abstract class AbstractDataExporterExcel extends StreamExporterAbstract {

    private List<DBDAttributeBinding> columns;
    private String tableName;
    
    // POI Variables
    private Workbook wb;
    private Sheet sheet;
    private int rowNum;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        //rowNum = 1;
    }

    @Override
    public void dispose()
    {
        super.dispose();     
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException
    {
        columns = getSite().getAttributes();
        printHeader();
    }
    
    protected abstract Workbook initWorkbook();

    private void printHeader()
    {
    	// XLS/XLSX workbook & spreadsheet creation
        wb = initWorkbook();
        tableName = getSite().getSource().getName();
        sheet = wb.createSheet(tableName);	// add sheet to workbook
        rowNum = 1;
        
        int columnsSize = columns.size();
        Row row = sheet.createRow((short)0);
        for (int i = 0; i < columnsSize; i++) {
            String colName = columns.get(i).getLabel();
            colName = columns.get(i).getName();
            row.createCell(i).setCellValue(colName);
        }
        
    }

    @Override
    public void exportRow(DBCSession session, Object[] row) throws DBException, IOException
    {
    	Row rowOut = sheet.createRow(rowNum);
    	rowNum++;
    	
    	for (int i = 0; i < row.length; i++) {
    		Cell currentCell = rowOut.createCell(i);
    		
    		DBDAttributeBinding column = columns.get(i);
            if (DBUtils.isNullValue(row[i])) {
                writeTextCell(null, null);
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent)row[i];
                
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs != null) {
                        if (ContentUtils.isTextContent(content)) {
                            try (Reader reader = cs.getContentReader()) {
                                writeCellValue(reader, currentCell);
                            }
                        } else {
                            //getSite().writeBinaryData(cs, currentCell);
                        }
                    }
                }
                finally {
                    content.release();
                }
            } else {
               writeTextCell(super.getValueDisplayString(column, row[i]), currentCell);
            }
    		
    		
    	}
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException
    {
    	try {
            wb.write(getSite().getOutputStream());
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    private void writeTextCell(@Nullable String value, Cell currentCell)
    {
    	if (value != null) {
    		value = value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
    		currentCell.setCellValue(value);
    	}
    }

    private void writeCellValue(Reader reader, Cell currentCell) throws IOException
    {
    	String value = "";
        char buffer[] = new char[2000];
        for (;;) {
            int count = reader.read(buffer);
            if (count <= 0) {
                break;
            }
            for (int i = 0; i < count; i++) {
                if (buffer[i] == '<') {
                    value.concat("&lt;");
                }
                else if (buffer[i] == '>') {
                	value.concat("&gt;");
                } else if (buffer[i] == '&') {
                	value.concat("&amp;");
                } else {
                	
                	value.concat(String.valueOf(buffer[i]));
                }
            }
        }
      currentCell.setCellValue(value);
        
    }

}
