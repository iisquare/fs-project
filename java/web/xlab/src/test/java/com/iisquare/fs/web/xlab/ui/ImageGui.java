package com.iisquare.fs.web.xlab.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageGui extends JComponent {

    private BufferedImage image;

    public ImageGui() {
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        if(image == null)  {
            g2d.setPaint(Color.BLACK);
            g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
        } else {
            g2d.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
        }
    }

    public void window(String title) {
        JDialog ui = new JDialog();
        ui.setTitle(title);
        ui.getContentPane().setLayout(new BorderLayout());
        ui.getContentPane().add(this, BorderLayout.CENTER);
        ui.setSize(new Dimension(330, 240));
        ui.setVisible(true);
    }

    public void window(String title, Dimension size) {
        JDialog ui = new JDialog();
        ui.setTitle(title);
        ui.getContentPane().setLayout(new BorderLayout());
        ui.getContentPane().add(this, BorderLayout.CENTER);
        ui.setSize(size);
        ui.setVisible(true);
    }

    public void imshow(BufferedImage image) {
        this.image = image;
        this.repaint();
    }

}
