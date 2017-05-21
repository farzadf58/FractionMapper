/* compiled 3-12-2017*/
 /*F.Fereidouni@Spechron.com*/

import blablab.MetaData;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.TextReader;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import java.net.URL;
import java.util.Date;

public class Fraction_Mapper
        implements PlugInFilter {

    protected ImagePlus imp;
    FileInfo fi = new FileInfo();

    public int setup(String string, ImagePlus imagePlus) {
        this.imp = imagePlus;
        return 31;
    }

    public void run(ImageProcessor imageProcessor) {
        int n;
        int phasor_dim = 400;
        ImageStack imageStack = this.imp.getStack();

        double threshold = Prefs.get((String) "FractionMapper.threshold", (double) 50.0);
        boolean bl = Prefs.get((String) "FractionMapper.ShowFraction", (boolean) false);
        boolean show2d =Prefs.get((String) "FractionMapper.Show2d", (boolean) true);
        double[][] Fr = new double[2][2];
        this.fi.directory = Prefs.get((String) "FractionMapper.directory", IJ.getDirectory("luts"));
        this.fi.fileName = Prefs.get((String) "FractionMapper.filename", "jet.lut");

        Fr[0][0] = Prefs.get((String) "FractionMapper.Fr00", (double) 0.0);
        Fr[0][1] = Prefs.get((String) "FractionMapper.Fr01", (double) 0.5);
        Fr[1][0] = Prefs.get((String) "FractionMapper.Fr10", (double) 0.5);
        Fr[1][1] = Prefs.get((String) "FractionMapper.Fr11", (double) 1.0);
        GenericDialog genericDialog = new GenericDialog("Fraction Mapper");
        genericDialog.addMessage("LUT:  " + this.fi.fileName);
        genericDialog.addNumericField("Threshold:", threshold, 0);
        genericDialog.addMessage("Range 1----------");
        genericDialog.addNumericField("From:", Fr[0][0] * 100.0, 0);
        genericDialog.addNumericField("To:", Fr[0][1] * 100.0, 0);
        genericDialog.addMessage("Range 2----------");
        genericDialog.addNumericField("From:", Fr[1][0] * 100.0, 0);
        genericDialog.addNumericField("To:", Fr[1][1] * 100.0, 0);
        genericDialog.addCheckbox("Show fraction?", bl);
        genericDialog.addCheckbox("Show 2D plot?", show2d);
        Button button = new Button("Change LUT");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OpenDialog od = new OpenDialog("Open LUT", null);
                fi.fileName = od.getFileName();
                fi.directory = od.getDirectory();

            }

        });
        genericDialog.add((Component) button);
        genericDialog.showDialog();
        threshold = genericDialog.getNextNumber();
        Fr[0][0] = genericDialog.getNextNumber() / 100.0;
        Fr[0][1] = genericDialog.getNextNumber() / 100.0;
        Fr[1][0] = genericDialog.getNextNumber() / 100.0;
        Fr[1][1] = genericDialog.getNextNumber() / 100.0;
        bl = genericDialog.getNextBoolean();
        show2d=genericDialog.getNextBoolean();
        if (genericDialog.wasCanceled()) {
            return;
        }
        Prefs.set((String) "FractionMapper.threshold", (double) threshold);
        Prefs.set((String) "FractionMapper.ShowFraction", (boolean) bl);
        Prefs.set((String) "FractionMapper.Show2d", (boolean) show2d);
        Prefs.set((String) "FractionMapper.Fr00", (double) Fr[0][0]);
        Prefs.set((String) "FractionMapper.Fr01", (double) Fr[0][1]);
        Prefs.set((String) "FractionMapper.Fr10", (double) Fr[1][0]);
        Prefs.set((String) "FractionMapper.Fr11", (double) Fr[1][1]);
        Prefs.set((String) "FractionMapper.directory", this.fi.directory);
        Prefs.set((String) "FractionMapper.filename", this.fi.fileName);
        Prefs.savePreferences();
        this.fi.reds = new byte[256];
        this.fi.greens = new byte[256];
        this.fi.blues = new byte[256];
        this.fi.lutSize = 256;
        this.openLut(this.fi);
        int[] RED = new int[256];
        int[] BLUE = new int[256];
        int[] GREEN = new int[256];
        for (n = 0; n < 256; ++n) {
            RED[n] = this.fi.reds[n] & 255;
            GREEN[n] = this.fi.greens[n] & 255;
            BLUE[n] = this.fi.blues[n] & 255;
        }

        int DimX = imageProcessor.getWidth();
        int DimY = imageProcessor.getHeight();
        int n3 = imageStack.getSize();
        double[][][] Intensity = new double[DimX][DimY][n3 + 1];
        double[] IntensityMax = new double[3];
        
        double[][] alpha = new double[DimX][DimY];
        double d2 = 0.0;

        // fixing the negative values. I will ssearch for the negative numbers, and put them zero. larger than 1 set to 1.
   

        for (int i = 0; i < DimY; ++i) {
            IJ.showProgress((int) i, (int) (DimY * 2));
            for (int j = 0; j < DimX; ++j) {
                for (int k = 1; k <= n3; ++k) {
                    Intensity[j][i][k] = imageStack.getVoxel(j, i, k - 1);

                    if (Intensity[j][i][k] > IntensityMax[k]) {
                        IntensityMax[k] = Intensity[j][i][k];
                    }
                }
                Intensity[j][i][0] = Intensity[j][i][1] + Intensity[j][i][2];

                if (Intensity[j][i][0] <= d2) {
                    continue;
                }
                d2 = Intensity[j][i][0];
            }
        }

        
        
        
        double d3 = 0.0;
        double d4 = 0.0;
        double d5 = 0.0;
        for (int j = 0; j < DimY; ++j) {
            IJ.showProgress((int) (j + DimY), (int) (DimY * 2));
            for (int i = 0; i < DimX; ++i) {
                if (Intensity[i][j][0] <= threshold) {
                    continue;
                }
                alpha[i][j] = Intensity[i][j][1] / Intensity[i][j][0];
                if (alpha[i][j]<0){alpha[i][j]=0;}
                if (alpha[i][j]>1){alpha[i][j]=1;}
            }
        }

        //filling the histogram    
        // generating new image to show the phasor
        ImagePlus phasor = NewImage.createRGBImage("Phasor plot-" + imp.getTitle(), phasor_dim , phasor_dim,
                1, NewImage.FILL_BLACK);
        ImageProcessor ip_phasor = phasor.getProcessor();
        
        

        //coordinates for histogram
        double[] phasormap_r = new double[DimX * DimY];
        double[] phasormap_i = new double[DimX * DimY];
        int phasor_hist[][] = new int[phasor_dim + 1][phasor_dim + 1];
        double max = 0;
        double min = 255 * 255 * 255;
        for (int i = 0; i < DimX * DimY; i++) {
            phasormap_r[i] = -2;
            phasormap_i[i] = -2;
        }

        int jx = 0;
        int jy = 0;



        for (int y = 0; y < DimY; y++) {
            for (int x = 0; x < DimX; x++) {

                if (Intensity[x][y][0] > threshold) {
                    jx = (int) (phasor_dim * Intensity[x][y][1] / IntensityMax[1]);
                    jy = (int) (phasor_dim *(1- Intensity[x][y][2] / IntensityMax[2]));

                    phasormap_r[(x) + (y) * DimX] = (double) jx;
                    phasormap_i[(x) + (y) * DimX] = (double) jy;
                    //phasormap_r[x + y * DimX] = (double) jx;
                    //phasormap_i[x + y * DimX] = (double) jy;
                    if (jx < phasor_dim & jy < phasor_dim & jx > 0 & jy > 0) {
                        phasor_hist[jx][jy] += 1;
                        if (phasor_hist[jx][jy] > max) {
                            max = phasor_hist[jx][jy];
                        }
                        if (phasor_hist[jx][jy] < min & phasor_hist[jx][jy] > 0) {
                            min = phasor_hist[jx][jy];
                        }

                    }
                }
            }
        }

        MetaData meta = new MetaData(phasor);
        meta.set(MetaData.MetaDataType.SX, DimX);
        meta.set(MetaData.MetaDataType.SY, DimY);
        meta.set(MetaData.MetaDataType.PHASORMAP_r, phasormap_r);
        meta.set(MetaData.MetaDataType.PHASORMAP_i, phasormap_i);
        meta.set(MetaData.MetaDataType.IMAGE_TITLE, imp.getTitle());

        for (int y = 0; y < DimY; y++) {
            for (int x = 0; x < DimX; x++) {

                jx = (int) (phasor_dim * Intensity[x][y][1] / IntensityMax[1]);
                jy = (int) (phasor_dim *(1- Intensity[x][y][2] / IntensityMax[2]));
                if (jx < phasor_dim & jy < phasor_dim & jx > 0 & jy > 0) {
                    if (Intensity[x][y][0] > threshold) {
                        if ((phasor_hist[jx][jy] - min) / (max - min) >= 0) {
                            ip_phasor.setColor(Color.HSBtoRGB((float) ((phasor_hist[jx][jy] - min) / (max - min)), 1f, 1f));
                            ip_phasor.drawPixel(jx, jy);

                        }
                    }
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Date date = new Date();
        String string = String.format("%tc", date);
        ImagePlus imagePlus = NewImage.createImage((String) ("F -" + this.imp.getTitle() + " " + string), (int) DimX, (int) DimY, (int) 1, (int) 32, (int) 1);
        ImageProcessor imageProcessor2 = imagePlus.getProcessor();
        ImagePlus imagePlus2 = NewImage.createRGBImage((String) ("Fraction  Map-" + this.imp.getTitle() + " " + string), (int) DimX, (int) DimY, (int) 1, (int) 1);
        ImageProcessor imageProcessor3 = imagePlus2.getProcessor();

        int[] arrn4 = new int[3];
        int n4 = 0;
        for (int j = 0; j < DimY; ++j) {
            for (int i = 0; i < DimX; ++i) {
                if (Intensity[i][j][0] <= threshold) {
                    continue;
                }
                if (alpha[i][j] >= 0.0 && alpha[i][j] <= 1.0) {
                    d3 += 1.0;
                }
                if (alpha[i][j] >= Fr[0][0] && alpha[i][j] <= Fr[0][1] || alpha[i][j] > Fr[1][0] && alpha[i][j] <= Fr[1][1]) {
                    n4 = (int) ((1.0 - alpha[i][j]) * 255.0);
                    arrn4[0] = (int) (Intensity[i][j][0] * (double) RED[n4] / d2);
                    arrn4[1] = (int) (Intensity[i][j][0] * (double) GREEN[n4] / d2);
                    arrn4[2] = (int) (Intensity[i][j][0] * (double) BLUE[n4] / d2);
                    if (alpha[i][j] >= Fr[0][0] && alpha[i][j] <= Fr[0][1]) {
                        d4 += 1.0;
                    }
                    if (alpha[i][j] >= Fr[1][0] && alpha[i][j] <= Fr[1][1]) {
                        d5 += 1.0;
                    }
                    imageProcessor3.putPixel(i, j, arrn4);
                } else {
                    arrn4[0] = (int) (Intensity[i][j][0] * 255.0 / d2);
                    arrn4[1] = (int) (Intensity[i][j][0] * 255.0 / d2);
                    arrn4[2] = (int) (Intensity[i][j][0] * 255.0 / d2);
                    imageProcessor3.putPixel(i, j, arrn4);
                }
                imageProcessor2.putPixelValue(i, j, alpha[i][j]);
            }
        }
        String string2 = "";
        String string3 = "";
        string2 = String.format("%.2f", d4 / d3 * 100.0);
        string3 = String.format("%.2f", d5 / d3 * 100.0);
        ResultsTable resultsTable = Analyzer.getResultsTable();
        if (resultsTable == null) {
            resultsTable = new ResultsTable();
            Analyzer.setResultsTable((ResultsTable) resultsTable);
        }
        resultsTable.incrementCounter();
        resultsTable.addValue("file name", this.imp.getTitle());
        resultsTable.addValue("Date/Time", string);
        resultsTable.addValue("Fraction 1", string2);
        resultsTable.addValue("Fraction 2", string3);
        resultsTable.addValue("Threshold", threshold);
        resultsTable.addValue("Range 1 from ", Fr[0][0] * 100.0);
        resultsTable.addValue("To", Fr[0][1] * 100.0);
        resultsTable.addValue("Range 2 from", Fr[1][0] * 100.0);
        resultsTable.addValue("To ", Fr[1][1] * 100.0);
        resultsTable.show("Results");
        imagePlus2.draw();
        imagePlus2.show();
if (show2d) {
        phasor.show();
        phasor.updateAndDraw();
}

        if (bl) {
            imagePlus.show();
        }

    }

    boolean openLut(FileInfo fileInfo) {
        String string;
        File file;
        boolean bl = fileInfo.url != null && !fileInfo.url.equals("");
        int n = 0;
        String string2 = string = bl ? fileInfo.url : fileInfo.directory + fileInfo.fileName;
        if (!bl && (n = (int) (file = new File(string)).length()) > 10000) {
            IJ.error((String) string);
            return false;
        }
        int n2 = 0;
        try {
            if (n > 768) {
                n2 = this.openBinaryLut(fileInfo, bl, false);
            }
            if (n2 == 0 && (n == 0 || n == 768 || n == 970)) {
                n2 = this.openBinaryLut(fileInfo, bl, true);
            }
            if (n2 == 0 && n > 768) {
                n2 = this.openTextLut(fileInfo);
            }
            if (n2 == 0) {
                IJ.error((String) string);
            }
        } catch (IOException var6_7) {
            IJ.error((String) "LUT Loader", (String) ("" + var6_7));
        }
        return n2 == 256;
    }

    int openBinaryLut(FileInfo fileInfo, boolean bl, boolean bl2) throws IOException {
        InputStream inputStream = bl ? new URL(fileInfo.url + fileInfo.fileName).openStream() : new FileInputStream(fileInfo.directory + fileInfo.fileName);
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int n = 256;
        if (!bl2) {
            int n2 = dataInputStream.readInt();
            if (n2 != 1229147980) {
                dataInputStream.close();
                return 0;
            }
            short s = dataInputStream.readShort();
            n = dataInputStream.readShort();
            short s2 = dataInputStream.readShort();
            short s3 = dataInputStream.readShort();
            long l = dataInputStream.readLong();
            long l2 = dataInputStream.readLong();
            int n3 = dataInputStream.readInt();
        }
        dataInputStream.read(fileInfo.reds, 0, n);
        dataInputStream.read(fileInfo.greens, 0, n);
        dataInputStream.read(fileInfo.blues, 0, n);
        if (n < 256) {
            this.interpolate(fileInfo.reds, fileInfo.greens, fileInfo.blues, n);
        }
        dataInputStream.close();
        return 256;
    }

    int openTextLut(FileInfo fileInfo) throws IOException {
        TextReader textReader = new TextReader();
        textReader.hideErrorMessages();
        ImageProcessor imageProcessor = textReader.open(fileInfo.directory + fileInfo.fileName);
        if (imageProcessor == null) {
            return 0;
        }
        int n = imageProcessor.getWidth();
        int n2 = imageProcessor.getHeight();
        if (n < 3 || n > 4 || n2 < 256 || n2 > 258) {
            return 0;
        }
        int n3 = n == 4 ? 1 : 0;
        int n4 = n2 > 256 ? 1 : 0;
        imageProcessor.setRoi(n3, n4, 3, 256);
        imageProcessor = imageProcessor.crop();
        for (int i = 0; i < 256; ++i) {
            fileInfo.reds[i] = (byte) imageProcessor.getPixelValue(0, i);
            fileInfo.greens[i] = (byte) imageProcessor.getPixelValue(1, i);
            fileInfo.blues[i] = (byte) imageProcessor.getPixelValue(2, i);
        }
        return 256;
    }

    void interpolate(byte[] arrby, byte[] arrby2, byte[] arrby3, int n) {
        byte[] arrby4 = new byte[n];
        byte[] arrby5 = new byte[n];
        byte[] arrby6 = new byte[n];
        System.arraycopy(arrby, 0, arrby4, 0, n);
        System.arraycopy(arrby2, 0, arrby5, 0, n);
        System.arraycopy(arrby3, 0, arrby6, 0, n);
        double threshold = (double) n / 256.0;
        for (int i = 0; i < 256; ++i) {
            int n2 = (int) ((double) i * threshold);
            int n3 = n2 + 1;
            if (n3 == n) {
                n3 = n - 1;
            }
            double d2 = (double) i * threshold - (double) n2;
            arrby[i] = (byte) ((1.0 - d2) * (double) (arrby4[n2] & 255) + d2 * (double) (arrby4[n3] & 255));
            arrby2[i] = (byte) ((1.0 - d2) * (double) (arrby5[n2] & 255) + d2 * (double) (arrby5[n3] & 255));
            arrby3[i] = (byte) ((1.0 - d2) * (double) (arrby6[n2] & 255) + d2 * (double) (arrby6[n3] & 255));
        }
    }
}
