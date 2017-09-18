package com.easyyang.dbtest.inter;

import com.easyyang.dbtest.entity.DataSourceDescriptor;

import javax.sql.DataSource;

public interface IHADataSourceCreator {
    DataSource createHADataSource(DataSourceDescriptor paramDataSourceDescriptor) throws Exception;
}