package madkit.gui;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;


public interface PrintableFrame extends Printable {

	@Override
	default public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		if (pageIndex > 0) { /* We have only one page, and 'page' is zero-based */
			return NO_SUCH_PAGE;
		}
      Graphics2D g2d = (Graphics2D)graphics;
      final double imageableX = pageFormat.getImageableX();
		g2d.translate(imageableX, pageFormat.getImageableY());
		
		getPrintableContainer().printAll(graphics); 
		return PAGE_EXISTS;	
	}
	
   default public Container getPrintableContainer(){
   	return (Container) this;
   }
}
