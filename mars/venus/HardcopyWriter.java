package mars.venus;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.JobAttributes;
import java.awt.PageAttributes;
import java.awt.PrintJob;
import java.awt.Toolkit;
import java.io.FileReader;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class HardcopyWriter extends Writer {
   protected PrintJob job;
   protected Graphics page;
   protected String jobname;
   protected int fontsize;
   protected String time;
   protected Dimension pagesize;
   protected int pagedpi;
   protected Font font;
   protected Font headerfont;
   protected FontMetrics metrics;
   protected FontMetrics headermetrics;
   protected int x0;
   protected int y0;
   protected int width;
   protected int height;
   protected int headery;
   protected int charwidth;
   protected int lineheight;
   protected int lineascent;
   protected int chars_per_line;
   protected int lines_per_page;
   protected int chars_per_tab = 4;
   protected int charnum = 0;
   protected int linenum = 0;
   protected int pagenum = 0;
   private boolean last_char_was_return = false;
   protected static Properties printprops = new Properties();

   public HardcopyWriter(Frame frame, String jobname, int fontsize, double leftmargin, double rightmargin, double topmargin, double bottommargin) throws PrintCanceledException {
      Toolkit toolkit = frame.getToolkit();
      synchronized(printprops) {
         JobAttributes ja = new JobAttributes();
         PageAttributes pa = new PageAttributes();
         this.job = toolkit.getPrintJob(frame, jobname, ja, pa);
      }

      if (this.job == null) {
         throw new PrintCanceledException("User cancelled print request");
      } else {
         this.pagedpi = 72;
         this.pagesize = new Dimension((int)(8.5 * (double)this.pagedpi), 11 * this.pagedpi);
         fontsize = fontsize * this.pagedpi / 72;
         this.x0 = (int)(leftmargin * (double)this.pagedpi);
         this.y0 = (int)(topmargin * (double)this.pagedpi);
         this.width = this.pagesize.width - (int)((leftmargin + rightmargin) * (double)this.pagedpi);
         this.height = this.pagesize.height - (int)((topmargin + bottommargin) * (double)this.pagedpi);
         this.font = new Font("Lucida Sans Typewriter", 0, fontsize);
         this.metrics = frame.getFontMetrics(this.font);
         this.lineheight = this.metrics.getHeight();
         this.lineascent = this.metrics.getAscent();
         this.charwidth = this.metrics.charWidth('0');
         this.chars_per_line = this.width / this.charwidth;
         this.lines_per_page = this.height / this.lineheight;
         this.headerfont = new Font("SansSerif", 2, fontsize);
         this.headermetrics = frame.getFontMetrics(this.headerfont);
         this.headery = this.y0 - (int)(0.125 * (double)this.pagedpi) - this.headermetrics.getHeight() + this.headermetrics.getAscent();
         DateFormat df = DateFormat.getDateTimeInstance(1, 3);
         df.setTimeZone(TimeZone.getDefault());
         this.time = df.format(new Date());
         this.jobname = jobname;
         this.fontsize = fontsize;
      }
   }

   public void write(char[] buffer, int index, int len) {
      synchronized(this.lock) {
         for(int i = index; i < index + len; ++i) {
            if (this.page == null) {
               this.newpage();
            }

            if (buffer[i] == '\n') {
               if (!this.last_char_was_return) {
                  this.newline();
               }
            } else if (buffer[i] == '\r') {
               this.newline();
               this.last_char_was_return = true;
            } else {
               this.last_char_was_return = false;
               if (!Character.isWhitespace(buffer[i]) || Character.isSpaceChar(buffer[i]) || buffer[i] == '\t') {
                  if (this.charnum >= this.chars_per_line) {
                     this.newline();
                     if (this.page == null) {
                        this.newpage();
                     }
                  }

                  if (Character.isSpaceChar(buffer[i])) {
                     ++this.charnum;
                  } else if (buffer[i] == '\t') {
                     this.charnum += this.chars_per_tab - this.charnum % this.chars_per_tab;
                  } else {
                     this.page.drawChars(buffer, i, 1, this.x0 + this.charnum * this.charwidth, this.y0 + this.linenum * this.lineheight + this.lineascent);
                     ++this.charnum;
                  }
               }
            }
         }

      }
   }

   public void flush() {
   }

   public void close() {
      synchronized(this.lock) {
         if (this.page != null) {
            this.page.dispose();
         }

         this.job.end();
      }
   }

   public void setFontStyle(int style) {
      synchronized(this.lock) {
         Font current = this.font;

         try {
            this.font = new Font("Lucida Sans Typewriter", style, this.fontsize);
         } catch (Exception var5) {
            this.font = current;
         }

         if (this.page != null) {
            this.page.setFont(this.font);
         }

      }
   }

   public void pageBreak() {
      synchronized(this.lock) {
         this.newpage();
      }
   }

   public int getCharactersPerLine() {
      return this.chars_per_line;
   }

   public int getLinesPerPage() {
      return this.lines_per_page;
   }

   protected void newline() {
      this.charnum = 0;
      ++this.linenum;
      if (this.linenum >= this.lines_per_page) {
         this.page.dispose();
         this.page = null;
      }

   }

   protected void newpage() {
      this.page = this.job.getGraphics();
      this.linenum = 0;
      this.charnum = 0;
      ++this.pagenum;
      this.page.setFont(this.headerfont);
      this.page.drawString(this.jobname, this.x0, this.headery);
      String s = "- " + this.pagenum + " -";
      int w = this.headermetrics.stringWidth(s);
      this.page.drawString(s, this.x0 + (this.width - w) / 2, this.headery);
      w = this.headermetrics.stringWidth(this.time);
      this.page.drawString(this.time, this.x0 + this.width - w, this.headery);
      int y = this.headery + this.headermetrics.getDescent() + 1;
      this.page.drawLine(this.x0, y, this.x0 + this.width, y);
      this.page.setFont(this.font);
   }

   public static void main(String[] args) {
      try {
         if (args.length != 1) {
            throw new IllegalArgumentException("Wrong # of arguments");
         }

         FileReader in = new FileReader(args[0]);
         HardcopyWriter out = null;
         Frame f = new Frame("PrintFile: " + args[0]);
         f.setSize(200, 50);
         f.setVisible(true);

         try {
            out = new HardcopyWriter(f, args[0], 10, 0.5, 0.5, 0.5, 0.5);
         } catch (PrintCanceledException var6) {
            System.exit(0);
         }

         f.setVisible(false);
         char[] buffer = new char[4096];

         int numchars;
         while((numchars = in.read(buffer)) != -1) {
            out.write(buffer, 0, numchars);
         }

         in.close();
         out.close();
      } catch (Exception var7) {
         System.err.println(var7);
         System.err.println("Usage: java HardcopyWriter$PrintFile <filename>");
         System.exit(1);
      }

      System.exit(0);
   }

   public static class PrintCanceledException extends Exception {
      public PrintCanceledException(String msg) {
         super(msg);
      }
   }
}
