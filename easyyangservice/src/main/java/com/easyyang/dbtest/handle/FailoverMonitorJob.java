package com.easyyang.dbtest.handle;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.target.HotSwappableTargetSource;

public class FailoverMonitorJob
  implements Runnable
{
  private final transient Logger logger = LoggerFactory.getLogger(FailoverMonitorJob.class);
  private String detectingSQL;
  private long detectingRequestTimeout;
  private long recheckInterval;
  private int recheckTimes;
  private HotSwappableTargetSource hotSwapTargetSource;
  private DataSource masterDataSource;
  private DataSource standbyDataSource;
  private DataSource masterDetectorDataSource;
  private DataSource standbyDetectorDataSource;
  private DataSource currentDetectorDataSource;
  private ExecutorService executor;
  
  public FailoverMonitorJob(ExecutorService es)
  {
    Validate.notNull(es);
    this.executor = es;
  }
  
  public void run()
  {
    Future<Integer> future = this.executor.submit(new Callable()
    {
      /* Error */
      public Integer call()
        throws java.lang.Exception
      {
        return null;
      }
    });
    try
    {
      Integer result = (Integer)future.get(getDetectingRequestTimeout(), 
        TimeUnit.MILLISECONDS);
      if (result.intValue() == -1) {
        doSwap();
      }
    }
    catch (InterruptedException e)
    {
      this.logger.warn("interrupted when getting query result in FailoverMonitorJob.");
    }
    catch (ExecutionException e)
    {
      this.logger.warn("exception occured when checking failover status in FailoverMonitorJob");
    }
    catch (TimeoutException e)
    {
      this.logger.warn("exceed DetectingRequestTimeout threshold. Switch to standby data source.");
      doSwap();
    }
  }
  
  private void doSwap()
  {
    synchronized (this.hotSwapTargetSource)
    {
      DataSource target = (DataSource)getHotSwapTargetSource()
        .getTarget();
      if (target == this.masterDataSource)
      {
        getHotSwapTargetSource().swap(this.standbyDataSource);
        this.currentDetectorDataSource = this.standbyDetectorDataSource;
      }
      else
      {
        getHotSwapTargetSource().swap(this.masterDataSource);
        this.currentDetectorDataSource = this.masterDetectorDataSource;
      }
    }
  }
  
  public String getDetectingSQL()
  {
    return this.detectingSQL;
  }
  
  public void setDetectingSQL(String detectingSQL)
  {
    this.detectingSQL = detectingSQL;
  }
  
  public long getDetectingRequestTimeout()
  {
    return this.detectingRequestTimeout;
  }
  
  public void setDetectingRequestTimeout(long detectingRequestTimeout)
  {
    this.detectingRequestTimeout = detectingRequestTimeout;
  }
  
  public HotSwappableTargetSource getHotSwapTargetSource()
  {
    return this.hotSwapTargetSource;
  }
  
  public void setHotSwapTargetSource(HotSwappableTargetSource hotSwapTargetSource)
  {
    this.hotSwapTargetSource = hotSwapTargetSource;
  }
  
  public DataSource getMasterDataSource()
  {
    return this.masterDataSource;
  }
  
  public void setMasterDataSource(DataSource masterDataSource)
  {
    this.masterDataSource = masterDataSource;
  }
  
  public DataSource getStandbyDataSource()
  {
    return this.standbyDataSource;
  }
  
  public void setStandbyDataSource(DataSource standbyDataSource)
  {
    this.standbyDataSource = standbyDataSource;
  }
  
  public void setRecheckInterval(long recheckInterval)
  {
    this.recheckInterval = recheckInterval;
  }
  
  public long getRecheckInterval()
  {
    return this.recheckInterval;
  }
  
  public void setRecheckTimes(int recheckTimes)
  {
    this.recheckTimes = recheckTimes;
  }
  
  public int getRecheckTimes()
  {
    return this.recheckTimes;
  }
  
  public void setMasterDetectorDataSource(DataSource masterDetectorDataSource)
  {
    this.masterDetectorDataSource = masterDetectorDataSource;
  }
  
  public DataSource getMasterDetectorDataSource()
  {
    return this.masterDetectorDataSource;
  }
  
  public void setStandbyDetectorDataSource(DataSource standbyDetectorDataSource)
  {
    this.standbyDetectorDataSource = standbyDetectorDataSource;
  }
  
  public DataSource getStandbyDetectorDataSource()
  {
    return this.standbyDetectorDataSource;
  }
  
  public void setCurrentDetectorDataSource(DataSource currentDetectorDataSource)
  {
    this.currentDetectorDataSource = currentDetectorDataSource;
  }
  
  public DataSource getCurrentDetectorDataSource()
  {
    return this.currentDetectorDataSource;
  }
}
