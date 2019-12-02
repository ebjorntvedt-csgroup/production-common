package esa.s1pdgs.cpoc.ipf.preparation.worker.utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJobGeneration;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJobGenerationState;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJobProduct;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJobState;
import esa.s1pdgs.cpoc.common.ApplicationLevel;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.routing.LevelProductsRoute;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.routing.LevelProductsRouteFrom;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.routing.LevelProductsRouteTo;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.routing.LevelProductsRouting;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductionEvent;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

public class TestL1Utils {

    public static LevelProductsRouting buildL1Routing() {
        final LevelProductsRouting r = new LevelProductsRouting();
        r.addRoute(new LevelProductsRoute(new LevelProductsRouteFrom("EN", "A"),
                new LevelProductsRouteTo(Arrays.asList("EN_RAW__0_GRDF_1.xml",
                        "EN_RAW__0_GRDH_1.xml", "EN_RAW__0_GRDM_1.xml",
                        "EN_RAW__0_SLC__1.xml", "EN_RAW__0_SLC__1_GRDF_1.xml",
                        "EN_RAW__0_SLC__1_GRDH_1.xml"))));
        r.addRoute(new LevelProductsRoute(new LevelProductsRouteFrom("EN", "B"),
                new LevelProductsRouteTo(Arrays.asList("EN_RAW__0_SLC__1.xml",
                        "EN_RAW__0_SLC__1_GRDF_1.xml",
                        "EN_RAW__0_SLC__1_GRDH_1.xml"))));
        r.addRoute(new LevelProductsRoute(new LevelProductsRouteFrom("EW", "A"),
                new LevelProductsRouteTo(Arrays.asList("EW_RAW__0_GRDH_1.xml",
                        "EW_RAW__0_GRDM_1.xml", "EW_RAW__0_SLC__1.xml",
                        "EW_RAW__0_SLC__1_GRDH_1.xml",
                        "EW_RAW__0_SLC__1_GRDM_1.xml"))));
        r.addRoute(new LevelProductsRoute(new LevelProductsRouteFrom("EW", "B"),
                new LevelProductsRouteTo(Arrays.asList("EW_RAW__0_SLC__1.xml",
                        "EW_RAW__0_SLC__1_GRDH_1.xml",
                        "EW_RAW__0_SLC__1_GRDM_1.xml"))));
        r.addRoute(new LevelProductsRoute(new LevelProductsRouteFrom("IW", "A"),
                new LevelProductsRouteTo(Arrays.asList("IW_RAW__0_GRDH_1.xml",
                        "IW_RAW__0_GRDM_1.xml", "IW_RAW__0_SLC__1.xml",
                        "IW_RAW__0_SLC__1_GRDH_1.xml",
                        "IW_RAW__0_SLC__1_GRDM_1.xml"))));
        r.addRoute(new LevelProductsRoute(new LevelProductsRouteFrom("IW", "B"),
                new LevelProductsRouteTo(Arrays.asList("IW_RAW__0_SLC__1.xml",
                        "IW_RAW__0_SLC__1_GRDH_1.xml",
                        "IW_RAW__0_SLC__1_GRDM_1.xml"))));
        r.addRoute(new LevelProductsRoute(new LevelProductsRouteFrom("S[1-6]", "A"),
                new LevelProductsRouteTo(Arrays.asList("SM_RAW__0_GRDF_1.xml",
                        "SM_RAW__0_GRDH_1.xml", "SM_RAW__0_GRDM_1.xml",
                        "SM_RAW__0_SLC__1.xml", "SM_RAW__0_SLC__1_GRDF_1.xml",
                        "SM_RAW__0_SLC__1_GRDH_1.xml"))));
        r.addRoute(new LevelProductsRoute(new LevelProductsRouteFrom("S[1-6]", "B"),
                new LevelProductsRouteTo(Arrays.asList("SM_RAW__0_SLC__1.xml",
                        "SM_RAW__0_SLC__1_GRDF_1.xml",
                        "SM_RAW__0_SLC__1_GRDH_1.xml"))));

        return r;
    }

    public static  AppDataJob buildJobGeneration(
            final boolean preSearchInfo) throws InternalErrorException {
        final AppDataJob ret = new AppDataJob();
        ret.setId(123);
        ret.setState(AppDataJobState.GENERATING);
        ret.setPod("hostname");
        ret.setLevel(ApplicationLevel.L0);

        final GenericMessageDto<ProductionEvent> message1 =
                new GenericMessageDto<ProductionEvent>(1, "input-key",
                        new ProductionEvent(
                                "S1A_IW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE",
                                "S1A_IW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE",
                                ProductFamily.L0_ACN, "NRT"));
        ret.setMessages(Arrays.asList(message1));

        final Calendar start1 = Calendar.getInstance();
        start1.set(2017, Calendar.DECEMBER, 13, 14, 59, 48);
        final Calendar stop1 = Calendar.getInstance();
        stop1.set(2017, Calendar.DECEMBER, 13, 15, 17, 25);
        final AppDataJobProduct product = new AppDataJobProduct();
        product.setMissionId("S1");
        product.setProductName(
                "S1A_IW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE");
        product.setSatelliteId("A");
        product.setStartTime("2017-12-13T12:16:23.000000Z");
        product.setStopTime("2017-12-13T12:16:56.000000Z");
        product.setAcquisition("IW");
        product.setProcessMode("NRT");
        if (preSearchInfo) {
            product.setProductType("IW_RAW__0S");
            product.setDataTakeId("021735");
            product.setInsConfId(6);
            product.setNumberSlice(3);
            product.setTotalNbOfSlice(10);
            product.setSegmentStartDate("2017-12-13T12:16:23.224083Z");
            product.setSegmentStopDate("2017-12-13T12:16:56.224083Z");
        }
        ret.setProduct(product);

        final AppDataJobGeneration gen1 = new AppDataJobGeneration();
        gen1.setTaskTable("IW_RAW__0_GRDH_1.xml");
        gen1.setState(AppDataJobGenerationState.INITIAL);
        gen1.setCreationDate(new Date(0L));
        final AppDataJobGeneration gen2 = new AppDataJobGeneration();
        gen2.setTaskTable("IW_RAW__0_GRDM_1.xml");
        gen2.setState(AppDataJobGenerationState.READY);
        gen2.setCreationDate(new Date(0L));
        final AppDataJobGeneration gen3 = new AppDataJobGeneration();
        gen3.setTaskTable("IW_RAW__0_SLC__1.xml");
        gen3.setState(AppDataJobGenerationState.PRIMARY_CHECK);
        gen3.setCreationDate(new Date(0L));
        final AppDataJobGeneration gen4 = new AppDataJobGeneration();
        gen4.setTaskTable("IW_RAW__0_SLC__1_GRDH_1.xml");
        gen4.setState(AppDataJobGenerationState.SENT);
        gen4.setCreationDate(new Date(0L));
        ret.setGenerations(Arrays.asList(gen1, gen2, gen3, gen4));

        return ret;
    }
}
