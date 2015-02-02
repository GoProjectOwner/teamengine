package com.occamlab.te.spi.jaxrs;

import com.occamlab.te.spi.jaxrs.resources.ImageResource;
import com.occamlab.te.spi.jaxrs.resources.StoreRunResources;
import com.occamlab.te.spi.jaxrs.resources.TestFinalResult;
import com.occamlab.te.spi.jaxrs.resources.TestGetMapResources;
import com.occamlab.te.spi.jaxrs.resources.TestRunResource;
import com.occamlab.te.spi.jaxrs.resources.TestSuiteOverviewResource;
import com.occamlab.te.spi.jaxrs.resources.TestSuiteSetResource;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

/**
 * Designates the components of the application in a portable manner. This
 * information is used to configure a JAX-RS runtime environment (such as a
 * hosting container).
 * 
 * @see javax.ws.rs.core.Application
 */
public class ApplicationComponents extends Application {

    /**
     * Identifies the set of root resource and provider classes that implement
     * the service API.
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> appClasses = new HashSet<Class<?>>();
        appClasses.add(TestSuiteSetResource.class);
        appClasses.add(TestSuiteOverviewResource.class);
        appClasses.add(TestRunResource.class);
        appClasses.add(ImageResource.class);
        appClasses.add(StoreRunResources.class);
        appClasses.add(TestGetMapResources.class);
        appClasses.add(TestFinalResult.class);
        return appClasses;
    }
}
