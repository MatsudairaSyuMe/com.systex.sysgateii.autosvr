package com.systex.sysgateii.autosvr.util;

import java.sql.SQLException;
import java.util.Arrays;

import com.systex.sysgateii.autosvr.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.autosvr.dao.GwDao;

public class TWCategory {
	
    private static String[] twCategories; 
    static {
        initializeCategories();
    }

    private static void initializeCategories() {
        GwDao jsel2ins = null;
        try {
            jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
            twCategories = jsel2ins.selectTWPBCategory("PB.APNO");
            System.out.println("Categories: " + Arrays.toString(twCategories));
        } catch (SQLException e) {
            System.err.println("Error categories from database");
           //20240503 MAtsudairaSyuMe mark for System Information Leak  e.printStackTrace();
        } catch (Exception e) {
            //20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
        }
    }

    public static String[] getTwCategories() {
        return twCategories;
    }
}
