package io.pivotal;

import javax.sql.DataSource;

import io.pivotal.domain.Customer;
import io.pivotal.spring.cloud.service.gemfire.GemfireServiceConnectorConfig;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.ServiceConnectorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.gemfire.support.GemfireCacheManager;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@EnableCaching
public class DemoConfig extends AbstractCloudConfig {
	
	@Bean
	public DataSource dataSource() {
		DataSource dataSource = connectionFactory().dataSource();
		
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.addScript(new ClassPathResource("sql/create_table.sql"));
        
        DatabasePopulatorUtils.execute(databasePopulator, dataSource);
        
        return dataSource;
	}
	
	public ServiceConnectorConfig createGemfireConnectorConfig() {

        GemfireServiceConnectorConfig gemfireConfig = new GemfireServiceConnectorConfig();
        gemfireConfig.setPoolSubscriptionEnabled(true);
        gemfireConfig.setPdxSerializer(new ReflectionBasedAutoSerializer(".*"));
        gemfireConfig.setPdxReadSerialized(false);

        return gemfireConfig;
    }
    
	@Bean(name = "gemfireCache")
    public ClientCache getGemfireClientCache() throws Exception {		
		
		Cloud cloud = new CloudFactory().getCloud();
		ClientCache clientCache = cloud.getSingletonServiceConnector(ClientCache.class,  createGemfireConnectorConfig());

        return clientCache;
    }


	@Bean(name = "customer")
	public Region<String, Customer> customerRegion(@Autowired ClientCache clientCache) {
		ClientRegionFactory<String, Customer> customerRegionFactory = clientCache.createClientRegionFactory(ClientRegionShortcut.PROXY);

		Region<String, Customer> customerRegion = customerRegionFactory.create("customer");

		return customerRegion;
	}
	
	@Bean(name="cacheManager")
	public GemfireCacheManager createGemfireCacheManager(@Autowired ClientCache gemfireCache) {

		GemfireCacheManager gemfireCacheManager = new GemfireCacheManager();
		gemfireCacheManager.setCache(gemfireCache);

		return gemfireCacheManager;
	}

}
