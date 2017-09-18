package com.easyyang.dbtest.handle;

import com.easyyang.dbtest.entity.DataSourceDescriptor;
import com.easyyang.dbtest.inter.IHADataSourceCreator;

import javax.sql.DataSource;

public class NonHADataSourceCreator implements IHADataSourceCreator {
    public DataSource createHADataSource(DataSourceDescriptor descriptor) throws Exception {
        return descriptor.getTargetDataSource();
    }
}