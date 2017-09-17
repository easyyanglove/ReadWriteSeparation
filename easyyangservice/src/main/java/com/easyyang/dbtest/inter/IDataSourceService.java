package com.easyyang.dbtest.inter;

import com.easyyang.dbtest.entity.DataSourceDescriptor;

import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

public interface IDataSourceService {
    Map<String, DataSource> getDataSources();

    Set<DataSourceDescriptor> getDataSourceDescriptors();
}
