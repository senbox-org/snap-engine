/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.jp2.reader.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kraftek on 7/15/2015.
 */
public class CodeStreamInfo {

    private int tx0;
    private int ty0;
    private int tileWidth;
    private int tileHeight;
    private int numTilesX;
    private int numTilesY;
    private String csty;
    private String prg;
    private int numLayers;
    private int mct;
    private List<TileComponentInfo> componentTilesInfo;

    public CodeStreamInfo() {
        componentTilesInfo = new ArrayList<>();
    }

    public int getTx0() {
        return tx0;
    }

    public void setTx0(int tx0) {
        this.tx0 = tx0;
    }

    public int getTy0() {
        return ty0;
    }

    public void setTy0(int ty0) {
        this.ty0 = ty0;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    public int getNumTilesX() {
        return numTilesX;
    }

    public void setNumTilesX(int numTilesX) {
        this.numTilesX = numTilesX;
    }

    public int getNumTilesY() {
        return numTilesY;
    }

    public void setNumTilesY(int numTilesY) {
        this.numTilesY = numTilesY;
    }

    public String getCsty() {
        return csty;
    }

    public void setCsty(String csty) { this.csty = csty; }

    public String getPrg() {
        return prg;
    }

    public void setPrg(String prg) { this.prg = prg; }

    public int getNumLayers() {
        return numLayers;
    }

    public void setNumLayers(int numLayers) {
        this.numLayers = numLayers;
    }

    public int getMct() {
        return mct;
    }

    public void setMct(int mct) {
        this.mct = mct;
    }

    public List<TileComponentInfo> getComponentTilesInfo() {
        return componentTilesInfo;
    }

    public void addComponentTileInfo(TileComponentInfo componentTileInfo) {
        componentTilesInfo.add(componentTileInfo);
    }

    public int getNumResolutions() {
        int numResolutions = 0;
        if (componentTilesInfo.size() > 0) {
            numResolutions = componentTilesInfo.get(0).getNumResolutions();
        }
        return numResolutions;
    }

    public MetadataElement toMetadataElement() {
        MetadataElement element = new MetadataElement("Code Stream");
        element.addAttribute(new MetadataAttribute("tx0", ProductData.ASCII.createInstance(String.valueOf(this.tx0)), false));
        element.addAttribute(new MetadataAttribute("ty0", ProductData.ASCII.createInstance(String.valueOf(this.ty0)), false));
        element.addAttribute(new MetadataAttribute("tileWidth", ProductData.ASCII.createInstance(String.valueOf(this.tileWidth)), false));
        element.addAttribute(new MetadataAttribute("tileHeight", ProductData.ASCII.createInstance(String.valueOf(this.tileHeight)), false));
        element.addAttribute(new MetadataAttribute("numTilesX", ProductData.ASCII.createInstance(String.valueOf(this.numTilesX)), false));
        element.addAttribute(new MetadataAttribute("numTilesY", ProductData.ASCII.createInstance(String.valueOf(this.numTilesY)), false));
        element.addAttribute(new MetadataAttribute("csty", ProductData.ASCII.createInstance(String.valueOf(this.csty)), false));
        element.addAttribute(new MetadataAttribute("prg", ProductData.ASCII.createInstance(String.valueOf(this.prg)), false));
        element.addAttribute(new MetadataAttribute("numLayers", ProductData.ASCII.createInstance(String.valueOf(this.numLayers)), false));
        element.addAttribute(new MetadataAttribute("mct", ProductData.ASCII.createInstance(String.valueOf(this.mct)), false));
        for (TileComponentInfo tcInfo : this.componentTilesInfo) {
            element.addElement(tcInfo.toMetadataElement());
        }
        return element;
    }

    public static class TileComponentInfo {
        private String csty;
        private int numResolutions;
        private int codeBlockWidth;
        private int codeBlockHeight;
        private int codeBlockSty;
        private int qmfbid;
        private List<int[]> preccIntSize;
        private String qntsty;
        private int numGBits;
        private List<int[]> stepSizes;
        private int roiShift;

        public TileComponentInfo() {
            preccIntSize = new ArrayList<>();
            stepSizes = new ArrayList<>();
        }

        public String getCsty() {
            return csty;
        }

        public void setCsty(String csty) {
            this.csty = csty;
        }

        public int getNumResolutions() {
            return numResolutions;
        }

        public void setNumResolutions(int numResolutions) {
            this.numResolutions = numResolutions;
        }

        public int getCodeBlockWidth() {
            return codeBlockWidth;
        }

        public void setCodeBlockWidth(int codeBlockWidth) {
            //Assert.argument(codeBlockWidth == (int)Math.pow(2, numResolutions), "codeBlockWidth must be 2^numResolutions");
            this.codeBlockWidth = codeBlockWidth;
        }

        public int getCodeBlockHeight() {
            return codeBlockHeight;
        }

        public void setCodeBlockHeight(int codeBlockHeight) {
            //Assert.argument(codeBlockHeight == (int)Math.pow(2, numResolutions), "codeBlockHeight must be 2^numResolutions");
            this.codeBlockHeight = codeBlockHeight;
        }

        public int getCodeBlockSty() {
            return codeBlockSty;
        }

        public void setCodeBlockSty(int codeBlockSty) {
            this.codeBlockSty = codeBlockSty;
        }

        public int getQmfbid() {
            return qmfbid;
        }

        public void setQmfbid(int qmfbid) {
            this.qmfbid = qmfbid;
        }

        public List<int[]> getPreccIntSize() {
            return preccIntSize;
        }

        public void addPreccInt(int width, int height) {
            preccIntSize.add(new int[] { width, height });
        }

        public String getQntsty() {
            return qntsty;
        }

        public void setQntsty(String qntsty) {
            this.qntsty = qntsty;
        }

        public int getNumGBits() {
            return numGBits;
        }

        public void setNumGBits(int numGBits) {
            this.numGBits = numGBits;
        }

        public List<int[]> getStepSizes() {
            return stepSizes;
        }

        public void addStepSize(int m, int e) {
            stepSizes.add(new int[] { m, e });
        }

        public int getRoiShift() {
            return roiShift;
        }

        public void setRoiShift(int roiShift) {
            this.roiShift = roiShift;
        }

        public MetadataElement toMetadataElement() {
            MetadataElement element = new MetadataElement("Codestream Tile Component");
            element.addAttribute(new MetadataAttribute("csty", ProductData.ASCII.createInstance(String.valueOf(this.csty)), false));
            element.addAttribute(new MetadataAttribute("numResolutions", ProductData.ASCII.createInstance(String.valueOf(this.numResolutions)), false));
            element.addAttribute(new MetadataAttribute("codeBlockWidth", ProductData.ASCII.createInstance(String.valueOf(this.codeBlockWidth)), false));
            element.addAttribute(new MetadataAttribute("codeBlockHeight", ProductData.ASCII.createInstance(String.valueOf(this.codeBlockHeight)), false));
            element.addAttribute(new MetadataAttribute("codeBlockSty", ProductData.ASCII.createInstance(String.valueOf(this.codeBlockSty)), false));
            element.addAttribute(new MetadataAttribute("qmfbid", ProductData.ASCII.createInstance(String.valueOf(this.qmfbid)), false));
            element.addAttribute(new MetadataAttribute("qntsty", ProductData.ASCII.createInstance(String.valueOf(this.qntsty)), false));
            element.addAttribute(new MetadataAttribute("numGBits", ProductData.ASCII.createInstance(String.valueOf(this.numGBits)), false));
            element.addAttribute(new MetadataAttribute("roiShift", ProductData.ASCII.createInstance(String.valueOf(this.roiShift)), false));
            if (this.preccIntSize.size() > 0) {
                StringBuilder values = new StringBuilder();
                for (int[] pair : this.preccIntSize) {
                    values.append("(").append(pair[0]).append(",").append(pair[1]).append(") ");
                }
                values.deleteCharAt(values.length() - 1);
                element.addAttribute(new MetadataAttribute("preccintSize", ProductData.ASCII.createInstance(values.toString()), false));
            }
            if (this.stepSizes.size() > 0) {
                StringBuilder values = new StringBuilder();
                for (int[] pair : this.stepSizes) {
                    values.append("(").append(pair[0]).append(",").append(pair[1]).append(") ");
                }
                values.deleteCharAt(values.length() - 1);
                element.addAttribute(new MetadataAttribute("stepSizes", ProductData.ASCII.createInstance(values.toString()), false));
            }
            return element;
        }
    }
}
