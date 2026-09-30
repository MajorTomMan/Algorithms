package com.majortom.algorithms.visualization.impl.visualizer.hash;

/** Stable ids shared by hash capture, layout and FX commit. */
public final class HashVisualIds {
  private static final String BUCKET = "hash:bucket:";
  private static final String ENTRY = "hash:entry:";
  private static final String LINK = "hash:link:";

  private HashVisualIds() {}

  public static String bucket(int index) {
    return BUCKET + index;
  }

  public static String entry(long id) {
    return ENTRY + id;
  }

  public static String link(String sourceId, String targetId) {
    return LINK + sourceId + "->" + targetId;
  }

  public static boolean isBucket(String id) {
    return id != null && id.startsWith(BUCKET);
  }

  public static int bucketIndex(String id) {
    if (!isBucket(id)) return -1;
    try {
      return Integer.parseInt(id.substring(BUCKET.length()));
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }
}
