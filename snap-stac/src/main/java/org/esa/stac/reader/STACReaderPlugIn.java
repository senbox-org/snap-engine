package org.esa.stac.reader;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.stac.internal.StacItem;
import org.json.simple.JSONObject;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

public class STACReaderPlugIn implements ProductReaderPlugIn {

    public static final String[] FORMAT_NAMES = new String[]{"STAC"};
    public static final String[] JSON_FILE_EXTENSION = {".json"};

    public STACReaderPlugIn(){

    }


    @Override
    public DecodeQualification getDecodeQualification(Object productInputFile) {
        if (productInputFile instanceof String){
            try{
                if (((String) productInputFile).startsWith("http")){
                    new StacItem((String) productInputFile);
                }else{
                    new StacItem(new File((String) productInputFile));
                }
                return DecodeQualification.INTENDED;
            }catch (Exception e){
                return DecodeQualification.UNABLE;
            }
        }else if (productInputFile instanceof File){
            try{
                new StacItem((File) productInputFile);
            }catch (Exception e){
                return DecodeQualification.UNABLE;
            }
            return DecodeQualification.INTENDED;
        }else if (productInputFile instanceof JSONObject){
            try{
                new StacItem((JSONObject) productInputFile);
            }catch (Exception e){
                return DecodeQualification.UNABLE;
            }
            return DecodeQualification.INTENDED;
        }else if (productInputFile instanceof StacItem){
            return DecodeQualification.INTENDED;
        }else {
            return DecodeQualification.UNABLE;
        }
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[0];
    }

    @Override
    public ProductReader createReaderInstance() {
        return new STACReader(this);
    }


    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return JSON_FILE_EXTENSION;
    }

    @Override
    public String getDescription(Locale locale) {
        return "STAC Item.";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return null;
    }
}
