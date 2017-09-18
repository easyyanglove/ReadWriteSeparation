package com.easyyang.dbtest.config;

import com.easyyang.dbtest.entity.DataSourceDescriptor;
import com.easyyang.dbtest.handle.NonHADataSourceCreator;
import com.easyyang.dbtest.inter.IDataSourceService;
import com.easyyang.dbtest.inter.IHADataSourceCreator;
import com.easyyang.dbtest.utils.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.*;

/**
 * 多数据源配置
 */
public class MultipleDataSource implements IDataSourceService, InitializingBean {

    private Set<DataSourceDescriptor> dataSourceDescriptors = new HashSet();
    private IHADataSourceCreator haDataSourceCreator;
    private Map<String, DataSource> dataSources = new HashMap();

    public Map<String, DataSource> getDataSources() {
        return this.dataSources;
    }

    public void afterPropertiesSet() throws Exception {
        if (getHaDataSourceCreator() == null) {
            setHaDataSourceCreator(new NonHADataSourceCreator());
        }
        if (CollectionUtils.isEmpty(this.dataSourceDescriptors)) {
            return;
        }

        for (DataSourceDescriptor descriptor : getDataSourceDescriptors()) {
            Validate.notEmpty(descriptor.getIdentity());
            Validate.notNull(descriptor.getTargetDataSource());

            DataSource dataSourceToUse = descriptor.getTargetDataSource();
            if (descriptor.getStandbyDataSource() != null) {
                dataSourceToUse = getHaDataSourceCreator().createHADataSource(descriptor);
            }
            this.dataSources.put(descriptor.getIdentity(), new LazyConnectionDataSourceProxy(dataSourceToUse));
        }
    }

    public void setDataSourceDescriptors(Set<DataSourceDescriptor> dataSourceDescriptors) {
        this.dataSourceDescriptors = dataSourceDescriptors;
    }

    public Set<DataSourceDescriptor> getDataSourceDescriptors() {
        return this.dataSourceDescriptors;
    }

    public void setHaDataSourceCreator(IHADataSourceCreator haDataSourceCreator) {
        this.haDataSourceCreator = haDataSourceCreator;
    }

    public IHADataSourceCreator getHaDataSourceCreator() {
        return this.haDataSourceCreator;
    }
}