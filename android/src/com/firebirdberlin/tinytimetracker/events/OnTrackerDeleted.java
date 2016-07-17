package com.firebirdberlin.tinytimetracker.events;

import com.firebirdberlin.tinytimetracker.models.TrackerEntry;

public class OnTrackerDeleted {

  public TrackerEntry tracker;

  public OnTrackerDeleted(TrackerEntry newTracker) {
      this.tracker = newTracker;
  }
}
