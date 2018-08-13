package com.pcf.readlog.service;

import org.cloudfoundry.operations.applications.ApplicationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class EventScheduler {

    @Autowired
    PcfLogService service;

    public HashMap<Long, List<ApplicationEvent>> eventMap = new HashMap();


    @Scheduled(fixedDelayString = "${appEvent.fixedDelay}")
    public void appEventScheduler() {
        System.out.println("appEventScheduler STARTED");
        List<ApplicationEvent> list = service.getFilteredEventsByName();

        if (eventMap.size() > 20) {
            eventMap.clear();
        }

        if (list != null && list.size() > 0) {
            eventMap.put(System.currentTimeMillis(), list);
        }

        System.out.println(list);
        System.out.println("appEventScheduler END");

    }


}
