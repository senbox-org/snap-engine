package org.esa.stac.internal;
import org.json.simple.JSONObject;

public class EstablishedModifiers implements STACUtils {

    public static DownloadModifier planetaryComputer() {
        String signingURL = "https://planetarycomputer.microsoft.com/api/sas/v1/sign?href=";
        return input -> {
            JSONObject signedObject = STACUtils.getJSONFromURLStatic(signingURL + input);
            return (String) signedObject.get("href");
        };
    }

}
