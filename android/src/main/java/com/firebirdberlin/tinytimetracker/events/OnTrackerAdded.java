package com.firebirdberlin.tinytimetracker.events;

import com.firebirdberlin.tinytimetracker.models.TrackerEntry;

public class OnTrackerAdded {

  public TrackerEntry tracker;

  public OnTrackerAdded(TrackerEntry newTracker) {
      this.tracker = newTracker;
  }
}
