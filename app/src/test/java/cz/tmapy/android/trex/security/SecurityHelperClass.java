package cz.tmapy.android.trex.security;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by Kamil Svoboda on 15.3.2016.
 */
public class SecurityHelperClass {

    @Test
    public void testGetMd5HashTest() throws Exception {
        assertNotNull(SecurityHelper.GetMd5Hash(""));
    }

    @Test
    public void testGetMd5ForLocationTest() throws Exception {
        //tento test neprojde, protože používá Android knihovnu MD5 a ta se v testu nedá inicializovat
        assertNotNull(SecurityHelper.GetMd5ForLocation("deviceId", new Date( ), "securityString"));
    }
}
