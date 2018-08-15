package com.pcf.readlog.service;

import com.pcf.readlog.AppConfiguration;
import com.pcf.readlog.model.PcfAppDetails;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationEvent;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.GetApplicationEventsRequest;
import org.cloudfoundry.operations.applications.LogsRequest;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

@Service
public class PcfLogService {

    private static final Logger logger = LoggerFactory.getLogger(PcfLogService.class);

    @Autowired
    private AppConfiguration appConfiguration;
    private DefaultCloudFoundryOperations defaultCloudFoundryOperations;

    private List<String> appNameList = new ArrayList<>();
    private List<String> appStatusList = new ArrayList<>();

    @Value("${appEvent.fixedDelay}")
    private String fixeddelay;

    @PostConstruct
    public void initDefaultCloudFoundryOperations() {
        logger.info("----------- : initDefaultCloudFoundryOperations() : START");

        DefaultConnectionContext connectionContext = DefaultConnectionContext.builder()
                .apiHost(appConfiguration.getTarget()).build();

        PasswordGrantTokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
                .password(appConfiguration.getPassword()).username(appConfiguration.getUser()).build();

        ReactorCloudFoundryClient cfClient = ReactorCloudFoundryClient.builder().connectionContext(connectionContext)
                .tokenProvider(tokenProvider).build();

        ReactorDopplerClient reactorDopplerClient = ReactorDopplerClient.builder().connectionContext(connectionContext)
                .tokenProvider(tokenProvider).build();

        this.defaultCloudFoundryOperations = DefaultCloudFoundryOperations.builder().cloudFoundryClient(cfClient)
                .organization(appConfiguration.getOrganisation()).space(appConfiguration.getSpace())
                .dopplerClient(reactorDopplerClient).build();
        logger.info("-----------  : initDefaultCloudFoundryOperations() : END");
    }


    public void initApplicationNames() {
        logger.info("-----------  : initApplicationNames() : START");
        CountDownLatch latch = new CountDownLatch(1);
        this.defaultCloudFoundryOperations.applications().list().map(ApplicationSummary::getName).subscribe(name -> {
            logger.info("-----------  : Application Name  : " + name.toString());
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            //e.printStackTrace();
            logger.error("-----------  : initApplicationNames() : " + e.toString());
        }
        logger.info("-----------  : initApplicationNames() : END");
    }

    @PostConstruct
    public void initApplicationDetails() {
        logger.info("-----------  : initApplicationNamesAndStatus() : START");

        CountDownLatch latch = new CountDownLatch(2);

        this.defaultCloudFoundryOperations.applications().list().map(ApplicationSummary::getName).subscribe(name -> {
            logger.info("-----------  : Application Name  : " + name.toString());
            this.appNameList.add(name.toString());
            latch.countDown();
        });

        this.defaultCloudFoundryOperations.applications().list().map(ApplicationSummary::getRequestedState)
                .subscribe(requestedState -> {
                    logger.info("-----------  : requestedState  : " + requestedState.toString());
                    this.appStatusList.add(requestedState.toString());
                    latch.countDown();
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            //e.printStackTrace();
            logger.error("-----------  : initApplicationNamesAndStatus() : " + e.toString());
        }
        logger.info("-----------  : initApplicationNamesAndStatus() : END");
    }

    public Map<String, PcfAppDetails> getApplicationDetails() {
        logger.info("-----------  : getApplicationDetails() : START");
        Map<String, PcfAppDetails> applicationDetailsMap = new HashMap<>();
        for (int i = 0; i < appNameList.size(); i++) {
            PcfAppDetails pcfAppDetails = new PcfAppDetails();
            logger.info("----------- " + appNameList.get(i) + " " + appStatusList.get(i));
            pcfAppDetails.setApplicationName(appNameList.get(i));
            pcfAppDetails.setStatus(appStatusList.get(i));
            applicationDetailsMap.put("app" + i, pcfAppDetails);
        }
        logger.info("----------- " + " appNameList.size() : " + appNameList.size() + " appNameList.size() : " + appNameList.size());
        logger.info("-----------  : geApplicationNames() size : " + applicationDetailsMap.size());
        logger.info("-----------  : getApplicationDetails() : END");
        return applicationDetailsMap;
    }

    public String getLogMessage(String appName) {
        logger.info("-----------  : getLogMessage() : START");

        CountDownLatch latch = new CountDownLatch(1);
        List<String> LogList = new CopyOnWriteArrayList<>();
        // read log
        this.defaultCloudFoundryOperations.applications()
                .logs(LogsRequest.builder().name(appName).recent(Boolean.TRUE).build()).subscribe(output -> {

            if (LogList.size() <= 25) {
                LogList.add(output.toString() + "\n\n");
                latch.countDown();
            }

        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("-----------  : getLogMessage() : END");

        return LogList.toString();
    }

    public List<String> getApplicationNames_Lazy() {
        logger.info("-----------  : getApplicationNames_Lazy() : START");

        DefaultConnectionContext connectionContext = DefaultConnectionContext.builder()
                .apiHost(appConfiguration.getTarget()).build();

        PasswordGrantTokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
                .password(appConfiguration.getPassword()).username(appConfiguration.getUser()).build();

        ReactorCloudFoundryClient cfClient = ReactorCloudFoundryClient.builder().connectionContext(connectionContext)
                .tokenProvider(tokenProvider).build();

        ReactorDopplerClient reactorDopplerClient = ReactorDopplerClient.builder().connectionContext(connectionContext)
                .tokenProvider(tokenProvider).build();

        DefaultCloudFoundryOperations cfOperations = DefaultCloudFoundryOperations.builder()
                .cloudFoundryClient(cfClient).organization(appConfiguration.getOrganisation())
                .space(appConfiguration.getSpace()).dopplerClient(reactorDopplerClient).build();

        CountDownLatch latch = new CountDownLatch(1);
        List<String> listApplications = new ArrayList<>();

        cfOperations.applications().list().map(ApplicationSummary::getName).subscribe(name -> {
            // System.out.println(name);
            listApplications.add(name.toString());
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            //e.printStackTrace();
            logger.error("-----------  : getApplicationNames_Lazy() : " + e.toString());
        }
        logger.info("-----------  : getApplicationNames_Lazy() : END");

        return listApplications;
    }


    public List<ApplicationEvent> getFilteredEventsByName() {

        initApplicationDetails();


        List<ApplicationEvent> events = new ArrayList();

        if (appNameList != null && appNameList.size() > 0) {
            CountDownLatch latch = new CountDownLatch(1);
            Flux<ApplicationEvent> appEvent = defaultCloudFoundryOperations.applications()
                    .getEvents(GetApplicationEventsRequest.builder().name(appNameList.get(0)).maxNumberOfEvents(10).build());
            appEvent.subscribe(applicationEvent -> {
                long lastTimeStamp = System.currentTimeMillis() - Integer.parseInt(fixeddelay);
                if (applicationEvent.getTime().getTime() > lastTimeStamp) {
                    events.add(applicationEvent);
                }
                latch.countDown();
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("EVENT:::::::" + events);
        return events;
    }

}
