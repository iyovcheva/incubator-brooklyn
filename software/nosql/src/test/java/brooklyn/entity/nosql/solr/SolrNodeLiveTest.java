/*
 * Copyright 2012-2013 by Cloudsoft Corp.
 */
package brooklyn.entity.nosql.solr;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import brooklyn.entity.proxying.EntitySpec;
import brooklyn.test.EntityTestUtils;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.text.Strings;

import com.google.common.collect.ImmutableList;

/**
 * Solr live tests.
 *
 * Test the operation of the {@link SolrNode} class using the jclouds {@code rackspace-cloudservers-uk}
 * and {@code aws-ec2} providers, with different OS images. The tests use the {@link SolrJSupport} class
 * to exercise the node, and will need to have {@code brooklyn.jclouds.provider.identity} and {@code .credential}
 * set, usually in the {@code .brooklyn/bropoklyn.properties} file.
 */
public class SolrNodeLiveTest extends AbstractSolrNodeTest {

    private static final Logger log = LoggerFactory.getLogger(SolrNodeLiveTest.class);

    @DataProvider(name = "virtualMachineData")
    public Object[][] provideVirtualMachineData() {
        return new Object[][] { // ImageName, Provider, Region
            new Object[] { "ubuntu", "aws-ec2", "eu-west-1" },
            new Object[] { "Ubuntu 12.0", "rackspace-cloudservers-uk", "" },
            new Object[] { "CentOS 6.2", "rackspace-cloudservers-uk", "" },
        };
    }

    @Test(groups = "Live", dataProvider = "virtualMachineData")
    protected void testOperatingSystemProvider(String imageName, String provider, String region) throws Exception {
        log.info("Testing Solr on {}{} using {}", new Object[] { provider, Strings.isNonEmpty(region) ? ":" + region : "", imageName });

        Map<String, String> properties = MutableMap.of("image-name-matches", imageName);
        testLocation = app.getManagementContext().getLocationRegistry()
                .resolve(provider + (Strings.isNonEmpty(region) ? ":" + region : ""), properties);

        solr = app.createAndManageChild(EntitySpec.create(SolrNode.class)
                .configure("solrPort", "9876+"));
        app.start(ImmutableList.of(testLocation));
        EntityTestUtils.assertAttributeEqualsEventually(solr, SolrNode.SERVICE_UP, true);
    }
}
