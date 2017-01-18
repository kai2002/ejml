/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.dense.row.decomposition.bidiagonal;

import org.ejml.data.DMatrixRow_F64;
import org.ejml.dense.row.CommonOps_R64;
import org.ejml.dense.row.decomposition.qr.QrHelperFunctions_R64;
import org.ejml.interfaces.decomposition.BidiagonalDecomposition_F64;

/**
 * <p>
 * Performs a {@link BidiagonalDecomposition_F64} using
 * householder reflectors.  This is efficient on wide or square matrices.
 * </p>
 *
 * @author Peter Abeles
 */
public class BidiagonalDecompositionRow_R64
        implements BidiagonalDecomposition_F64<DMatrixRow_F64>
{
    // A combined matrix that stores te upper Hessenberg matrix and the orthogonal matrix.
    private DMatrixRow_F64 UBV;

    // number of rows
    private int m;
    // number of columns
    private int n;
    // the smaller of m or n
    private int min;

    // the first element in the orthogonal vectors
    private double gammasU[];
    private double gammasV[];
    // temporary storage
    private double b[];
    private double u[];

    /**
     * Creates a decompose that defines the specified amount of memory.
     *
     * @param numElements number of elements in the matrix.
     */
    public BidiagonalDecompositionRow_R64(int numElements) {

        UBV = new DMatrixRow_F64(numElements);
        gammasU = new double[ numElements ];
        gammasV = new double[ numElements ];
        b = new double[ numElements ];
        u = new double[ numElements ];
    }

    public BidiagonalDecompositionRow_R64() {
        this(1);
    }

    /**
     * Computes the decomposition of the provided matrix.  If no errors are detected then true is returned,
     * false otherwise.
     *
     * @param A  The matrix that is being decomposed.  Not modified.
     * @return If it detects any errors or not.
     */
    @Override
    public boolean decompose( DMatrixRow_F64 A  )
    {
        init(A);
        return _decompose();
    }

    /**
     * Sets up internal data structures and creates a copy of the input matrix.
     *
     * @param A The input matrix.  Not modified.
     */
    protected void init(DMatrixRow_F64 A ) {
        UBV = A;

        m = UBV.numRows;
        n = UBV.numCols;

        min = Math.min(m,n);
        int max = Math.max(m,n);

        if( b.length < max+1 ) {
            b = new double[ max+1 ];
            u = new double[ max+1 ];
        }
        if( gammasU.length < m ) {
            gammasU = new double[ m ];
        }
        if( gammasV.length < n ) {
            gammasV = new double[ n ];
        }
    }

    /**
     * The raw UBV matrix that is stored internally.
     *
     * @return UBV matrix.
     */
    public DMatrixRow_F64 getUBV() {
        return UBV;
    }

    @Override
    public void getDiagonal(double[] diag, double[] off) {
        diag[0] = UBV.get(0);
        for( int i = 1; i < n; i++ ) {
            diag[i] = UBV.unsafe_get(i,i);
            off[i-1] = UBV.unsafe_get(i-1,i);
        }
    }

    /**
     * Returns the bidiagonal matrix.
     *
     * @param B If not null the results are stored here, if null a new matrix is created.
     * @return The bidiagonal matrix.
     */
    @Override
    public DMatrixRow_F64 getB(DMatrixRow_F64 B , boolean compact ) {
        B = handleB(B, compact,m,n,min);

        //System.arraycopy(UBV.data, 0, B.data, 0, UBV.getNumElements());

        B.set(0,0,UBV.get(0,0));
        for( int i = 1; i < min; i++ ) {
            B.set(i,i, UBV.get(i,i));
            B.set(i-1,i, UBV.get(i-1,i));
        }
        if( n > m )
            B.set(min-1,min,UBV.get(min-1,min));

        return B;
    }

    public static DMatrixRow_F64 handleB(DMatrixRow_F64 B, boolean compact,
                                        int m , int n , int min ) {
        int w = n > m ? min + 1 : min;

        if( compact ) {
            if( B == null ) {
                B = new DMatrixRow_F64(min,w);
            } else {
                B.reshape(min,w, false);
                B.zero();
            }
        } else {
            if( B == null ) {
                B = new DMatrixRow_F64(m,n);
            } else {
                B.reshape(m,n, false);
                B.zero();
            }
        }
        return B;
    }

    /**
     * Returns the orthogonal U matrix.
     *
     * @param U If not null then the results will be stored here.  Otherwise a new matrix will be created.
     * @return The extracted Q matrix.
     */
    @Override
    public DMatrixRow_F64 getU(DMatrixRow_F64 U , boolean transpose , boolean compact ) {
        U = handleU(U, transpose, compact,m,n,min);
        CommonOps_R64.setIdentity(U);

        for( int i = 0; i < m; i++ ) u[i] = 0;

        for( int j = min-1; j >= 0; j-- ) {
            u[j] = 1;
            for( int i = j+1; i < m; i++ ) {
                u[i] = UBV.get(i,j);
            }
            if( transpose )
                QrHelperFunctions_R64.rank1UpdateMultL(U, u, gammasU[j], j, j, m);
            else
                QrHelperFunctions_R64.rank1UpdateMultR(U, u, gammasU[j], j, j, m, this.b);
        }

        return U;
    }

    public static DMatrixRow_F64 handleU(DMatrixRow_F64 U,
                                        boolean transpose, boolean compact,
                                        int m, int n , int min ) {
        if( compact ){
            if( transpose ) {
                if( U == null )
                    U = new DMatrixRow_F64(min,m);
                else {
                    U.reshape(min,m, false);
                }
            } else {
                if( U == null )
                    U = new DMatrixRow_F64(m,min);
                else
                    U.reshape(m,min, false);
            }
        } else  {
            if( U == null )
                U = new DMatrixRow_F64(m,m);
            else
                U.reshape(m,m, false);
        }

        return U;
    }

    /**
     * Returns the orthogonal V matrix.
     *
     * @param V If not null then the results will be stored here.  Otherwise a new matrix will be created.
     * @return The extracted Q matrix.
     */
    @Override
    public DMatrixRow_F64 getV(DMatrixRow_F64 V , boolean transpose , boolean compact ) {
        V = handleV(V, transpose, compact,m,n,min);
        CommonOps_R64.setIdentity(V);

//        UBV.print();

        // todo the very first multiplication can be avoided by setting to the rank1update output
        for( int j = min-1; j >= 0; j-- ) {
            u[j+1] = 1;
            for( int i = j+2; i < n; i++ ) {
                u[i] = UBV.get(j,i);
            }
            if( transpose )
                QrHelperFunctions_R64.rank1UpdateMultL(V, u, gammasV[j], j + 1, j + 1, n);
            else
                QrHelperFunctions_R64.rank1UpdateMultR(V, u, gammasV[j], j + 1, j + 1, n, this.b);
        }

        return V;
    }

    public static DMatrixRow_F64 handleV(DMatrixRow_F64 V, boolean transpose, boolean compact,
                                        int m , int n , int min ) {
        int w = n > m ? min + 1 : min;

        if( compact ) {
            if( transpose ) {
                if( V == null ) {
                    V = new DMatrixRow_F64(w,n);
                } else
                    V.reshape(w,n, false);
            } else {
                if( V == null ) {
                    V = new DMatrixRow_F64(n,w);
                } else
                    V.reshape(n,w, false);
            }
        } else {
            if( V == null ) {
                V = new DMatrixRow_F64(n,n);
            } else
                V.reshape(n,n, false);
        }

        return V;
    }

    /**
     * Internal function for computing the decomposition.
     */
    private boolean _decompose() {
        for( int k = 0; k < min; k++ ) {
//            UBV.print();
            computeU(k);
//            System.out.println("--- after U");
//            UBV.print();
            computeV(k);
//            System.out.println("--- after V");
//            UBV.print();
        }

        return true;
    }

    protected void computeU( int k) {
        double b[] = UBV.data;

        // find the largest value in this column
        // this is used to normalize the column and mitigate overflow/underflow
        double max = 0;

        for( int i = k; i < m; i++ ) {
            // copy the householder vector to vector outside of the matrix to reduce caching issues
            // big improvement on larger matrices and a relatively small performance hit on small matrices.
            double val = u[i] = b[i*n+k];
            val = Math.abs(val);
            if( val > max )
                max = val;
        }

        if( max > 0 ) {
            // -------- set up the reflector Q_k
            double tau = QrHelperFunctions_R64.computeTauAndDivide(k, m, u, max);

            // write the reflector into the lower left column of the matrix
            // while dividing u by nu
            double nu = u[k] + tau;
            QrHelperFunctions_R64.divideElements_Bcol(k + 1, m, n, u, b, k, nu);
            u[k] = 1.0;

            double gamma = nu/tau;
            gammasU[k] = gamma;

            // ---------- multiply on the left by Q_k
            QrHelperFunctions_R64.rank1UpdateMultR(UBV, u, gamma, k + 1, k, m, this.b);

            b[k*n+k] = -tau*max;
        } else {
            gammasU[k] = 0;
        }
    }

    protected void computeV(int k) {
        double b[] = UBV.data;

        int row = k*n;

        // find the largest value in this column
        // this is used to normalize the column and mitigate overflow/underflow
        double max = QrHelperFunctions_R64.findMax(b, row + k + 1, n - k - 1);

        if( max > 0 ) {
            // -------- set up the reflector Q_k

            double tau = QrHelperFunctions_R64.computeTauAndDivide(k + 1, n, b, row, max);

            // write the reflector into the lower left column of the matrix
            double nu = b[row+k+1] + tau;
            QrHelperFunctions_R64.divideElements_Brow(k + 2, n, u, b, row, nu);

            u[k+1] = 1.0;

            double gamma = nu/tau;
            gammasV[k] = gamma;

            // writing to u could be avoided by working directly with b.
            // requires writing a custom rank1Update function
            // ---------- multiply on the left by Q_k
            QrHelperFunctions_R64.rank1UpdateMultL(UBV, u, gamma, k + 1, k + 1, n);

            b[row+k+1] = -tau*max;
        } else {
            gammasV[k] = 0;
        }
    }

    /**
     * Returns gammas from the householder operations for the U matrix.
     *
     * @return gammas for householder operations
     */
    public double[] getGammasU() {
        return gammasU;
    }

    /**
     * Returns gammas from the householder operations for the V matrix.
     *
     * @return gammas for householder operations
     */
    public double[] getGammasV() {
        return gammasV;
    }

    @Override
    public boolean inputModified() {
        return true;
    }
}