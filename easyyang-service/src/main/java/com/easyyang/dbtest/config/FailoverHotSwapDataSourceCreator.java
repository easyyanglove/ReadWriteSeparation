package com.easyyang.dbtest.config;

import com.easyyang.dbtest.entity.DataSourceDescriptor;
import com.easyyang.dbtest.handle.FailoverMonitorJob;
import com.easyyang.dbtest.handle.PassiveEventHotSwappableAdvice;
import com.easyyang.dbtest.inter.IHADataSourceCreator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class FailoverHotSwapDataSourceCreator implements IHADataSourceCreator, InitializingBean, DisposableBean {
    private final transient Logger logger = LoggerFactory.getLogger(FailoverHotSwapDataSourceCreator.class);
    private boolean passiveFailoverEnable = false;
    private boolean positiveFailoverEnable = true;
    private ConcurrentMap<ScheduledFuture<?>, ScheduledExecutorService> schedulerFutures = new ConcurrentHashMap();
    private List<ExecutorService> jobExecutorRegistry = new ArrayList();
    private long monitorPeriod = 15000L;
    private int initialDelay = 0;
    private String detectingSql;
    private long detectingTimeoutThreshold = 15000L;
    private long recheckInterval = 5000L;
    private int recheckTimes = 3;

    public DataSource createHADataSource(DataSourceDescriptor descriptor) throws Exception {
        DataSource activeDataSource = descriptor.getTargetDataSource();
        DataSource standbyDataSource = descriptor.getStandbyDataSource();
        if ((activeDataSource == null) && (standbyDataSource == null)) {
            throw new IllegalArgumentException( "must have at least one data source active.");
        }

        if ((activeDataSource == null) || (standbyDataSource == null)) {
            this.logger.warn("only one data source is available for use, so no HA support.");
            if (activeDataSource == null) {
                return standbyDataSource;
            }
            return activeDataSource;
        }

        HotSwappableTargetSource targetSource = new HotSwappableTargetSource(activeDataSource);
        ProxyFactory pf = new ProxyFactory();
        pf.setInterfaces(new Class[]{DataSource.class});
        pf.setTargetSource(targetSource);
        if (isPositiveFailoverEnable()) {
            DataSource targetDetectorDataSource = descriptor.getTargetDetectorDataSource();
            DataSource standbyDetectorDataSource = descriptor.getStandbyDetectorDataSource();
            if ((targetDetectorDataSource == null) || (standbyDetectorDataSource == null)) {
                throw new IllegalArgumentException("targetDetectorDataSource or standbyDetectorDataSource can't be null if positive failover is enabled.");
            }
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            ExecutorService jobExecutor = Executors.newFixedThreadPool(1);
            this.jobExecutorRegistry.add(jobExecutor);

            FailoverMonitorJob job = new FailoverMonitorJob(jobExecutor);
            job.setHotSwapTargetSource(targetSource);
            job.setMasterDataSource(activeDataSource);
            job.setStandbyDataSource(standbyDataSource);
            job.setMasterDetectorDataSource(targetDetectorDataSource);
            job.setStandbyDetectorDataSource(standbyDetectorDataSource);
            job.setCurrentDetectorDataSource(targetDetectorDataSource);
            job.setDetectingRequestTimeout(getDetectingTimeoutThreshold());
            job.setDetectingSQL(getDetectingSql());
            job.setRecheckInterval(this.recheckInterval);
            job.setRecheckTimes(this.recheckTimes);

            ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(job, this.initialDelay, this.monitorPeriod, TimeUnit.MILLISECONDS);
            this.schedulerFutures.put(future, scheduler);
        }

        if (isPassiveFailoverEnable()) {
            PassiveEventHotSwappableAdvice advice = new PassiveEventHotSwappableAdvice();
            advice.setRetryInterval(this.recheckInterval);
            advice.setRetryTimes(Integer.valueOf(this.recheckTimes));
            advice.setDetectingSql(this.detectingSql);
            advice.setTargetSource(targetSource);
            advice.setMainDataSource(activeDataSource);
            advice.setStandbyDataSource(standbyDataSource);
            pf.addAdvice(advice);
        }
        return (DataSource) pf.getProxy();
    }

    public void afterPropertiesSet()
            throws Exception {
        if ((!isPassiveFailoverEnable()) && (!isPositiveFailoverEnable())) {
            return;
        }
        if (StringUtils.isEmpty(this.detectingSql)) {
            throw new IllegalArgumentException(
                    "A 'detectingSql' should be provided if positive failover function is enabled.");
        }
        if ((this.monitorPeriod <= 0L) || (this.detectingTimeoutThreshold <= 0L) ||
                (this.recheckInterval <= 0L) || (this.recheckTimes <= 0)) {
            throw new IllegalArgumentException(
                    "'monitorPeriod' OR 'detectingTimeoutThreshold' OR 'recheckInterval' OR 'recheckTimes' must be positive.");
        }
        if (isPositiveFailoverEnable()) {
            if (this.detectingTimeoutThreshold > this.monitorPeriod) {
                throw new IllegalArgumentException(
                        "the 'detectingTimeoutThreshold' should be less(or equals) than 'monitorPeriod'.");
            }
            if (this.recheckInterval * this.recheckTimes > this.detectingTimeoutThreshold) {
                throw new IllegalArgumentException(
                        " 'recheckInterval * recheckTimes' can not be longer than 'detectingTimeoutThreshold'");
            }
        }
    }

    public void destroy() throws Exception {
        Iterator localIterator = this.schedulerFutures.entrySet().iterator();
        while (localIterator.hasNext()) {
            Map.Entry<ScheduledFuture<?>, ScheduledExecutorService> e = (Map.Entry) localIterator.next();
            ScheduledFuture<?> future = (ScheduledFuture) e.getKey();
            ScheduledExecutorService scheduler = (ScheduledExecutorService) e.getValue();
            future.cancel(true);
            shutdownExecutor(scheduler);
        }
        for (ExecutorService executor : this.jobExecutorRegistry) {
            shutdownExecutor(executor);
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (Exception ex) {
            this.logger.warn("interrupted when shutting down executor service.");
        }
    }

    public void setMonitorPeriod(long monitorPeriod) {
        this.monitorPeriod = monitorPeriod;
    }

    public long getMonitorPeriod() {
        return this.monitorPeriod;
    }

    public void setInitialDelay(int initialDelay) {
        this.initialDelay = initialDelay;
    }

    public int getInitialDelay() {
        return this.initialDelay;
    }

    public void setPassiveFailoverEnable(boolean passiveFailoverEnable) {
        this.passiveFailoverEnable = passiveFailoverEnable;
    }

    public boolean isPassiveFailoverEnable() {
        return this.passiveFailoverEnable;
    }

    public void setPositiveFailoverEnable(boolean positiveFailoverEnable) {
        this.positiveFailoverEnable = positiveFailoverEnable;
    }

    public boolean isPositiveFailoverEnable() {
        return this.positiveFailoverEnable;
    }

    public void setDetectingSql(String detectingSql) {
        this.detectingSql = detectingSql;
    }

    public String getDetectingSql() {
        return this.detectingSql;
    }

    public void setDetectingTimeoutThreshold(long detectingTimeoutThreshold) {
        this.detectingTimeoutThreshold = detectingTimeoutThreshold;
    }

    public long getDetectingTimeoutThreshold() {
        return this.detectingTimeoutThreshold;
    }

    public void setRecheckInterval(long recheckInterval) {
        this.recheckInterval = recheckInterval;
    }

    public long getRecheckInterval() {
        return this.recheckInterval;
    }

    public void setRecheckTimes(int recheckTimes) {
        this.recheckTimes = recheckTimes;
    }

    public int getRecheckTimes() {
        return this.recheckTimes;
    }
}
