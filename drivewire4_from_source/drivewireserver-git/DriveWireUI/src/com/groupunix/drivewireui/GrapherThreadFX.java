package com.groupunix.drivewireui;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * JavaFX-compatible grapher thread for DriveWire statistics visualization.
 * Replaces SWT-based GrapherThread with JavaFX Canvas and GraphicsContext.
 * 
 * Creates three graphs:
 * - Memory usage (total/used/free)
 * - Disk operations per second
 * - Virtual serial operations per second
 */
public class GrapherThreadFX implements Runnable {
    
    private long lastdisk = 0;
    private long lastvser = 0;
    
    private int interval = 2000;
    private int samples = 200;
    private boolean wanttodie = false;
    
    private int topgap = 20;
    private int ylabel = 30;
    private int xlabel = 70;
    
    private int[] disksamp = new int[samples];
    private int[] vsersamp = new int[samples];
    private long[] memfsamp = new long[samples];
    private long[] memtsamp = new long[samples];
    
    private int diskmax = 0;
    private int vsmax = 0;
    
    private int pos = 0;
    
    // JavaFX colors (replacing SWT Color)
    private Color colorGraphBGH = Color.rgb(0x90, 0x90, 0x90);
    private Color colorGraphBGL = Color.rgb(0x30, 0x30, 0x30);
    private Color colorLabel = Color.rgb(0xB5, 0xB5, 0xB5);
    private Color colorDiskOps = Color.rgb(0x80, 0x80, 0xB5);
    private Color colorMemGraphTotal = Color.rgb(0x80, 0xB5, 0x80);
    private Color colorMemGraphUsed = Color.rgb(0xB5, 0x80, 0x80);
    private Color colorVSerialOps = Color.rgb(0x80, 0xB5, 0xB5);
    
    // JavaFX Canvas references (set by MainWindowController)
    private Canvas canvasMemUse;
    private Canvas canvasDiskOps;
    private Canvas canvasVSerialOps;
    
    // WritableImage for off-screen rendering (optional optimization)
    private WritableImage imageMemUse;
    private WritableImage imageDiskOps;
    private WritableImage imageVSerialOps;
    
    private Font graphFont;
    
    /**
     * Set the canvas references for graph rendering.
     */
    public void setCanvases(Canvas memCanvas, Canvas diskCanvas, Canvas vserialCanvas) {
        this.canvasMemUse = memCanvas;
        this.canvasDiskOps = diskCanvas;
        this.canvasVSerialOps = vserialCanvas;
        
        // Initialize images if canvases are available
        if (memCanvas != null) {
            imageMemUse = new WritableImage((int)memCanvas.getWidth(), (int)memCanvas.getHeight());
        }
        if (diskCanvas != null) {
            imageDiskOps = new WritableImage((int)diskCanvas.getWidth(), (int)diskCanvas.getHeight());
        }
        if (vserialCanvas != null) {
            imageVSerialOps = new WritableImage((int)vserialCanvas.getWidth(), (int)vserialCanvas.getHeight());
        }
        
        // Initialize font
        graphFont = Font.font("Segoe UI", FontWeight.NORMAL, 11);
    }
    
    @Override
    public void run() {
        Thread.currentThread().setName("dwuiGrapherFX-" + Thread.currentThread().getId());
        
        // Initialize sample arrays
        for (int i = 0; i < samples; i++) {
            disksamp[i] = 0;
            vsersamp[i] = 0;
            memfsamp[i] = 0;
            memtsamp[i] = 0;
        }
        
        // Wait for server status
        System.out.println("GrapherThreadFX: Waiting for server status...");
        int waitCount = 0;
        while ((MainWin.serverStatus == null) && (!wanttodie)) {
            try {
                Thread.sleep(interval);
                waitCount++;
                if (waitCount % 5 == 0) {
                    System.out.println("GrapherThreadFX: Still waiting for server status (waited " + (waitCount * interval / 1000) + " seconds)...");
                }
            } catch (InterruptedException e) {
                wanttodie = true;
            }
        }
        
        if (MainWin.serverStatus != null) {
            System.out.println("GrapherThreadFX: Server status available, starting graph updates");
        } else {
            System.out.println("GrapherThreadFX: Exiting - server status never became available");
        }
        
        if (!wanttodie) {
            // Initialize baseline values
            lastdisk = MainWin.serverStatus.getDiskops();
            lastvser = MainWin.serverStatus.getVserialops();
            
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                wanttodie = true;
            }
            
            while ((MainWin.serverStatus != null) && (!wanttodie)) {
                // Snapshot current values
                synchronized(MainWin.serverStatus) {
                    this.disksamp[pos] = (int) (MainWin.serverStatus.getDiskops() - lastdisk);
                    this.vsersamp[pos] = (int) (MainWin.serverStatus.getVserialops() - lastvser);
                    
                    this.memfsamp[pos] = MainWin.serverStatus.getMemfree();
                    this.memtsamp[pos] = MainWin.serverStatus.getMemtotal();
                    
                    lastdisk = MainWin.serverStatus.getDiskops();
                    lastvser = MainWin.serverStatus.getVserialops();
                }
                
                // Update max values
                if (this.disksamp[pos] > this.diskmax)
                    diskmax = this.disksamp[pos];
                
                if (this.vsersamp[pos] > this.vsmax)
                    vsmax = this.vsersamp[pos];
                
                // Draw graphs on JavaFX Application Thread
                Platform.runLater(() -> {
                    if (!wanttodie) {
                        drawMemGraph();
                        drawDiskOpsGraph();
                        drawVSerialOpsGraph();
                    }
                });
                
                pos++;
                if (pos == samples)
                    pos = 0;
                
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    wanttodie = true;
                }
            }
        }
    }
    
    /**
     * Draw memory usage graph.
     */
    private void drawMemGraph() {
        if (canvasMemUse == null) return;
        
        GraphicsContext gc = canvasMemUse.getGraphicsContext2D();
        double width = canvasMemUse.getWidth() - xlabel;
        double height = canvasMemUse.getHeight() - ylabel - topgap;
        
        // Find max memory value for scaling
        long maxmem = 0;
        for (int i = 0; i < samples; i++) {
            if (memtsamp[i] > maxmem)
                maxmem = memtsamp[i];
        }
        
        if (maxmem == 0) maxmem = 1; // Avoid division by zero
        
        double vscale = height / maxmem;
        double hscale = width / samples;
        
        // Clear and draw gradient background
        gc.setFill(createGradient(colorGraphBGH, colorGraphBGL, width + xlabel, height + ylabel + topgap));
        gc.fillRect(0, 0, width + xlabel, height + ylabel + topgap);
        
        // Draw memory samples
        for (int i = 0; i < samples; i++) {
            int samp = pos + 1 + i;
            if (samp > (samples - 1))
                samp = samp - samples;
            
            if (memtsamp[samp] > 0) {
                // Total memory line
                gc.setFill(colorMemGraphTotal);
                double top = memtsamp[samp] * vscale;
                gc.fillRect(i * hscale, height - top + topgap, 1, 2);
                
                // Used memory bar
                gc.setFill(colorMemGraphUsed);
                double usedTop = (memtsamp[samp] - memfsamp[samp]) * vscale;
                gc.fillRect(i * hscale, height - usedTop + topgap, hscale, usedTop);
            }
        }
        
        // Draw grid lines and labels
        gc.setFont(graphFont);
        gc.setStroke(colorGraphBGL);
        gc.setLineWidth(1);
        gc.setLineDashes(2, 2);
        
        for (int i = 0; i < 5; i++) {
            double y = i * (height / 5);
            double mb = (height - y) / vscale / 1024.0;
            
            gc.setFill(colorLabel);
            gc.fillText(String.format("%d MB", (int)Math.round(mb)), width + 4, y - 5 + topgap);
            
            gc.strokeLine(0, y + topgap, width, y + topgap);
        }
        
        gc.setLineDashes(null);
        
        // Draw info text
        gc.setFill(colorLabel);
        double totalMB = memtsamp[pos] / 1024.0;
        double usedMB = (memtsamp[pos] - memfsamp[pos]) / 1024.0;
        double freeMB = memfsamp[pos] / 1024.0;
        gc.fillText(String.format("Total: %.1f MB   Used: %.1f MB   Free: %.1f MB", 
            totalMB, usedMB, freeMB), 5, height + 5 + topgap);
    }
    
    /**
     * Draw disk operations graph.
     */
    private void drawDiskOpsGraph() {
        if (canvasDiskOps == null) return;
        
        GraphicsContext gc = canvasDiskOps.getGraphicsContext2D();
        double width = canvasDiskOps.getWidth() - xlabel;
        double height = canvasDiskOps.getHeight() - ylabel - topgap;
        
        // Find max disk ops for scaling
        long maxdisk = 0;
        for (int i = 0; i < samples; i++) {
            if (this.disksamp[i] > maxdisk)
                maxdisk = disksamp[i];
        }
        
        if (maxdisk == 0) maxdisk = 1;
        
        double vscale = height / maxdisk;
        double hscale = width / samples;
        
        // Clear and draw gradient background
        gc.setFill(createGradient(colorGraphBGH, colorGraphBGL, width + xlabel, height + ylabel + topgap));
        gc.fillRect(0, 0, width + xlabel, height + ylabel + topgap);
        
        // Draw disk operation bars
        for (int i = 0; i < samples; i++) {
            int samp = pos + 1 + i;
            if (samp > (samples - 1))
                samp = samp - samples;
            
            if (disksamp[samp] > 0) {
                gc.setFill(colorDiskOps);
                double top = disksamp[samp] * vscale;
                gc.fillRect(i * hscale, height - top + topgap, hscale, top);
            }
        }
        
        // Draw grid lines and labels
        gc.setFont(graphFont);
        gc.setStroke(colorGraphBGL);
        gc.setLineWidth(1);
        gc.setLineDashes(2, 2);
        
        for (int i = 0; i < 5; i++) {
            double y = i * (height / 5);
            double ops = (height - y) / vscale;
            
            gc.setFill(colorLabel);
            gc.fillText(String.format("%d", (int)Math.round(ops)), width + 4, y - 5 + topgap);
            
            gc.strokeLine(0, y + topgap, width, y + topgap);
        }
        
        gc.setLineDashes(null);
        
        // Draw info text
        gc.setFill(colorLabel);
        gc.fillText(String.format("Disk operations/sec:  %d   Max:  %d", 
            this.disksamp[pos], this.diskmax), 5, height + 5 + topgap);
    }
    
    /**
     * Draw virtual serial operations graph.
     */
    private void drawVSerialOpsGraph() {
        if (canvasVSerialOps == null) return;
        
        GraphicsContext gc = canvasVSerialOps.getGraphicsContext2D();
        double width = canvasVSerialOps.getWidth() - xlabel;
        double height = canvasVSerialOps.getHeight() - ylabel - topgap;
        
        // Find max vserial ops for scaling
        long maxops = 0;
        for (int i = 0; i < samples; i++) {
            if (this.vsersamp[i] > maxops)
                maxops = vsersamp[i];
        }
        
        if (maxops == 0) maxops = 1;
        
        double vscale = height / maxops;
        double hscale = width / samples;
        
        // Clear and draw gradient background
        gc.setFill(createGradient(colorGraphBGH, colorGraphBGL, width + xlabel, height + ylabel + topgap));
        gc.fillRect(0, 0, width + xlabel, height + ylabel + topgap);
        
        // Draw vserial operation bars
        for (int i = 0; i < samples; i++) {
            int samp = pos + 1 + i;
            if (samp > (samples - 1))
                samp = samp - samples;
            
            if (vsersamp[samp] > 0) {
                gc.setFill(colorVSerialOps);
                double top = vsersamp[samp] * vscale;
                gc.fillRect(i * hscale, height - top + topgap, hscale, top);
            }
        }
        
        // Draw grid lines and labels
        gc.setFont(graphFont);
        gc.setStroke(colorGraphBGL);
        gc.setLineWidth(1);
        gc.setLineDashes(2, 2);
        
        for (int i = 0; i < 5; i++) {
            double y = i * (height / 5);
            double ops = (height - y) / vscale;
            
            gc.setFill(colorLabel);
            gc.fillText(String.format("%d", (int)Math.round(ops)), width + 4, y - 5 + topgap);
            
            gc.strokeLine(0, y + topgap, width, y + topgap);
        }
        
        gc.setLineDashes(null);
        
        // Draw info text
        gc.setFill(colorLabel);
        gc.fillText(String.format("Virtual serial operations/sec:  %d   Max:  %d", 
            this.vsersamp[pos], this.vsmax), 5, height + 5 + topgap);
    }
    
    /**
     * Create a linear gradient for modern look (replaces SWT fillGradientRectangle).
     */
    private javafx.scene.paint.LinearGradient createGradient(Color from, Color to, double width, double height) {
        return new javafx.scene.paint.LinearGradient(
            0, 0, 0, height,
            false,
            javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, from),
            new javafx.scene.paint.Stop(1, to)
        );
    }
    
    /**
     * Stop the grapher thread.
     */
    public void stop() {
        wanttodie = true;
    }
}

