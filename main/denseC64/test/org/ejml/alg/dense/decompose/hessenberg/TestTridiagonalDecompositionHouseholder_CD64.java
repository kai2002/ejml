/*
 * Copyright (c) 2009-2016, Peter Abeles. All Rights Reserved.
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

package org.ejml.alg.dense.decompose.hessenberg;

import org.ejml.data.CDenseMatrix64F;
import org.ejml.interfaces.decomposition.TridiagonalSimilarDecomposition;

/**
 * @author Peter Abeles
 */
public class TestTridiagonalDecompositionHouseholder_CD64 extends StandardTridiagonalTests_CD64 {

    @Override
    protected TridiagonalSimilarDecomposition<CDenseMatrix64F> createDecomposition() {
        return new TridiagonalDecompositionHouseholder_CD64();
    }
}