/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.ContourPacked;
import boofcv.struct.ConfigLength;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericBinaryContourInterface extends BoofStandardJUnit {

	boolean supportsInternalContour = true;

	public static GrayU8 TEST1 = new GrayU8(new byte[][]
					{{0,0,0,0,0,0,0,1,0,0,0,1,1},
					 {0,0,0,0,0,0,0,1,0,0,0,1,1},
					 {0,0,0,0,0,0,0,1,0,0,1,1,0},
					 {0,0,0,0,0,0,0,0,1,1,1,1,0},
					 {0,0,1,0,0,0,0,0,1,1,1,0,0},
					 {0,0,1,0,0,0,1,1,1,1,1,0,0},
					 {1,1,1,1,1,1,1,1,1,1,0,0,0},
					 {0,0,0,1,1,1,1,1,0,0,0,0,0}});

	public static GrayU8 TEST2 = new GrayU8(new byte[][]
					{{0,0,1,0,0,0,0,1,0,0,0,0,0},
					 {0,1,0,1,0,0,1,0,0,1,0,0,0},
					 {0,0,1,0,0,1,0,1,0,1,1,1,0},
					 {0,0,0,0,1,0,0,0,1,1,1,1,0},
					 {0,0,1,0,1,0,0,0,1,0,0,0,0},
					 {0,0,0,0,1,0,1,1,1,0,1,1,0},
					 {1,1,1,0,0,1,0,0,1,0,0,1,0},
					 {0,0,0,1,1,1,1,1,0,0,0,0,0}});

	public static GrayU8 TEST3 = new GrayU8(new byte[][]
					{{0,0,0,0,0},
					 {0,0,1,0,0},
					 {0,1,1,1,0},
					 {0,1,0,1,0},
					 {0,1,1,1,0},
					 {0,0,0,0,0}});

	public static GrayU8 TEST4 = new GrayU8(new byte[][]
					{{0,0,0,0,0,0,0},
					 {0,0,1,1,1,1,1},
					 {0,1,0,1,1,1,1},
					 {0,1,1,1,0,1,1},
					 {0,1,1,1,1,1,1},
					 {0,1,1,1,1,1,1},
					 {0,1,1,1,1,1,1},
					 {0,0,0,0,0,0,0}});


	public static GrayU8 TEST5 = new GrayU8(new byte[][]
			{{1,1,1,1,1,1},
			 {1,0,0,0,0,1},
			 {1,0,1,1,0,1},
			 {1,0,1,1,0,1},
			 {1,0,1,1,0,1},
			 {1,1,1,1,1,1}});

	public static GrayU8 TEST6 = new GrayU8(new byte[][]
			{{1,1,1,1,1,1},
			 {1,0,0,1,0,1},
			 {1,0,1,1,0,1},
			 {1,0,1,1,0,1},
			 {1,0,1,1,0,1},
			 {1,1,1,1,1,1}});

	public static GrayU8 TEST7 = new GrayU8(new byte[][]
			{{1,1,1,1,1,1},
			 {1,0,0,0,0,1},
			 {1,0,1,1,0,1},
			 {1,0,1,1,0,1},
			 {1,0,0,0,1,1},
			 {1,1,1,1,1,1}});

	protected abstract BinaryContourInterface create();

	@Test void checkDefaults() {
		BinaryContourInterface alg = create();

		assertEquals(ConnectRule.FOUR,alg.getConnectRule());
		assertEquals(supportsInternalContour,alg.isSaveInternalContours());
		assertTrue(alg.getMinContour(null).isIdentical(ConfigLength.fixed(0)));
		assertTrue(alg.getMaxContour(null).isIdentical(ConfigLength.fixed(-1)));
	}

	void checkExternalSize(BinaryContourInterface alg , int which , int expected )
	{
		DogArray<Point2D_I32> points = new DogArray<>(Point2D_I32::new);
		alg.loadContour(alg.getContours().get(which).externalIndex,points);
		assertEquals(expected,points.size);
	}

	void checkInternalSize(BinaryContourInterface alg , int blob, int which , int expected )
	{
		DogArray<Point2D_I32> points = new DogArray<>(Point2D_I32::new);
		alg.loadContour(alg.getContours().get(blob).internalIndexes.get(which),points);
		assertEquals(expected,points.size);
	}

	static void printResults( BinaryContourInterface alg ) {
		List<ContourPacked> contours = alg.getContours();
		DogArray<Point2D_I32> points = new DogArray<>(Point2D_I32::new);

		int sizes[] = new int[contours.size()];

		for (int j = 0; j < contours.size(); j++) {
			alg.loadContour(contours.get(j).externalIndex,points);
			sizes[j] = points.size;
		}
		Arrays.sort(sizes);
		for (int i = 0; i < sizes.length; i++) {
			System.out.print(sizes[i]+",");
		}
	}

	public static void checkExpectedExternal(int expected[] , BinaryContourInterface alg ) {
		List<ContourPacked> contours = alg.getContours();
		assertEquals(expected.length, contours.size());

		DogArray<Point2D_I32> points = new DogArray<>(Point2D_I32::new);

		boolean matched[] = new boolean[ expected.length ];

		for (int i = 0; i < expected.length; i++) {
			int e = expected[i];
			boolean foo = false;
			for (int j = 0; j < contours.size(); j++) {
				if( matched[j])
					continue;
				alg.loadContour(contours.get(j).externalIndex,points);
				if( points.size == e ) {
					matched[j] = true;
					foo = true;
					break;
				}
			}
			assertTrue(foo);
		}
	}

}
