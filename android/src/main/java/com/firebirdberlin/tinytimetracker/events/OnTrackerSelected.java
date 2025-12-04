package com.firebirdberlin.tinytimetracker.events;

import com.firebirdberlin.tinytimetracker.models.TrackerEntry;

public class OnTrackerSelected {

  public TrackerEntry newTracker;

  public OnTrackerSelected(TrackerEntry newTracker) {
      this.newTracker = newTracker;
  }
}
