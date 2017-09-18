package com.easyyang.dbtest.handle;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.SQLException;

public class PassiveEventHotSwappableAdvice
  implements MethodInterceptor, InitializingBean
{
  private final transient Logger logger = LoggerFactory.getLogger(PassiveEventHotSwappableAdvice.class);
  private static final Integer DEFAULT_RETRY_TIMES = Integer.valueOf(3);
  private SQLStateSQLExceptionTranslator sqlExTranslator = new SQLStateSQLExceptionTranslator();
  private Integer swapTimesThreshold = Integer.valueOf(Integer.MAX_VALUE);
  private Integer retryTimes = DEFAULT_RETRY_TIMES;
  private long retryInterval = 1000L;
  private String detectingSql = "SELECT 1";
  private HotSwappableTargetSource targetSource;
  private DataSource mainDataSource;
  private DataSource standbyDataSource;
  
  public Object invoke(MethodInvocation invocation)
    throws Throwable
  {
    if (!StringUtils.equalsIgnoreCase(invocation.getMethod().getName(),
      "getConnection")) {
      return invocation.proceed();
    }
    try
    {
      return invocation.proceed();
    }
    catch (Throwable t)
    {
      if ((t instanceof SQLException))
      {
        DataAccessException dae = this.sqlExTranslator
          .translate(
          "translate to check whether it's a resource failure exception", 
          null, (SQLException)t);
        if ((dae instanceof DataAccessResourceFailureException))
        {
          this.logger.warn(
            "failed to get Connection from data source with exception:\n{}", 
            t);
          doSwap();
          return invocation.getMethod()
            .invoke(this.targetSource.getTarget(), 
            invocation.getArguments());
        }
      }
      throw t;
    }
  }
  
  private void doSwap()
  {
    synchronized (this.targetSource)
    {
      DataSource target = (DataSource)getTargetSource().getTarget();
      if (target == this.mainDataSource)
      {
        this.logger.warn("hot swap from '" + target + "' to '" + 
          this.standbyDataSource + "'.");
        getTargetSource().swap(this.standbyDataSource);
      }
      else
      {
        this.logger.warn("hot swap from '" + target + "' to '" + 
          this.mainDataSource + "'.");
        getTargetSource().swap(this.mainDataSource);
      }
    }
  }
  
  public Integer getSwapTimesThreshold()
  {
    return this.swapTimesThreshold;
  }
  
  public void setSwapTimesThreshold(Integer swapTimesThreshold)
  {
    this.swapTimesThreshold = swapTimesThreshold;
  }
  
  public HotSwappableTargetSource getTargetSource()
  {
    return this.targetSource;
  }
  
  public void setTargetSource(HotSwappableTargetSource targetSource)
  {
    this.targetSource = targetSource;
  }
  
  public void afterPropertiesSet()
    throws Exception
  {
    if ((this.targetSource == null) || (this.mainDataSource == null) || 
      (this.standbyDataSource == null)) {
      throw new IllegalArgumentException(
        "the target source, main data source and standby data source must be set.");
    }
  }
  
  public void setRetryTimes(Integer retryTimes)
  {
    this.retryTimes = retryTimes;
  }
  
  public Integer getRetryTimes()
  {
    return this.retryTimes;
  }
  
  public DataSource getMainDataSource()
  {
    return this.mainDataSource;
  }
  
  public void setMainDataSource(DataSource mainDataSource)
  {
    this.mainDataSource = mainDataSource;
  }
  
  public DataSource getStandbyDataSource()
  {
    return this.standbyDataSource;
  }
  
  public void setStandbyDataSource(DataSource standbyDataSource)
  {
    this.standbyDataSource = standbyDataSource;
  }
  
  public void setRetryInterval(long retryInterval)
  {
    this.retryInterval = retryInterval;
  }
  
  public long getRetryInterval()
  {
    return this.retryInterval;
  }
  
  public void setDetectingSql(String detectingSql)
  {
    this.detectingSql = detectingSql;
  }
  
  public String getDetectingSql()
  {
    return this.detectingSql;
  }
}
