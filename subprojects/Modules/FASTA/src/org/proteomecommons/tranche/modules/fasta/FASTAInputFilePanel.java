/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.proteomecommons.tranche.modules.fasta;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import org.tranche.gui.GUIUtil;
import org.tranche.gui.GenericButton;
import org.tranche.gui.GenericFrame;
import org.tranche.gui.GenericMenu;
import org.tranche.gui.GenericMenuBar;
import org.tranche.gui.GenericMenuItem;
import org.tranche.gui.GenericPopupListener;
import org.tranche.gui.GenericScrollBar;
import org.tranche.gui.GenericTable;
import org.tranche.gui.PreferencesUtil;
import org.tranche.gui.SortableTableModel;
import org.tranche.gui.Styles;
import org.tranche.hash.BigHash;

/**
 *
 * @author James "Augie" Hill<augman85@gmail.com>
 */
public class FASTAInputFilePanel extends JPanel {

    private FASTAInputFileTableModel model;
    private FASTAInputFileTable table;

    public FASTAInputFilePanel() {
        setLayout(new BorderLayout());
        setBorder(Styles.BORDER_BLACK_1);

        JMenuBar menuBar = new GenericMenuBar();
        {
            JMenu filesMenu = new GenericMenu("Files");

            JMenuItem addMenuItem = new GenericMenuItem("Add");
            addMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            AddInputFileFrame frame = new AddInputFileFrame();
                            frame.setLocationRelativeTo(FASTAInputFilePanel.this);
                            frame.setVisible(true);
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            filesMenu.add(addMenuItem);

            JMenuItem removeMenuItem = new GenericMenuItem("Remove Selected");
            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            int[] selectedRows = table.getSelectedRows();
                            for (int i = selectedRows.length - 1; i >= 0; i--) {
                                model.remove(selectedRows[i]);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            filesMenu.add(removeMenuItem);

            menuBar.add(filesMenu);
        }
        add(menuBar, BorderLayout.NORTH);

        model = new FASTAInputFileTableModel();
        table = new FASTAInputFileTable(model);

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(Styles.BORDER_BLACK_1);
        {
            JMenuItem removeMenuItem = new GenericMenuItem("Remove");
            removeMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {

                        public void run() {
                            int[] selectedRows = table.getSelectedRows();
                            for (int i = selectedRows.length - 1; i >= 0; i--) {
                                model.remove(selectedRows[i]);
                            }
                        }
                    };
                    t.setDaemon(true);
                    t.start();
                }
            });
            popupMenu.add(removeMenuItem);
        }
        table.addMouseListener(new GenericPopupListener(popupMenu));

        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(Styles.BORDER_NONE);
        pane.setVerticalScrollBar(new GenericScrollBar());
        pane.setBackground(Color.GRAY);
        add(pane, BorderLayout.CENTER);
    }

    public void addFileToDownload(BigHash hash, String extension) {
        FASTAInputFile inputFile = new FASTAInputFile(true, hash.toString(), FASTAReaderFormatDropDown.getInstance());
        model.add(inputFile);
    }

    public List<FASTAInputFile> getFilesToDownload() {
        List<FASTAInputFile> files = model.getRows();
        // remove the files that dont have a valid hash as its location
        for (int i = files.size() - 1; i >= 0; i--) {
            try {
                BigHash.createHashFromString(files.get(i).getLocation());
            } catch (Exception e) {
                files.remove(i);
            }
        }
        return files;
    }

    public List<FASTAInputFile> getLocalFiles() {
        List<FASTAInputFile> files = model.getRows();
        // remove files that are tranche hashes
        for (int i = files.size() - 1; i >= 0; i--) {
            try {
                File file = new File(files.get(i).getLocation());
                if (!file.exists()) {
                    files.remove(i);
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        return files;
    }

    private class AddInputFileFrame extends GenericFrame implements ActionListener {

        private String LOCATION_PREFERENCE = "FASTA MODULE INPUT FILE LOCATION";
        private JTextField locationTextField = new JTextField();
        private FASTAReaderFormatDropDown fileFormatComboBox = FASTAReaderFormatDropDown.getInstance();

        public AddInputFileFrame() {
            setTitle("Add Input File");
            setSize(350, 190);
            setDefaultCloseOperation(GenericFrame.DISPOSE_ON_CLOSE);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            {
                JLabel locationLabel = new JLabel("File Location or Tranche Hash:");
                gbc.anchor = gbc.NORTH;
                gbc.fill = gbc.HORIZONTAL;
                gbc.gridwidth = gbc.REMAINDER;
                gbc.insets = new Insets(10, 10, 0, 10);
                gbc.weightx = 1;
                gbc.weighty = 1;
                add(locationLabel, gbc);
            }

            {
                locationTextField.addActionListener(this);
                locationTextField.setToolTipText("Either a local file or the Tranche hash of the file to be used.");
                gbc.anchor = gbc.SOUTH;
                gbc.insets = new Insets(5, 10, 0, 10);
                add(locationTextField, gbc);
            }

            {
                JButton selectFileButton = new GenericButton("Select File");
                selectFileButton.setToolTipText("Select a local file.");
                selectFileButton.setFont(Styles.FONT_10PT_BOLD);
                selectFileButton.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        // query for a file
                        final JFileChooser jfc = GUIUtil.makeNewFileChooser();

                        Thread t = new Thread() {

                            public void run() {
                                // get the users last known input file
                                String lastLocation = PreferencesUtil.getPreference(LOCATION_PREFERENCE);
                                if (lastLocation != null) {
                                    File file = new File(lastLocation);
                                    if (file.exists()) {
                                        jfc.setCurrentDirectory(file.getParentFile());
                                    }
                                }

                                jfc.setFileSelectionMode(jfc.FILES_ONLY);
                                jfc.showOpenDialog(AddInputFileFrame.this);

                                // check for a submitted file
                                File selectedFile = jfc.getSelectedFile();

                                // the user cancelled
                                if (selectedFile == null) {
                                    return;
                                }

                                // set the appropriate string
                                locationTextField.setText(selectedFile.toString());
                            }
                        };
                        t.setDaemon(true);
                        t.start();
                    }
                });
                gbc.anchor = gbc.NORTH;
                gbc.insets = new Insets(0, 10, 0, 10);
                add(selectFileButton, gbc);
            }

            {
                JLabel formatLabel = new JLabel("FASTA Format:");
                gbc.anchor = gbc.CENTER;
                gbc.gridwidth = gbc.RELATIVE;
                gbc.insets = new Insets(5, 10, 0, 0);
                gbc.weightx = 0;
                add(formatLabel, gbc);
            }

            {
                gbc.gridwidth = gbc.REMAINDER;
                gbc.insets = new Insets(5, 5, 0, 10);
                gbc.weightx = 1;
                add(fileFormatComboBox, gbc);
            }

            {
                JButton addButton = new GenericButton("Add Input File");
                addButton.setBorder(Styles.BORDER_BUTTON_TOP);
                addButton.addActionListener(this);
                gbc.anchor = gbc.SOUTH;
                gbc.fill = gbc.BOTH;
                gbc.gridheight = gbc.REMAINDER;
                gbc.insets = new Insets(10, 0, 0, 0);
                gbc.weighty = 2;
                add(addButton, gbc);
            }
        }

        public void actionPerformed(ActionEvent e) {
            Thread t = new Thread() {

                public void run() {
                    if (locationTextField.getText() == null) {
                        return;
                    }

                    String location = locationTextField.getText().trim();

                    // is this a file or a Tranche hash?
                    boolean isTrancheHash = false;

                    try {
                        File file = new File(location);
                        if (!file.exists()) {
                            isTrancheHash = true;
                            // make sure it is a valid hash
                            BigHash.createHashFromString(location);
                            // make sure this hash isn't already in the list
                            for (FASTAInputFile inputFile : getFilesToDownload()) {
                                if (inputFile.getLocation().equals(location)) {
                                    return;
                                }
                            }
                        } else {
                            // make sure this file is not already in the list
                            for (FASTAInputFile inputFile : getLocalFiles()) {
                                if (inputFile.getLocation().equals(location)) {
                                    return;
                                }
                            }
                            // remember the old location
                            PreferencesUtil.setPreference(LOCATION_PREFERENCE, location);
                            PreferencesUtil.save();
                        }
                    } catch (Exception ee) {
                        JOptionPane.showMessageDialog(FASTAInputFilePanel.this, "The file could not be found or given Tranche hash is invalid.", "File Not Found", JOptionPane.ERROR_MESSAGE);
                        return;
                    } finally {
                        // close the frame
                        dispose();
                    }

                    // add the input file to the table
                    FASTAInputFile inputFile = new FASTAInputFile(isTrancheHash, location, fileFormatComboBox);
                    model.add(inputFile);
                }
            };
            t.setDaemon(true);
            t.start();
        }
    }

    public class FASTAInputFileTable extends GenericTable {

        private FASTAInputFileTableModel model;

        public FASTAInputFileTable(FASTAInputFileTableModel model) {
            super(model, ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            this.model = model;

            // make the combo box editable
            getColumnModel().getColumn(getColumnModel().getColumnIndex("FASTA Format")).setCellEditor(new DefaultCellEditor(FASTAReaderFormatDropDown.getInstance()));
        }

        public String getToolTipText(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            String returnVal = null;
            if (row >= 0) {
                if (columnAtPoint(e.getPoint()) == table.getColumnModel().getColumnIndex("File Location")) {
                    returnVal = model.getRow(row).getLocation();
                }
            }

            // if the text for the tool tip text is too long, shorten it.
            if (returnVal != null) {
                if (returnVal.length() > 80) {
                    returnVal = returnVal.substring(0, 80) + "...";
                } else if (returnVal.equals("")) {
                    returnVal = null;
                }
            }

            return returnVal == null ? null : returnVal;
        }
    }

    public class FASTAInputFileTableModel extends SortableTableModel {

        private ArrayList<FASTAInputFile> items = new ArrayList<FASTAInputFile>();
        private String[] headers = new String[]{"File Location", "FASTA Format"};

        public void add(FASTAInputFile inputFile) {
            items.add(inputFile);
            sort(table.getPressedColumn());
            fireTableDataChanged();
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public int getColumnCount() {
            return headers.length;
        }

        public String getColumnName(int column) {
            if (column < getColumnCount()) {
                return headers[column];
            } else {
                return "";
            }
        }

        public FASTAInputFile getRow(int index) {
            return items.get(index);
        }

        public List<FASTAInputFile> getRows() {
            ArrayList<FASTAInputFile> list = new ArrayList<FASTAInputFile>();
            for (FASTAInputFile file : items) {
                list.add(file);
            }
            return list;
        }

        public int getRowCount() {
            return items.size();
        }

        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return items.get(row).getLocation();
                case 1:
                    return items.get(row).getFormatMenu().getSelectedFormat();
                default:
                    return null;
            }
        }

        public boolean isCellEditable(int row, int column) {
            return (column == table.getColumnModel().getColumnIndex("FASTA Format"));
        }

        public void remove(int index) {
            items.remove(index);
            fireTableRowsDeleted(index, index);
        }

        public void setValueAt(Object value, int row, int col) {
            if (col == table.getColumnModel().getColumnIndex("File Location")) {
                return;
            } else if (col == table.getColumnModel().getColumnIndex("FASTA Format")) {
                items.get(row).getFormatMenu().setSelectedItem(value);
                fireTableCellUpdated(row, col);
            }
        }

        public void sort(int column) {
            table.setPressedColumn(column);
            synchronized (items) {
                Collections.sort(items, new FASTAInputFileComparator(column));
            }
        }

        private class FASTAInputFileComparator implements Comparator {

            private int column;

            public FASTAInputFileComparator(int column) {
                this.column = column;
            }

            public int compare(Object o1, Object o2) {
                if (table.getDirection()) {
                    Object temp = o1;
                    o1 = o2;
                    o2 = temp;
                }

                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return 1;
                } else if (o1 instanceof FASTAInputFile && o2 instanceof FASTAInputFile) {
                    if (column == table.getColumnModel().getColumnIndex("File Location")) {
                        return ((FASTAInputFile) o1).getLocation().toLowerCase().compareTo(((FASTAInputFile) o2).getLocation().toLowerCase());
                    } else if (column == table.getColumnModel().getColumnIndex("FASTA Format")) {
                        return ((FASTAInputFile) o1).getFormatMenu().getSelectedFormat().toLowerCase().compareTo(((FASTAInputFile) o2).getFormatMenu().getSelectedFormat().toLowerCase());
                    } else {
                        return 0;
                    }
                } else {
                    return 1;
                }
            }
        }
    }
}
