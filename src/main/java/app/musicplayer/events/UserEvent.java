/*
 * JavaFX Music Player. The MIT License (MIT).
 * Copyright (c) Almas Baim.
 * Copyright (c) Gerardo Prada, Michael Martin.
 * See LICENSE for details.
 */

package app.musicplayer.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Almas Baim (https://github.com/AlmasB)
 */
public class UserEvent extends Event {

    public static final EventType<UserEvent> ANY = new EventType<>(Event.ANY, "USER_EVENT_ANY");
    public static final EventType<UserEvent> CLICK_IMPORT = new EventType<>(ANY, "CLICK_IMPORT");

    public UserEvent(EventType<? extends UserEvent> eventType) {
        super(eventType);
    }
}
