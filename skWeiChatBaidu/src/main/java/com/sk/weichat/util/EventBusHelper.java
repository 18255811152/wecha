package com.sk.weichat.util;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.v7.app.AppCompatActivity;

import de.greenrobot.event.EventBus;

public class EventBusHelper {
    private EventBusHelper() {
    }

    public static void register(LifecycleOwner activity) {
        activity.getLifecycle().addObserver(new LifecycleObserver() {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            void create() {
                EventBus.getDefault().register(activity);
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            void destroy() {
                EventBus.getDefault().unregister(activity);
            }
        });
    }
}
