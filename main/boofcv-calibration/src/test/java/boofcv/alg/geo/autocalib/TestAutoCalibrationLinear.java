/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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

package boofcv.alg.geo.autocalib;

import boofcv.struct.calib.CameraPinhole;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAutoCalibrationLinear extends CommonAutoCalibrationChecks {

	@Test
	public void solve_ZeroCP() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0.1,0,0,0,0);

		checkSolve(intrinsic, false,false,5);
	}

	@Test
	public void solve_ZeroCP_ZSkew() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0,0,0,0,0);

		checkSolve(intrinsic, false,true,4);
	}

	@Test
	public void solve_ZeroCP_ZSkew_Aspect() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0,0,0,0,0);

		checkSolve(intrinsic, true,true,3);
	}

	private void checkSolve(CameraPinhole intrinsic, boolean knownAspect, boolean zeroSkew, int numProjectives ) {
		renderGood(intrinsic);

		double aspect = intrinsic.fy/intrinsic.fx;
		AutoCalibrationLinear alg;
		if( zeroSkew ) {
			if( knownAspect ) {
				alg = new AutoCalibrationLinear(aspect);
			} else {
				alg = new AutoCalibrationLinear(true);
			}
		} else {
			alg = new AutoCalibrationLinear(false);
		}

		assertEquals(numProjectives,alg.getMinimumProjectives());

		addProjectives(alg);

		assertEquals(AutoCalibrationLinear.Result.SUCCESS,alg.solve());

		assertEquals(intrinsic.cx, alg.getCx(), UtilEjml.TEST_F64);
		assertEquals(intrinsic.cy, alg.getCy(), UtilEjml.TEST_F64);
		assertEquals(intrinsic.fx, alg.getFx(), UtilEjml.TEST_F64);
		assertEquals(intrinsic.fy, alg.getFy(), UtilEjml.TEST_F64);
		assertEquals(intrinsic.skew, alg.getSkew(), UtilEjml.TEST_F64);
	}

	@Test
	public void geometry_no_rotation() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0.1,0,0,0,0);
		renderTranslationOnly(intrinsic);

		AutoCalibrationLinear alg = new AutoCalibrationLinear(false);
		addProjectives(alg);

		assertEquals(AutoCalibrationLinear.Result.POOR_GEOMETRY,alg.solve());
	}

	@Test
	public void geometry_no_translation() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0.1,0,0,0,0);
		renderRotationOnly(intrinsic);

		AutoCalibrationLinear alg = new AutoCalibrationLinear(false);
		addProjectives(alg);

		assertEquals(AutoCalibrationLinear.Result.POOR_GEOMETRY,alg.solve());
	}

	@Test
	public void constructMatrix() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0,0,0,1000,1050);

		renderGood(intrinsic);

		AutoCalibrationLinear alg = new AutoCalibrationLinear(false);
		addProjectives(alg);

		DMatrixRMaj L = new DMatrixRMaj(3,3);
		alg.constructMatrix(L);

		DMatrixRMaj q = new DMatrixRMaj(10,1);
		q.data[0] = Q.get(0,0);
		q.data[1] = Q.get(0,1);
		q.data[2] = Q.get(0,2);
		q.data[3] = Q.get(0,3);
		q.data[4] = Q.get(1,1);
		q.data[5] = Q.get(1,2);
		q.data[6] = Q.get(1,3);
		q.data[7] = Q.get(2,2);
		q.data[8] = Q.get(2,3);
		q.data[9] = Q.get(3,3);

//		double[] sv = SingularOps_DDRM.singularValues(L);
//		for (int i = 0; i < sv.length; i++) {
//			System.out.println("sv["+i+"] = "+sv[i]);
//		}

		DMatrixRMaj found = new DMatrixRMaj(L.numRows,1);

		// See if it's the null space
		CommonOps_DDRM.mult(L,q,found);
		assertEquals(0,NormOps_DDRM.normF(found), UtilEjml.TEST_F64);
	}
}