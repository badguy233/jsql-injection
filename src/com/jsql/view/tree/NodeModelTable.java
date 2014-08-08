/*******************************************************************************
 * Copyhacked (H) 2012-2014.
 * This program and the accompanying materials
 * are made available under no term at all, use it like
 * you want, but share and discuss about it
 * every time possible with every body.
 * 
 * Contributors:
 *      ron190 at ymail dot com - initial implementation
 ******************************************************************************/
package com.jsql.view.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.jsql.model.bean.Table;
import com.jsql.view.GUIMediator;
import com.jsql.view.GUITools;

/**
 * Table model displaying the table icon on the label.
 */
public class NodeModelTable extends NodeModel {
    /**
     * Node as a table model.
     * @param table Element table coming from model
     */
    public NodeModelTable(Table table) {
        super(table);
    }

    @Override
    Icon getLeafIcon(boolean leaf) {
        if (leaf) {
            return new ImageIcon(getClass().getResource("/com/jsql/view/images/tableGo.png"));
        } else {
            return GUITools.TABLE_ICON;
        }
    }

    @Override
    protected void displayProgress(NodePanel panel, DefaultMutableTreeNode currentNode) {
        if ("information_schema".equals(this.getParent().toString())) {
            panel.showLoader();

            if (GUIMediator.model().suspendables.get(this.dataObject).isPaused()) {
                ImageIcon animatedGIFPaused = new IconOverlap(GUITools.PATH_PROGRESSBAR, GUITools.PATH_PAUSE);
                animatedGIFPaused.setImageObserver(new AnimatedObserver(GUIMediator.databaseTree(), currentNode));
                panel.setLoaderIcon(animatedGIFPaused);
            }
        } else {
            super.displayProgress(panel, currentNode);
        }
    }

    @Override
    void runAction() {
        final Table selectedTable = (Table) this.dataObject;
        if (!this.hasBeenSearched && !this.isRunning) {
            new SwingWorker<Object, Object>(){

                @Override
                protected Object doInBackground() throws Exception {
                    GUIMediator.model().dao.listColumns(selectedTable);
                    return null;
                }
                
            }.execute();
            this.isRunning = true;
        }
    }

    @Override
    void displayMenu(JPopupMenu tablePopupMenu, TreePath path) {
        final DefaultMutableTreeNode currentTableNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final NodeModel currentTableModel = (NodeModel) currentTableNode.getUserObject();

        JMenuItem mnCheckAll = new JMenuItem("Check All", 'C');
        JMenuItem mnUncheckAll = new JMenuItem("Uncheck All", 'U');

        mnCheckAll.setIcon(GUITools.EMPTY);
        mnUncheckAll.setIcon(GUITools.EMPTY);

        if (!this.hasBeenSearched) {
            mnCheckAll.setEnabled(false);
            mnUncheckAll.setEnabled(false);

            tablePopupMenu.add(mnCheckAll);
            tablePopupMenu.add(mnUncheckAll);
            tablePopupMenu.add(new JSeparator());
        }

        class CheckUncheck implements ActionListener {
            private boolean check;
            
            CheckUncheck(boolean check) {
                this.check = check;
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                DefaultTreeModel treeModel = (DefaultTreeModel) GUIMediator.databaseTree().getModel();

                int tableChildCount = treeModel.getChildCount(currentTableNode);
                for (int i = 0; i < tableChildCount; i++) {
                    DefaultMutableTreeNode currentChild = (DefaultMutableTreeNode) treeModel.getChild(currentTableNode, i);
                    if (currentChild.getUserObject() instanceof NodeModel) {
                        NodeModel columnTreeNodeModel = (NodeModel) currentChild.getUserObject();
                        columnTreeNodeModel.isChecked = check;
                        currentTableModel.hasChildChecked = check;
                    }
                }

                treeModel.nodeChanged(currentTableNode);
            }
        }

        class CheckAll extends CheckUncheck {
            CheckAll() {
                super(true);
            }
        }

        class UncheckAll extends CheckUncheck {
            UncheckAll() {
                super(false);
            }
        }

        mnCheckAll.addActionListener(new CheckAll());
        mnUncheckAll.addActionListener(new UncheckAll());

        mnCheckAll.setIcon(GUITools.EMPTY);
        mnUncheckAll.setIcon(GUITools.EMPTY);

        tablePopupMenu.add(mnCheckAll);
        tablePopupMenu.add(mnUncheckAll);
        tablePopupMenu.add(new JSeparator());
    }
    
    @Override boolean verifyShowPopup() {
        return this.hasBeenSearched || !this.hasBeenSearched && this.isRunning;
    }
}