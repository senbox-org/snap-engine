package org.esa.snap.product.library.v2;

import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.QueryException;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.datasource.remote.DownloadStrategy;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.datasource.remote.scihub.SciHubDataSource;
import ro.cs.tao.datasource.remote.scihub.parameters.SciHubParameterProvider;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 5/8/2019.
 */
public class TestDataSource {

    public static void main(String args[]) {
        System.out.println("TestDataSource for product library.");
        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        System.out.println("SupportedSensors: " + sciHubParameterProvider.getSupportedSensors());

        SciHub_Sentinel2_Test();
    }

    private static void SciHub_Sentinel2_Test() {
        try {
            Logger logger = LogManager.getLogManager().getLogger("");
            for (Handler handler : logger.getHandlers()) {
                handler.setLevel(Level.INFO);
            }
            DataSource dataSource = getDatasourceRegistry().getService(SciHubDataSource.class);
            dataSource.setCredentials("kraftek", "cei7samurai");
            String[] sensors = dataSource.getSupportedSensors();

            DataQuery query = dataSource.createQuery(sensors[1]);
            query.addParameter(CommonParameterNames.PLATFORM, "Sentinel-2");
            QueryParameter<Date> begin = query.createParameter(CommonParameterNames.START_DATE, Date.class);
            begin.setMinValue(Date.from(LocalDateTime.of(2019, 2, 1, 0, 0, 0, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));
            begin.setMaxValue(Date.from(LocalDateTime.of(2019, 3, 1, 0, 0, 0, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));
            query.addParameter(begin);
            Polygon2D aoi = Polygon2D.fromWKT("POLYGON((22.8042573604346 43.8379609098684," +
                    "24.83885442747927 43.8379609098684," +
                    "24.83885442747927 44.795645304033826," +
                    "22.8042573604346 44.795645304033826," +
                    "22.8042573604346 43.8379609098684))");

            query.addParameter(CommonParameterNames.FOOTPRINT, aoi);

            query.addParameter(CommonParameterNames.CLOUD_COVER, 100.);
            query.setPageSize(50);
            query.setMaxResults(83);
            List<EOProduct> results = query.execute();
            results.forEach(r -> {
                System.out.println("ID=" + r.getId());
                System.out.println("NAME=" + r.getName());
                System.out.println("LOCATION=" + r.getLocation());
                System.out.println("FOOTPRINT=" + r.getGeometry());
                System.out.println("Attributes ->");
                r.getAttributes()
                        .forEach(a -> System.out.println("\tName='" + a.getName() +
                                "', value='" + a.getValue() + "'"));
            });
            DownloadStrategy strategy = (DownloadStrategy) dataSource.getProductFetchStrategy(sensors[0]);
            strategy.setFetchMode(FetchMode.OVERWRITE);
            strategy.fetch(results.get(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void SciHub_Sentinel1_Test() {
        try {
            Logger logger = LogManager.getLogManager().getLogger("");
            for (Handler handler : logger.getHandlers()) {
                handler.setLevel(Level.INFO);
            }
            DataSource dataSource = getDatasourceRegistry().getService(SciHubDataSource.class.getName());
            //new SciHubDataSource();
            dataSource.setCredentials("kraftek", "cei7samurai");
            String[] sensors = dataSource.getSupportedSensors();

            DataQuery query = dataSource.createQuery(sensors[0]);
            query.addParameter(CommonParameterNames.PLATFORM, "Sentinel-1");
            QueryParameter<Date> begin = query.createParameter(CommonParameterNames.START_DATE, Date.class);
            begin.setMinValue(Date.from(LocalDateTime.of(2017, 5, 30, 0, 0, 0, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));
            begin.setMaxValue(Date.from(LocalDateTime.of(2017, 6, 1, 0, 0, 0, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()));
            query.addParameter(begin);
            query.addParameter(CommonParameterNames.POLARISATION, "VV");
            query.addParameter("sensorOperationalMode", "IW");
            query.addParameter(CommonParameterNames.PRODUCT_TYPE, "SLC");
            query.setPageSize(50);
            query.setMaxResults(83);
            //SentinelDownloadStrategy downloader = new SentinelDownloadStrategy("E:\\NewFormat");
            List<EOProduct> results = query.execute();
            //downloader.download(results);
            results.forEach(r -> {
                System.out.println("ID=" + r.getId());
                System.out.println("NAME=" + r.getName());
                System.out.println("LOCATION=" + r.getLocation());
                System.out.println("FOOTPRINT=" + r.getGeometry());
                System.out.println("Attributes ->");
                r.getAttributes()
                        .forEach(a -> System.out.println("\tName='" + a.getName() +
                                "', value='" + a.getValue() + "'"));
            });
        } catch (QueryException e) {
            e.printStackTrace();
        }
    }

    private static ServiceRegistry<DataSource> getDatasourceRegistry() {
        return ServiceRegistryManager.getInstance().getServiceRegistry(DataSource.class);
    }
}
