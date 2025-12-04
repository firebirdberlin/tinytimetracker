package com.firebirdberlin.tinytimetracker.events;

public class OnLogEntryDeleted {

  public long id;
  public long tracker_id;

  public OnLogEntryDeleted(long tracker_id, long id) {
      this.id = id;
      this.tracker_id = tracker_id;
  }
}
