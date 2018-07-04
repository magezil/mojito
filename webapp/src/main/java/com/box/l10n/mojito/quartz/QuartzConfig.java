package com.box.l10n.mojito.quartz;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@ConfigurationProperties("l10n.org")
public class QuartzConfig {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    DataSource dataSource;

    @Autowired
    QuartzPropertiesConfig quartzPropertiesConfig;

    @Autowired
    Trigger[] triggers;

    @Autowired
    JobDetail[] jobDetails;

    @Autowired
    Scheduler scheduler;

    /**
     * Creates the scheduler with triggers/jobs defined in spring beans.
     *
     * The spring beans should use the default group so that it is easy to keep track of new or removed triggers/jobs.
     *
     * In {@link #startScheduler()} triggers/jobs present in Quartz but without a matching spring bean will be
     * removed.
     *
     * Other job and trigger created dynamically must not used the default group else they'll be removed.
     *
     * @return
     * @throws SchedulerException
     */
    @Bean
    public SchedulerFactoryBean scheduler() throws SchedulerException {

        SchedulerFactoryBean schedulerFactory = new SchedulerFactoryBean();
        schedulerFactory.setDataSource(dataSource);
        schedulerFactory.setQuartzProperties(quartzPropertiesConfig.getQuartzProperties());
        schedulerFactory.setJobFactory(springBeanJobFactory());
        schedulerFactory.setOverwriteExistingJobs(true);
        schedulerFactory.setTriggers(triggers);
        schedulerFactory.setAutoStartup(false);

        return schedulerFactory;
    }

    /**
     * Starts the scheduler after remove outdated trigger/jobs
     * @throws SchedulerException
     */
    @PostConstruct
    void startScheduler() throws SchedulerException {
        scheduler.unscheduleJobs(new ArrayList<TriggerKey>(getOutdatedTriggerKeys()));
        scheduler.deleteJobs(new ArrayList<JobKey>(getOutdatedJobKeys()));
        scheduler.startDelayed(2);
    }

    Set<JobKey> getOutdatedJobKeys() throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(Scheduler.DEFAULT_GROUP));

        Set<JobKey> newJobKeys = new HashSet<>();

        for (JobDetail jobDetail : jobDetails) {
            newJobKeys.add(jobDetail.getKey());
        }

        jobKeys.removeAll(newJobKeys);

        return jobKeys;
    }

    Set<TriggerKey> getOutdatedTriggerKeys() throws SchedulerException {

        Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(Scheduler.DEFAULT_GROUP));
        Set<TriggerKey> newTriggerKeys = new HashSet<>();

        for (Trigger trigger : triggers) {
            newTriggerKeys.add(trigger.getKey());
        }

        triggerKeys.removeAll(newTriggerKeys);

        return triggerKeys;
    }

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        AutoWiringSpringBeanJobFactory jobFactory = new AutoWiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }
}