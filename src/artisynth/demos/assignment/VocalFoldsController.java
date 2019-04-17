package artisynth.demos.assignment;

import java.util.ArrayList;

import artisynth.core.femmodels.FemNode3d;
import artisynth.core.modelbase.ControllerBase;
import maspack.matrix.Point3d;

public class VocalFoldsController extends ControllerBase {
   
   private static final double VOCAL_FOLD_WIDTH = VocalFoldBuilder.getWidth();
   private static final double MAX_DISPLACEMENT_X = 4 / 2;
   private static final double MAX_DISPLACEMENT_Y = 1.0;
   private static final int FREQUENCY = 200;  // Hz
   private static final double PERIOD = 1.0 / FREQUENCY;  // sec
   
   private ArrayList<FemNode3d> leftNodes;
   private ArrayList<FemNode3d> rightNodes;
   
   private ArrayList<Point3d> leftOrigPos;
   private ArrayList<Point3d> rightOrigPos;
   
   private double elapsed = 0;
   
   
   VocalFoldsController(
         ArrayList<FemNode3d> leftMedialNodes,
         ArrayList<Point3d> leftMedialOrigPos, 
         ArrayList<FemNode3d> rightMedialNodes,
         ArrayList<Point3d> rightMedialOrigPos) {
      leftNodes = sorted(leftMedialNodes);
      leftOrigPos = duplicatePoints(leftMedialOrigPos);
      rightNodes = sorted(rightMedialNodes);
      rightOrigPos = duplicatePoints(rightMedialOrigPos);
   }
   
   private ArrayList<Point3d> duplicatePoints(ArrayList<Point3d> pointList) {
      ArrayList<Point3d> dup = new ArrayList<Point3d>();
      for (Point3d p : pointList) {
         if ( dup.size() == 0 ) {
            dup.add (new Point3d(p.x, p.y, p.z));
            continue;
         }
         Point3d point = new Point3d(p.x, p.y, p.z);
         boolean added = false;
         for ( int i=0; i<dup.size (); i++ ) {
            if ( dup.get (i).z > point.z ) {
               dup.add (Math.max (0, i), point);
               added = true;
               break;
            }
         }
         if ( !added )
            dup.add (point);
      }
      return dup;
   }
   
   ArrayList<FemNode3d> sorted(ArrayList<FemNode3d> nodeList) {
      ArrayList<FemNode3d> dup = new ArrayList<FemNode3d>();
      for (FemNode3d node : nodeList) {
         if ( dup.size() == 0 ) {
            dup.add (node);
            continue;
         }
         boolean added = false;
         for ( int i=0; i<dup.size (); i++ ) {
            if ( dup.get (i).getPosition().z > node.getPosition().z ) {
               dup.add (i, node);
               added = true;
               break;
            }
         }
         if ( !added )
            dup.add (node);
      }
      return dup;
   }
   
   @Override
   public void initialize(double t0) {
      System.out.println("Initializing vocal folds movement controller.");
      for (int i=0; i<leftNodes.size(); i++) {
         FemNode3d node = leftNodes.get(i);
         Point3d pos = leftOrigPos.get (i);
         node.setPosition (pos);
      }
      
      for (int i=0; i<rightNodes.size(); i++) {
         FemNode3d node = rightNodes.get(i);
         Point3d pos = rightOrigPos.get (i);
         node.setPosition (pos);
      }
   }
   
   
   @Override
   public void apply(double t0, double t1) {
      elapsed += (t1 - t0) / 20;
      double phase = Math.abs (Math.sin(elapsed / PERIOD));
//      System.out.printf ("t=%.4f   elapsed=%.4f   period=%.4f   phase=%.4f\n", t1, elapsed, PERIOD, phase);
      for ( int i=0; i<leftOrigPos.size(); i++ ) {
         FemNode3d node = leftNodes.get (i);
         Point3d p = leftOrigPos.get(i);
         double dispX = phase * calcMaxDisplaceX(p.z);
         double dispY = phase * calcMaxDisplaceY(p.z);
//         System.out.printf("disp: %.2f, %.2f move to %.2f\n", disp, p.x, p.x - disp);
         node.setPosition (p.x - dispX, p.y + dispY, p.z);  // displace towards -x
      }
      
      for ( int i=0; i<rightOrigPos.size(); i++ ) {
         FemNode3d node = rightNodes.get (i);
         Point3d p = rightOrigPos.get(i);
         double dispX = phase * calcMaxDisplaceX(p.z);
         double dispY = phase * calcMaxDisplaceY(p.z);
         node.setPosition (p.x + dispX, p.y + dispY, p.z);  // displace towards -x
      }
   }
   
   
   private double calcMaxDisplaceX(double zOffset) {
      double x = zOffset / (VOCAL_FOLD_WIDTH);
//      x = 0.5;
      return Math.abs (MAX_DISPLACEMENT_X * Math.cos (x * 0.5 * Math.PI));
   };
   
   private double calcMaxDisplaceY(double zOffset) {
      double x = zOffset / (VOCAL_FOLD_WIDTH);
//      x = 0.5;
      return Math.abs (MAX_DISPLACEMENT_Y * Math.cos (x * 0.5 * Math.PI));
   };
}
