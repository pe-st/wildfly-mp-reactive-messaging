/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.examples.blog.mp.reactive_messaging.rhosak;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@ApplicationScoped
public class MessagingBean {

    @Inject
    @Channel("to-kafka")
    private Emitter<String> emitter;

    private LinkedList<String> recentlyReceived = new LinkedList<>();

    public MessagingBean() {
        //Needed for CDI spec compliance
        //The @Inject annotated one will be used
    }

    public Response send(String value) {
        System.out.println("Sending " + value);
        emitter.send(value);
        return Response.accepted().build();
    }

    @Incoming("from-kafka")
    public void receive(String value) {
        System.out.println("Received: " + value);
        synchronized (recentlyReceived) {
            if (recentlyReceived.size() > 3) {
                recentlyReceived.removeFirst();
            }
            recentlyReceived.add(value);
        }
    }

    public List<String> getRecent() {
        synchronized (recentlyReceived) {
            return new ArrayList<>(recentlyReceived);
        }
    }
}
