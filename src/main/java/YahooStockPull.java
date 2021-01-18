
/**
*
* Pulls yahoo historical quotes.
* 
* This is a tweaked version of Brad's stock quote pull.  It processes symbols from a file
* as opposed to a parameter.  It is solely used for my own personal use and not 
* distributed. 
* 
* Original author info below:
* ---------------------------
* Author: Brad Lucas brad@beaconhill.com
* Latest: https://github.com/bradlucas/get-yahoo-quotes
*
* Copyright (c) 2017 Brad Lucas - All Rights Reserved
*
*
* History
*
* 06-04-2017 : Created script
*
*/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Calendar;
import java.util.List;
//import java.util.Map;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.client.CookieStore;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.protocol.HttpClientContext;
//import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.methods.HttpGet;

// import org.apache.commons.lang3.StringEscapeUtils; used to be in lang3
import org.apache.commons.text.StringEscapeUtils;

public class YahooStockPull {

  private static boolean debugIt = true;

  HttpClient client = HttpClientBuilder.create().build();
  HttpClientContext context = HttpClientContext.create();

  public YahooStockPull() {
    CookieStore cookieStore = new BasicCookieStore();
    client = HttpClientBuilder.create().build();
    context = HttpClientContext.create();
    context.setCookieStore(cookieStore);
  }

  public String getPage(String symbol) {
    String rtn = null;
    String url = String.format("https://finance.yahoo.com/quote/%s/?p=%s",symbol, symbol);
    HttpGet request = new HttpGet(url);
    System.out.println(url);

    // Changed header 01/17/2021... old header caused CrumbStore not to appear in content
    // request.addHeader("User-Agent",
    //    "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13");
    request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:74.0) Gecko/20100101 Firefox/74.0");
    try {
      HttpResponse response = client.execute(request, context);
      System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

      BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      StringBuffer result = new StringBuffer();
      String line = "";
      while ((line = rd.readLine()) != null) {
        if (debugIt && 1 == 0)
          System.out.println("getPage, line:" + line);
        if (debugIt && line.indexOf("CrumbSt") > -1) {  // Specific to getting crumb info
          int beginIndex = line.indexOf("CrumbSt");
          System.out.println("has CrumbSt: " + line.substring(beginIndex));
        }
        result.append(line);
      }
      rtn = result.toString();
      HttpClientUtils.closeQuietly(response);
    } catch (Exception ex) {
      System.out.println("Exception");
      System.out.println(ex);
    }
    System.out.println("returning from getPage");
    return rtn;
  }

  public List<String> splitPageData(String page) {
    // Return the page as a list of string using } to split the page
    return Arrays.asList(page.split("}"));
  }

  public String findCrumb(List<String> lines) {
    String crumb = "";
    String rtn = "";
    for (String l : lines) {
      if (l.indexOf("CrumbStore") > -1) {
        rtn = l;
        break;
      }
    }
    // ,"CrumbStore":{"crumb":"OKSUqghoLs8"
    if (rtn != null && !rtn.isEmpty()) {
      String[] vals = rtn.split(":"); // get third item
      crumb = vals[2].replace("\"", ""); // strip quotes
      crumb = StringEscapeUtils.unescapeJava(crumb); // unescape escaped values
                                                     // (particularly, \u002f
    }
    else 
      System.out.println("Didn't find crumb");
    
    return crumb;
  }

  public String getCrumb(String symbol) {
    return findCrumb(splitPageData(getPage(symbol)));
  }

  /* This is the original version... main difference to one I added is the addition of eventType
   * so that I could get history, dividend or splits.  Also prefixed output filename with 'output' 
   * directory
   * 
   * public void downloadData(String symbol, long startDate, long endDate, String
   * crumb) { String filename = String.format("%s.csv", symbol); String url =
   * String.format(
   * "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%s&period2=%s&interval=1d&events=history&crumb=%s",
   * symbol, startDate, endDate, crumb); HttpGet request = new HttpGet(url);
   * System.out.println(url);
   * 
   * // Changed header 1/17/2021 // request.addHeader("User-Agent", //
   * "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13"
   * ); request.addHeader("User-Agent",
   * "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:74.0) Gecko/20100101 Firefox/74.0"
   * ); try { HttpResponse response = client.execute(request, context);
   * System.out.println("Response Code : " +
   * response.getStatusLine().getStatusCode()); HttpEntity entity =
   * response.getEntity();
   * 
   * String reasonPhrase = response.getStatusLine().getReasonPhrase(); int
   * statusCode = response.getStatusLine().getStatusCode();
   * 
   * System.out.println(String.format("statusCode: %d", statusCode));
   * System.out.println(String.format("reasonPhrase: %s", reasonPhrase));
   * 
   * if (entity != null) { BufferedInputStream bis = new
   * BufferedInputStream(entity.getContent()); BufferedOutputStream bos = new
   * BufferedOutputStream( new FileOutputStream(new File(filename))); int inByte;
   * while ((inByte = bis.read()) != -1) bos.write(inByte); bis.close();
   * bos.close(); } HttpClientUtils.closeQuietly(response);
   * 
   * } catch (Exception ex) { System.out.println("Exception");
   * System.out.println(ex); } }
   */

  public void downloadData2(String symbol, String eventType, long startDate, long endDate, String crumb) {
    String filename = String.format(".%soutput%s%s_%s.csv", File.separator, File.separator, symbol, eventType);
    String url = String.format(
        "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%s&period2=%s&interval=1d&events=%s&crumb=%s",
        symbol, startDate, endDate, eventType, crumb);
    HttpGet request = new HttpGet(url);
    System.out.println(url);

    // request.addHeader("User-Agent",
    //     "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13");
    request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:74.0) Gecko/20100101 Firefox/74.0");
    try {
      HttpResponse response = client.execute(request, context);
      System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
      HttpEntity entity = response.getEntity();

      String reasonPhrase = response.getStatusLine().getReasonPhrase();
      int statusCode = response.getStatusLine().getStatusCode();

      System.out.println(String.format("statusCode: %d", statusCode));
      System.out.println(String.format("reasonPhrase: %s", reasonPhrase));

      if (entity != null) {
        BufferedInputStream bis = new BufferedInputStream(entity.getContent());
        BufferedOutputStream bos = new BufferedOutputStream(
            new FileOutputStream(new File(filename)));
        int inByte;
        while ((inByte = bis.read()) != -1)
          bos.write(inByte);
        bis.close();
        bos.close();
      }
      HttpClientUtils.closeQuietly(response);

    } catch (Exception ex) {
      System.out.println("Exception");
      System.out.println(ex);
    }
  }
  
  private boolean downloadExists(String symbol, String eventType) {
    String fileName = String.format(".%soutput%s%s_%s.csv", File.separator, File.separator, symbol, eventType);
    return (new File(fileName)).exists();
  }  
  
  private void processSymbolList(String[] symbolArray) {
    long numberOfSeconds = 1;
    Random random = new Random();
    for (String symbol : symbolArray) {
      String crumb = getCrumb(symbol);
      if (crumb != null && !crumb.isEmpty()) {
        System.out.println(String.format("Downloading data to %s", symbol));
        if (debugIt) System.out.println("Crumb: " + crumb);
        if (downloadExists(symbol,"history")) {
         System.out.println("Symbol: " + symbol + " type: history Exists, nothing done"); 
        }
        else {
          // c.downloadData(symbol, 0, System.currentTimeMillis(), crumb);
          downloadData2(symbol, "history", 0, System.currentTimeMillis(), crumb);
          downloadData2(symbol, "div", 0, System.currentTimeMillis(), crumb);
          downloadData2(symbol, "split", 0, System.currentTimeMillis(), crumb);
        }

      } else {
        System.out.println(String.format("Error retreiving data for symbol %s", symbol));
      }
      // Sleep between calls, just in case :)
      numberOfSeconds = random.nextInt(4) + 1;
      try {
        if (debugIt) System.out.println("Sleeping for:" + Long.toString(numberOfSeconds));
        java.util.concurrent.TimeUnit.SECONDS.sleep(numberOfSeconds);
      } catch (InterruptedException e) { 
        // do nothing
      }
    }    
  }

  // Return an array with all the symbols from the file passed in, the symbol should
  // be the first blank delimitted value in the file
  private String[] processFile(String fileName) {   
    String inLine;
    List<String> listOfSymbols = new ArrayList<String>();    
    // try {
    try (BufferedReader in = new BufferedReader(new FileReader(fileName))) {;
      while ((inLine = in.readLine()) != null) {
        String theArray[] = inLine.trim().split("[ \\t]+"); // One or more
                                                            // spaces
        if (theArray.length < 1) {
          System.out.println("Invalid record for parsing, bypassed: " + inLine);
        } else {
          listOfSymbols.add(theArray[0].toUpperCase());  // Only take symbol value
        } 
      } 
    } // end of try block
    catch (FileNotFoundException e) {
      System.out.println("File not found " + e);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return listOfSymbols.toArray(new String[listOfSymbols.size()]);
  }

  // }
  
  public static void main(String[] args) {
    YahooStockPull yahooStockPull = new YahooStockPull();
    String[] arrayToProcess = null;
    if (args.length > 0) {
      if (args[0].compareTo("-f") == 0) {
        arrayToProcess = yahooStockPull.processFile(args[1]);
      }
      else {
        // Copy args into arrayToProcess
        arrayToProcess = Arrays.copyOf(args, args.length);        
      }
    } 
    if (arrayToProcess.length > 0) {
      yahooStockPull.processSymbolList(arrayToProcess);
    }
    else {
      System.out.println("Parms either: listOfSymbols or '-f filenameOfSymbols'");
    }
  }
}