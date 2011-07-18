/*
 * $Id: XmlPrinterTest.java 1011 2008-06-16 17:57:36Z amandel $
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

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jcoderz.commons.LogFormatterOutputTest;


/**
 * This class is used for testing the XmlPrinter. It uses the handler being
 * installed by the LogElementTest and sets a formatter using th XmlPrinter.
 * The log messages are generated by the LogFormatterOutputTest.
 *
 */
public class XmlPrinterTest
      extends LogFormatterOutputTest
{
   private static final String CLASSNAME = FormatTest.class.getName();
   private static final Logger logger = Logger.getLogger(CLASSNAME);

   private static final Logger ROOT_LOGGER = Logger.getLogger("");

   private Handler mHandler = null;

   private static class LogElementHandler
         extends Handler
   {
      /**
       * Creates a new instance of this and sets teh xml formatter as formatter.
       */
      private LogElementHandler ()
      {
         setFormatter(new XmlFormatter());
      }

      /**
       * Closes this Handler, this is a NOP.
       *
       * @see java.util.logging.Handler#close()
       */
      public void close ()
            throws SecurityException
      {
         // nop
      }

      /**
       * Flushes this Handler, this is a NOP.
       *
       * @see java.util.logging.Handler#flush()
       */
      public void flush ()
      {
         // nop
      }

      /**
       * Just formats the LogRecord with the xml formatter.
       *
       * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
       */
      public void publish (LogRecord record)
      {
         getFormatter().format(record);
      }
   }

   private static class XmlFormatter
         extends Formatter
   {
      final PrintWriter mPrinter;
      final CharArrayWriter mCharWriter;
      XmlPrinter mXmlPrinter = null;

      private XmlFormatter ()
      {
         mCharWriter = new CharArrayWriter();
         mPrinter = new PrintWriter(mCharWriter);
         try
         {
            mXmlPrinter = new XmlPrinter();
         }
         catch (InstantiationException e)
         {
            fail("Could not instantiate an Xml Printer");
         }
      }

      /** {@inheritDoc} */
      public String format (LogRecord record)
      {
         mCharWriter.reset();
         final LogItem element = new LogElement(record);
         mXmlPrinter.print(mPrinter, element);
         return mCharWriter.toString();
      }
   }

   /**
    * Creates a new test case instance.
    */
   public XmlPrinterTest ()
   {
      super();
   }

   public void testSimpleLogRecord ()
   {
      logger.info("testSimpleLogRecord: " + getClass().getName());
   }


   /**
    * Installs an additional Handler, which publishes RocRecords using
    * LogElements.
    *
    * @see junit.framework.TestCase#setUp()
    */
   protected void setUp ()
   {
      mHandler = new LogElementHandler();
      ROOT_LOGGER.addHandler(mHandler);
   }

   /**
    * Deinstalls the additional Handler.
    *
    * @see junit.framework.TestCase#setUp()
    */
   protected void tearDown ()
   {
      if (mHandler != null)
      {
         ROOT_LOGGER.removeHandler(mHandler);
         mHandler = null;
      }
   }
}
