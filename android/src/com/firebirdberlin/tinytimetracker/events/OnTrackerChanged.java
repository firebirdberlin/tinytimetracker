package com.firebirdberlin.tinytimetracker.events;

import com.firebirdberlin.tinytimetracker.models.TrackerEntry;

public class OnTrackerChanged {

  public TrackerEntry tracker;

  public OnTrackerChanged(TrackerEntry tracker) {
      this.tracker = tracker;
  }
}
