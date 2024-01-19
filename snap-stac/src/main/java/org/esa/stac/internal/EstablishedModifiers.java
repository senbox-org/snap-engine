package org.esa.stac.internal;
import org.json.simple.JSONObject;

public class EstablishedModifiers implements STACUtils {

    public static DownloadModifier planetaryComputer() {
        String signingURL = "https://planetarycomputer.microsoft.com/api/sas/v1/sign?href=";
        return input -> {
            for (int tryCount = 0; tryCount < 20; tryCount++){
                try{
                    JSONObject signedObject = STACUtils.getJSONFromURLStatic(signingURL + input);
                    return (String) signedObject.get("href");
                }catch(Exception e){
                    try {
                        Thread.sleep((long) Math.pow(500, tryCount));
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            return null;
        };
    }

}
