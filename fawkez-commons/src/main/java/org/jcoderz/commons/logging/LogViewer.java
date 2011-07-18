/*
 * $Id: LogViewer.java 1011 2008-06-16 17:57:36Z amandel $
 *
 * Copyright 2006, The jCoderZ.org Project. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *    * Neither the name of the jCoderZ.org Project nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jcoderz.commons.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jcoderz.commons.ArgumentMalformedException;
import org.jcoderz.commons.types.Date;
import org.jcoderz.commons.types.Period;



/**
 * This implements a viewer for existing log files. The output generated by
 * this should never be viewed using this, since the format might be different
 * to a 'real' log file.
 *
 * TODO: A lot of functionality is still to implement (behaviour according to
 * command line options).
 *
 */
public final class LogViewer
      implements Runnable
{
   /** Line separator to be used in output files. */
   public static final String LINE_SEPARATOR
         = System.getProperty("line.separator");

   private static final String DEFAULT_DIR = "." + File.separator + "log";
   private static final String DEFAULT_FILE = "log.out";

   /** Time interval [ms] used for polling the log file for new data. */
   private static final int LOGFILE_POLL_INTERVAL = 100;

   private static final String [] EMPTY_STRINGS = new String[0];

   /**
    * Level for displaying message stack trace.
    */
   private static final int LEVEL_MESSAGE_STACKTRACE = 2;
   /**
    * Level for displaying exception stack trace.
    */
   private static final int LEVEL_EXCEPTION_STACKTRACE = 1;
   /**
    * The min length of the date argument.
    */
   private static final int DATE_MIN_LEN = 10;

   /** Number of tokens expected for a period. */
   private static final int PERIOD_TOKEN_COUNT = 2;

   /**
    * Command line options.
    * TODO: Extend options and implement usage.
    *
    * The way OptionsBuilder is used is as proposed by the authors
    * of the commonscli package, but cause checkstyle warnings here...
    */
   private static final Option LOGFILE_OPTION = OptionBuilder.hasArg()
         .withArgName("logfile").withDescription(
         "open file <logfile> instead of log.out")
         .withValueSeparator().withLongOpt("logFile").create("F");

   private static final Option LOGDIR_OPTION = OptionBuilder.hasArg()
         .withArgName("logdir").withDescription(
         "find log files within <logdir> instead of ./log")
         .withValueSeparator().withLongOpt("logDir").create("D");

   private static final Option LINES_OPTION = OptionBuilder.hasArg()
         .withArgName("lines").withDescription(
         "display <lines> last log records instead of 25")
         .withValueSeparator().withLongOpt("lines").create("l");

   private static final Option DATE_OPTION = OptionBuilder.hasOptionalArgs()
         .withArgName("dateFrom,dateTo").withDescription(
         "display date [and search for given date/timestamp range. "
         + "The following date formats are supported: "
         + "<yyyy-MM-dd|yyyy-MM-dd'T'HH:mm:ss'Z'|"
         + "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'>. One of both dates may be omitted].")
         .withValueSeparator().withLongOpt("date").create("d");

   private static final Option TIME_OPTION =  new Option("t", "timestamp",
         false, "display timestamp.");

   private static final Option BATCH_OPTION = new Option("b", "batch", false,
         "batch mode, terminate if end of log file reached.");

   private static final Option OUTFILE_OPTION = OptionBuilder.hasArg()
         .withArgName("file").withDescription("send output to file")
         .withValueSeparator().withLongOpt("outfile").create("o");

   private static final Option THREADID_OPTION = OptionBuilder.hasOptionalArgs()
         .withArgName("tid1,tid2,...").withDescription(
          "display thread id [and filter for given thread ids")
          .withValueSeparator().withLongOpt("thread").create("T");

   private static final Option XML_OPTION
         = new Option("xml", "output in xml format.");

   private static final Option STACKTRACE_OPTION = OptionBuilder.hasArgs()
         .withArgName("{0|1|2}").withDescription("display stack trace details; "
         + "0: no stacktrace, 1: only for exceptions (default), "
         + "2: all stack trace")
         .withValueSeparator().create("stack");

   private static final Option IMPACT_OPTION = OptionBuilder.hasOptionalArgs()
         .withArgName("bi1,bi2,...").withDescription("display business impact "
         + "[and filter for given business impact ids]")
         .withValueSeparator().withLongOpt("impact").create("i");

   private static final Option CATEGORY_OPTION = OptionBuilder.hasOptionalArgs()
         .withArgName("cat1,cat2,...").withDescription("display category "
         + "[and filter for given categories]")
         .withValueSeparator().withLongOpt("cat").create("c");

   private static final Option LEVEL_OPTION = OptionBuilder.hasOptionalArgs()
      .withArgName("lev1,lev2,...").withDescription("display log level "
      + "[and filter for given levels]")
      .withValueSeparator().withLongOpt("level").create("L");

   private static final Option STANDARD_OPTION = OptionBuilder
         .withDescription("set standard mode, same as -t -i -c -L -stack 1")
         .withLongOpt("standard").create("s");

/*
      System.err.println(
         "  -P[id] PID                     "
            + "display process ID [and search for given process ID]");
      System.err.println(
         "  -r[ecno] [MIN[,MAX]]           "
            + "display record number [and search for given record range]");
      System.err.println(
         "  -e[rrordetails] LEVEL          "
            + "set error details to given level");
      System.err.println(
         "  -tr[acedetails] LEVEL          "
            + "set trace details to given level");
      System.err.println(
         "  -s                             "
            + "standard mode: same as -t -r -P -IP");
      System.err.println(
         "  -f[ormats] (e|t)               "
            + "display only error or trace messages");
      System.err.println(
         "  -lev[el] [<lev1>,[<lev2>]]     "
            + "display message level [and search for given level range]");
      System.err.println(
         "  -logger                        display logger name");
      System.err.println("  -class                         display class name");
      System.err.println(
         "  -method                        display method name");
      System.err.println(
         "  -enc[oding] <encoding>         "
            + "define the encoding for standard output,");
      System.err.println(
         "                                 "
            + "e.g. cp437 (PC US) or cp850 (PC latin1 with Euro)");
      System.err.println(
         "                                 "
            + "Note: use CHCP to find out the codepage used by DOS.");

   -log [-l[ines]=NO]                   - display NO last lines, if
   NO is -1, ALL lines will be displayed,
   default: use the last 25 records
   combine other filter options with -l
[-P[id=PID]]                      [with PID]
[-X[id=TxID]]                     [with TxID]
[-S[essionID=SID]]                [with SID]
[-IP]                             [with IP]
[-p[rogram=NAME]]                 [with NAME]
[-r[ecno]]                        [show record number]
[-e[rrordetails]=LEVEL]           [set errordetails to level 'LEVEL']
[-tr[acedetails]=LEVEL]           [set tracedetails to level 'LEVEL']
[-s]                              [standard: same as -t -r -P -IP]
[-f[ormats]=(e|t)]                [choose only error or trace to be
   displayed]
-lev[el] [<lev1>,[<lev2>]]        display message level [and search for
   given level range]
-logger                           display logger name
-class                            display class name
-method                           display method name
-enc[oding] <encoding>            define the encoding for standard,
*/
   private PrintWriter mOut;

   private Options mOptions;

   private CommandLine mCommandLine;
   private DisplayOptions mDisplayOptions;
   private final List mFilters = new ArrayList();
   private LogPrinter mDisplay;

   private LogReader mLogReader;

   private LogViewer ()
   {
      installOptions();
   }

   /**
    * The main entry method. Installs a new instance of this, which reads the
    * supplied log file.
    *
    * @param argv Contains the command line arguments.
    */
   public static void main (String[] argv)
   {
      final LogViewer logConsole = new LogViewer();

      if (argv.length < 1)
      {
         logConsole.printUsage();
      }
      else
      {
         try
         {
            logConsole.readCommandLineArgs(argv);
            logConsole.init();
            logConsole.run();
         }
         catch (Exception ex)
         {
            Throwable th = ex;
            System.err.println("Caught exception: " + th);
            while (th != null)
            {
               th.printStackTrace();
               th = th.getCause();
               if (th != null)
               {
                  System.err.println("Caused by: " + th);
               }
            }
         }
         finally
         {
            logConsole.close();
         }
      }
   }

   private void init ()
         throws ArgumentMalformedException, java.text.ParseException
   {
      initDisplayOptions();
      initDisplayFilters();
      setInput();
      setOutput();
      installDisplay();
   }

   private void initDisplayFilters ()
         throws ArgumentMalformedException, java.text.ParseException
   {
      initThreadIdFilter();
      initBusinessImpactFilter();
      initCategoryFilter();
      initLogLevelFilter();
      initPeriodFilter();
   }

   private void initThreadIdFilter ()
   {
      if (mCommandLine.hasOption(THREADID_OPTION.getOpt()))
      {
         final String[] threadIds = parseOptionValues(
               mCommandLine.getOptionValues(THREADID_OPTION.getOpt()));
         if ((threadIds != null) && (threadIds.length > 0))
         {
            final List ids = new ArrayList();
            for (int i = 0; i < threadIds.length; ++i)
            {
               ids.add(Long.valueOf(threadIds[i]));
            }
            mFilters.add(new ThreadIdFilter(ids));
         }
      }
   }

   private void initBusinessImpactFilter ()
   {
      if (mCommandLine.hasOption(IMPACT_OPTION.getOpt()))
      {
         final String[] impacts = parseOptionValues(
               mCommandLine.getOptionValues(IMPACT_OPTION.getOpt()));
         if ((impacts != null) && (impacts.length > 0))
         {
            mFilters.add(new BusinessImpactFilter(Arrays.asList(impacts)));
         }
      }
   }

   private void initCategoryFilter ()
   {
      if (mCommandLine.hasOption(CATEGORY_OPTION.getOpt()))
      {
         final String[] cats = parseOptionValues(
               mCommandLine.getOptionValues(CATEGORY_OPTION.getOpt()));
         if ((cats != null) && (cats.length > 0))
         {
            mFilters.add(new CategoryFilter(Arrays.asList(cats)));
         }
      }
   }

   private void initLogLevelFilter ()
   {
      if (mCommandLine.hasOption(LEVEL_OPTION.getOpt()))
      {
         final String[] levels = parseOptionValues(
               mCommandLine.getOptionValues(LEVEL_OPTION.getOpt()));
         if ((levels != null) && (levels.length > 0))
         {
            mFilters.add(new LevelFilter(Arrays.asList(levels)));
         }
      }
   }

   private void initPeriodFilter ()
         throws ArgumentMalformedException, java.text.ParseException
   {
      if (mCommandLine.hasOption(DATE_OPTION.getOpt()))
      {
         final Period [] periods = getPeriodsFromOptionValues(
               mCommandLine.getOptionValues(DATE_OPTION.getOpt()));
         if ((periods != null) && (periods.length > 0))
         {
            mFilters.add(new PeriodFilter(periods));
         }
      }
   }

   static Period [] getPeriodsFromOptionValues (String [] optionValues)
         throws ArgumentMalformedException, java.text.ParseException
   {
      Period [] result = new Period[0];
      final List vals = new ArrayList();
      for (int i = 0; i < optionValues.length; i++)
      {
         final List values = new ArrayList();
         final StringTokenizer tokens = new StringTokenizer(
               optionValues[i], ",");
         while (tokens.hasMoreTokens())
         {
            values.add(tokens.nextToken());
         }

         String dateFrom = null;
         String dateTo = null;
         if (values.size() != PERIOD_TOKEN_COUNT)
         {
            if (values.size() == 1 && optionValues[i].startsWith(",", 0))
            {
               dateTo =  (String) values.get(0);
            }
            else if (values.size() == 1)
            {
               dateFrom = (String) values.get(0);
            }
            else
            {
               throw new LoggingException("Illegal time interval has been "
                     + "specified in the command line " + optionValues[i]);
            }
         }
         else
         {
            dateFrom = (String) values.get(0);
            dateTo = (String) values.get(1);
         }

         final Period period = Period.createPeriod(getDateFrom(dateFrom),
               getDateTo(dateTo));

         vals.add(period);
      }

      result = (Period []) vals.toArray(result);

      return result;
   }

   static Date getDateFrom (String str)
         throws java.text.ParseException
   {
      return getDateFromOptValue(str, true);
   }

   static Date getDateTo (String str)
         throws java.text.ParseException
   {
      return getDateFromOptValue(str, false);
   }

   static Date getDateFromOptValue (String str, boolean dateFrom)
         throws java.text.ParseException
   {
      final Date result;
      if (str == null || str.length() == 0)
      {
         result = dateFrom ? Date.OLD_DATE : Date.FUTURE_DATE;
      }
      else
      {
         if (str.length() == DATE_MIN_LEN)
         {
            result = Date.fromString(str + "Z", Date.DATE_FORMAT);
         }
         else if (str.length() == DATE_MIN_LEN + 1)
         {
            result = Date.fromString(str, Date.DATE_FORMAT);
         }
         else
         {
            result = Date.fromString(str);
         }
      }
      return result;
   }

   /**
    * Splits the strings contained within the supplied array into single values.
    * Each String of <code>optionValues</code> might contain a comma separated
    * value list, which is split by this.
    *
    * @param optionValues
    *
    * @return String array containing the split values. Might be null.
    */
   private String [] parseOptionValues (final String [] optionValues)
   {
      String[] rc = null;
      if (optionValues != null)
      {
         final List values = new ArrayList();
         for (int i = 0; i < optionValues.length; ++i)
         {
            final StringTokenizer tokens = new StringTokenizer(
                  optionValues[i], "\t ,");
            while (tokens.hasMoreTokens())
            {
               values.add(tokens.nextToken());
            }
         }
         rc = (String[]) values.toArray(EMPTY_STRINGS);
      }
      return rc;
   }

   /**
    *
    */
   private void initDisplayOptions ()
   {
      mDisplayOptions = new DisplayOptions();
      if (mCommandLine.hasOption(THREADID_OPTION.getOpt()))
      {
         mDisplayOptions.displayThreadId(true);
      }
      if (mCommandLine.hasOption(TIME_OPTION.getOpt()))
      {
         mDisplayOptions.displayTimestamp(true);
      }
      if (mCommandLine.hasOption(DATE_OPTION.getOpt()))
      {
         mDisplayOptions.displayTimestamp(true);
      }
      if (mCommandLine.hasOption(IMPACT_OPTION.getOpt()))
      {
         mDisplayOptions.displayBusinessImpact(true);
      }
      if (mCommandLine.hasOption(CATEGORY_OPTION.getOpt()))
      {
         mDisplayOptions.displayCategory(true);
      }
      if (mCommandLine.hasOption(LEVEL_OPTION.getOpt()))
      {
         mDisplayOptions.displayLoggerLevel(true);
      }
      if (mCommandLine.hasOption(STACKTRACE_OPTION.getOpt()))
      {
         setStackTraceDetails();
      }
      if (mCommandLine.hasOption(STANDARD_OPTION.getOpt()))
      {
         setStandardDisplay(mDisplayOptions);
      }
   }

   private void setStandardDisplay (final DisplayOptions dp)
   {
      dp.displayTimestamp(true);
      dp.displayBusinessImpact(true);
      dp.displayCategory(true);
      dp.displayLoggerLevel(true);
      dp.displayStackTrace(true);
      dp.displayMessageStackTrace(false);
   }

   private void setStackTraceDetails ()
   {
      final String[] details = parseOptionValues(
            mCommandLine.getOptionValues(STACKTRACE_OPTION.getOpt()));
      if ((details != null) && (details.length > 0))
      {
         final int level = Integer.parseInt(details[0]);
         mDisplayOptions.displayMessageStackTrace(
               level >= LEVEL_MESSAGE_STACKTRACE);
         mDisplayOptions.displayStackTrace(
               level >= LEVEL_EXCEPTION_STACKTRACE);
      }
   }

   private void setInput ()
         throws LoggingException
   {
      final String logDir = mCommandLine.getOptionValue(
            LOGDIR_OPTION.getOpt(), DEFAULT_DIR);

      if ((logDir == null) || (logDir.length() == 0))
      {
         throw new LoggingException("Undefined log directory.");
      }

      final String fileName = logDir + File.separator
            + mCommandLine.getOptionValue(
                  LOGFILE_OPTION.getOpt(), DEFAULT_FILE);

      final LogReader logReader;
      try
      {
         logReader = new LogReader(fileName);
         setFilters(logReader);
         mLogReader = logReader;
      }
      catch (InstantiationException ex)
      {
         throw new LoggingException("Error instantiating a LogReader for file "
               + fileName, ex);
      }
   }

   private void setOutput ()
         throws LoggingException
   {
      if (mCommandLine.hasOption(OUTFILE_OPTION.getOpt()))
      {
         final String fileName = mCommandLine.getOptionValue(
               OUTFILE_OPTION.getOpt());
         try
         {
            final File file = new File(fileName);
            mOut = new PrintWriter(new FileOutputStream(file));
         }
         catch (FileNotFoundException ex)
         {
            throw new LoggingException(
                  "Could not open the output file " + fileName, ex);
         }
      }
      else
      {
         mOut = new PrintWriter(System.out);
      }
   }

   /**
    * Closes the log console. Closes the output stream.
    */
   private void close ()
   {
      if (mOut != null)
      {
         mOut.flush();
         mOut.close();
      }
      if (mLogReader != null)
      {
         mLogReader.close();
      }
   }

   /**
    * The run method.
    * Reads the log file and prints the log records until end of file is reached
    * (batch mode) or until it is terminated.
    *
    * @see java.lang.Runnable#run()
    */
   public void run ()
   {
      try
      {
         if (mCommandLine.hasOption(BATCH_OPTION.getOpt()))
         {
            runBatchMode();
         }
         else
         {
            runLiveMode();
         }
      }
      catch (Exception ex)
      {
         System.err.println("Caught exception while viewing log file: " + ex);
         ex.printStackTrace();
      }
   }

   /**
    * Runs the live mode.
    */
   private void runLiveMode ()
   {
      while (true)
      {
         LogFileEntry logRecord = null;

         if (mLogReader.available())
         {
            do
            {
               logRecord = mLogReader.readLogFileEntry();
               if ((logRecord != null) && (mDisplay != null))
               {
                  mDisplay.print(mOut, logRecord);
                  logRecord.release();
               }
            }
            while (logRecord != null);
            mOut.flush();
         }

         try
         {
            Thread.sleep(LOGFILE_POLL_INTERVAL);
         }
         catch (InterruptedException e)
         {
            // ignore
         }
      }
   }

   /**
    * Runs the batch mode.
    */
   private void runBatchMode ()
   {
      LogFileEntry logRecord = null;
      do
      {
         logRecord = mLogReader.readLogFileEntry();
         if ((logRecord != null) && (mDisplay != null))
         {
            mDisplay.print(mOut, logRecord);
            logRecord.release();
         }
      }
      while (logRecord != null);

      mOut.flush();
   }

   private void installOptions ()
   {
      mOptions = new Options();

      mOptions.addOption(LOGFILE_OPTION);
      mOptions.addOption(LOGDIR_OPTION);
//      mOptions.addOption(LINES_OPTION);
      mOptions.addOption(BATCH_OPTION);
      mOptions.addOption(OUTFILE_OPTION);
      mOptions.addOption(XML_OPTION);
      mOptions.addOption(STACKTRACE_OPTION);
      mOptions.addOption(DATE_OPTION);
      mOptions.addOption(TIME_OPTION);
      mOptions.addOption(THREADID_OPTION);
      mOptions.addOption(IMPACT_OPTION);
      mOptions.addOption(CATEGORY_OPTION);
      mOptions.addOption(LEVEL_OPTION);
      mOptions.addOption(STANDARD_OPTION);
   }

   private void readCommandLineArgs (final String[] args)
         throws ParseException
   {
      mCommandLine = new GnuParser().parse(mOptions, args, true);
   }

   private void installDisplay ()
         throws LoggingException
   {
      if (mCommandLine.hasOption(XML_OPTION.getOpt()))
      {
         try
         {
            mDisplay = new XmlPrinter();
         }
         catch (InstantiationException ex)
         {
            throw new LoggingException("Error instantiating an XmlPrinter", ex);
         }
      }
      else
      {
         mDisplay = new BasicPrinter();
      }
      mDisplay.setDisplayOptions(mDisplayOptions);
   }

   /**
    * Sets the filters for the supplied LogReader as specified in the command
    * line options.
    *
    * @param logReader The LogReader for which to set the filters
    *
    * TODO Implement Me
    */
   private void setFilters (final LogReader logReader)
   {
      for (final Iterator iter = mFilters.iterator(); iter.hasNext(); )
      {
         logReader.addFilter((Filter) iter.next());
      }
   }

   private void printUsage ()
   {
      new HelpFormatter().printHelp(
            "java org.jcoderz.commons.LogViewer", mOptions);
   }
}
