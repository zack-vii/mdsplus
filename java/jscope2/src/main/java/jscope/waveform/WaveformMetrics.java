package jscope.waveform;

/* $Id$ */
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.IndexColorModel;
import java.io.Serializable;
import java.util.Vector;
import jscope.data.signal.Signal;
import jscope.data.signal.Signal.Mode1D;
import jscope.dialog.ColorMap;

public final class WaveformMetrics implements Serializable{
	private static final long	serialVersionUID	= 1L;
	private static final int	INT_MAX_VALUE		= (int)WaveformMetrics.MAX_VALUE;
	private static final int	INT_MIN_VALUE		= (int)WaveformMetrics.MIN_VALUE;
	// static IndexColorModel cm = null;
	private static final double	LOG10				= Math.log(10);
	private static final double	MAX_VALUE			= 10000.;
	private static final double	MIN_VALUE			= -10000.;

	private static final void drawRectagle(final Graphics g, final IndexColorModel cm, final int x, final int y, final int w, final int h, final int cIdx) {
		g.setColor(new Color(cm.getRed(cIdx), cm.getGreen(cIdx), cm.getBlue(cIdx), cm.getAlpha(cIdx)));
		g.fillRect(x, y, w, h);
	}

	private static final double log10(final double x) {
		if(x < 10E-100) return -100;
		return Math.log(x) / WaveformMetrics.LOG10;
	}

	private static final double pow10(final double x) {
		return Math.exp(WaveformMetrics.LOG10 * x);
	}
	private double			FACT_X, FACT_Y, OFS_X, OFS_Y;
	public final int		horizontal_offset, vertical_offset;
	public final int		start_x;
	public final boolean	x_log, y_log;
	public final double		x_offset;
	public final double		x_range;
	public final double		xmax, xmin;
	public final double		ymax;
	public final double		ymin;
	public final double		xrange, yrange;
	public final double		y_range;

	public WaveformMetrics(double _xmax, double _xmin, double _ymax, double _ymin, final Rectangle limits, final Dimension d, final boolean _x_log, final boolean _y_log, final int horizontal_offset, final int vertical_offset){
		final int ylabel_width = limits.width, xlabel_height = limits.height;
		this.horizontal_offset = horizontal_offset;
		this.vertical_offset = vertical_offset;
		this.start_x = ylabel_width;
		this.x_log = _x_log;
		this.y_log = _y_log;
		final int border_y = xlabel_height;
		this.y_range = (d.height - border_y - 2 * vertical_offset) / (double)d.height;
		this.x_range = (d.width - this.start_x - 2 * horizontal_offset) / (double)d.width;
		if(_ymin > _ymax) _ymin = _ymax;
		if(_xmin > _xmax) _xmin = _xmax;
		if(this.x_log){
			_xmax = WaveformMetrics.log10(_xmax);
			_xmin = WaveformMetrics.log10(_xmin);
		}
		final double delta_x = _xmax - _xmin;
		this.xmax = _xmax + delta_x / 100.;
		this.xmin = _xmin - delta_x / 100.;
		if(this.xmax > this.xmin){
			this.xrange = this.xmax - this.xmin;
			this.x_offset = this.start_x / (double)d.width;
		}else{
			this.xrange = this.xmax == 0 ? 1 : .1 * this.xmax;
			this.x_offset = 0.5;
		}
		if(this.y_log){
			_ymax = WaveformMetrics.log10(_ymax);
			_ymin = WaveformMetrics.log10(_ymin);
		}
		final double delta_y = _ymax - _ymin;
		this.ymax = _ymax + delta_y / 50;
		this.ymin = _ymin - delta_y / 50.;
		if(this.ymax > this.ymin) this.yrange = this.ymax - this.ymin;
		else this.yrange = this.ymax == 0 ? 1 : .1 * this.ymax;
	}

	public final void toImage(final Signal s, final Image img, final Dimension d, final ColorMap colorMap) {
		int xSt, xEt, ySt, yEt;
		final Graphics2D g2 = (Graphics2D)img.getGraphics();
		final IndexColorModel cm = colorMap.getIndexColorModel(8);
		this.computeFactors(d);
		g2.setColor(Color.white);
		g2.fillRect(0, 0, d.width - 1, d.height - 1);
		final double[] x2D = s.getX2D();
		final float[] y2D = s.getY2D();
		final float[] z2D = s.getZ();
		float z2D_min, z2D_max;
		z2D_min = z2D_max = z2D[0];
		for(final float element : z2D){
			if(element < z2D_min) z2D_min = element;
			if(element > z2D_max) z2D_max = element;
		}
		for(xSt = 0; xSt < x2D.length; xSt++)
			if(x2D[xSt] >= this.xmin) break;
		for(xEt = 0; xEt < x2D.length; xEt++)
			if(x2D[xEt] >= this.xmax) break;
		for(ySt = 0; ySt < y2D.length; ySt++)
			if(y2D[ySt] >= this.ymin) break;
		for(yEt = 0; yEt < y2D.length; yEt++)
			if(y2D[yEt] >= this.ymax) break;
		if(yEt == 0) return;
		int p = 0;
		int h = 0;
		int w = 0;
		int yPix0;
		int yPix1;
		int xPix0;
		int xPix1;
		int pix;
		yPix1 = (this.toYPixel(y2D[ySt + 1]) + this.toYPixel(y2D[ySt])) / 2;
		yPix1 = 2 * this.toYPixel(y2D[ySt]) - yPix1;
		float currMax = z2D_min, currMin = z2D_max;
		for(int y = ySt; y < yEt; y++){
			p = y * x2D.length + xSt;
			for(int x = xSt; x < xEt && p < z2D.length; x++){
				if(z2D[p] > currMax) currMax = z2D[p];
				if(z2D[p] < currMin) currMin = z2D[p];
				p++;
			}
		}
		for(int y = ySt; y < yEt; y++){
			yPix0 = yPix1;
			try{
				yPix1 = (this.toYPixel(y2D[y + 1]) + this.toYPixel(y2D[y])) / 2;
				h = Math.abs(yPix0 - yPix1) + 2;
			}catch(final Exception e){
				yPix1 = 2 * this.toYPixel(y2D[yEt - 1]) - yPix1;
				h = Math.abs(yPix0 - yPix1) + 2;
			}
			p = y * x2D.length + xSt;
			xPix1 = (this.toXPixel(x2D[xSt]) + this.toXPixel(x2D[xSt + 1])) / 2;
			xPix1 = 2 * this.toXPixel(x2D[xSt]) - xPix1;
			for(int x = xSt; x < xEt && p < z2D.length; x++){
				xPix0 = xPix1;
				try{
					xPix1 = (this.toXPixel(x2D[x + 1]) + this.toXPixel(x2D[x])) / 2;
					w = Math.abs(xPix1 - xPix0);
				}catch(final Exception e){
					w = 2 * (this.toXPixel(x2D[xEt - 1]) - xPix1);
				}
				pix = (int)(255 * (z2D[p++] - currMin) / (currMax - currMin));
				pix = (pix > 255) ? 255 : pix;
				pix = (pix < 0) ? 0 : pix;
				WaveformMetrics.drawRectagle(g2, cm, xPix0, yPix1, w, h, pix);
			}
		}
	}

	public final Vector<Polygon> toPolygons(final Signal sig, final Dimension d) {
		return this.toPolygons(sig, d, false);
	}

	public final Vector<Polygon> toPolygons(final Signal sig, final Dimension d, final boolean appendMode) {
		try{
			return this.toPolygonsDoubleX(sig, d);
		}catch(final Exception exc){
			exc.printStackTrace();
		}
		return null;
	}

	public final Vector<Polygon> toPolygonsDoubleX(final Signal sig, final Dimension d) {
		this.computeFactors(d);
		final Vector<Polygon> curr_vect = new Vector<Polygon>(5);
		final double x[];
		final float y[];
		try{
			x = sig.getX();
			y = sig.getY();
		}catch(final Exception exc){
			return curr_vect;
		}
		if(x == null || y == null || x.length == 0 || y.length == 0) return curr_vect;
		final double dxmin = this.x_log ? WaveformMetrics.pow10(this.xmin) : this.xmin;
		final double dxmax = this.x_log ? WaveformMetrics.pow10(this.xmax) : this.xmax;
		int first, next, length = sig.getNumPoints();
		for(next = 1; next < length && (x[next] < dxmin || Double.isNaN(y[next-1])) ; next++) ;
		first = next - 1;
		float last_y, first_y, max_y, min_y;
		Polygon curr_polygon = null;
		int pol_idx = 0;
		int curr_X = this.toXPixel(x[first], d);
		final int[] xpoints = new int[length];
		final int[] ypoints = new int[length];
		int curr_num_points = 0;
		while(next < length){
			first_y = min_y = max_y = last_y = y[first];
			for(next = first + 1; next < length//
			        && (pol_idx >= sig.getNumNaNs() || next != sig.getNaNs()[pol_idx])//
			        && (this.toXPixel(x[next])) == curr_X; next++){
				last_y = y[next];
				if(last_y < min_y) min_y = last_y;
				if(last_y > max_y) max_y = last_y;
			}
			xpoints[curr_num_points] = curr_X;
			ypoints[curr_num_points] = this.toYPixel(first_y);
			curr_num_points++;
			if(max_y > min_y){
				if(last_y != max_y && first_y != max_y){
					xpoints[curr_num_points] = curr_X;
					ypoints[curr_num_points] = this.toYPixel(max_y);
					curr_num_points++;
				}
				if(last_y != min_y && first_y != min_y){
					xpoints[curr_num_points] = curr_X;
					ypoints[curr_num_points] = this.toYPixel(min_y);
					curr_num_points++;
				}
				xpoints[curr_num_points] = curr_X;
				ypoints[curr_num_points] = this.toYPixel(last_y);
				curr_num_points++;
			}
			if(next >= length || Double.isNaN(y[next])){
				curr_polygon = new Polygon(xpoints, ypoints, curr_num_points);
				curr_vect.addElement(curr_polygon);
				pol_idx++;
				curr_num_points = 0;
				for(; next < length && Double.isNaN(y[next]); next++){/*NOP*/}
			}
			if(next < length){
				curr_X = this.toXPixel(x[next]);
				first = next;
				if(sig.isIncreasingX() && x[next] > dxmax) length = next + 1;
			}
		}
		if(sig.getMode1D() == Mode1D.STEP) for(int vi = 0; vi < curr_vect.size(); vi++){
			curr_polygon = curr_vect.elementAt(vi);
			final int np = curr_polygon.npoints * 2 - 1;
			final int[] sx = new int[np], sy = new int[np];
			for(int j = 0, i = 0; i < curr_polygon.npoints; i++, j++){
				sx[j] = curr_polygon.xpoints[i];
				sy[j] = curr_polygon.ypoints[i];
				j++;
				if(j == np) break;
				sx[j] = curr_polygon.xpoints[i + 1];
				sy[j] = curr_polygon.ypoints[i];
			}
			curr_vect.setElementAt(new Polygon(sx, sy, np), vi);
		}
		return curr_vect;
	}

	public final int toXPixel(double x) {
		if(this.x_log) x = WaveformMetrics.log10(x);
		final double xpix = x * this.FACT_X + this.OFS_X;
		if(xpix >= WaveformMetrics.MAX_VALUE) return WaveformMetrics.INT_MAX_VALUE;
		if(xpix <= WaveformMetrics.MIN_VALUE) return WaveformMetrics.INT_MIN_VALUE;
		return (int)xpix;
	}

	public final int toXPixel(double x, final Dimension d) {
		if(this.x_log) x = WaveformMetrics.log10(x);
		final double ris = (this.x_offset + this.x_range * (x - this.xmin) / this.xrange) * d.width + 0.5;
		if(ris >= WaveformMetrics.MAX_VALUE) return WaveformMetrics.INT_MAX_VALUE;
		if(ris <= WaveformMetrics.MIN_VALUE) return WaveformMetrics.INT_MIN_VALUE;
		return (int)ris;
	}

	public final double toXValue(final int xpix) {
		final double x = (xpix - this.OFS_X) * this.FACT_X;
		if(this.x_log) return WaveformMetrics.pow10(x);
		return x;
	}

	public final double toXValue(final int xpix, final Dimension d) {
		final double x = (((xpix - 0.5) / d.width - this.x_offset) * this.xrange / this.x_range + this.xmin);
		if(this.x_log) return WaveformMetrics.pow10(x);
		return x;
	}

	public final int toYPixel(double y) {
		if(this.y_log) y = WaveformMetrics.log10(y);
		final double ypix = y * this.FACT_Y + this.OFS_Y;
		if(ypix >= WaveformMetrics.MAX_VALUE) return WaveformMetrics.INT_MAX_VALUE;
		if(ypix <= WaveformMetrics.MIN_VALUE) return WaveformMetrics.INT_MIN_VALUE;
		return (int)ypix;
	}

	public final int toYPixel(double y, final Dimension d) {
		if(this.y_log) y = WaveformMetrics.log10(y);
		final double ris = (this.y_range * (this.ymax - y) / this.yrange) * d.height + 0.5;
		if(ris >= WaveformMetrics.MAX_VALUE) return WaveformMetrics.INT_MAX_VALUE;
		if(ris <= WaveformMetrics.MIN_VALUE) return WaveformMetrics.INT_MIN_VALUE;
		return (int)ris;
	}

	public final double toYValue(final double ypix) {
		final double y = (ypix - this.OFS_Y) * this.FACT_Y;
		if(this.y_log) return WaveformMetrics.log10(y);
		return y;
	}

	public final double toYValue(final int ypix, final Dimension d) {
		final double y = (this.ymax - ((ypix - 0.5) / d.height) * this.yrange / this.y_range);
		if(this.y_log) return WaveformMetrics.pow10(y);
		return y;
	}

	final void computeFactors(final Dimension d) {
		this.OFS_X = this.x_offset * d.width - this.xmin * this.x_range * d.width / this.xrange + this.horizontal_offset + 0.5;
		this.FACT_X = this.x_range * d.width / this.xrange;
		this.OFS_Y = this.y_range * this.ymax * d.height / this.yrange + this.vertical_offset + 0.5;
		this.FACT_Y = -this.y_range * d.height / this.yrange;
	}
}
