package application.test.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class GuiDTW extends JFrame {

    Cell[][] grid = new Cell[5][5];
    GridLayout gridLayout;

    public GuiDTW(){
        this.setBackground(Color.GRAY);
        this.setSize(600,600);
        gridLayout = new GridLayout(6,6);
        this.setLayout(gridLayout);
        this.draw();
    }

    public void draw(){
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                Cell currentCell = new Cell();
                currentCell.setSize(10,10);
                currentCell.setVisible(true);
                currentCell.addActionListener(this);
                this.add(currentCell);
                grid[i][j] = currentCell;
            }
        }
    }

    public class Cell extends JTextField{

        public Cell(){
            this.setSize(20,20);
            this.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {

                }

                @Override
                public void removeUpdate(DocumentEvent e) {

                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    System.out.println("change " + e.toString());
                }
            });
        }

        public void addActionListener(GuiDTW guiDTW) {
            System.out.println("testing");
        }
    }

    public static void main(String[] args) {
        GuiDTW gdtw = new GuiDTW();
        gdtw.setVisible(true);
    }
}
