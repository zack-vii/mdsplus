package jscope.waveform;

/* $Id$ */
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Vector;
import javax.swing.JComponent;

/**
 * RowColumnContainer object is a component that can contain other AWT
 * components in a grid disposition. This component create on the
 * container a RowColumnLayout manager, so no layout manager
 * can be added to this component.
 *
 * @see RowColumnLayout
 */
public class RowColumnContainer extends JComponent{
	private static final long serialVersionUID = 1L;
	class Btm extends Component{
		private static final long serialVersionUID = 1L;

		Btm(){
			this.setBackground(Color.lightGray);
		}

		@Override
		public void paint(final Graphics g) {
			final Rectangle d = this.getBounds();
			if(d.width > d.height) this.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
			else this.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
			g.draw3DRect(0, 0, d.width - 1, d.height - 1, true);
		}

		@Override
		public void print(final Graphics g) {/**/}

		@Override
		public void printAll(final Graphics g) {/**/}
	}
	private Component			maximizeC		= null;
	/**
	 * Normalize height of the components in column.
	 *
	 * @see RowColumnLayout
	 */
	protected float				ph[];
	/**
	 * Normalized width of the columns.
	 *
	 * @see RowColumnLayout
	 */
	protected final float[]		pw				= new float[RowColumnLayout.MAX_COLUMN];
	private final Vector<Point>	real_position	= new Vector<Point>();
	/**
	 * RowColumnLayout
	 *
	 * @see RowColumnLayout
	 */
	protected RowColumnLayout	row_col_layout;
	/**
	 * number of component in column
	 */
	protected int[]				rows;
	private Point				split_pos		= null;

	/**
	 * Constructs a new RowColumnContainer with one row an column.
	 */
	public RowColumnContainer(){
		this.setName("RowColumnContainer");
		this.rows = new int[RowColumnLayout.MAX_COLUMN];
		this.rows[0] = 1;
		this.row_col_layout = new RowColumnLayout(this.rows);
		this.setLayout(this.row_col_layout);
	}

	/**
	 * Constructs a new RowColumnContainer with a defined number
	 * of column and component in column, and array of component
	 * to add.
	 *
	 * @param rows
	 *            an array of number of component in column
	 * @param columns
	 *            number of columns
	 * @param c
	 *            an array of component to add
	 */
	public RowColumnContainer(final int rows[], final Component c[]){
		int num_component;
		this.setName("RowColumnContainer");
		if(rows == null || rows.length == 0) throw new IllegalArgumentException("Defined null or empty row column container");
		this.rows = new int[rows.length];
		for(int i = 0; i < rows.length; i++)
			this.rows[i] = rows[i];
		this.row_col_layout = new RowColumnLayout(rows);
		this.setLayout(this.row_col_layout);
		num_component = this.getComponentNumber();
		Btm b;
		for(int i = 0; i < num_component - 1; i++){
			this.add(b = new Btm());
			this.setListener(b);
		}
		if(c != null){
			if(num_component != c.length) throw new IllegalArgumentException("Invalid componet number");
			this.add(c);
			this.validate();
		}
	}

	/**
	 * Add components to the container. The array component length must
	 * be equals to the component needed on the row column grid.
	 *
	 * @param c
	 *            an array of component
	 */
	public void add(final Component c[]) {
		int i, j, k;
		if(c.length != this.getComponentNumber()) throw new IllegalArgumentException("Invalid component number");
		for(i = 0, k = 0; i < this.rows.length; i++)
			for(j = 0; j < this.rows[i]; j++){
				super.add(c[k]);
				k++;
			}
	}

	/**
	 * Adds a specific component in row column position, if row, col position
	 * position is not present in row column grid, the grid is modified
	 * to add new componet.
	 *
	 * @param c
	 *            the component to be added
	 * @param row
	 *            component position in column
	 * @param col
	 *            column position of the component
	 */
	public void add(final Component c, final int row, final int col) {
		int new_rows[], cmp_idx = 0, i;
		Btm b;
		int rrow = row;
		final int rcol = col;
		if(this.getGridComponent(row, col) != null) throw new IllegalArgumentException("Component already added in this position");
		if(col > this.rows.length){
			if(row != 1) rrow = 1;
			new_rows = new int[col];
			for(i = 0; i < this.rows.length; i++)
				new_rows[i] = this.rows[i];
			new_rows[col - 1] = 1;
			this.rows = new_rows;
			cmp_idx = -1;
		}else{
			rrow = this.rows[col - 1] + 1;
			cmp_idx = this.getComponentIndex(rrow - 1, col) + 1;
			for(i = 0; i < this.real_position.size(); i += 2){
				final Point in_pos = this.real_position.elementAt(i);
				if(row < in_pos.y && col == in_pos.x){
					cmp_idx--;
					rrow--;
				}
			}
			for(int j = 0; j < this.real_position.size(); j += 2){
				final Point in_p = this.real_position.elementAt(j);
				if(row < in_p.y && col == in_p.x){
					final Point real_p = this.real_position.elementAt(j + 1);
					real_p.y++;
					if(in_p.x == real_p.x && in_p.y == real_p.y){
						this.real_position.removeElementAt(j);
						this.real_position.removeElementAt(j);
						j -= 2;
						continue;
					}
					if(rrow <= real_p.y && rcol == real_p.x) this.real_position.setElementAt(new Point(real_p.x, real_p.y), j + 1);
				}
			}
			this.rows[col - 1]++;
		}
		if(cmp_idx >= 0) super.add(c, cmp_idx);
		else super.add(c);
		super.add(b = new Btm(), 0);
		this.setListener(b);
		if(rrow != row || rcol != col) this.setRealPosition(new Point(col, row), new Point(rcol, rrow));
	}

	public int getColumns() {
		return this.row_col_layout.getColumns();
	}

	/**
	 * Gets the index of the component in this container.
	 *
	 * @param c
	 *            component
	 * @return index of the component in the container or -1 if not presentt
	 */
	public int getComponentIndex(final Component c) {
		int idx;
		for(idx = 0; idx < this.getGridComponentCount(); idx++)
			if(this.getGridComponent(idx) == c) break;
		if(idx < this.getGridComponentCount()) return idx + 1;
		return -1;
	}

	/**
	 * Gets the (row, col) component index in this container.
	 *
	 * @param row
	 *            component index in the column
	 * @param col
	 *            column index of the component
	 * @return the index component in this container,
	 *         -1 if the (row, col) value does not exist.
	 *         .
	 */
	public int getComponentIndex(final int row, final int col) {
		int cmp_idx = 0;
		if(col > this.rows.length || row > this.rows[col - 1]) return -1;
		for(int i = 0; i < col - 1; i++)
			cmp_idx += this.rows[i];
		return(cmp_idx + row + this.getGridComponentCount() - 2);
	}

	/**
	 * Returns the number of componet to add to this container.
	 *
	 * @return number of componet to add to this container
	 */
	public int getComponentNumber() {
		int num = 0;
		for(int i = 0; i < this.rows.length && this.rows[i] != 0; i++)
			num += this.rows[i];
		return num;
	}

	/**
	 * Get (row , col) position of the component in the container
	 *
	 * @param c
	 *            componet
	 * @return (row, col) position of the component
	 *         or null if component not presente in the container
	 */
	public Point getComponentPosition(final Component c) {
		int col = 0, row = 0;
		if(c == null) return null;
		int idx = this.getComponentIndex(c);
		for(col = 0; col < this.rows.length; col++){
			for(row = 0; row < this.rows[col] && idx != 0; row++)
				idx--;
			if(idx == 0) break;
		}
		final Point p = new Point(col + 1, row);
		return p;
	}

	/**
	 * Get defined number of component in column col.
	 *
	 * @param col
	 *            number of column
	 * @return number of component in column ith.
	 */
	public int getComponentsInColumn(final int col) {
		return this.rows[col];
	}

	/**
	 * Get defined number of component in columns
	 *
	 * @return an integer array ith element is number of component in columns ith
	 */
	public int[] getComponentsInColumns() {
		return this.rows;
	}

	public int[] getComponetNumInColumns() {
		return this.rows;
	}

	/**
	 * Get number of columns.
	 *
	 * @return number of columns
	 */
	public int getFilledColumns() {
		int col = 0;
		for(final int row : this.rows)
			if(row != 0) col++;
		return col;
	}

	/**
	 * Gets the nth user added component in this container.
	 *
	 * @param n
	 *            the index of the component to get.
	 * @return the n<sup>th</sup> component in this container.
	 * @exception ArrayIndexOutOfBoundsException
	 *                if the n<sup>th</sup> value does not exist.
	 *                Need not be called from AWT thread.
	 */
	/*
	 Component cmp_xxx;
	 int cmp_xxx_idx;
	 public Component getGridComponent(int n)
	{
	cmp_xxx_idx = n;
	try {
	    SwingUtilities.invokeAndWait(new Runnable() {
	            public void run() {
	           System.out.println( " get grid component e in dispatcher thread "+SwingUtilities.isEventDispatchThread());
	                cmp_xxx = getComponent(getGridComponentCount() - 1 + cmp_xxx_idx);
	            }
	        });
	} catch(InterruptedException e){}
	  catch(InvocationTargetException  e){}
	      return cmp_xxx;
	 }
	 */
	public Component getGridComponent(final int n) {
		// System.out.println( " get grid component e in dispatcher thread "+SwingUtilities.isEventDispatchThread());
		return this.getComponent(this.getGridComponentCount() - 1 + n);
	}

	/**
	 * Gets the (row, col) component in this container.
	 *
	 * @param row
	 *            component index in the column
	 * @param col
	 *            column index of the component
	 * @return the (row, col) component in this container,
	 *         or null value if does not exist.
	 */
	public Component getGridComponent(int row, int col) {
		int idx;
		Point curr_pos;
		final Point p = this.getRealPosition(curr_pos = new Point(col, row));
		if(p != null){
			col = p.x;
			row = p.y;
		}else if(this.positionOverwrite(curr_pos)) return null;
		idx = this.getComponentIndex(row, col);
		if(idx < 0) return null;
		return this.getComponent(idx);
	}

	/**
	 * Gets the number of components, added by user, in this panel.
	 *
	 * @return the number of components in this panel.
	 */
	public int getGridComponentCount() {
		// NOTE: return only the number of user added componet and not
		// internal resize button component.
		return (super.getComponentCount() + 1) / 2;
	}

	public Component getMaximizeComponent() {
		return this.maximizeC;
	}

	public float[] getNormalizedHeight() {
		if(this.isMaximize()) return this.ph;
		return this.row_col_layout.getPercentHeight();
	}

	public float[] getNormalizedWidth() {
		if(this.isMaximize()) return this.ph;
		return this.row_col_layout.getPercentWidth();
	}

	public Point getSplitPosition() {
		return this.split_pos;
	}

	public boolean isMaximize() {
		return(this.maximizeC != null);
	}

	public void maximizeComponent(final Component c) {
		this.maximizeC = c;
		if(c == null){
			this.update();
			return;
		}
		final int n_com = this.getGridComponentCount();
		if(n_com == 1) return;
		int i, j, k;
		final float m_ph[] = new float[n_com];
		final float m_pw[] = new float[this.rows.length];
		this.ph = new float[n_com];
		final Point p = this.getComponentPosition(c);
		for(i = 0, k = 0; i < this.rows.length; i++){
			if(this.rows[i] == 0){
				this.pw[i] = 0.f;
				continue;
			}
			this.pw[i] = this.row_col_layout.getPercentWidth(i);
			if(i == p.x - 1) m_pw[i] = 1;
			else m_pw[i] = 0;
			for(j = 0; j < this.rows[i]; j++){
				if(i == p.x - 1 && j == p.y - 1) m_ph[k] = 1;
				else m_ph[k] = 0;
				this.ph[k] = this.row_col_layout.getPercentHeight(k);
				k++;
			}
		}
		this.row_col_layout.setRowColumn(this.rows, m_ph, m_pw);
		this.invalidate();
		this.validate();
	}

	/**
	 * Removes the specified component from this container.
	 *
	 * @param c
	 *            the component to be removed
	 */
	public void removeComponent(final Component c) {
		if(c == null) return;
		final Point p = this.getComponentPosition(c);
		if(p == null) return;
		this.removeComponent(p.y, p.x);
	}

	/**
	 * Removes component in (row, col) position from this container.
	 *
	 * @param row
	 *            component index in the column
	 * @param col
	 *            column index of the component
	 */
	public void removeComponent(final int row, final int col) {
		final int idx = this.getComponentIndex(row, col);
		final int b_idx = idx - this.getGridComponentCount();
		// remove component
		this.remove(idx);
		// remove resize button
		if(b_idx >= 0) this.remove(b_idx);
		else this.remove(b_idx + 1);
		int size = this.real_position.size();
		for(int j = 0; j < size; j += 2){
			final Point real_p = this.real_position.elementAt(j + 1);
			if(row == real_p.y && col == real_p.x){
				this.real_position.removeElementAt(j);
				this.real_position.removeElementAt(j);
				break;
			}
		}
		size = this.real_position.size();
		boolean found;
		for(int i = row + 1; i <= this.rows[col - 1]; i++){
			found = false;
			for(int j = 0; j < size; j += 2){
				final Point real_p = this.real_position.elementAt(j + 1);
				if(i == real_p.y && col == real_p.x){
					found = true;
					real_p.y--;
					this.real_position.setElementAt(new Point(real_p.x, real_p.y), j + 1);
					break;
				}
			}
			if(!found) this.setRealPosition(new Point(col, i + 1), new Point(col, i));
		}
		this.rows[col - 1]--;
		this.ph = null;
		this.update();
	}

	/**
	 * Repaint all added component.
	 */
	public void repaintAll() {
		for(int i = 0; i < this.getGridComponentCount(); i++)
			this.getGridComponent(i).repaint();
	}

	public void resetMaximizeComponent() {
		this.maximizeC = null;
	}

	public void resetSplitPosition() {
		this.split_pos = null;
	}

	/**
	 * Set new grid configuration.
	 *
	 * @param rows
	 *            an array of number of component in column
	 */
	public final void setRowColumn(final int rows[]) {
		this.rows = rows;
		this.row_col_layout.setRowColumn(rows);
	}

	/**
	 * Add component to the container. (row, col) position is automatic
	 * evaluated.
	 *
	 * @param c
	 *            component to add
	 * @return index of the added component
	 */
	public int splitContainer(final Component c) {
		int i, j, idx = 1, col = 0, row = 0;
		boolean not_add = true;
		for(j = this.rows.length; j <= this.rows.length * 4 && not_add; j++)
			for(i = 0, idx = 0; i < this.rows.length; i++){
				if(this.rows[i] < j && not_add){
					row = this.rows[i] + 1;
					col = i + 1;
					not_add = false;
				}
				idx += this.rows[i];
			}
		this.add(c, row, col);
		this.split_pos = new Point(col, row);
		this.update();
		return idx - 1;
	}

	/**
	 * Update RowColumnLayout.
	 *
	 * @see RowColumnLayout
	 */
	public void update() {
		this.row_col_layout.setRowColumn(this.rows, this.ph, this.pw);
		this.invalidate();
		this.validate();
	}

	/**
	 * Update RowColumnLayout with defined row component height and
	 * column width.
	 *
	 * @param ph
	 *            Vector of normalize height of component. The sum of ph[x] of the objects in a
	 *            column must be 1.
	 * @param pw
	 *            Vector of normalize width of the culomn. The sum of pw[x] must be 1
	 * @see RowColumnLayout
	 */
	public void update(final float ph_in[], final float pw_in[]) {
		this.ph = ph_in;
		System.arraycopy(pw_in, 0, this.pw, 0, this.pw.length);
		this.update();
	}

	/**
	 * Update container with new vector of values of components in
	 * columns. Column number is equal to the number of the
	 * non zero element in array rows.
	 *
	 * @param rows_in
	 *            an array of number of component in column
	 * @param c
	 *            an array of new componet to add
	 */
	public void update(final int rows_in[], final Component c[]) {
		if(rows_in == null || rows_in.length == 0) throw new IllegalArgumentException("Defined null or empty row column container");
		final int curr_rows[] = this.rows;// row_col_layout.GetRows();
		int col;
		int idx_w = this.getGridComponentCount() - 1;
		Btm b;
		int idx = 0;
		if(curr_rows.length > rows_in.length) col = curr_rows.length;
		else col = rows_in.length;
		for(int i = 0; i < col; i++)
			if(i > rows_in.length) for(int k = 0; k < curr_rows[i]; k++){
				this.remove(idx_w);
				this.remove(0);
				idx_w--;
			}
			else if(i > curr_rows.length - 1) for(int k = 0; k < rows_in[i]; k++){
				this.add(b = new Btm(), 0);
				this.setListener(b);
				this.add(c[idx++]);
			}
			else if(curr_rows[i] > rows_in[i]){
				idx_w += rows_in[i];
				for(int k = rows_in[i]; k < curr_rows[i]; k++){
					if(idx_w > 0) this.remove(idx_w);
					this.remove(0);
					idx_w--;
				}
			}else{
				idx_w += curr_rows[i];
				for(int k = curr_rows[i]; k < rows_in[i]; k++){
					this.add(c[idx++], idx_w);
					this.add(b = new Btm(), 0);
					this.setListener(b);
					idx_w++;
				}
			}
		if(!rows_in.equals(this.rows)){
			this.rows = new int[rows_in.length];
			for(int i = 0; i < rows_in.length; i++)
				this.rows[i] = rows_in[i];
		}
		this.row_col_layout.setRowColumn(rows_in);
		this.invalidate();
		this.validate();
	}

	private Point getRealPosition(final Point in_pos) {
		for(int i = 0; i < this.real_position.size(); i += 2){
			final Point p = this.real_position.elementAt(i);
			if(in_pos.x == p.x && in_pos.y == p.y) return this.real_position.elementAt(i + 1);
		}
		return null;
	}

	/*
	
	 private void upadateRealPosition(Point p)
	 {
	    for(int j = 1; j < real_position.size(); j+=2)
	    {
	         Point real_pos = (Point)real_position.elementAt(j);
	         if(rrow <= real_pos.y && rcol == real_pos.x)
	         {
	             real_position.setElementAt(new Point(real_pos.x, real_pos.y++), j);
	         }
	    }
	 }
	 */
	private boolean positionOverwrite(final Point pos) {
		for(int i = 1; i < this.real_position.size(); i += 2){
			final Point real_pos = this.real_position.elementAt(i);
			if(pos.x == real_pos.x && pos.y == real_pos.y) // real_pos.y++;
			    // Point in_pos = (Point)real_position.elementAt(i - 1);
			    // if(in_pos.x == real_pos.x && in_pos.y == real_pos.y)
			    // {
			    // real_position.removeElementAt(i);
			    // real_position.removeElementAt(i-1);
			    // }
			    return true;
		}
		return false;
	}

	/**
	 * Enable event capability on resize button
	 *
	 * @param b
	 *            a resize button
	 */
	private void setListener(final Component b) {
		b.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e) {
				final Component ob = e.getComponent();
				final int m_button = e.getModifiers();
				if(ob instanceof Btm) if((m_button & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) RowColumnContainer.this.row_col_layout.resizeRowColumn(ob);
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				final Component ob = e.getComponent();
				final int m_button = e.getModifiers();
				if(ob instanceof Btm) if(!((m_button & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK)) RowColumnContainer.this.row_col_layout.resizeRowColumn(ob, e.getPoint().x, e.getPoint().y);
			}
		});
		b.addMouseMotionListener(new MouseMotionAdapter(){
			@Override
			public void mouseDragged(final MouseEvent e) {
				final Component ob = e.getComponent();
				final int m_button = e.getModifiers();
				if(!((m_button & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK)) RowColumnContainer.this.row_col_layout.drawResize(ob, e.getPoint().x, e.getPoint().y);
			}
		});
	}

	private void setRealPosition(final Point in_pos, final Point real_pos) {
		if(in_pos == null || real_pos == null) return;
		if(in_pos.x != real_pos.x || in_pos.y != real_pos.y){
			this.real_position.addElement(in_pos); // added position i
			this.real_position.addElement(real_pos); // real position i+1
		}
	}
}
