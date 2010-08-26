package phelps.math;



/**
	Matrix manipulations: add, multiply, invert, transpose, determinant, Gauss-Jordan, simplex.

	<ul>
	<li>construction and getters/setters: {@link #Matrix(double[][])}, {@link #Matrix(int,int)}, {@link #Matrix(Matrix)}, {@link #get(int,int)}, {@link #set(int,int, double)}, {@link #equals(Object)}
	<li>one-matrix operations: {@link #transpose()}, {@link #invert()}, {@link #determinant()}
	<li>two-matrix operations: {@link #add(Matrix)}, {@link #multiply(double)}, {@link #multiply(Matrix)},
	<li>solve as system of equations: {@link #solveGaussJordan(int)}, {@link #maximize(double[])}
	</ul>

	@see "Introductory Linear Algebra with Applications, by John W. Brown and Donald R. Sherbert"

	@version $Revision: 1.6 $ $Date: 2003/02/06 06:56:10 $
*/
public class Matrix {
  static final boolean DEBUG = !true;

  private double[][] x_;


  /** Create a new matrix of the indicated number of m rows and columns. */
  public Matrix(int rows, int cols) {
	assert rows>=1 && cols>=1;
	x_ = new double[rows][cols];
  }

  public Matrix(double[][] x) {
	x_ = (x!=null? x: new double[0][0]);
  }

  /** Create a new Matrix by copying (not sharing) the content of the one passed in. */
  public Matrix(Matrix m) { this((double[][])m.x_.clone()); }


  /** @return number of rows in matrix */
  public int getRows() { return x_.length; }
  /** @return number of columns in matrix */
  public int getCols() { return x_[0].length; }


  public double get(int row, int col) {
	assert 0<=row && row<=getRows() && 0<=col && col<=getCols();
	return x_[row][col];
  }

  public void set(int row, int col, double val) {
	assert 0<=row && row<=getRows() && 0<=col && col<=getCols();
	x_[row][col] = val;
  }


  public boolean equals(Object o) {
	if (this==o) return true;
	if (o==null || !(o instanceof Matrix)) return false;

	Matrix x = (Matrix)o;
	int m=getRows(), n=getCols();
	if (m!=x.getRows() || n!=x.getCols()) return false;

	double[][] x1=x_, x2=x.x_;
	for (int i=0; i<m; i++) for (int j=0; j<n; j++) if (x1[i][j] != x2[i][j]) return false;
	return true;
  }

  public int hashCode() {
	return x_!=null? (x_.length << 8) + x_[0].length: 0;
  }


  /**
	Return a new matrix that is the transpose of this one.
  */
  public Matrix transpose() {
	int m=getRows(), n=getCols();
	double[][] t = new double[n][m], x=x_;
	for (int i=0; i<m; i++) for (int j=0; j<n; j++) t[j][i] = x[i][j];
	return new Matrix(t);
  }

  /**
	Returns new matrix that is the inverse.
	Subsequently multiplying this matrix by its inverse yields an identity matrix.
	@throws IllegalStateException if matrix is not invertable.
  */
  public Matrix invert() throws java.lang.IllegalStateException {
	int m=getRows(), n=getCols();
	double[][] x=x_;
	if (m==2 && n==2) { // formula for 2x2 impervious to roundoff error
		double a=x[0][0], b=x[0][1], c=x[1][0], d=x[1][1];
		if (a*d - b*c == 0.0) throw new IllegalStateException("matrix not invertable");
		double[][] inv = new double[2][2];
		return new Matrix(new double[][] { { d, -b }, { -c, a }});
	}

	double[][] gjx = new double[m][n*2];
	for (int i=0; i<m; i++) for (int j=0; j<n; j++) gjx[i][j] = x[i][j];
	for (int i=0, j=n; i<m; i++, j++) gjx[i][j] = 1.0;
	//dump(gjx);

	solveGaussJordan(gjx, m, n*2, m);
	//dump(gjx);

	double[][] inv = new double[m][n];
	for (int i=0; i<m; i++) for (int j=0; j<n; j++) inv[i][j] = gjx[i][j+n];
	//dump(inv);

	return new Matrix(inv);
  }

  /** Return the determinant of the matrix. */
  public double determinant() throws java.lang.IllegalStateException {
	int m=getRows(), n=getCols();
	if (m!=n) throw new IllegalStateException("only square matrices have determinants");

	double[][] x = x_;

	// is this the most efficient way?
	double sum=0.0;
	for (int j=0; j<n; j++) {
		double prod = 1.0;
		for (int i=0, jj=j; i<m; i++, jj++) { if (jj==n) jj=0; prod *= x[i][jj]; }
		sum += prod;
	}

	for (int j=0; j<n; j++) {
		double prod = 1.0;
		for (int i=0, jj=j; i<m; i++, jj--) { if (jj<0) jj=n-1; prod *= x[i][jj]; }
		sum -= prod;
	}

	return sum;
  }



  /**
	Return new matrix that is sum of this and passed matrix.
	Matrices must have same dimensions.
	@throws IllegalArgumentException if passed matrix is of different dimension.
  */
  public Matrix add(Matrix x) throws IllegalArgumentException {
	int m=getRows(), n=getCols();
	if (x==null || m!=x.getRows() || n!=x.getCols()) throw new IllegalArgumentException("matrices of different dimensions");

	double[][] sum = new double[m][n], x1=x_, x2=x.x_;
	for (int i=0; i<m; i++) for (int j=0; j<n; j++) sum[i][j] = x1[i][j] + x2[i][j];

	return new Matrix(sum);
  }

  /**
	Return new matrix that is result of multiplying each element by the scalar <var>s</var>.
  */
  public Matrix multiply(double s) {
	int m=getRows(), n=getCols();
	double[][] prod = new double[m][n], x=x_;
	for (int i=0; i<m; i++) for (int j=0; j<n; j++) prod[i][j] = x[i][j] * s;
	return new Matrix(prod);
  }

  /**
	Return new matrix that is product of this and passed matrix.
	@throws IllegalArgumentException if passed matrix is of different dimension.
  */
  public Matrix multiply(Matrix x) throws IllegalArgumentException {
	int m=getRows(), n=getCols(), p=x.getCols();
	//assert x!=null && n==p: n+" != "+p;     // m x n  matrix must have  n x p
	if (x==null || n!=x.getRows()) throw new IllegalArgumentException("matrices of incompatible dimensions");

	double[][] prod = new double[m][p], x1=x_, x2=x.x_;
	for (int i=0; i<m; i++) {
		for (int k=0; k<p; k++) {
			double sum = 0.0; for (int j=0; j<n; j++) sum += x1[i][j] * x2[j][k];
			prod[i][k] = sum;
		}
	}

	return new Matrix(prod);
  }


  /**
	Treat the m x n matrix as a system of m equations in m unknowns,
	with the first m of n columns as coefficients, and
	the remaining n-m columns (typically n-m == 1) as constants.
	The resulting <em>mutated</em> matrix reports the solution such that the first variable equals the value get(0, m+1), the second at get(1, m+1), and so on.
	If there were multiple equations with the same coeffients but different constants, their results are available at get(0, m+2), get(1, m+2), and so on.
	The first (and typically only) solution vector is also the method's return value.

	If underdetermined....
  */
  public double[] solveGaussJordan(int vars) { return solveGaussJordan(x_, getRows(), getCols(), vars); }

  /**
	Used by public version and by invert().
  */
  double[] solveGaussJordan(double[][] x, int m, int n, int vars) {
	assert m < n: m+" >= "+n;

	for (int v=0; v<vars; v++) {
		// find pivot: non-zero in column v
		int p = -1;
		for (int i=v; i<m; i++) if (x[i][v] != 0.0) { p=i; break; }
		if (p==-1) continue;    // underdetermined

		double[] pivot=x[p];

		// swap rows if 0 at that point in current row
		if (p!=v) { x[p]=x[v]; x[v]=pivot; }

		// make 1 by multiplying by reciprocal
		double r = 1.0 / pivot[v];
		pivot[v]=1.0; for (int j=v+1; j<n; j++) pivot[j] *= r;

		// make 0s above and below
		for (int i=0; i<m; i++) {
			double[] row = x[i]; if (row==pivot || row[v]==0.0) continue;
			double z = row[v];
			row[v]=0.0; for (int j=v+1; j<n; j++) row[j] -= pivot[j] * z;
		}
	}

	double[] sol = new double[m];
	for (int i=0; i<m; i++) sol[i] = x[i][m];

	return sol;
  }


  /**
	Treat matrix as a system of equations and maximize the passed program with the Simplex method.
	Returns vector that is the solution (which is the same as the first row of the mutated matrix).
	If maximum is unbounded, null is return.
	@return values for the variables in [0..vars-1] and maximum obtained at these settings in [vars].
  */
  public double[] maximize(double[] maximize) {
	int vars = maximize.length;     // number of slack variables
	int[] basic = new int[vars];

	// construct simplex matrix: add max fn and slack vars
	int m = 1 + getRows(), n = /*1 =>implicit +*/ getCols() + vars, lastcol = n-1;
	double[][] x=x_, s = new double[m][n];
	/*s[0][0]=1.0;*/ for (int i=0; i<vars; i++) s[0][i] = -maximize[i];
	for (int i=1, mj=getCols()-1; i<m; i++) {
		double con = x[i-1][mj];
		if (con>=0.0) for (int j=0; j<vars; j++) s[i][j] = x[i-1][j];
		else for (int j=0; j<vars; j++) s[i][j] = -x[i-1][j];
		s[i][lastcol] = Math.abs(con);
	}
	for (int i=1, j=vars; i<m; i++, j++) s[i][j] = 1.0;

	//dump(s); System.out.println();

	// solve
	double[] maxrow = s[0];
	while (true) {  // while negative entries
		// select most negative entry
		double min=0.0; int pj=-1;
		for (int j=0; j<vars; j++) if (maxrow[j] < min) { min = maxrow[j]; pj=j; }
		if (min==0.0) break;    // no negative => done

		// smallest quotient
		min=Double.MAX_VALUE; int pi=-1;
		for (int i=1; i<m; i++) {
			if (s[i][pj] < 0.0) continue;
			double q = s[i][lastcol] / s[i][pj];
			if (q < min) { min=q; pi=i; }
		}
		if (pi==-1) return null;    // could be more informative

		// pivot
		basic[pj] = pi;  // mark as basic var
		double[] pivot = s[pi];
		double r = 1.0 / pivot[pj];
		for (int j=0; j<n; j++) pivot[j] *= r;  pivot[pj]=1.0;

		for (int i=0; i<m; i++) {
			double[] row = s[i];  if (row==pivot || row[pj]==0.0) continue;
			double z = row[pj];
			for (int j=0; j<n; j++) row[j] -= pivot[j] * z;  row[pj]=0.0;
		}

		//dump(s); System.out.println();
	}

	//dump(s); System.out.println();
	// extract solution
//System.out.println("max = "+s[0][lastcol]);
	double[] sol = new double[vars + 1];
//for (int i=0; i<vars; i++) System.out.print(basic[i]+" ");  System.out.println();
	for (int i=0; i<vars; i++) if (basic[i] > 0) sol[i] = s[basic[i]][lastcol];
	sol[vars] = s[0][lastcol];

	return sol;
  }


  // LATER

  //public double[] minimize(double[] minimize) {}

  // more sophisticated versions of simplex
}
