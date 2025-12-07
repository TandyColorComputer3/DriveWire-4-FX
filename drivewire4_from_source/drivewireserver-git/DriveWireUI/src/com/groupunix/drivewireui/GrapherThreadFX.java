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
    
    // Modern color scheme - dark theme with vibrant accents
    private Color colorGraphBG = Color.rgb(0x1A, 0x1A, 0x2E);  // Dark blue-gray background
    private Color colorGraphBGLight = Color.rgb(0x2A, 0x2A, 0x3E);  // Slightly lighter for gradient
    private Color colorGrid = Color.rgb(0x3A, 0x3A, 0x4E);  // Subtle grid lines
    private Color colorLabel = Color.rgb(0xCC, 0xCC, 0xDD);  // Light gray for labels
    
    // Vibrant modern colors for graphs
    private Color colorDiskOps = Color.rgb(0x00, 0xFF, 0xFF);  // Cyan
    private Color colorDiskOpsGlow = Color.rgb(0x00, 0xFF, 0xFF, 0.3);  // Cyan glow
    
    private Color colorMemGraphTotal = Color.rgb(0x00, 0xFF, 0x88);  // Green
    private Color colorMemGraphTotalGlow = Color.rgb(0x00, 0xFF, 0x88, 0.3);  // Green glow
    private Color colorMemGraphUsed = Color.rgb(0xFF, 0x44, 0x88);  // Pink
    private Color colorMemGraphUsedGlow = Color.rgb(0xFF, 0x44, 0x88, 0.3);  // Pink glow
    
    private Color colorVSerialOps = Color.rgb(0xAA, 0x66, 0xFF);  // Purple
    private Color colorVSerialOpsGlow = Color.rgb(0xAA, 0x66, 0xFF, 0.3);  // Purple glow
    
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
        
        // Initialize modern font
        graphFont = Font.font("Segoe UI", FontWeight.BOLD, 10);
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
        
        // Clear and draw modern dark gradient background
        gc.setFill(createGradient(colorGraphBG, colorGraphBGLight, width + xlabel, height + ylabel + topgap));
        gc.fillRect(0, 0, width + xlabel, height + ylabel + topgap);
        
        // Draw subtle grid lines first
        gc.setStroke(colorGrid);
        gc.setLineWidth(0.5);
        for (int i = 1; i < 5; i++) {
            double y = i * (height / 5);
            gc.strokeLine(0, y + topgap, width, y + topgap);
        }
        
        // Build smooth line paths for memory graphs
        double[] totalX = new double[samples];
        double[] totalY = new double[samples];
        double[] usedX = new double[samples];
        double[] usedY = new double[samples];
        int validSamples = 0;
        
        for (int i = 0; i < samples; i++) {
            int samp = pos + 1 + i;
            if (samp > (samples - 1))
                samp = samp - samples;
            
            if (memtsamp[samp] > 0) {
                double x = i * hscale + hscale / 2;
                double totalTop = memtsamp[samp] * vscale;
                double usedTop = (memtsamp[samp] - memfsamp[samp]) * vscale;
                
                totalX[validSamples] = x;
                totalY[validSamples] = height - totalTop + topgap;
                usedX[validSamples] = x;
                usedY[validSamples] = height - usedTop + topgap;
                validSamples++;
            }
        }
        
        // Draw gradient area fill for used memory (with glow effect)
        if (validSamples > 1) {
            gc.setFill(createGradientArea(colorMemGraphUsed, colorMemGraphUsedGlow, height + topgap));
            gc.beginPath();
            gc.moveTo(usedX[0], height + topgap);
            for (int i = 0; i < validSamples; i++) {
                if (i == 0) {
                    gc.lineTo(usedX[i], usedY[i]);
                } else {
                    // Smooth curve using quadratic bezier
                    double cpx = (usedX[i-1] + usedX[i]) / 2;
                    double cpy = (usedY[i-1] + usedY[i]) / 2;
                    gc.quadraticCurveTo(usedX[i-1], usedY[i-1], cpx, cpy);
                }
            }
            gc.lineTo(usedX[validSamples-1], height + topgap);
            gc.closePath();
            gc.fill();
            
            // Draw smooth line for used memory with glow
            gc.setStroke(colorMemGraphUsed);
            gc.setLineWidth(2.5);
            gc.beginPath();
            gc.moveTo(usedX[0], usedY[0]);
            for (int i = 1; i < validSamples; i++) {
                double cpx = (usedX[i-1] + usedX[i]) / 2;
                double cpy = (usedY[i-1] + usedY[i]) / 2;
                gc.quadraticCurveTo(usedX[i-1], usedY[i-1], cpx, cpy);
            }
            gc.stroke();
        }
        
        // Draw smooth line for total memory
        if (validSamples > 1) {
            gc.setStroke(colorMemGraphTotal);
            gc.setLineWidth(2);
            gc.beginPath();
            gc.moveTo(totalX[0], totalY[0]);
            for (int i = 1; i < validSamples; i++) {
                double cpx = (totalX[i-1] + totalX[i]) / 2;
                double cpy = (totalY[i-1] + totalY[i]) / 2;
                gc.quadraticCurveTo(totalX[i-1], totalY[i-1], cpx, cpy);
            }
            gc.stroke();
        }
        
        // Draw grid labels
        gc.setFont(graphFont);
        gc.setFill(colorLabel);
        for (int i = 0; i < 5; i++) {
            double y = i * (height / 5);
            double mb = (height - y) / vscale / 1024.0;
            gc.fillText(String.format("%d MB", (int)Math.round(mb)), width + 6, y + 4 + topgap);
        }
        
        // Draw modern info text with better styling
        // Use the most recent sample (pos-1, wrapping around if needed)
        int latestPos = (pos > 0) ? pos - 1 : samples - 1;
        gc.setFill(colorLabel);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        double totalMB = memtsamp[latestPos] / 1024.0;
        double usedMB = (memtsamp[latestPos] - memfsamp[latestPos]) / 1024.0;
        double freeMB = memfsamp[latestPos] / 1024.0;
        gc.fillText(String.format("Total: %.1f MB   Used: %.1f MB   Free: %.1f MB", 
            totalMB, usedMB, freeMB), 8, height + 12 + topgap);
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
        
        // Clear and draw modern dark gradient background
        gc.setFill(createGradient(colorGraphBG, colorGraphBGLight, width + xlabel, height + ylabel + topgap));
        gc.fillRect(0, 0, width + xlabel, height + ylabel + topgap);
        
        // Draw subtle grid lines first
        gc.setStroke(colorGrid);
        gc.setLineWidth(0.5);
        for (int i = 1; i < 5; i++) {
            double y = i * (height / 5);
            gc.strokeLine(0, y + topgap, width, y + topgap);
        }
        
        // Build smooth line path for disk operations
        double[] xPoints = new double[samples];
        double[] yPoints = new double[samples];
        int validSamples = 0;
        
        for (int i = 0; i < samples; i++) {
            int samp = pos + 1 + i;
            if (samp > (samples - 1))
                samp = samp - samples;
            
            if (disksamp[samp] > 0) {
                xPoints[validSamples] = i * hscale + hscale / 2;
                double top = disksamp[samp] * vscale;
                yPoints[validSamples] = height - top + topgap;
                validSamples++;
            }
        }
        
        // Draw gradient area fill with glow effect
        if (validSamples > 1) {
            gc.setFill(createGradientArea(colorDiskOps, colorDiskOpsGlow, height + topgap));
            gc.beginPath();
            gc.moveTo(xPoints[0], height + topgap);
            for (int i = 0; i < validSamples; i++) {
                if (i == 0) {
                    gc.lineTo(xPoints[i], yPoints[i]);
                } else {
                    // Smooth curve using quadratic bezier
                    double cpx = (xPoints[i-1] + xPoints[i]) / 2;
                    double cpy = (yPoints[i-1] + yPoints[i]) / 2;
                    gc.quadraticCurveTo(xPoints[i-1], yPoints[i-1], cpx, cpy);
                }
            }
            gc.lineTo(xPoints[validSamples-1], height + topgap);
            gc.closePath();
            gc.fill();
            
            // Draw smooth line with glow
            gc.setStroke(colorDiskOps);
            gc.setLineWidth(3);
            gc.beginPath();
            gc.moveTo(xPoints[0], yPoints[0]);
            for (int i = 1; i < validSamples; i++) {
                double cpx = (xPoints[i-1] + xPoints[i]) / 2;
                double cpy = (yPoints[i-1] + yPoints[i]) / 2;
                gc.quadraticCurveTo(xPoints[i-1], yPoints[i-1], cpx, cpy);
            }
            gc.stroke();
        }
        
        // Draw grid labels
        gc.setFont(graphFont);
        gc.setFill(colorLabel);
        for (int i = 0; i < 5; i++) {
            double y = i * (height / 5);
            double ops = (height - y) / vscale;
            gc.fillText(String.format("%d", (int)Math.round(ops)), width + 6, y + 4 + topgap);
        }
        
        // Draw modern info text
        // Use the most recent sample (pos-1, wrapping around if needed)
        int latestPos = (pos > 0) ? pos - 1 : samples - 1;
        gc.setFill(colorLabel);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        gc.fillText(String.format("Disk operations/sec:  %d   Max:  %d", 
            this.disksamp[latestPos], this.diskmax), 8, height + 12 + topgap);
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
        
        // Clear and draw modern dark gradient background
        gc.setFill(createGradient(colorGraphBG, colorGraphBGLight, width + xlabel, height + ylabel + topgap));
        gc.fillRect(0, 0, width + xlabel, height + ylabel + topgap);
        
        // Draw subtle grid lines first
        gc.setStroke(colorGrid);
        gc.setLineWidth(0.5);
        for (int i = 1; i < 5; i++) {
            double y = i * (height / 5);
            gc.strokeLine(0, y + topgap, width, y + topgap);
        }
        
        // Build smooth line path for vserial operations
        double[] xPoints = new double[samples];
        double[] yPoints = new double[samples];
        int validSamples = 0;
        
        for (int i = 0; i < samples; i++) {
            int samp = pos + 1 + i;
            if (samp > (samples - 1))
                samp = samp - samples;
            
            if (vsersamp[samp] > 0) {
                xPoints[validSamples] = i * hscale + hscale / 2;
                double top = vsersamp[samp] * vscale;
                yPoints[validSamples] = height - top + topgap;
                validSamples++;
            }
        }
        
        // Draw gradient area fill with glow effect
        if (validSamples > 1) {
            gc.setFill(createGradientArea(colorVSerialOps, colorVSerialOpsGlow, height + topgap));
            gc.beginPath();
            gc.moveTo(xPoints[0], height + topgap);
            for (int i = 0; i < validSamples; i++) {
                if (i == 0) {
                    gc.lineTo(xPoints[i], yPoints[i]);
                } else {
                    // Smooth curve using quadratic bezier
                    double cpx = (xPoints[i-1] + xPoints[i]) / 2;
                    double cpy = (yPoints[i-1] + yPoints[i]) / 2;
                    gc.quadraticCurveTo(xPoints[i-1], yPoints[i-1], cpx, cpy);
                }
            }
            gc.lineTo(xPoints[validSamples-1], height + topgap);
            gc.closePath();
            gc.fill();
            
            // Draw smooth line with glow
            gc.setStroke(colorVSerialOps);
            gc.setLineWidth(3);
            gc.beginPath();
            gc.moveTo(xPoints[0], yPoints[0]);
            for (int i = 1; i < validSamples; i++) {
                double cpx = (xPoints[i-1] + xPoints[i]) / 2;
                double cpy = (yPoints[i-1] + yPoints[i]) / 2;
                gc.quadraticCurveTo(xPoints[i-1], yPoints[i-1], cpx, cpy);
            }
            gc.stroke();
        }
        
        // Draw grid labels
        gc.setFont(graphFont);
        gc.setFill(colorLabel);
        for (int i = 0; i < 5; i++) {
            double y = i * (height / 5);
            double ops = (height - y) / vscale;
            gc.fillText(String.format("%d", (int)Math.round(ops)), width + 6, y + 4 + topgap);
        }
        
        // Draw modern info text
        // Use the most recent sample (pos-1, wrapping around if needed)
        int latestPos = (pos > 0) ? pos - 1 : samples - 1;
        gc.setFill(colorLabel);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        gc.fillText(String.format("Virtual serial operations/sec:  %d   Max:  %d", 
            this.vsersamp[latestPos], this.vsmax), 8, height + 12 + topgap);
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
     * Create a vertical gradient for area fills (bright at top, transparent at bottom).
     */
    private javafx.scene.paint.LinearGradient createGradientArea(Color color, Color glowColor, double bottomY) {
        return new javafx.scene.paint.LinearGradient(
            0, 0, 0, bottomY,
            false,
            javafx.scene.paint.CycleMethod.NO_CYCLE,
            new javafx.scene.paint.Stop(0, color),
            new javafx.scene.paint.Stop(0.7, glowColor),
            new javafx.scene.paint.Stop(1, Color.TRANSPARENT)
        );
    }
    
    /**
     * Stop the grapher thread.
     */
    public void stop() {
        wanttodie = true;
    }
}

