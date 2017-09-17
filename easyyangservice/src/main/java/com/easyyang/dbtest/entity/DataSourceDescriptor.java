package com.easyyang.dbtest.entity;

/**
 * Created by jiangpy on 14:41.
 */
import javax.sql.DataSource;

public class DataSourceDescriptor
{
    private String identity;
    private DataSource targetDataSource;
    private DataSource targetDetectorDataSource;
    private DataSource standbyDataSource;
    private DataSource standbyDetectorDataSource;
    private int poolSize = Runtime.getRuntime().availableProcessors() * 5;

    public String getIdentity()
    {
        return this.identity;
    }

    public void setIdentity(String identity)
    {
        this.identity = identity;
    }

    public DataSource getTargetDataSource()
    {
        return this.targetDataSource;
    }

    public void setTargetDataSource(DataSource targetDataSource)
    {
        this.targetDataSource = targetDataSource;
    }

    public DataSource getTargetDetectorDataSource()
    {
        return this.targetDetectorDataSource;
    }

    public void setTargetDetectorDataSource(DataSource targetDetectorDataSource)
    {
        this.targetDetectorDataSource = targetDetectorDataSource;
    }

    public DataSource getStandbyDataSource()
    {
        return this.standbyDataSource;
    }

    public void setStandbyDataSource(DataSource standbyDataSource)
    {
        this.standbyDataSource = standbyDataSource;
    }

    public DataSource getStandbyDetectorDataSource()
    {
        return this.standbyDetectorDataSource;
    }

    public void setStandbyDetectorDataSource(DataSource standbyDetectorDataSource)
    {
        this.standbyDetectorDataSource = standbyDetectorDataSource;
    }

    public void setPoolSize(int poolSize)
    {
        this.poolSize = poolSize;
    }

    public int getPoolSize()
    {
        return this.poolSize;
    }

    public int hashCode()
    {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.identity == null ? 0 : this.identity.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataSourceDescriptor other = (DataSourceDescriptor)obj;
        if (this.identity == null)
        {
            if (other.identity != null) {
                return false;
            }
        }
        else if (!this.identity.equals(other.identity)) {
            return false;
        }
        return true;
    }

    public String toString()
    {
        return

                "DataSourceDescriptor [identity=" + this.identity + ", poolSize=" + this.poolSize + ", standbyDataSource=" + this.standbyDataSource + ", standbyDetectorDataSource=" + this.standbyDetectorDataSource + ", targetDataSource=" + this.targetDataSource + ", targetDetectorDataSource=" + this.targetDetectorDataSource + "]";
    }
}
