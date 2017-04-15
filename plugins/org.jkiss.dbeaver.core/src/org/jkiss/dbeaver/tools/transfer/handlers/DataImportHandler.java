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
package org.jkiss.dbeaver.tools.transfer.handlers;

import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Map;

public class DataImportHandler extends DataTransferImportHandler implements IElementUpdater {

	@Override
    protected IDataTransferNode adaptTransferNode(Object object)
    {
        final DBSDataContainer adapted = RuntimeUtils.getObjectAdapter(object, DBSDataContainer.class);
        if (adapted != null) {
            // return new DatabaseTransferConsumer(adapted);    // FIXME
            return new DatabaseTransferProducer(adapted);
        } else {
            return null;
        }
    }

    /*	@Override
    protected IDataTransferProducer chooseProducer(ExecutionEvent event, IDataTransferConsumer consumer)
    {
        final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
        final DBNNode rootNode = DBeaverCore.getInstance().getLiveProjects().size() == 1 ?
            navigatorModel.getRoot().getProject(DBeaverCore.getInstance().getProjectRegistry().getActiveProject()) : navigatorModel.getRoot();
        DBNNode node = BrowseObjectDialog.selectObject(
            HandlerUtil.getActiveShell(event),
            "Select source container for '" + consumer.getTargetName() + "'",
            rootNode,
            null,
            new Class[] {DBSObjectContainer.class, DBSDataContainer.class},
            new Class[] {DBSDataContainer.class});
        if (node instanceof DBNDatabaseNode) {
            DBSObject object = ((DBNDatabaseNode) node).getObject();
            if (object instanceof DBSDataContainer) {
                return new DatabaseTransferProducer((DBSDataContainer) object);
            }
        }
        return null;
    }*/

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        DBNNode node = NavigatorUtils.getSelectedNode(element);
        if (node != null) {
            element.setText("Import " + node.getNodeType() + " Data");
        }
    }

}