/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.sfm;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.scene.PointTrackerToSimilarImages;
import boofcv.alg.sfm.structure2.GeneratePairwiseImageGraph;
import boofcv.alg.sfm.structure2.MetricFromUncalibratedPairwiseGraph;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class ExampleMultiviewSceneReconstruction {
	public static void main(String[] args) {
		MediaManager media = DefaultMediaManager.INSTANCE;

		int radius = 10;
		var configTracker = new ConfigPointTracker();
		configTracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		configTracker.klt.pruneClose = true;
		configTracker.klt.toleranceFB = 4;
		configTracker.klt.templateRadius = radius;
		configTracker.klt.maximumTracks = 400;
		configTracker.klt.config.maxIterations = 30;
		configTracker.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		configTracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		configTracker.detDesc.detectPoint.shiTomasi.radius = radius;
		configTracker.detDesc.detectPoint.general.radius = 15;
//		configTracker.detDesc.detectPoint.general.threshold = 0;
		configTracker.detDesc.detectPoint.general.selector = ConfigSelectLimit.selectUniform(2.0);

		PointTracker<GrayU8> tracker = FactoryPointTracker.tracker(configTracker, GrayU8.class,null);

		var similarImages = new PointTrackerToSimilarImages();

		SimpleImageSequence<GrayU8> sequence = media.openVideo(UtilIO.pathExample("moo.mp4"), ImageType.SB_U8);
		similarImages.initialize(sequence.getWidth(), sequence.getHeight());

		while( sequence.hasNext() ) {
			GrayU8 frame = sequence.next();
			tracker.process(frame);
			int active = tracker.getTotalActive();
			int dropped = tracker.getDroppedTracks(null).size();
			tracker.spawnTracks();
			similarImages.processFrame(tracker);
			String id = similarImages.frames.getTail().frameID;
			System.out.println("frame id = "+id+" active="+active+" dropped="+dropped);

//			if( sequence.getFrameNumber() >= 20 )
//				break;
		}

		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Creating Pairwise");
		var generatePairwise = new GeneratePairwiseImageGraph();
		generatePairwise.setVerbose(System.out,null);
		generatePairwise.process(similarImages);
		var pairwise = generatePairwise.getGraph();
		System.out.println("  nodes.size="+pairwise.nodes.size);
		System.out.println("  edges.size="+pairwise.edges.size);
		int nodesWithNo3D = 0;
		for (int i = 0; i < pairwise.nodes.size; i++) {
			PairwiseImageGraph2.View n = pairwise.nodes.get(i);
			boolean found = false;
			for (int j = 0; j < n.connections.size; j++) {
				if( n.connections.get(j).is3D ) {
					found = true;
					break;
				}
			}
			if( !found ) {
				System.out.println("   no 3D in "+n.id);
				nodesWithNo3D++;
			}
		}
		System.out.println("  nodes with no 3D "+nodesWithNo3D);

		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Metric Reconstruction");
		var metric = new MetricFromUncalibratedPairwiseGraph();
		metric.setVerbose(System.out,null);
		metric.getInitProjective().setVerbose(System.out,null);
//		projective.getExpandProjective().setVerbose(System.out,null);
		if( !metric.process(similarImages,pairwise) ) {
			System.err.println("Reconstruction failed");
			System.exit(0);
		}

		System.out.println("----------------------------------------------------------------------------");
		System.out.println("Printing view info");
		for( var v : metric.getWorkGraph().viewList ) {
			System.out.printf("view='%s' fx=%.1f t={%.1f, %.1f, %.1f}\n",v.pview.id,
					v.pinhole.fx,
					v.world_to_view.T.x,v.world_to_view.T.y,v.world_to_view.T.z);
		}
		// TODO visualize
		System.out.println("done");
	}
}
