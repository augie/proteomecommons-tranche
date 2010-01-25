/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.proteomecommons.tranche.serverlogs;

/**
 *
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
public class BandwidthChartUtil {
  /**
   * These static options are the drop-down values for parameters
   * for bandwidth graph.
   */
  public final static String bandwidthWindow30Hour = "30-hour",
          bandwidthWindow14day = "14-day",
          bandwidthWindow30day = "30-day",
          bandwidthWindow365day = "365-day",
          bandwidthWindowAll = "all";
  
  public final static String bandwidthServerAll = "all",
          bandwidthServerCore = "core";
  
  // If new time window options appear, must add!
  public final static String[] allBandwidthWindowOptions = {
      bandwidthWindow30Hour,
      bandwidthWindow14day,
      bandwidthWindow30day,
      bandwidthWindow365day,
      bandwidthWindowAll
  };
  
  // If new server options appear, must add!
  public final static String[] allBandwidthServerOptions = {
      bandwidthServerAll,
      bandwidthServerCore
  };
  
  /**
   * Checks validity of option
   */
  public static boolean isValidBandwidthWindowOption(String option) {
      if (option == null || option.trim().equals("")) {
          // This will happen in most cases. Means parameter not set
          return false;
      }
      
      // See whether matches any of the valid options
      for (String o : allBandwidthWindowOptions) {
          if (option.equals(o)) {
              return true;
          }
      }
      
      System.out.println("Invalid bandwidth window option: "+option);
      return false;
  }
  
  /**
   * Checks validity of option 
   */
  public static boolean isValidBandwidthServerOption(String option) {
      if (option == null || option.trim().equals("")) {
          // This will happen in most cases. Means parameter not set
          return false;
      }
      
      // See whether matches any of the valid options
      for (String o : allBandwidthServerOptions) {
          if (option.equals(o)) {
              return true;
          }
      }
      
      System.out.println("Invalid bandwidth server option: "+option);
      return false;
  }
  
  /**
   * Default values for above.
   */
  public final static String bandwidthWindowDefault = bandwidthWindow30Hour;
  
  public final static String bandwidthServerDefault = bandwidthServerAll;
}
