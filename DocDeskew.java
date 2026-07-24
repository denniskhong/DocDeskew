/**
 * DocDeskew (Document Perspective Deskew Utility) - Enhanced Edition
 * 
 * Features:
 * - Dual Engine Architecture (OpenCV + Pure Java Fallback)
 * - Precision Loupe & Draggable Corners
 * - Clipboard Integration (Copy/Paste via Edit Menu)
 * - Lossless Dimension Heuristic
 * - Tabbed Interface for Results
 * - Auto-Detection of Document Corners
 * - Save Reminders & Smart Exit Handling
 */

import org.opencv.core.*;
import org.opencv.core.Point; 
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocDeskew extends JFrame {
    private BufferedImage sourceImage;
    private ImagePanel sourceImagePanel;
    private JTabbedPane tabbedPane;
    private final List<java.awt.Point> selectedPoints = new ArrayList<>();
    
    // Flag to track if there are deskewed images that haven't been saved
    private boolean hasUnsavedChanges = false;
    
    private static boolean isOpenCVAvailable = false;

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            isOpenCVAvailable = true;
            System.out.println("OpenCV loaded successfully. Using Homographic Warp & Edge Detection.");
        } catch (UnsatisfiedLinkError e) {
            isOpenCVAvailable = false;
            System.out.println("OpenCV native library not found. Falling back to Pure Java features.");
        }
    }

    public DocDeskew() {
        setTitle("DocDeskew - " + (isOpenCVAvailable ? "[OpenCV Engine Active]" : "[Pure Java Fallback]"));
        setSize(1100, 800);
        setLocationRelativeTo(null);

        // Intercept window closing to check for unsaved changes
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                attemptExit();
            }
        });

        tabbedPane = new JTabbedPane();
        sourceImagePanel = new ImagePanel();
        tabbedPane.addTab("Source Image", sourceImagePanel);
        
        add(tabbedPane, BorderLayout.CENTER);

        // Deskew Toolbar
        JPanel bottomPanel = new JPanel();
        JButton deskewButton = new JButton("Deskew Now");
        deskewButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        deskewButton.addActionListener(e -> performPerspectiveTransform());
        bottomPanel.add(deskewButton);
        add(bottomPanel, BorderLayout.SOUTH);

        buildMenu();
    }

    private void buildMenu() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Image...");
        JMenuItem saveItem = new JMenuItem("Save Current Tab As...");
        JMenuItem exitItem = new JMenuItem("Exit");

        openItem.addActionListener(e -> openImageFile());
        saveItem.addActionListener(e -> saveResultImage());
        exitItem.addActionListener(e -> attemptExit());

        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem copyItem = new JMenuItem("Copy to Clipboard");
        JMenuItem pasteItem = new JMenuItem("Paste from Clipboard");
        JMenuItem clearItem = new JMenuItem("Clear Points");

        copyItem.addActionListener(e -> copyToClipboard());
        pasteItem.addActionListener(e -> pasteFromClipboard());
        clearItem.addActionListener(e -> {
            selectedPoints.clear();
            sourceImagePanel.repaint();
        });

        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(clearItem);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem installGuideItem = new JMenuItem("OpenCV Installation Guide");
        JMenuItem aboutItem = new JMenuItem("About DocDeskew");
        
        installGuideItem.addActionListener(e -> showInstallationGuide());
        aboutItem.addActionListener(e -> showAboutDialog());
        
        helpMenu.add(installGuideItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
    
    private void attemptExit() {
        if (hasUnsavedChanges) {
            int choice = JOptionPane.showConfirmDialog(this, 
                "You have unsaved deskewed images. Are you sure you want to exit without saving?", 
                "Unsaved Changes", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);
                
            if (choice != JOptionPane.YES_OPTION) {
                return; // Cancel the exit
            }
        }
        System.exit(0);
    }

    private void showInstallationGuide() {
        String msg = "<html><body style='width: 450px; font-family: sans-serif;'>"
            + "<h2>Enabling OpenCV for Advanced Features</h2>"
            + "<p>Status: This program is currently running in <b>" + (isOpenCVAvailable ? "OpenCV Mode" : "Pure Java Fallback Mode") + "</b>.</p>"
            + "<p>To unlock True Perspective Homography and Auto-Document Detection, the OpenCV native library must be installed on your system:</p>"
            + "<h3>Windows</h3>"
            + "<ul><li>Download OpenCV from <b>opencv.org</b> and extract it.</li>"
            + "<li>Add the <code>build/java/x64</code> directory to your System PATH environment variable.</li></ul>"
            + "<h3>macOS</h3>"
            + "<ul><li>Open your terminal and run: <code>brew install opencv</code></li></ul>"
            + "<h3>Linux (Ubuntu / Mint)</h3>"
            + "<ul><li>Open your terminal and run: <code>sudo apt install libopencv-java</code></li></ul>"
            + "<p><i>Note: After installing, simply restart the application using the provided launcher scripts for your platform.</i></p>"
            + "</body></html>";
            
        JOptionPane.showMessageDialog(this, msg, "Setup Guide", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showAboutDialog() {
        String msg = "<html><body style='width: 300px; font-family: sans-serif; text-align: center;'>"
            + "<h2>DocDeskew</h2>"
            + "<p><i>Document Perspective Deskew Utility</i></p>"
            + "<hr>"
            + "<p>Created by <b>Gemini 3.1 Pro</b></p>"
            + "<p style='font-size: 10px; color: gray;'>Featuring Dual Engine Architecture</p>"
            + "</body></html>";
            
        JOptionPane.showMessageDialog(this, msg, "About DocDeskew", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = ImageIO.read(chooser.getSelectedFile());
                if (img != null) setImage(img);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void pasteFromClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                Image img = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                BufferedImage bImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = bImg.createGraphics();
                g2d.drawImage(img, 0, 0, null);
                g2d.dispose();
                setImage(bImg);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to paste image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copyToClipboard() {
        Component currentComp = tabbedPane.getSelectedComponent();
        BufferedImage imgToCopy = null;

        if (currentComp == sourceImagePanel) {
            imgToCopy = sourceImage;
        } else if (currentComp instanceof JPanel) {
            // Find the image inside the result tab
            for (Component c : ((JPanel) currentComp).getComponents()) {
                if (c instanceof JLabel) {
                    Icon icon = ((JLabel) c).getIcon();
                    if (icon instanceof ImageIcon) {
                        Image img = ((ImageIcon) icon).getImage();
                        imgToCopy = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                        imgToCopy.getGraphics().drawImage(img, 0, 0, null);
                    }
                }
            }
        }

        if (imgToCopy != null) {
            TransferableImage trans = new TransferableImage(imgToCopy);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(trans, null);
            JOptionPane.showMessageDialog(this, "Image copied to clipboard.");
        }
    }

    private void setImage(BufferedImage img) {
        BufferedImage bgrImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        bgrImage.getGraphics().drawImage(img, 0, 0, null);
        this.sourceImage = bgrImage;
        this.selectedPoints.clear();
        tabbedPane.setSelectedIndex(0);
        
        autoDetectCorners();
        sourceImagePanel.setImage(bgrImage);
    }

    private void autoDetectCorners() {
        selectedPoints.clear();
        if (isOpenCVAvailable && sourceImage != null) {
            try {
                byte[] pixels = ((DataBufferByte) sourceImage.getRaster().getDataBuffer()).getData();
                Mat srcMat = new Mat(sourceImage.getHeight(), sourceImage.getWidth(), CvType.CV_8UC3);
                srcMat.put(0, 0, pixels);

                Mat gray = new Mat();
                Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

                Mat edges = new Mat();
                Imgproc.Canny(gray, edges, 75, 200);

                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                contours.sort((c1, c2) -> Double.compare(Imgproc.contourArea(c2), Imgproc.contourArea(c1)));

                for (MatOfPoint contour : contours) {
                    MatOfPoint2f approx = new MatOfPoint2f();
                    MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                    double epsilon = 0.02 * Imgproc.arcLength(contour2f, true);
                    Imgproc.approxPolyDP(contour2f, approx, epsilon, true);

                    if (approx.toArray().length == 4) {
                        for (Point p : approx.toArray()) {
                            selectedPoints.add(new java.awt.Point((int) p.x, (int) p.y));
                        }
                        sortCorners(selectedPoints);
                        break;
                    }
                }
                srcMat.release(); gray.release(); edges.release(); hierarchy.release();
            } catch (Exception e) {
                System.out.println("Auto-detect failed, falling back to defaults.");
            }
        }

        // Fallback or if OpenCV failed to find a quad
        if (selectedPoints.size() != 4 && sourceImage != null) {
            selectedPoints.clear();
            int w = sourceImage.getWidth();
            int h = sourceImage.getHeight();
            int mx = w / 10;
            int my = h / 10;
            selectedPoints.add(new java.awt.Point(mx, my));
            selectedPoints.add(new java.awt.Point(w - mx, my));
            selectedPoints.add(new java.awt.Point(w - mx, h - my));
            selectedPoints.add(new java.awt.Point(mx, h - my));
        }
    }

    private void sortCorners(List<java.awt.Point> pts) {
        // Simple heuristic to sort corners: TL, TR, BR, BL
        Collections.sort(pts, (p1, p2) -> Integer.compare(p1.y, p2.y));
        java.awt.Point top1 = pts.get(0);
        java.awt.Point top2 = pts.get(1);
        java.awt.Point bot1 = pts.get(2);
        java.awt.Point bot2 = pts.get(3);
        
        java.awt.Point tl = top1.x < top2.x ? top1 : top2;
        java.awt.Point tr = top1.x < top2.x ? top2 : top1;
        java.awt.Point bl = bot1.x < bot2.x ? bot1 : bot2;
        java.awt.Point br = bot1.x < bot2.x ? bot2 : bot1;

        pts.clear();
        pts.add(tl); pts.add(tr); pts.add(br); pts.add(bl);
    }

    private void saveResultImage() {
        Component currentComp = tabbedPane.getSelectedComponent();
        BufferedImage imgToSave = null;

        if (currentComp == sourceImagePanel && sourceImage != null) {
            imgToSave = sourceImage;
        } else if (currentComp instanceof JPanel) {
            for (Component c : ((JPanel) currentComp).getComponents()) {
                if (c instanceof JLabel) {
                    Icon icon = ((JLabel) c).getIcon();
                    if (icon instanceof ImageIcon) {
                        Image img = ((ImageIcon) icon).getImage();
                        imgToSave = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
                        imgToSave.getGraphics().drawImage(img, 0, 0, null);
                    }
                }
            }
        }

        if (imgToSave == null) return;

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Image", "png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".png")) file = new File(path + ".png");

            try {
                ImageIO.write(imgToSave, "png", file);
                hasUnsavedChanges = false; // Reset the flag upon successful save
                JOptionPane.showMessageDialog(this, "Saved successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Custom panel updated with Drag-and-Drop capability
    private class ImagePanel extends JPanel {
        private BufferedImage displayImage;
        private java.awt.Point mousePos = new java.awt.Point(0, 0);
        private int draggedPointIndex = -1;
        private final int DRAG_RADIUS = 15;

        public void setImage(BufferedImage img) {
            this.displayImage = img;
            repaint();
        }

        public ImagePanel() {
            setBackground(Color.DARK_GRAY);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (displayImage == null) return;
                    java.awt.Point imgPt = getOriginalImageCoord(e.getPoint());
                    if (imgPt == null) return;

                    // Hit test for dragging existing points
                    for (int i = 0; i < selectedPoints.size(); i++) {
                        java.awt.Point p = getPanelCoord(selectedPoints.get(i));
                        if (p.distance(e.getPoint()) <= DRAG_RADIUS) {
                            draggedPointIndex = i;
                            return;
                        }
                    }

                    // Add new point if less than 4
                    if (selectedPoints.size() < 4) {
                        selectedPoints.add(imgPt);
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    draggedPointIndex = -1;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    mousePos = e.getPoint();
                    if (displayImage != null) repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    mousePos = e.getPoint();
                    if (draggedPointIndex != -1 && displayImage != null) {
                        java.awt.Point imgPt = getOriginalImageCoord(e.getPoint());
                        if (imgPt != null) {
                            selectedPoints.set(draggedPointIndex, imgPt);
                            repaint();
                        }
                    }
                }
            });
        }

        private double getScale() {
            if (displayImage == null) return 1.0;
            double sx = (double) getWidth() / displayImage.getWidth();
            double sy = (double) getHeight() / displayImage.getHeight();
            return Math.min(sx, sy);
        }

        private java.awt.Point getOriginalImageCoord(java.awt.Point panelPt) {
            if (displayImage == null) return null;
            double scale = getScale();
            int panelW = (int) (displayImage.getWidth() * scale);
            int panelH = (int) (displayImage.getHeight() * scale);
            int xOffset = (getWidth() - panelW) / 2;
            int yOffset = (getHeight() - panelH) / 2;

            int imgX = (int) ((panelPt.x - xOffset) / scale);
            int imgY = (int) ((panelPt.y - yOffset) / scale);

            if (imgX >= 0 && imgY >= 0 && imgX < displayImage.getWidth() && imgY < displayImage.getHeight()) {
                return new java.awt.Point(imgX, imgY);
            }
            return null;
        }

        private java.awt.Point getPanelCoord(java.awt.Point imgPt) {
            double scale = getScale();
            int panelW = (int) (displayImage.getWidth() * scale);
            int panelH = (int) (displayImage.getHeight() * scale);
            int xOffset = (getWidth() - panelW) / 2;
            int yOffset = (getHeight() - panelH) / 2;

            return new java.awt.Point((int) (imgPt.x * scale + xOffset), (int) (imgPt.y * scale + yOffset));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (displayImage == null) return;

            double scale = getScale();
            int panelW = (int) (displayImage.getWidth() * scale);
            int panelH = (int) (displayImage.getHeight() * scale);
            int xOffset = (getWidth() - panelW) / 2;
            int yOffset = (getHeight() - panelH) / 2;

            g.drawImage(displayImage, xOffset, yOffset, panelW, panelH, this);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int i = 0; i < selectedPoints.size(); i++) {
                java.awt.Point p1 = getPanelCoord(selectedPoints.get(i));
                g2d.setColor(Color.RED);
                g2d.fillOval(p1.x - 5, p1.y - 5, 10, 10);

                if (i > 0) {
                    java.awt.Point p0 = getPanelCoord(selectedPoints.get(i - 1));
                    g2d.setColor(Color.GREEN);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawLine(p0.x, p0.y, p1.x, p1.y);
                }
            }
            if (selectedPoints.size() == 4) {
                java.awt.Point p3 = getPanelCoord(selectedPoints.get(3));
                java.awt.Point p0 = getPanelCoord(selectedPoints.get(0));
                g2d.setColor(Color.GREEN);
                g2d.drawLine(p3.x, p3.y, p0.x, p0.y);
            }

            if (mousePos != null && (selectedPoints.size() < 4 || draggedPointIndex != -1)) {
                drawLoupe(g2d);
            }
            g2d.dispose();
        }

        private void drawLoupe(Graphics2D g2d) {
            java.awt.Point imgPt = getOriginalImageCoord(mousePos);
            if (imgPt == null) return;

            int loupeSize = 130;
            int zoom = 4;
            
            int cropW = loupeSize / zoom;
            int cropH = loupeSize / zoom;
            int cropX = Math.max(0, Math.min(imgPt.x - (cropW / 2), displayImage.getWidth() - cropW));
            int cropY = Math.max(0, Math.min(imgPt.y - (cropH / 2), displayImage.getHeight() - cropH));

            BufferedImage sub = displayImage.getSubimage(cropX, cropY, cropW, cropH);

            int drawX = mousePos.x + 20;
            int drawY = mousePos.y - loupeSize - 20;
            if (drawX + loupeSize > getWidth()) drawX = mousePos.x - loupeSize - 20;
            if (drawY < 0) drawY = mousePos.y + 20;

            Ellipse2D clip = new Ellipse2D.Float(drawX, drawY, loupeSize, loupeSize);
            g2d.setClip(clip);
            g2d.drawImage(sub, drawX, drawY, loupeSize, loupeSize, null);
            g2d.setClip(null);

            g2d.setColor(Color.RED);
            int cx = drawX + (loupeSize / 2);
            int cy = drawY + (loupeSize / 2);
            g2d.drawLine(cx - 8, cy, cx + 8, cy);
            g2d.drawLine(cx, cy - 8, cx, cy + 8);
        }
    }

    private void performPerspectiveTransform() {
        if (selectedPoints.size() != 4 || sourceImage == null) {
            JOptionPane.showMessageDialog(this, "Please define exactly 4 corners.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BufferedImage outputImage;
        if (isOpenCVAvailable) {
            outputImage = performOpenCVTransform();
        } else {
            outputImage = performBilinearTransform();
        }

        if (outputImage != null) {
            displayResultInNewTab(outputImage);
        }
    }

    private void displayResultInNewTab(BufferedImage result) {
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBackground(Color.DARK_GRAY);

        // Simple scaled display for result tab
        JLabel resultLabel = new JLabel(new ImageIcon(result)) {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                double scale = Math.min((double) getWidth() / result.getWidth(), (double) getHeight() / result.getHeight());
                int w = (int) (result.getWidth() * scale);
                int h = (int) (result.getHeight() * scale);
                int x = (getWidth() - w) / 2;
                int y = (getHeight() - h) / 2;
                g.drawImage(result, x, y, w, h, this);
            }
        };
        
        resultPanel.add(resultLabel, BorderLayout.CENTER);
        
        String tabTitle = "Result " + tabbedPane.getTabCount();
        tabbedPane.addTab(tabTitle, resultPanel);
        
        // Switch to the newly created tab and flag that we have unsaved changes
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        hasUnsavedChanges = true;
    }

    private BufferedImage performOpenCVTransform() {
        java.awt.Point p0 = selectedPoints.get(0);
        java.awt.Point p1 = selectedPoints.get(1);
        java.awt.Point p2 = selectedPoints.get(2);
        java.awt.Point p3 = selectedPoints.get(3);

        double widthA = Math.hypot(p1.x - p0.x, p1.y - p0.y);
        double widthB = Math.hypot(p2.x - p3.x, p2.y - p3.y);
        int outWidth = (int) Math.max(widthA, widthB);

        double heightA = Math.hypot(p3.x - p0.x, p3.y - p0.y);
        double heightB = Math.hypot(p2.x - p1.x, p2.y - p1.y);
        int outHeight = (int) Math.max(heightA, heightB);

        byte[] pixels = ((DataBufferByte) sourceImage.getRaster().getDataBuffer()).getData();
        Mat srcMat = new Mat(sourceImage.getHeight(), sourceImage.getWidth(), CvType.CV_8UC3);
        srcMat.put(0, 0, pixels);

        MatOfPoint2f srcPoints = new MatOfPoint2f(
                new org.opencv.core.Point(p0.x, p0.y),
                new org.opencv.core.Point(p1.x, p1.y),
                new org.opencv.core.Point(p2.x, p2.y),
                new org.opencv.core.Point(p3.x, p3.y)
        );

        MatOfPoint2f dstPoints = new MatOfPoint2f(
                new org.opencv.core.Point(0, 0),
                new org.opencv.core.Point(outWidth - 1, 0),
                new org.opencv.core.Point(outWidth - 1, outHeight - 1),
                new org.opencv.core.Point(0, outHeight - 1)
        );

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
        Mat dstMat = new Mat();
        Imgproc.warpPerspective(srcMat, dstMat, perspectiveTransform, new Size(outWidth, outHeight));

        byte[] outPixels = new byte[(int) (dstMat.total() * dstMat.channels())];
        dstMat.get(0, 0, outPixels);
        BufferedImage outputImage = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] targetPixels = ((DataBufferByte) outputImage.getRaster().getDataBuffer()).getData();
        System.arraycopy(outPixels, 0, targetPixels, 0, outPixels.length);
        
        srcMat.release();
        dstMat.release();
        perspectiveTransform.release();
        
        return outputImage;
    }

    private BufferedImage performBilinearTransform() {
        java.awt.Point tl = selectedPoints.get(0);
        java.awt.Point tr = selectedPoints.get(1);
        java.awt.Point br = selectedPoints.get(2);
        java.awt.Point bl = selectedPoints.get(3);

        int outWidth = (int) Math.max(
                Math.hypot(tr.x - tl.x, tr.y - tl.y), 
                Math.hypot(br.x - bl.x, br.y - bl.y));

        int outHeight = (int) Math.max(
                Math.hypot(bl.x - tl.x, bl.y - tl.y), 
                Math.hypot(br.x - tr.x, br.y - tr.y));

        BufferedImage outputImage = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < outHeight; y++) {
            double v = (double) y / (outHeight - 1);
            for (int x = 0; x < outWidth; x++) {
                double u = (double) x / (outWidth - 1);

                double srcX = (1 - v) * ((1 - u) * tl.x + u * tr.x) + v * ((1 - u) * bl.x + u * br.x);
                double srcY = (1 - v) * ((1 - u) * tl.y + u * tr.y) + v * ((1 - u) * bl.y + u * br.y);

                int rgb = getInterpolatedRGB(sourceImage, srcX, srcY);
                outputImage.setRGB(x, y, rgb);
            }
        }
        return outputImage;
    }

    private int getInterpolatedRGB(BufferedImage img, double x, double y) {
        int xInt = (int) Math.floor(x);
        int yInt = (int) Math.floor(y);
        
        if (xInt < 0 || yInt < 0 || xInt >= img.getWidth() - 1 || yInt >= img.getHeight() - 1) {
            if (xInt >= 0 && yInt >= 0 && xInt < img.getWidth() && yInt < img.getHeight()) {
                return img.getRGB(xInt, yInt);
            }
            return Color.BLACK.getRGB();
        }

        double xFract = x - xInt;
        double yFract = y - yInt;

        int c00 = img.getRGB(xInt, yInt);
        int c10 = img.getRGB(xInt + 1, yInt);
        int c01 = img.getRGB(xInt, yInt + 1);
        int c11 = img.getRGB(xInt + 1, yInt + 1);

        int r = blend( (c00 >> 16) & 0xFF, (c10 >> 16) & 0xFF, (c01 >> 16) & 0xFF, (c11 >> 16) & 0xFF, xFract, yFract);
        int g = blend( (c00 >> 8) & 0xFF,  (c10 >> 8) & 0xFF,  (c01 >> 8) & 0xFF,  (c11 >> 8) & 0xFF,  xFract, yFract);
        int b = blend( c00 & 0xFF,         c10 & 0xFF,         c01 & 0xFF,         c11 & 0xFF,         xFract, yFract);

        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private int blend(int c00, int c10, int c01, int c11, double u, double v) {
        double top = c00 * (1 - u) + c10 * u;
        double bottom = c01 * (1 - u) + c11 * u;
        return (int) Math.round(top * (1 - v) + bottom * v);
    }

    // Boilerplate wrapper to allow copying Image to system clipboard
    private class TransferableImage implements Transferable {
        private final Image image;

        public TransferableImage(Image image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DocDeskew().setVisible(true));
    }
}
